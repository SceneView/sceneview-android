package io.github.sceneview.demo.demos.internal.room

import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.DepthBackProjection
import io.github.sceneview.demo.demos.internal.DepthFrame
import io.github.sceneview.demo.demos.internal.RerunSyntheticRoom
import io.github.sceneview.demo.demos.internal.Vec3
import java.util.Random
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A hole in wall [wall] of a [SyntheticRoom]: [from] metres along it, [width] wide, [sill] up, [height] tall. */
class SyntheticOpening(val wall: Int, val from: Float, val width: Float, val sill: Float, val height: Float)

/** A box standing in the room: furniture. */
class SyntheticBox(val x0: Float, val x1: Float, val z0: Float, val z1: Float, val top: Float)

/** Floor outside the room, over [x0]..[x1] × [z0]..[z1]: the next room's, seen through a doorway. */
class SyntheticFloor(val x0: Float, val x1: Float, val z0: Float, val z1: Float)

/**
 * A room to scan: its floor's outline [x], [z] counter-clockwise seen from above (+Y up, the room
 * on the left of each wall's walk), its walls floor to [height], the [openings] cut in them and
 * the [boxes] standing in it. [hidden] walls are never seen: a ray that reaches one returns no
 * depth. [drift] moves wall [driftWall] out by that much in every other frame: a scan that drew
 * the wall twice. [noise] is the depth's Gaussian spread, as a share of the depth. Walls are seen
 * only up to [seenUpTo] — a phone held towards the floor sees their foot and nothing higher — and
 * the floor goes on outside the room over [nextDoor], for whatever a doorway lets the camera see.
 * Nothing further than [reach] across the floor from the camera returns depth: a scan that stopped
 * before it went round the room.
 */
class SyntheticRoom(
    val x: FloatArray,
    val z: FloatArray,
    val height: Float = 2.5f,
    val openings: List<SyntheticOpening> = emptyList(),
    val boxes: List<SyntheticBox> = emptyList(),
    val hidden: Set<Int> = emptySet(),
    val driftWall: Int = -1,
    val drift: Float = 0f,
    val noise: Float = 0f,
    val seenUpTo: Float = Float.POSITIVE_INFINITY,
    val nextDoor: List<SyntheticFloor> = emptyList(),
    val reach: Float = Float.POSITIVE_INFINITY,
) {
    val area: Float
        get() {
            var a = 0f
            for (i in x.indices) {
                val j = (i + 1) % x.size
                a += x[i] * z[j] - x[j] * z[i]
            }
            return abs(a) / 2
        }

    val perimeter: Float get() = x.indices.sumOf { i -> wallLength(i).toDouble() }.toFloat()

    fun wallLength(i: Int): Float {
        val j = (i + 1) % x.size
        return hypot(x[j] - x[i], z[j] - z[i])
    }

    fun inside(px: Float, pz: Float): Boolean {
        var crossings = false
        for (i in x.indices) {
            val j = (i + 1) % x.size
            if ((z[i] > pz) != (z[j] > pz)) {
                val at = x[i] + (pz - z[i]) * (x[j] - x[i]) / (z[j] - z[i])
                if (at > px) crossings = !crossings
            }
        }
        return crossings
    }

    /** How far ([px], [pz]) is from the nearest wall. */
    fun clearance(px: Float, pz: Float): Float {
        var best = Float.MAX_VALUE
        for (i in x.indices) {
            val j = (i + 1) % x.size
            val dx = x[j] - x[i]
            val dz = z[j] - z[i]
            val t = (((px - x[i]) * dx + (pz - z[i]) * dz) / (dx * dx + dz * dz)).coerceIn(0f, 1f)
            best = min(best, hypot(px - x[i] - t * dx, pz - z[i] - t * dz))
        }
        return best
    }
}

/** One synthetic scan: the fused cloud, the camera's path, ARCore-like planes, and the poses it was rendered from. */
class SyntheticCapture(val cloud: DenseCloud, val trail: FloatArray, val planes: List<RoomPlane>, val poses: List<DebugPose>) {
    fun scan(): RoomScan = RoomScan(cloud.positions, cloud.normals, trail, planes)
}

/**
 * Renders depth frames of a [SyntheticRoom] from a walk through it and fuses them the way a phone
 * scan does ([DepthBackProjection], [DenseFusion]): the cloud the engine reads on a device, with
 * nothing from a real home in it.
 */
object SyntheticScan {
    private const val WIDTH = 160
    private const val HEIGHT = 90
    private const val FOCAL = 125f
    private const val EYE_M = 1.4f
    private const val STEP_M = 1f
    private const val CLEAR_M = 0.6f
    private const val YAWS = 8
    val PITCHES = floatArrayOf(-0.6f, 0f, 0.5f)
    private const val MM = 1000f
    private const val GREY = 0xff808080.toInt()
    private const val PLANE_FROM_M = 0.3f

