package io.github.sceneview.demo.demos.internal.model

import io.github.sceneview.demo.demos.internal.BakePhoto
import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.OpenedCapture
import io.github.sceneview.demo.demos.internal.RerunCapturePack
import io.github.sceneview.demo.demos.internal.RerunMarchingCubes
import io.github.sceneview.demo.demos.internal.RerunMesh
import io.github.sceneview.demo.demos.internal.RerunMeshGlb
import io.github.sceneview.demo.demos.internal.RerunResultView
import io.github.sceneview.demo.demos.internal.RerunRrdReader
import io.github.sceneview.demo.demos.internal.RerunScanFile
import io.github.sceneview.demo.demos.internal.RerunScanModel
import io.github.sceneview.demo.demos.internal.RerunSessionStore
import io.github.sceneview.demo.demos.internal.RerunTsdf
import io.github.sceneview.demo.demos.internal.SvpcCodec
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * One recording through the finished-model pipeline, as the result screen builds a kept scan's
 * model — its dense surfels fused into a TSDF, meshed, then cleaned and textured from its key
 * photos — next to the raw model the app showed before. Writes, in [out]:
 * `before.glb`, `after.glb`, `before.png`, `after.png` and `stats.json`.
 */
internal object CleanModelCorpus {
    val EXTENSIONS = setOf(RerunScanFile.EXTENSION, "rrd")

    /** A scan of the corpus: a `*.svscan` or `*.rrd` file, or a session folder as the app keeps it. */
    fun isScan(file: File): Boolean = if (file.isDirectory) {
        File(file, RerunCapturePack.MANIFEST).isFile
    } else {
        file.isFile && file.extension.lowercase() in EXTENSIONS
    }

    /** What [file] gave: its figures, as written to `stats.json`. [progress] gets the finish's. */
    fun run(file: File, out: File, progress: (Float) -> Unit = {}): JsonObject {
        out.mkdirs()
        val stats = runCatching { measure(file, out, progress) }.getOrElse { failure ->
            buildJsonObject {
                put("file", file.name)
                put("error", "${failure::class.simpleName}: ${failure.message}")
            }
        }
        File(out, "stats.json").writeText(stats.toString())
        return stats
    }

