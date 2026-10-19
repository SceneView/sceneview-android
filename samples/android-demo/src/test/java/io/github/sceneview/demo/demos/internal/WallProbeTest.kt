package io.github.sceneview.demo.demos.internal

import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/** Scratch probe (#4719): how much of a far, noisy wall survives fusion and meshing. */
class WallProbeTest {
    private val wall = SyntheticShape.Box(Vec3(-3f, 0f, -4.2f), Vec3(3f, 3f, -4f)) { _, _, _ -> 0xFFC8B090.toInt() }
    private val ground = SyntheticShape.Box(Vec3(-6f, -0.1f, -6f), Vec3(6f, 0f, 6f)) { _, _, _ -> 0xFF507040.toInt() }
    private val scene = SyntheticScene(listOf(wall, ground))

    private fun frames(noiseK: Float, confAt: (Float) -> Int, seed: Int): List<DepthFrame> {
        val random = Random(seed)
        val out = ArrayList<DepthFrame>()
        for (zi in 0 until 5) for (xi in 0 until 9) {
            val at = Vec3(-2f + xi * 0.5f, 1.4f, -1.5f + zi * 0.5f)
            val yaw = (random.nextFloat() - 0.5f) * 0.5f
            val pose = RerunSyntheticRoom.lookPose(at, yaw, Math.toRadians(-8.0).toFloat())
            val clean = scene.render(pose)
            val depth = clean.depthMm.copyOf()
            val conf = ByteArray(depth.size)
            for (i in depth.indices) {
                val d = (depth[i].toInt() and 0xFFFF) / 1000f
                if (d == 0f) continue
                val g = (random.nextFloat() + random.nextFloat() + random.nextFloat() - 1.5f) * 2f
                val noisy = d + g * noiseK * d * d
                depth[i] = (noisy * 1000f).toInt().coerceIn(1, Short.MAX_VALUE.toInt()).toShort()
                conf[i] = confAt(d).coerceIn(0, 255).toByte()
            }
            out += DepthFrame(clean.width, clean.height, depth, conf, clean.colors, clean.fx, clean.fy, clean.cx, clean.cy, pose)
        }
        return out
    }

    /** Share of the wall's 10 cm cells (front face, z = -4) that hold a kept vertex, and the grey share. */
    private fun wallCover(mesh: RerunMesh): Pair<Float, Float> {
        val cells = HashSet<Long>()
        var onWall = 0
        var grey = 0
        for (v in 0 until mesh.vertexCount) {
            val x = mesh.positions[v * 3]
            val y = mesh.positions[v * 3 + 1]
            val z = mesh.positions[v * 3 + 2]
            if (abs(z + 4f) > 0.15f || x < -3f || x > 3f || y < 0.05f || y > 3f) continue
            onWall++
            if (mesh.colors[v] == RerunMarchingCubes.NO_COLOR) grey++
            cells += ((x + 3f) / 0.1f).toLong() * 1000 + (y / 0.1f).toLong()
        }
        return cells.size / (60f * 30f) to if (onWall == 0) 0f else grey.toFloat() / onWall
    }

    @Test
    fun probe() {
        val cases = listOf(
            "clean, full conf" to frames(0f, { 255 }, 1),
            "noise 0.6% d², conf 200" to frames(0.006f, { 200 }, 2),
            "noise 1% d², conf 200" to frames(0.01f, { 200 }, 3),
            "noise 1% d², conf falls with d" to frames(0.01f, { d -> (255 * (1.3f - d / 6f)).toInt() }, 4),
        )
        for ((name, fs) in cases) {
            val tsdf = RerunTsdf()
            fs.forEach { tsdf.integrate(it) }
            for ((w, comp) in listOf(1f to 500, 0.5f to 500, 1f to 60, 0.5f to 60)) {
                val ex = RerunMarchingCubes.extract(tsdf, minWeight = w, minComponentTriangles = comp)
                val (cover, grey) = wallCover(ex.mesh)
                println(
                    "WALLPROBE $name | minW $w comp $comp -> tris ${ex.mesh.triangleCount} raw ${ex.rawTriangles} " +
                        "dropped ${ex.droppedComponents} wall cover ${"%.2f".format(cover)} grey ${"%.2f".format(grey)}",
                )
            }
        }
    }
}