    /** The walk's stops: a grid over the floor, each clear of the walls and boxes. */
    fun stops(room: SyntheticRoom): List<Vec3> {
        val stops = ArrayList<Vec3>()
        val minX = room.x.min()
        val maxX = room.x.max()
        val minZ = room.z.min()
        val maxZ = room.z.max()
        var row = 0
        var pz = minZ + CLEAR_M
        while (pz <= maxZ - CLEAR_M + 1e-3f) {
            val line = ArrayList<Vec3>()
            var px = minX + CLEAR_M
            while (px <= maxX - CLEAR_M + 1e-3f) {
                val inBox = room.boxes.any { px > it.x0 - CLEAR_M && px < it.x1 + CLEAR_M && pz > it.z0 - CLEAR_M && pz < it.z1 + CLEAR_M }
                if (room.inside(px, pz) && room.clearance(px, pz) >= CLEAR_M - 1e-3f && !inBox) line += Vec3(px, EYE_M, pz)
                px += STEP_M
            }
            // A walk back and forth: a path, not a jump from one row's end to the next row's start.
            stops += if (row % 2 == 0) line else line.reversed()
            pz += STEP_M
            row++
        }
        return stops
    }

    /** The poses: at each stop of [walk], a turn of [YAWS] headings at each of [pitches]. */
    fun poses(
        room: SyntheticRoom,
        upTo: Float = 1f,
        walk: List<Vec3> = stops(room),
        pitches: FloatArray = PITCHES,
    ): List<DebugPose> {
        val poses = ArrayList<DebugPose>()
        for (stop in walk) {
            for (pitch in pitches) {
                for (k in 0 until YAWS) {
                    poses += RerunSyntheticRoom.lookPose(stop, (k * 2 * Math.PI / YAWS).toFloat(), pitch)
                }
            }
        }
        return poses.subList(0, max(1, (poses.size * upTo).roundToInt()))
    }

    /**
     * The scan of [room]: the first [share] of the walk's frames, fused. [seed] fixes the noise:
     * the same room is the same cloud, every run. The camera stops at each of [walk] and looks at [pitches].
     */
    fun capture(
        room: SyntheticRoom,
        share: Float = 1f,
        seed: Long = 7L,
        walk: List<Vec3> = stops(room),
        pitches: FloatArray = PITCHES,
    ): SyntheticCapture {
        val random = Random(seed)
        val poses = poses(room, share, walk, pitches)
        val fusion = DenseFusion()
        for ((index, pose) in poses.withIndex()) {
            fusion.add(DepthBackProjection.project(frame(room, pose, index % 2 == 1, random)))
        }
        val cloud = fusion.cloud(minViews = DenseFusion.MIN_VIEWS)
        val trail = FloatArray(poses.size * 3)
        poses.forEachIndexed { i, p ->
            trail[i * 3] = p.x
            trail[i * 3 + 1] = p.y
            trail[i * 3 + 2] = p.z
        }
        return SyntheticCapture(cloud, trail, planesOf(room), poses)
    }

