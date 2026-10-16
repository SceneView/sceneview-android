package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.internal.ArDebugTrace
import io.github.sceneview.demo.demos.internal.DebugPlane
import io.github.sceneview.demo.demos.internal.DebugPlaneKind
import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.ReplayLens
import io.github.sceneview.demo.demos.internal.Vec3
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A camera photo of the session and the pose it was taken from: "here" in it is a 3D ray. */
class SpatialKeyframe(val index: Int, val time: Float, val pose: DebugPose, val path: String)

/**
 * A box turned [yawDegrees] about +Y — the way a Filament `Rotation(y = yawDegrees)` turns a node:
 * local +X lands on world (cos, 0, −sin). [size] is (width along local X, height, depth along
 * local Z); [center] is the box's middle.
 */
data class OrientedBox(val center: Vec3, val size: Vec3, val yawDegrees: Float) {
    private val c get() = cos(Math.toRadians(yawDegrees.toDouble())).toFloat()
    private val s get() = sin(Math.toRadians(yawDegrees.toDouble())).toFloat()

    /** World → box-local (centred). */
    fun toLocal(x: Float, y: Float, z: Float): Vec3 {
        val dx = x - center.x
        val dz = z - center.z
        return Vec3(dx * c - dz * s, y - center.y, dx * s + dz * c)
    }

    /** Box-local (centred) → world. */
    fun toWorld(lx: Float, ly: Float, lz: Float): Vec3 =
        Vec3(center.x + lx * c + lz * s, center.y + ly, center.z - lx * s + lz * c)

    fun contains(x: Float, y: Float, z: Float, margin: Float = 0f): Boolean {
        val l = toLocal(x, y, z)
        return abs(l.x) <= size.x / 2 + margin && abs(l.y) <= size.y / 2 + margin && abs(l.z) <= size.z / 2 + margin
    }

    /** Whether the segment [a]→[b] passes through the box (slab test in its frame), ends excluded. */
    fun blocks(a: Vec3, b: Vec3): Boolean {
        val la = toLocal(a.x, a.y, a.z)
        val lb = toLocal(b.x, b.y, b.z)
        var t0 = 0f
        var t1 = 1f - SEGMENT_END_SLACK
        val start = floatArrayOf(la.x, la.y, la.z)
        val delta = floatArrayOf(lb.x - la.x, lb.y - la.y, lb.z - la.z)
        val half = floatArrayOf(size.x / 2, size.y / 2, size.z / 2)
        for (axis in 0 until 3) {
            if (abs(delta[axis]) < 1e-6f) {
                if (abs(start[axis]) > half[axis]) return false
                continue
            }
            var near = (-half[axis] - start[axis]) / delta[axis]
            var far = (half[axis] - start[axis]) / delta[axis]
            if (near > far) near = far.also { far = near }
            t0 = maxOf(t0, near)
            t1 = minOf(t1, far)
            if (t0 > t1) return false
        }
        return true
    }

    /** The footprint's four corners on the floor plane, world xz pairs, counter-clockwise. */
    fun footprint(): FloatArray {
        val hx = size.x / 2
        val hz = size.z / 2
        val out = FloatArray(8)
        listOf(-hx to -hz, hx to -hz, hx to hz, -hx to hz).forEachIndexed { i, (lx, lz) ->
            val w = toWorld(lx, 0f, lz)
            out[i * 2] = w.x
            out[i * 2 + 1] = w.z
        }
        return out
    }

    private companion object {
        /** A segment ending on the box's face does not count as passing through it. */
        const val SEGMENT_END_SLACK = 0.02f
    }
}

/** What [SpatialSession.raycast] hit: a plane, the cloud, or nothing within [SpatialSession.MAX_RAY_M]. */
data class RayHit(val origin: Vec3, val point: Vec3?, val target: String) {
    val distance: Float get() = point?.let { (it - origin).length() } ?: 0f
}

/** The points of the cloud inside a box, in its local frame. */
class BoxPoints(val box: OrientedBox, val local: FloatArray) {
    val count: Int get() = local.size / 3
}

/**
 * The recorded AR session, as the model queries it: the colored feature cloud, ARCore's planes,
 * the posed keyframes and the floor. Built once from the Rerun replay ([of]); immutable.
 *
 * Heights the model sees are above the floor ([floorY]); positions are the session's own world
 * frame, metres, Y up.
 */
