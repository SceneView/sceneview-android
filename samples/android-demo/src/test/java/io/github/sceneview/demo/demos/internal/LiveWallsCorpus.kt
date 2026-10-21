package io.github.sceneview.demo.demos.internal

import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Recorded scans replayed through the live walls, before the survey's rules ([LegacyPlaneWalls])
 * and with them ([PlaneWallSource]): how many walls each shows. Counts only, never a dimension.
 */
internal object LiveWallsCorpus {
    /** How often the replay looks at the walls, in seconds of the recording; the last frame too. */
    const val STEP_S = 0.5f

    /** The walls shown at the end, the most shown at once, and how many different walls ever showed. */
    class Counts(val final: Int, val maxAtOnce: Int, val distinct: Int) {
        fun toMap(): Map<String, Int> = mapOf("final" to final, "maxAtOnce" to maxAtOnce, "distinct" to distinct)

        override fun toString(): String = "final $final, at once $maxAtOnce, distinct $distinct"
    }

    class Result(val name: String, val samples: Int, val before: Counts, val after: Counts) {
        fun toMap(): Map<String, Any> =
            mapOf("name" to name, "samples" to samples, "before" to before.toMap(), "after" to after.toMap())

        override fun toString(): String = "$name: before ($before) -> after ($after)"
    }

    private class Tally {
        private val ever = HashSet<Int>()
        var last = 0
            private set
        var most = 0
            private set

        fun add(ids: List<Int>) {
            ever += ids
            last = ids.size
            most = max(most, ids.size)
        }

        fun counts() = Counts(last, most, ever.size)
    }

    /**
     * Every capture under [root], by name: `.svscan` and `.rrd` files, and session directories
     * holding a capture's three files. One that does not read is left out.
     */
    fun captures(root: File): List<Pair<String, RerunCapturePack>> = root.walkTopDown()
        .sortedBy { it.path }
        .mapNotNull { file ->
            val name = file.relativeTo(root).path.ifEmpty { file.name }
            when {
                file.isFile && file.extension == RerunScanFile.EXTENSION ->
                    RerunScanFile.read(file.readBytes())?.let { name to it }
                file.isFile && file.extension == RRD ->
                    runCatching { RerunRrdReader.recording(file.readBytes(), FakeRerunImageCodec).pack }
                        .getOrNull()?.let { name to it }
                file.isDirectory && File(file, RerunCapturePack.MANIFEST).isFile ->
                    file.parentFile?.let { RerunSessionStore(it).capture(file.name) }?.let { name to it }
                else -> null
            }
        }
        .toList()

    /** [pack] replayed every [STEP_S] seconds and at its end; `null` when it does not open. */
    fun replay(name: String, pack: RerunCapturePack): Result? {
        val trace = pack.open()?.trace ?: return null
        val legacy = LegacyPlaneWalls()
        val source = PlaneWallSource()
        val before = Tally()
        val after = Tally()
        var samples = 0
        var t = 0f
        while (true) {
            val time = min(t, trace.duration)
            val frame = trace.frameAt(time)
            val planes = livePlanesOf(frame)
            val floorY = ArDebugGeometry.floorHeight(frame)
            before.add(legacy.update(planes, floorY))
            after.add(source.update(planes, floorY, time).walls.map { it.id })
            samples++
            if (time >= trace.duration) break
            t += STEP_S
        }
        return Result(name, samples, before.counts(), after.counts())
    }

    /** Each capture under [root] replayed, a `<name>.json` each and a `summary.json` written to [out]. */
    fun run(root: File, out: File): List<Result> {
        out.mkdirs()
        val results = captures(root).mapNotNull { (name, pack) -> replay(name, pack) }
        for (result in results) {
            File(out, result.name.replace(File.separatorChar, '_') + ".json").writeText(RerunJson.write(result.toMap()))
        }
        val total = mapOf(
            "sessions" to results.size,
            "beforeFinal" to results.sumOf { it.before.final },
            "afterFinal" to results.sumOf { it.after.final },
            "beforeMaxAtOnce" to results.sumOf { it.before.maxAtOnce },
            "afterMaxAtOnce" to results.sumOf { it.after.maxAtOnce },
            "beforeDistinct" to results.sumOf { it.before.distinct },
            "afterDistinct" to results.sumOf { it.after.distinct },
        )
        File(out, "summary.json").writeText(RerunJson.write(mapOf("total" to total, "sessions" to results.map { it.toMap() })))
        return results
    }

    private const val RRD = "rrd"
}
