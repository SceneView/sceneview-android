package io.github.sceneview.demo.demos.internal.semantic

import io.github.sceneview.demo.demos.RerunCaptureBuilder
import io.github.sceneview.demo.demos.ScanPhoto
import io.github.sceneview.demo.demos.internal.ArDebugEvent
import io.github.sceneview.demo.demos.internal.OpenedCapture
import io.github.sceneview.demo.demos.internal.ReplayLens
import io.github.sceneview.demo.demos.internal.RerunRrdReader
import io.github.sceneview.demo.demos.internal.RerunScanFile
import io.github.sceneview.demo.demos.internal.RerunSyntheticRoom
import io.github.sceneview.demo.demos.internal.ScanDevice
import io.github.sceneview.demo.demos.internal.ScanObjects
import io.github.sceneview.demo.demos.internal.SvpcCodec
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.channels.FileChannel

/**
 * Recognition run on real recordings, off the phone: every `.svscan` and `.rrd` under
 * `SCENEVIEW_SCAN_CORPUS` is opened with the demo's own readers, recognised with the bundled
 * model, and written under `SCENEVIEW_SCAN_OUT/<scan>/`: `recognized.json` (what was found: label,
 * box, confidence, views; and per keyframe its detections and timings), one annotated
 * `keyframe-NN.png` per keyframe, and `timings.txt`. Skipped when either variable is unset.
 *
 * ```
 * SCENEVIEW_SCAN_CORPUS=~/scans SCENEVIEW_SCAN_OUT=/tmp/recognized \
 *   ./gradlew :samples:android-demo:testDebugUnitTest --tests '*SceneRecognitionCorpusTest*'
 * ```
 *
 * The model is `src/main/assets/ml/yolox_tiny.onnx` (fetched by `tools/fetch-assets.sh
 * --scope android`) or the file `SCENEVIEW_SCENE_MODEL` names. `SCENEVIEW_SCENE_PHOTOS` runs the
 * models on a folder of plain photos instead, for the found / missed / time-per-photo table.
 */
class SceneRecognitionCorpusTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun realScans() {
        val corpus = System.getenv(CORPUS)?.let(::File)
        val out = System.getenv(OUT)?.let(::File)
        assumeTrue("$CORPUS and $OUT unset: no corpus run", corpus != null && out != null)
        val scans = corpus!!.walk().filter { it.isFile && it.extension in SCANS }.sorted().toList()
        assumeTrue("no .svscan or .rrd under $corpus", scans.isNotEmpty())
        val detector = models().firstOrNull()?.let { OnnxSceneDetector(it.readBytes(), it.nameWithoutExtension) }
        println("recognising ${scans.size} scans with ${detector?.name ?: "no model: openings only"}")
        (detector ?: NoDetector).use { model -> scans.forEach { run(it, model, out!!) } }
    }

    @Test
    fun photos() {
        val photos = System.getenv(PHOTOS)?.let(::File)
        val out = System.getenv(OUT)?.let(::File)
        assumeTrue("$PHOTOS and $OUT unset: no photo run", photos != null && out != null)
        val files = photos!!.listFiles { f -> f.extension.lowercase() in IMAGES }.orEmpty().sorted()
        val models = models()
        assumeTrue("no photos or no model", files.isNotEmpty() && models.isNotEmpty())
        for (model in models) {
            val dir = File(out, "photos-${model.nameWithoutExtension}").apply { mkdirs() }
            val table = StringBuilder("photo\twidth\theight\tdetect_ms\tfound\n")
            val sheet = ArrayList<SceneImage>()
            OnnxSceneDetector(mapped(model), model.nameWithoutExtension).use { detector ->
                // The first run pays the session's warm-up: timed apart, not in the table.
                val warm = System.nanoTime()
                detector.detect(SceneImage(WARM_UP, WARM_UP, IntArray(WARM_UP * WARM_UP)))
                table.append("# warm-up\t\t\t${(System.nanoTime() - warm) / NANOS_PER_MS}\t\n")
                var total = 0L
                for (file in files) {
                    val image = SceneTestKit.sceneImage(file.readBytes()) ?: continue
                    val t0 = System.nanoTime()
                    val found = detector.detect(image)
                    val ms = (System.nanoTime() - t0) / NANOS_PER_MS
                    total += ms
                    table.append("${file.name}\t${image.width}\t${image.height}\t$ms\t")
                        .append(found.joinToString(" ") { "${it.name}:%.2f".format(it.score) }).append('\n')
                    val annotated = SceneTestKit.annotate(image, found)
                    File(dir, "${file.nameWithoutExtension}.png").writeBytes(SceneTestKit.png(annotated))
                    sheet += annotated
                }
                table.append("# total\t\t\t$total\t\n")
            }
            File(dir, "timings.tsv").writeText(table.toString())
            File(dir, "contact-sheet.png").writeBytes(SceneTestKit.png(SceneTestKit.contactSheet(sheet)))
            println(table)
        }
    }

    @Test
    fun `a synthetic recording goes through the corpus run end to end`() {
        val scan = SyntheticRoomScan
        val picked = scan.frames.indices.step(KEYFRAME_STEP).toList()
        val frames = picked.map { scan.frames[it] }
        val paths = frames.indices.map { "images/%04d.jpg".format(it) }
        val events = frames.flatMapIndexed { k, frame ->
            val nanos = (k + 1) * FRAME_NANOS
            listOf(ArDebugEvent.CameraPose(nanos, frame.pose), ArDebugEvent.Image(nanos + 1, paths[k]))
        }
        val photos = frames.mapIndexed { k, frame -> ScanPhoto(paths[k], SceneTestKit.jpeg(frame), frame.pose) }
        val lens = ReplayLens.of(
            RerunSyntheticRoom.DEPTH_WIDTH.toFloat(), RerunSyntheticRoom.DEPTH_HEIGHT.toFloat(),
            RerunSyntheticRoom.FOCAL, RerunSyntheticRoom.FOCAL,
        )!!
        val device = ScanDevice("android", "Pixel 9", ScanDevice.TIER_DEPTH, ScanDevice.SOURCE_RAW_DEPTH)
        val pack = runBlocking { RerunCaptureBuilder.build(events, lens, photos, device, scan.cloud) }
        val file = folder.newFile("synthetic-room.svscan").apply { writeBytes(RerunScanFile.write(pack)) }
        val out = folder.newFolder("out")

        val recognition = TruthDetector(frames, TruthDetector.ROOM).use { run(file, it, out) }!!

        val labels = recognition.items.map { it.label }
        for (expected in listOf(SceneLabel.Sofa, SceneLabel.Table, SceneLabel.Refrigerator)) {
            assertTrue("$expected in $labels", expected in labels)
        }
        val dir = File(out, "synthetic-room")
        val report = Json.parseToJsonElement(File(dir, "recognized.json").readText()) as kotlinx.serialization.json.JsonObject
        assertEquals(recognition.items.size, report.getValue("items").jsonArray.size)
        assertEquals(SceneRecognizer.MAX_FRAMES, report.getValue("frames").jsonArray.size)
        val pngs = dir.listFiles { f -> f.name.startsWith("keyframe-") && f.extension == "png" }.orEmpty()
        assertEquals(SceneRecognizer.MAX_FRAMES, pngs.size)
        assertTrue(pngs.all { SceneTestKit.sceneImage(it.readBytes())?.width == RerunSyntheticRoom.DEPTH_WIDTH })
        val timings = File(dir, "timings.txt").readLines()
        assertEquals(SceneRecognizer.MAX_FRAMES + 2, timings.size)
        assertTrue(timings.last(), timings.last().startsWith("total"))
    }

    /**
     * Recognises the scan in [file] with [detector] and writes its outputs under [out]; `null`
     * (and a line saying why) for a file that is no readable scan or has no dense cloud.
     */
    private fun run(file: File, detector: SceneDetector, out: File): SceneRecognition? {
        val opened = open(file) ?: return null.also { println("${file.name}: not a readable scan") }
        val dense = opened.manifest.dense
        val cloud = dense?.let { opened.bytesOf(it.path) }?.let { SvpcCodec.decode(it) }
            ?: return null.also { println("${file.name}: no dense cloud (a sparse scan): nothing to lift into") }
        val objects = ScanObjects.of(cloud, dense.voxelM)
        val keyframes = SceneRecognizer.keyframes(opened.trace, opened.manifest.lens)
        val images = HashMap<String, SceneImage>()
        val recognition = runBlocking {
            SceneRecognizer(detector).recognize(keyframes, cloud, objects, image = { path ->
                opened.bytesOf(path)?.let { SceneTestKit.sceneImage(it) }?.also { images[path] = it }
            })
        }
        val dir = File(out, file.nameWithoutExtension).apply { mkdirs() }
        File(dir, "recognized.json").writeText(SceneInventory.report(recognition, file.name).toString())
        val timings = StringBuilder("keyframe\tpath\tdecode_ms\tdetect_ms\tlift_ms\tdetections\tlifted\n")
        recognition.frames.forEachIndexed { k, frame ->
            val image = images[frame.keyframe.path] ?: return@forEachIndexed
            val annotated = SceneTestKit.annotate(image, frame.detections, recognition.items, frame.keyframe.camera)
            File(dir, "keyframe-%02d.png".format(k)).writeBytes(SceneTestKit.png(annotated))
            timings.append("${frame.keyframe.index}\t${frame.keyframe.path}\t${frame.decodeMillis}\t")
                .append("${frame.detectMillis}\t${frame.liftMillis}\t${frame.detections.size}\t${frame.lifted.size}\n")
        }
        timings.append("total\t\t\t\t\t\t${recognition.totalMillis} ms, ${recognition.frames.size} keyframes\n")
        File(dir, "timings.txt").writeText(timings.toString())
        println("${file.name}: " + SceneInventory.lines(recognition.items).joinToString("; ").ifEmpty { "nothing" } +
            " (${recognition.totalMillis} ms)")
        return recognition
    }

    private fun open(file: File): OpenedCapture? = runCatching {
        val bytes = file.readBytes()
        when (file.extension) {
            "rrd" -> RerunRrdReader.recording(bytes, SceneTestKit.ImageIoCodec).pack.open()
            else -> RerunScanFile.read(bytes)?.open()
        }
    }.getOrNull()

    /** The models to run: `SCENEVIEW_SCENE_MODEL` (paths, `:`-separated), else the bundled one if fetched. */
    private fun models(): List<File> = System.getenv(MODEL)?.split(File.pathSeparator)?.map(::File)?.filter { it.isFile }
        ?: listOf(File(BUNDLED_MODEL)).filter { it.isFile }

    /** [model] mapped in memory, as the app maps it from the APK: a direct buffer, never on the heap. */
    private fun mapped(model: File) = FileChannel.open(model.toPath()).use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }

    private object NoDetector : SceneDetector {
        override val name = "none"

        override fun detect(image: SceneImage) = emptyList<Detection2D>()

        override fun close() = Unit
    }

    private companion object {
        const val CORPUS = "SCENEVIEW_SCAN_CORPUS"
        const val OUT = "SCENEVIEW_SCAN_OUT"
        const val PHOTOS = "SCENEVIEW_SCENE_PHOTOS"
        const val MODEL = "SCENEVIEW_SCENE_MODEL"

        /** Relative to the module, where Gradle runs its unit tests. */
        const val BUNDLED_MODEL = "src/main/assets/ml/yolox_tiny.onnx"
        val SCANS = setOf("svscan", "rrd")
        val IMAGES = setOf("jpg", "jpeg", "png")
        const val KEYFRAME_STEP = 6
        const val FRAME_NANOS = 100_000_000L
        const val NANOS_PER_MS = 1_000_000L
        const val WARM_UP = 64
    }
}