    /**
     * The planes ARCore would find: the floor, and each seen wall a little short of its corners,
     * no higher than it was seen.
     */
    fun planesOf(room: SyntheticRoom): List<RoomPlane> {
        val planes = ArrayList<RoomPlane>()
        val n = room.x.size
        planes += RoomPlane(RoomPlaneKind.Floor, FloatArray(n * 3) { k -> if (k % 3 == 0) room.x[k / 3] else if (k % 3 == 1) 0f else room.z[k / 3] })
        val top = min(2f, room.seenUpTo)
        for (i in 0 until n) {
            if (i in room.hidden || top <= PLANE_FROM_M) continue
            val j = (i + 1) % n
            val inset = 0.1f / room.wallLength(i)
            val ax = room.x[i] + (room.x[j] - room.x[i]) * inset
            val az = room.z[i] + (room.z[j] - room.z[i]) * inset
            val bx = room.x[j] - (room.x[j] - room.x[i]) * inset
            val bz = room.z[j] - (room.z[j] - room.z[i]) * inset
            planes += RoomPlane(
                RoomPlaneKind.Wall,
                floatArrayOf(ax, PLANE_FROM_M, az, bx, PLANE_FROM_M, bz, bx, top, bz, ax, top, az),
            )
        }
        return planes
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LongMethod")
    fun frame(room: SyntheticRoom, pose: DebugPose, drifted: Boolean, random: Random): DepthFrame {
        val depth = ShortArray(WIDTH * HEIGHT)
        val cx = WIDTH / 2f
        val cy = HEIGHT / 2f
        val n = room.x.size
        // Walls as offset segments: a drifted wall is moved out along its outward normal.
        val wx0 = FloatArray(n)
        val wz0 = FloatArray(n)
        val wx1 = FloatArray(n)
        val wz1 = FloatArray(n)
        for (i in 0 until n) {
            val j = (i + 1) % n
            val length = room.wallLength(i)
            // Counter-clockwise: the room on the left, the outside along (dz, −dx).
            val ox = (room.z[j] - room.z[i]) / length
            val oz = -(room.x[j] - room.x[i]) / length
            val shift = if (drifted && i == room.driftWall) room.drift else 0f
            wx0[i] = room.x[i] + ox * shift
            wz0[i] = room.z[i] + oz * shift
            wx1[i] = room.x[j] + ox * shift
            wz1[i] = room.z[j] + oz * shift
        }
        for (py in 0 until HEIGHT) {
            for (px in 0 until WIDTH) {
                val lx = (px - cx) / FOCAL
                val ly = -(py - cy) / FOCAL
                val len = sqrt(lx * lx + ly * ly + 1f)
                val dir = pose.rotate(lx / len, ly / len, -1f / len)
                var best = Float.MAX_VALUE
                var blind = false
                // Walls.
                for (i in 0 until n) {
                    val ex = wx1[i] - wx0[i]
                    val ez = wz1[i] - wz0[i]
                    val denom = dir.x * ez - dir.z * ex
                    if (abs(denom) < 1e-9f) continue
                    val qx = wx0[i] - pose.x
                    val qz = wz0[i] - pose.z
                    val t = (qx * ez - qz * ex) / denom
                    val s = (qx * dir.z - qz * dir.x) / denom
                    if (t <= 1e-4f || t >= best || s < 0f || s > 1f) continue
                    val y = pose.y + dir.y * t
                    if (y < 0f || y > room.height) continue
                    val along = s * room.wallLength(i)
                    val inHole = room.openings.any { o ->
                        o.wall == i && along > o.from && along < o.from + o.width && y > o.sill && y < o.sill + o.height
                    }
                    if (inHole) continue
                    best = t
                    blind = i in room.hidden || y > room.seenUpTo
                }
                // Floor and ceiling.
                for (level in floatArrayOf(0f, room.height)) {
                    if (abs(dir.y) < 1e-9f) continue
                    val t = (level - pose.y) / dir.y
                    if (t <= 1e-4f || t >= best) continue
                    val hx = pose.x + dir.x * t
                    val hz = pose.z + dir.z * t
                    val beyond = level == 0f && room.nextDoor.any { hx > it.x0 && hx < it.x1 && hz > it.z0 && hz < it.z1 }
                    if (!room.inside(hx, hz) && !beyond) continue
                    best = t
                    blind = false
                }
                // Boxes.
                for (box in room.boxes) {
                    val t = slab(pose, dir, box) ?: continue
                    if (t >= best) continue
                    best = t
                    blind = false
                }
                if (best == Float.MAX_VALUE || blind) continue
                if (hypot(dir.x, dir.z) * best > room.reach) continue
                var metres = best / len
                if (room.noise > 0f) metres += (random.nextGaussian() * room.noise * metres).toFloat()
                val mm = (metres * MM).roundToInt()
                if (mm in 1..Short.MAX_VALUE) depth[py * WIDTH + px] = mm.toShort()
            }
        }
        return DepthFrame(WIDTH, HEIGHT, depth, null, IntArray(WIDTH * HEIGHT) { GREY }, FOCAL, FOCAL, cx, cy, pose)
    }

    private fun slab(pose: DebugPose, dir: Vec3, box: SyntheticBox): Float? {
        var lo = 1e-4f
        var hi = Float.MAX_VALUE
        val origin = floatArrayOf(pose.x, pose.y, pose.z)
        val d = floatArrayOf(dir.x, dir.y, dir.z)
        val min = floatArrayOf(box.x0, 0f, box.z0)
        val max = floatArrayOf(box.x1, box.top, box.z1)
        for (a in 0 until 3) {
            if (abs(d[a]) < 1e-9f) {
                if (origin[a] < min[a] || origin[a] > max[a]) return null
                continue
            }
            var t0 = (min[a] - origin[a]) / d[a]
            var t1 = (max[a] - origin[a]) / d[a]
            if (t0 > t1) t0 = t1.also { t1 = t0 }
            lo = max(lo, t0)
            hi = min(hi, t1)
            if (lo > hi) return null
        }
        return lo
    }
}