class SpatialSession(
    /** Flat xyz, metres, world frame. */
    val points: FloatArray,
    /** `0xFFRRGGBB` per point, `0` without a colour; `null` when the session has none. */
    val colors: IntArray?,
    val planes: List<DebugPlane>,
    val keyframes: List<SpatialKeyframe>,
    val lens: ReplayLens,
    val floorY: Float,
    private val poseTimes: FloatArray,
    private val poses: List<DebugPose>,
    /** Unit normals per point, flat xyz, when the scan is a fused dense cloud; `null` for the sparse feature cloud. */
    val normals: FloatArray? = null,
) {
    val pointCount: Int get() = points.size / 3

    val duration: Float get() = poseTimes.lastOrNull() ?: 0f

    val floorPlane: DebugPlane? get() =
        planes.filter { it.kind == DebugPlaneKind.Floor }.minByOrNull { abs(planeHeight(it) - floorY) }

    /** The camera pose at [time] seconds: the recorded one nearest to it. */
    fun cameraPoseAt(time: Float): DebugPose {
        if (poses.isEmpty()) return DebugPose(0f, 0f, 0f)
        var lo = 0
        var hi = poseTimes.size - 1
        while (lo < hi) {
            val mid = (lo + hi) / 2
            if (poseTimes[mid] < time) lo = mid + 1 else hi = mid
        }
        val best = if (lo > 0 && abs(poseTimes[lo - 1] - time) < abs(poseTimes[lo] - time)) lo - 1 else lo
        return poses[best]
    }

    fun keyframe(index: Int): SpatialKeyframe? = keyframes.getOrNull(index)

    /**
     * The ray through normalised image point ([u], [v]) of keyframe [frame] — u right, v down,
     * both 0…1 — from the camera's pinhole: the recording's own intrinsics ([lens]).
     */
    fun rayOf(frame: SpatialKeyframe, u: Float, v: Float): Pair<Vec3, Vec3> {
        val lx = (u * 2f - 1f) * lens.halfWidthPerDepth
        val ly = -(v * 2f - 1f) * lens.halfHeightPerDepth
        val dir = frame.pose.rotate(lx, ly, -1f).normalized()
        return frame.pose.position to dir
    }

    /** Casts the ray of ([u], [v]) in keyframe [frame] against the planes and the cloud. */
    fun raycast(frame: SpatialKeyframe, u: Float, v: Float): RayHit {
        val (origin, dir) = rayOf(frame, u, v)
        var bestT = MAX_RAY_M
        var target = "nothing"
        for (plane in planes) {
            val t = intersectPlane(plane, origin, dir) ?: continue
            if (t < bestT) {
                bestT = t
                target = "plane ${plane.id} (${kindName(plane.kind)})"
            }
        }
        // The cloud: the nearest point within a few centimetres of the ray, in front of any plane.
        for (i in 0 until pointCount) {
            val p = Vec3(points[i * 3], points[i * 3 + 1], points[i * 3 + 2])
            val t = (p - origin).dot(dir)
            if (t <= MIN_RAY_M || t >= bestT) continue
            val off = (p - (origin + dir * t)).length()
            if (off <= RAY_POINT_RADIUS_M) {
                bestT = t
                target = "points"
            }
        }
        return RayHit(origin, if (bestT < MAX_RAY_M) origin + dir * bestT else null, target)
    }

    /** The cloud's points inside [box] grown by [margin] on every side, in the box's frame. */
    fun pointsIn(box: OrientedBox, margin: Float = 0f): BoxPoints {
        val out = ArrayList<Float>()
        for (i in 0 until pointCount) {
            val x = points[i * 3]
            val y = points[i * 3 + 1]
            val z = points[i * 3 + 2]
            if (box.contains(x, y, z, margin)) {
                val l = box.toLocal(x, y, z)
                out += l.x
                out += l.y
                out += l.z
            }
        }
        return BoxPoints(box, out.toFloatArray())
    }

    /** Whether floor point ([x], [z]) lies inside ARCore's floor polygon. */
    fun onFloor(x: Float, z: Float): Boolean = floorPlane?.let { insidePolygonXz(it.polygon, x, z) } ?: false

    /** The plane's mean height, world Y. */
    fun planeHeight(plane: DebugPlane): Float {
        var sum = 0f
        for (i in 0 until plane.vertexCount) sum += plane.polygon[i * 3 + 1]
        return if (plane.vertexCount == 0) 0f else sum / plane.vertexCount
    }

    fun planeCentroid(plane: DebugPlane): Vec3 {
        var x = 0f
        var y = 0f
        var z = 0f
        for (i in 0 until plane.vertexCount) {
            x += plane.polygon[i * 3]
            y += plane.polygon[i * 3 + 1]
            z += plane.polygon[i * 3 + 2]
        }
        val n = plane.vertexCount.coerceAtLeast(1).toFloat()
        return Vec3(x / n, y / n, z / n)
    }

    /** The plane's unit normal, Newell's method over its polygon. */
    fun planeNormal(plane: DebugPlane): Vec3 {
        var nx = 0f
        var ny = 0f
        var nz = 0f
        val p = plane.polygon
        val n = plane.vertexCount
        for (i in 0 until n) {
            val j = (i + 1) % n
            nx += (p[i * 3 + 1] - p[j * 3 + 1]) * (p[i * 3 + 2] + p[j * 3 + 2])
            ny += (p[i * 3 + 2] - p[j * 3 + 2]) * (p[i * 3] + p[j * 3])
            nz += (p[i * 3] - p[j * 3]) * (p[i * 3 + 1] + p[j * 3 + 1])
        }
        val v = Vec3(nx, ny, nz)
        return if (v.length() < 1e-6f) Vec3.Up else v.normalized()
    }

    /** Distance along the ray to [plane], or `null` when it misses the polygon. */
    private fun intersectPlane(plane: DebugPlane, origin: Vec3, dir: Vec3): Float? {
        val n = planeNormal(plane)
        val c = planeCentroid(plane)
        val denom = n.dot(dir)
        if (abs(denom) < 1e-5f) return null
        val t = (c - origin).dot(n) / denom
        if (t <= MIN_RAY_M) return null
        val hit = origin + dir * t
        // In the plane's own 2D basis, then point-in-polygon.
        val u = (if (abs(n.y) > 0.9f) Vec3(1f, 0f, 0f) else Vec3.Up).cross(n).normalized()
        val w = n.cross(u)
        val p = plane.polygon
        val count = plane.vertexCount
        val flat = FloatArray(count * 2)
        for (i in 0 until count) {
            val d = Vec3(p[i * 3], p[i * 3 + 1], p[i * 3 + 2]) - c
            flat[i * 2] = d.dot(u)
            flat[i * 2 + 1] = d.dot(w)
        }
        val h = hit - c
        return if (insidePolygon2d(flat, h.dot(u), h.dot(w))) t else null
    }

    companion object {
        const val MAX_RAY_M = 8f
        private const val MIN_RAY_M = 0.05f
        private const val RAY_POINT_RADIUS_M = 0.04f

        /** The session behind a replay: the trace's final state, the camera's lens. */
        fun of(trace: ArDebugTrace, lens: ReplayLens, floorY: Float? = null): SpatialSession {
            val last = trace.frameAt(trace.duration)
            val poseTimes = FloatArray(trace.poseCount) { trace.poseTime(it) }
            val poses = List(trace.poseCount) { trace.pose(it) }
            val floor = floorY ?: last.planes.filter { it.kind == DebugPlaneKind.Floor }
                .minOfOrNull { plane ->
                    (0 until plane.vertexCount).map { plane.polygon[it * 3 + 1] }.average().toFloat()
                }
                ?: 0f
            val provisional = SpatialSession(
                last.mapPoints, last.mapPointColors, last.planes, emptyList(), lens, floor, poseTimes, poses,
            )
            val keyframes = List(trace.imageCount) { i ->
                val t = trace.imageTime(i)
                SpatialKeyframe(i, t, provisional.cameraPoseAt(t), trace.imagePath(i))
            }
            return SpatialSession(
                last.mapPoints, last.mapPointColors, last.planes, keyframes, lens, floor, poseTimes, poses,
            )
        }

        fun kindName(kind: DebugPlaneKind): String = when (kind) {
            DebugPlaneKind.Floor -> "horizontal up"
            DebugPlaneKind.Ceiling -> "horizontal down"
            DebugPlaneKind.Wall -> "vertical"
            DebugPlaneKind.Unknown -> "unknown"
        }

        /** Point-in-polygon on the xz of a flat xyz polygon. */
        fun insidePolygonXz(polygon: FloatArray, x: Float, z: Float): Boolean {
            val n = polygon.size / 3
            val flat = FloatArray(n * 2)
            for (i in 0 until n) {
                flat[i * 2] = polygon[i * 3]
                flat[i * 2 + 1] = polygon[i * 3 + 2]
            }
            return insidePolygon2d(flat, x, z)
        }

        fun insidePolygon2d(flat: FloatArray, x: Float, y: Float): Boolean {
            val n = flat.size / 2
            var inside = false
            var j = n - 1
            for (i in 0 until n) {
                val xi = flat[i * 2]
                val yi = flat[i * 2 + 1]
                val xj = flat[j * 2]
                val yj = flat[j * 2 + 1]
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
                j = i
            }
            return inside
        }

        fun distance2d(ax: Float, az: Float, bx: Float, bz: Float): Float {
            val dx = bx - ax
            val dz = bz - az
            return sqrt(dx * dx + dz * dz)
        }
    }
}
