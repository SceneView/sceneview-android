package io.github.sceneview.demo.demos.internal.model

import io.github.sceneview.demo.demos.internal.RerunMesh
import io.github.sceneview.demo.demos.internal.RerunMeshGlb
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.system.exitProcess

/**
 * The clean model on a room as a phone really scans it ([RealScanRoom]): walls swelling by ±2 cm,
 * two walls scanned twice 3 cm apart, small key photos taken close and at an angle, then the same
 * room with a pleated curtain and a sofa. The finished model must come out light — about the
 * simplifier's budget, and never a heavier file than the raw surface — with its walls flat and
 * single, its furniture still in relief, and within a phone's heap.
 *
 * Each case gathers every figure and fails once, with all of them: CI prints only a failure.
 */
class CleanModelRealScanTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `a bare room scanned with real noise comes out light, flat and single-walled`() {
        check(furnished = false)
    }

    @Test
    fun `a furnished room keeps its curtain and sofa in relief while its walls flatten`() {
        check(furnished = true)
    }

    @Test
    fun `the furnished room finishes within a 192 MB heap`() {
        val raw = RealScanRoom.raw(furnished = true)
        val meshFile = folder.newFile("room.mesh")
        RealScanRoom.writeMesh(raw, meshFile)
        val log = folder.newFile("finish.log")
        val java = File(File(System.getProperty("java.home"), "bin"), "java").path
        val classpath = (
            System.getProperty("java.class.path").split(File.pathSeparator) +
                listOf(HeapCappedFinish::class.java, CleanModel::class.java, Unit::class.java, Json::class.java)
                    .mapNotNull { codeSource(it) }
            ).filter { it.isNotEmpty() }.distinct().joinToString(File.pathSeparator)
        val process = ProcessBuilder(
            java, "-Xmx${HeapCappedFinish.HEAP_MB}m", "-XX:+UseSerialGC", "-Djava.awt.headless=true",
            "-cp", classpath, HeapCappedFinish::class.java.name, meshFile.path,
        ).redirectErrorStream(true).redirectOutput(log).start()
        val done = process.waitFor(CHILD_TIMEOUT_S, TimeUnit.SECONDS)
        if (!done) process.destroyForcibly().waitFor()
        val output = log.readText()
        val tail = "furnished room in ${HeapCappedFinish.HEAP_MB} MB (${raw.triangleCount} raw triangles): " +
            output.takeLast(LOG_TAIL)
        assertTrue("timed out after $CHILD_TIMEOUT_S s: $tail", done)
        assertEquals(tail, 0, process.exitValue())
        assertTrue(tail, output.contains(HeapCappedFinish.FINISHED))
    }

    private fun check(furnished: Boolean) {
        val raw = RealScanRoom.raw(furnished)
        val rawGlb = RerunMeshGlb.write(raw)
        val result = CleanModel.build(raw, RealScanRoom.modelPhotos(furnished), JvmAtlasEncoder)
        val stats = result.stats
        val model = result.model
        val finalSurface = Surface(model.positions, model.texturedIndices + model.plainIndices)
        val rawSurface = Surface(raw.positions, raw.indices)
        val excluded: (Float, Float, Float) -> Boolean = if (furnished) ::nearFurniture else { _, _, _ -> false }
        val rawWalls = walls(rawSurface, excluded)
        val finalWalls = walls(finalSurface, excluded)
        val rawLayers = layers(rawSurface)
        val finalLayers = layers(finalSurface)
        val texture = stats.texture

        val report = StringBuilder()
        report.append(if (furnished) "furnished room" else "bare room")
            .append(": raw ${raw.triangleCount} triangles, glb ${rawGlb.size / KB} KB")
            .append(" -> ${stats.finalTriangles} triangles (${stats.simplifiedTriangles} simplified, target ${stats.simplifyTarget})")
            .append(", glb ${stats.glbBytes / KB} KB, atlas ${stats.atlasWidth}x${stats.atlasHeight}")
            .append("; flat ${percent(stats.geometry.flatShare)}, ${stats.geometry.planes} planes")
            .append(", doubled ${stats.geometry.doubledTriangles} triangles")
            .append("; textured ${texture?.let { percent(it.texturedShare) } ?: "none"}")
            .append(texture?.let { ", ${it.photosUsed}/${it.photosIn} photos" } ?: "")
            .append(stats.droppedTextureShare?.let { " (texture dropped at ${percent(it)})" } ?: "")
            .append("; time prepare ${stats.prepareMs}, clean ${stats.cleanMs}, simplify ${stats.simplifyMs}")
            .append(", texture ${stats.textureMs}, write ${stats.writeMs} = ${stats.totalMs} ms")
            .append("; walls rms ${mm(rawWalls.rms)} -> ${mm(finalWalls.rms)} mm (worst ${mm(finalWalls.worst)})")
            .append("; layers ${ratio(rawLayers)} -> ${ratio(finalLayers)}")

        val problems = mutableListOf<String>()
        if (raw.triangleCount !in MIN_RAW_TRIANGLES..MAX_RAW_TRIANGLES) problems += "raw ${raw.triangleCount} triangles"
        if (stats.finalTriangles > MAX_FINAL_TRIANGLES) problems += "${stats.finalTriangles} triangles"
        // The file shared and kept: never heavier than the raw surface, seams and atlas counted.
        if (result.glb.size > rawGlb.size) problems += "glb ${result.glb.size} > raw ${rawGlb.size}"
        val minFlat = if (furnished) MIN_FLAT_FURNISHED else MIN_FLAT_BARE
        if (stats.geometry.flatShare < minFlat) problems += "flat ${percent(stats.geometry.flatShare)}"
        if (finalWalls.rms > MAX_WALL_RMS_M || finalWalls.worst > MAX_WALL_WORST_M) problems += "walls not flat"
        if (finalLayers.any { it > MAX_LAYERS }) problems += "walls still doubled"
        if (furnished) {
            val rawCurtain = curtainRelief(rawSurface)
            val finalCurtain = curtainRelief(finalSurface)
            val rawSeat = seatRelief(rawSurface)
            val finalSeat = seatRelief(finalSurface)
            report.append("; curtain relief ${mm(rawCurtain)} -> ${mm(finalCurtain)} mm")
                .append(", seat relief ${mm(rawSeat)} -> ${mm(finalSeat)} mm")
            if (finalCurtain < rawCurtain * MIN_CURTAIN_KEPT) problems += "curtain flattened"
            if (finalSeat < rawSeat * MIN_SEAT_KEPT) problems += "sofa flattened"
        }
        GlbCheck.problems(result.glb).takeIf { it.isNotEmpty() }?.let { problems += "glb: $it" }
        if (stats.totalMs > MAX_MS) problems += "${stats.totalMs} ms"
        println(report)
        assertTrue("$problems | $report", problems.isEmpty())
    }

    /** A triangle soup's faces: unit normal, area, centroid. */
    private class Surface(val positions: FloatArray, val indices: IntArray) {
        val count = indices.size / 3
        val normal = FloatArray(count * 3)
        val centroid = FloatArray(count * 3)
        val area = FloatArray(count)

        init {
            for (t in 0 until count) {
                val a = indices[t * 3] * 3
                val b = indices[t * 3 + 1] * 3
                val c = indices[t * 3 + 2] * 3
                val ux = positions[b] - positions[a]
                val uy = positions[b + 1] - positions[a + 1]
                val uz = positions[b + 2] - positions[a + 2]
                val vx = positions[c] - positions[a]
                val vy = positions[c + 1] - positions[a + 1]
                val vz = positions[c + 2] - positions[a + 2]
                val nx = uy * vz - uz * vy
                val ny = uz * vx - ux * vz
                val nz = ux * vy - uy * vx
                val length = sqrt(nx * nx + ny * ny + nz * nz)
                area[t] = length / 2f
                if (length > 0f) {
                    normal[t * 3] = nx / length
                    normal[t * 3 + 1] = ny / length
                    normal[t * 3 + 2] = nz / length
                }
                for (axis in 0 until 3) {
                    centroid[t * 3 + axis] = (positions[a + axis] + positions[b + axis] + positions[c + axis]) / 3f
                }
            }
        }
    }

    private class Flatness(val rms: Float, val worst: Float)

    /**
     * The room's six surfaces — floor, ceiling, four walls — each measured as the RMS distance of
     * its faces' vertices to their own mean plane, area-weighted: faces within [NEAR_M] of the
     * surface, facing the room, [EDGE_M] clear of the other surfaces, and away from [excluded].
     */
    private fun walls(surface: Surface, excluded: (Float, Float, Float) -> Boolean): Flatness {
        val room = RealScanRoom.ROOM
        val lo = floatArrayOf(room.x0, room.y0, room.z0)
        val hi = floatArrayOf(room.x1, room.y1, room.z1)
        var totalWeight = 0.0
        var totalVariance = 0.0
        var worst = 0f
        for (axis in 0 until 3) {
            for (side in 0 until 2) {
                val at = if (side == 0) lo[axis] else hi[axis]
                val inward = if (side == 0) 1f else -1f
                var w = 0.0
                var sum = 0.0
                var squares = 0.0
                for (t in 0 until surface.count) {
                    val c = t * 3
                    if (abs(surface.centroid[c + axis] - at) > NEAR_M) continue
                    if (surface.normal[c + axis] * inward < FACING) continue
                    val clear = (0 until 3).all { a ->
                        a == axis || surface.centroid[c + a] in lo[a] + EDGE_M..hi[a] - EDGE_M
                    }
                    if (!clear) continue
                    if (excluded(surface.centroid[c], surface.centroid[c + 1], surface.centroid[c + 2])) continue
                    val weight = surface.area[t] / 3.0
                    for (k in 0 until 3) {
                        val d = surface.positions[surface.indices[c + k] * 3 + axis].toDouble()
                        w += weight
                        sum += weight * d
                        squares += weight * d * d
                    }
                }
                if (w <= 0.0) continue
                val mean = sum / w
                val variance = max(0.0, squares / w - mean * mean)
                worst = max(worst, sqrt(variance).toFloat())
                totalWeight += w
                totalVariance += variance * w
            }
        }
        val rms = if (totalWeight > 0.0) sqrt(totalVariance / totalWeight).toFloat() else 0f
        return Flatness(rms, worst)
    }

    /** The curtain's and the sofa's corners of the room, left out of the walls' flatness. */
    private fun nearFurniture(x: Float, @Suppress("UNUSED_PARAMETER") y: Float, z: Float): Boolean =
        (x > CURTAIN_ZONE_X && z in CURTAIN_ZONE_Z0..CURTAIN_ZONE_Z1) ||
            (z < SOFA_ZONE_Z && x in -SOFA_ZONE_X..SOFA_ZONE_X)

    /**
     * How many sheets the doubled walls show: the area of the faces within [NEAR_M] of each wall
     * and turned along its normal, inside the doubled patch, over the patch's area — about 2 for
     * a wall scanned twice, 1 for a single wall.
     */
    private fun layers(surface: Surface): List<Float> {
        var xWall = 0.0
        var zWall = 0.0
        val room = RealScanRoom.ROOM
        for (t in 0 until surface.count) {
            val c = t * 3
            val x = surface.centroid[c]
            val y = surface.centroid[c + 1]
            val z = surface.centroid[c + 2]
            if (y !in LAYER_Y0..LAYER_Y1) continue
            if (abs(x - room.x0) <= NEAR_M && abs(surface.normal[c]) >= ALONG && z in X_LAYER_Z0..X_LAYER_Z1) {
                xWall += surface.area[t]
            }
            if (abs(z - room.z1) <= NEAR_M && abs(surface.normal[c + 2]) >= ALONG && x in -Z_LAYER_X..Z_LAYER_X) {
                zWall += surface.area[t]
            }
        }
        val height = LAYER_Y1 - LAYER_Y0
        return listOf(
            (xWall / (height * (X_LAYER_Z1 - X_LAYER_Z0))).toFloat(),
            (zWall / (height * 2 * Z_LAYER_X)).toFloat(),
        )
    }

    /** The curtain's pleats: the area-weighted spread of its front's depth. */
    private fun curtainRelief(surface: Surface): Float = spread(surface, axis = 0) { t, x, y, z ->
        surface.normal[t * 3] < -AWAY && x in CURTAIN_X0..CURTAIN_X1 && y in CURTAIN_Y0..CURTAIN_Y1 &&
            z in CURTAIN_Z0..CURTAIN_Z1
    }

    /** The seat's cushions: the area-weighted spread of its top's height, the grooves between them included. */
    private fun seatRelief(surface: Surface): Float = spread(surface, axis = 1) { t, x, y, z ->
        surface.normal[t * 3 + 1] > AWAY && x in -SEAT_X..SEAT_X && y in SEAT_Y0..SEAT_Y1 && z in SEAT_Z0..SEAT_Z1
    }

    private fun spread(surface: Surface, axis: Int, keep: (Int, Float, Float, Float) -> Boolean): Float {
        var w = 0.0
        var sum = 0.0
        var squares = 0.0
        for (t in 0 until surface.count) {
            val c = t * 3
            if (!keep(t, surface.centroid[c], surface.centroid[c + 1], surface.centroid[c + 2])) continue
            val d = surface.centroid[c + axis].toDouble()
            w += surface.area[t]
            sum += surface.area[t] * d
            squares += surface.area[t] * d * d
        }
        if (w <= 0.0) return 0f
        val mean = sum / w
        return sqrt(max(0.0, squares / w - mean * mean)).toFloat()
    }

    private fun codeSource(type: Class<*>): String? =
        runCatching { File(type.protectionDomain.codeSource.location.toURI()).path }.getOrNull()

    private fun percent(share: Float) = "%.1f%%".format(share * PERCENT)
    private fun mm(m: Float) = "%.1f".format(m * MM_PER_M)
    private fun ratio(values: List<Float>) = values.joinToString("/") { "%.2f".format(it) }

    private companion object {
        const val KB = 1024
        const val PERCENT = 100f
        const val MM_PER_M = 1000f

        const val MIN_RAW_TRIANGLES = 300_000
        const val MAX_RAW_TRIANGLES = 700_000

        /** The simplifier's budget, and a margin for the seams' split. */
        const val MAX_FINAL_TRIANGLES = 55_000
        const val MIN_FLAT_BARE = 0.8f
        const val MIN_FLAT_FURNISHED = 0.75f
        const val MAX_WALL_RMS_M = 0.003f
        const val MAX_WALL_WORST_M = 0.005f
        const val MAX_LAYERS = 1.3f
        const val MIN_CURTAIN_KEPT = 0.7f
        const val MIN_SEAT_KEPT = 0.6f

        /** A desktop JVM's figure, a guard against a regression, not a phone's time. */
        const val MAX_MS = 15_000L

        const val NEAR_M = 0.06f
        const val EDGE_M = 0.2f
        const val FACING = 0.8f
        const val ALONG = 0.5f
        const val AWAY = 0.3f

        const val CURTAIN_ZONE_X = 2.0f
        const val CURTAIN_ZONE_Z0 = -1.4f
        const val CURTAIN_ZONE_Z1 = 1.2f
        const val SOFA_ZONE_Z = -2.3f
        const val SOFA_ZONE_X = 1.3f

        const val LAYER_Y0 = 0.55f
        const val LAYER_Y1 = 2.15f
        const val X_LAYER_Z0 = -0.85f
        const val X_LAYER_Z1 = 1.85f
        const val Z_LAYER_X = 1.35f

        const val CURTAIN_X0 = 2.2f
        const val CURTAIN_X1 = 2.47f
        const val CURTAIN_Y0 = 0.3f
        const val CURTAIN_Y1 = 2.2f
        const val CURTAIN_Z0 = -1.05f
        const val CURTAIN_Z1 = 0.85f

        const val SEAT_X = 0.75f
        const val SEAT_Y0 = 0.2f
        const val SEAT_Y1 = 0.6f
        const val SEAT_Z0 = -3.05f
        const val SEAT_Z1 = -2.66f

        const val CHILD_TIMEOUT_S = 300L
        const val LOG_TAIL = 4000
    }
}