    private fun measure(file: File, out: File, progress: (Float) -> Unit): JsonObject {
        val pack = read(file)
        val capture = checkNotNull(pack.open()) { "unreadable manifest" }
        val cloud = denseCloud(capture)
        if (cloud?.normals == null) {
            // The app meshes a kept scan from its surfels; without them it shows the points.
            return buildJsonObject {
                put("file", file.name)
                put("skipped", "no dense cloud with normals: the app shows this scan as points")
                put("photos", capture.trace.imageCount)
            }
        }
        val fuseStart = System.nanoTime()
        val tsdf = RerunTsdf().apply { integrateSurfels(cloud) }
        val fuseMs = since(fuseStart)
        val meshStart = System.nanoTime()
        val extraction = RerunMarchingCubes.extractWhole(tsdf, RerunMarchingCubes.DEFAULT_MIN_WEIGHT)
        val raw = extraction.mesh
        val meshMs = since(meshStart)
        check(raw.triangleCount > 0) { "the surfels mesh to no surface" }
        val beforeStart = System.nanoTime()
        val before = RerunMeshGlb.write(raw)
        val beforeWriteMs = since(beforeStart)

        val photos = ModelPhotos.of(capture.trace, capture.manifest.lens) { path -> decode(capture, path) }
        val result = CleanModel.build(raw, photos, JvmAtlasEncoder, progress = progress)
        val s = result.stats
        File(out, "before.glb").writeBytes(before)
        File(out, "after.glb").writeBytes(result.glb)
        thumbnail(before, raw)?.let { File(out, "before.png").writeBytes(it) }
        thumbnail(result.glb, result.model.asRerunMesh())?.let { File(out, "after.png").writeBytes(it) }
        val problems = GlbCheck.problems(result.glb)
        return buildJsonObject {
            put("file", file.name)
            put("dense_points", cloud.count)
            put("photos", capture.trace.imageCount)
            put("fuse_ms", fuseMs)
            put("mesh_ms", meshMs)
            put(
                "before",
                buildJsonObject {
                    put("triangles", raw.triangleCount)
                    put("vertices", raw.vertexCount)
                    put("glb_bytes", before.size)
                    put("components", s.geometry.piecesIn)
                    put("write_ms", beforeWriteMs)
                    put("ms", fuseMs + meshMs + beforeWriteMs)
                },
            )
            put(
                "after",
                buildJsonObject {
                    put("triangles", s.finalTriangles)
                    put("vertices", s.finalVertices)
                    put("glb_bytes", s.glbBytes)
                    put("components", s.geometry.piecesOut)
                    put("atlas", if (s.atlasWidth > 0) "${s.atlasWidth}x${s.atlasHeight}" else "none")
                    put("clean_input_triangles", s.cleanInputTriangles)
                    put("simplify_target", s.simplifyTarget)
                    s.droppedTextureShare?.let { put("dropped_texture_share", it) }
                    put("prepare_ms", s.prepareMs)
                    put("clean_ms", s.cleanMs)
                    put("simplify_ms", s.simplifyMs)
                    put("texture_ms", s.textureMs)
                    put("write_ms", s.writeMs)
                    put("pipeline_ms", s.totalMs)
                    put("ms", fuseMs + meshMs + s.totalMs)
                    put("glb_problems", problems.joinToString("; "))
                },
            )
            put(
                "geometry",
                buildJsonObject {
                    put("components_dropped", s.geometry.piecesDropped)
                    put("doubled_triangles", s.geometry.doubledTriangles)
                    put("planes", s.geometry.planes)
                    put("flat_share", s.geometry.flatShare)
                    put("holes_filled", s.geometry.holesFilled)
                },
            )
            s.texture?.let { t ->
                put(
                    "texture",
                    buildJsonObject {
                        put("photos_used", t.photosUsed)
                        put("charts", t.charts)
                        put("textured_share", t.texturedShare)
                        put("gain_min", t.gainMin)
                        put("gain_max", t.gainMax)
                        put("seam_raw", t.seamRaw)
                        put("seam_exposed", t.seamExposed)
                        put("seam_final", t.seamFinal)
                        put("texels_per_metre", t.texelsPerMetre)
                    },
                )
            }
        }
    }

    private fun read(file: File): RerunCapturePack {
        if (file.isDirectory) {
            // A session folder as the app keeps it (`files/rerun/sessions/<id>/`), read as the app reads it.
            return checkNotNull(RerunSessionStore(file.parentFile).capture(file.name)) { "incomplete session folder" }
        }
        val bytes = file.readBytes()
        return when (file.extension.lowercase()) {
            RerunScanFile.EXTENSION -> checkNotNull(RerunScanFile.read(bytes)) { "not a scan file" }
            else -> RerunRrdReader.recording(bytes, JvmRerunImageCodec).pack
        }
    }

    private fun denseCloud(capture: OpenedCapture): DenseCloud? {
        val dense = capture.manifest.dense ?: return null
        val blob = capture.bytesOf(dense.path) ?: return null
        return SvpcCodec.decode(blob)?.takeIf { it.count > 0 }
    }

    private fun decode(capture: OpenedCapture, path: String): BakePhoto? {
        val image = capture.bytesOf(path)?.let(JvmImages::decode) ?: return null
        return BakePhoto(ORIGIN, image.width, image.height, image.argb)
    }

    /** The model as the export thumbnail draws it (vertex colours, the result screen's view). */
    private fun thumbnail(glb: ByteArray, mesh: RerunMesh): ByteArray? {
        val model = RerunScanModel(RerunScanModel.Kind.Mesh, glb, mesh.bounds(), mesh.triangleCount, mesh.vertexCount)
        val argb = RerunResultView.thumbnail(model, THUMBNAIL_PX, THUMBNAIL_FOV_DEGREES) ?: return null
        return JvmImages.png(argb, THUMBNAIL_PX, THUMBNAIL_PX)
    }

    private fun since(start: Long): Long = (System.nanoTime() - start) / NANOS_PER_MS

    private val ORIGIN = DebugPose(0f, 0f, 0f)
    private const val THUMBNAIL_PX = 512
    private const val THUMBNAIL_FOV_DEGREES = 45.0
    private const val NANOS_PER_MS = 1_000_000L
}