/**
 * The finish of [CleanModelRealScanTest]'s furnished room in a JVM of its own, its heap capped
 * at a mid-range phone's ([HEAP_MB], a Pixel 4a's `dalvik.vm.heapgrowthlimit`). What the app
 * holds meanwhile stays live too: [APP_BYTES] for the rest of the app, the raw surface, and its
 * `.glb`, shown while the finish runs. Prints [FINISHED] and its figures when it fits.
 */
object HeapCappedFinish {
    const val FINISHED = "FINISHED"
    const val HEAP_MB = 192
    private const val APP_BYTES = 32 * 1024 * 1024
    private const val MB = 1024 * 1024
    private const val SAMPLE_MS = 20L
    private const val NANOS_PER_MS = 1_000_000L

    @JvmStatic
    fun main(args: Array<String>) {
        val app = ByteArray(APP_BYTES)
        val raw: RerunMesh = RealScanRoom.readMesh(File(args[0]))
        val rawGlb = RerunMeshGlb.write(raw)
        val runtime = Runtime.getRuntime()
        val peak = AtomicLong()
        thread(isDaemon = true) {
            while (true) {
                val used = runtime.totalMemory() - runtime.freeMemory()
                peak.accumulateAndGet(used) { a, b -> maxOf(a, b) }
                Thread.sleep(SAMPLE_MS)
            }
        }
        val start = System.nanoTime()
        val result = CleanModel.build(raw, RealScanRoom.modelPhotos(furnished = true), JvmAtlasEncoder)
        val ms = (System.nanoTime() - start) / NANOS_PER_MS
        println(
            "$FINISHED ${raw.triangleCount} -> ${result.stats.finalTriangles} triangles, " +
                "glb ${result.glb.size / 1024} KB (raw ${rawGlb.size / 1024} KB), " +
                "peak used ${peak.get() / MB} MB of $HEAP_MB, $ms ms, app ${app.size / MB} MB held",
        )
        System.out.flush()
        exitProcess(0)
    }
}
