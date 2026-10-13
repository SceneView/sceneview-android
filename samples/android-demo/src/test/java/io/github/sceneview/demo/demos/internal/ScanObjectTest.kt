package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Room Scan's tap-to-box: the voxel index, the ray, the floor / wall / ceiling sort, the region
 * growing and the oriented box — pure Kotlin on a synthetic room, no ARCore, no Filament.
 */
class ScanObjectTest {
    // ── The ray ───────────────────────────────────────────────────────────────

    @Test
    fun `a ray fired at a cube lands on it`() {
        val surfel = furnished.objects.hit(EYE, toward(Vec3(CUBE_X, CUBE_SIDE, CUBE_Z)))
        assertTrue("the ray hit nothing", surfel >= 0)
        assertEquals(ScanSurface.Object, furnished.objects.surfaceOf(surfel))
        val p = furnished.cloud.point(surfel)
        assertTrue("landed at $p, off the cube", abs(p.x - CUBE_X) <= CUBE_SIDE / 2 + VOXEL)
        assertTrue("landed at $p, off the cube", abs(p.z - CUBE_Z) <= CUBE_SIDE / 2 + VOXEL)
        assertTrue("landed at $p, off the cube", p.y in 0f..CUBE_SIDE + VOXEL)
    }

    @Test
    fun `a ray that leaves the scan hits nothing`() {
        assertEquals(-1, furnished.objects.hit(Vec3(0f, 5f, 0f), Vec3.Up))
        assertSame(ScanPick.Empty, furnished.objects.pick(Vec3(0f, 5f, 0f), Vec3.Up))
        assertEquals(-1, ScanObjects.of(DenseCloud.Empty).hit(EYE, Vec3.Up))
    }

    // ── The box ───────────────────────────────────────────────────────────────

    @Test
    fun `a cube on the floor gives its three dimensions within a voxel`() {
        val found = furnished.objects.pick(EYE, toward(Vec3(CUBE_X, CUBE_SIDE, CUBE_Z))) as ScanPick.Found
        val box = found.box
        assertEquals(CUBE_SIDE, box.length, VOXEL)
        assertEquals(CUBE_SIDE, box.depth, VOXEL)
        assertEquals(CUBE_SIDE, box.height, VOXEL)
        assertEquals(CUBE_X, box.centerX, VOXEL)
        assertEquals(CUBE_Z, box.centerZ, VOXEL)
        assertEquals("it stands on the floor", 0f, box.baseY, VOXEL)
        // What lights up is the cube, and nothing but the cube.
        assertTrue("only ${found.surfels.size} surfels lit", found.surfels.size > 3_000)
        for (i in found.surfels) {
            val p = furnished.cloud.point(i)
            val reach = CUBE_SIDE / 2 + VOXEL
            assertTrue("lit $p", abs(p.x - CUBE_X) <= reach && abs(p.z - CUBE_Z) <= reach)
        }
    }

    @Test
    fun `a box turned on the floor is measured along its own faces`() {
        val found = furnished.objects.pick(EYE, toward(Vec3(TURNED_X, TURNED_HEIGHT, TURNED_Z))) as ScanPick.Found
        val box = found.box
        assertEquals(TURNED_LENGTH, box.length, VOXEL)
        assertEquals(TURNED_DEPTH, box.depth, VOXEL)
        assertEquals(TURNED_HEIGHT, box.height, VOXEL)
        assertEquals(TURNED_X, box.centerX, VOXEL)
        assertEquals(TURNED_Z, box.centerZ, VOXEL)
        // A box has no front: its yaw is the same half a turn round.
        val off = abs(Math.toDegrees(box.yaw.toDouble()) - TURNED_YAW_DEG).mod(180.0)
        assertTrue("yaw ${Math.toDegrees(box.yaw.toDouble())}°", minOf(off, 180.0 - off) < 1.5)
    }

    @Test
    fun `a box lists its corners round the floor then above, and its top where the label goes`() {
        val box = ScanBox(centerX = 1f, centerZ = 2f, baseY = 0.1f, yaw = 0f, length = 2f, depth = 1f, height = 0.5f)
        val corners = box.corners()
        assertEquals(Vec3(0f, 0.1f, 1.5f), corners[0])
        assertEquals(Vec3(2f, 0.1f, 1.5f), corners[1])
        assertEquals(Vec3(2f, 0.1f, 2.5f), corners[2])
        assertEquals(Vec3(0f, 0.1f, 2.5f), corners[3])
        assertEquals(Vec3(2f, 0.6f, 2.5f), corners[6])
        assertEquals(Vec3(1f, 0.6f, 2f), box.top)
        assertEquals(12, ScanBox.EDGES.size)
    }

    // ── What is not an object ─────────────────────────────────────────────────

    @Test
    fun `a tap on the floor gives no box`() {
        val ray = toward(Vec3(1.2f, 0f, -1.1f))
        assertEquals(ScanSurface.Floor, furnished.objects.surfaceOf(furnished.objects.hit(EYE, ray)))
        assertSame(ScanPick.Empty, furnished.objects.pick(EYE, ray))
    }

    @Test
    fun `a tap on a wall or on the ceiling gives no box`() {
        val wall = toward(Vec3(HALF_ROOM, 1.3f, -0.5f))
        assertEquals(ScanSurface.Wall, furnished.objects.surfaceOf(furnished.objects.hit(EYE, wall)))
        assertSame(ScanPick.Empty, furnished.objects.pick(EYE, wall))
        val ceiling = toward(Vec3(0.3f, ROOM_HEIGHT, 0.2f))
        assertEquals(ScanSurface.Ceiling, furnished.objects.surfaceOf(furnished.objects.hit(EYE, ceiling)))
        assertSame(ScanPick.Empty, furnished.objects.pick(EYE, ceiling))
    }

    @Test
    fun `the floor of a noisy scan is as thick as the scan drew it`() {
        // A phone's depth draws a floor as several sheets, centimetres apart.
        val scene = Scene()
        scene.room(HALF_ROOM, HALF_ROOM, ROOM_HEIGHT)
        for (sheet in 1..3) scene.floor(HALF_ROOM, HALF_ROOM, y = sheet * 0.03f)
        scene.box(CUBE_X, CUBE_Z, yawDeg = 0f, length = CUBE_SIDE, depth = CUBE_SIDE, height = CUBE_SIDE)
        val objects = ScanObjects.of(scene.cloud())
        assertTrue("tolerance ${objects.floorTolerance}", objects.floorTolerance in 0.06f..0.14f)
        assertSame(ScanPick.Empty, objects.pick(EYE, toward(Vec3(1.2f, 0f, -1.1f))))
        val box = (objects.pick(EYE, toward(Vec3(CUBE_X, CUBE_SIDE, CUBE_Z))) as ScanPick.Found).box
        assertEquals(CUBE_SIDE, box.length, VOXEL)
        assertEquals(CUBE_SIDE, box.depth, VOXEL)
        assertEquals("measured from the ground, not from where the floor's noise stops", CUBE_SIDE, box.height, VOXEL)
        // A clean scan's floor is a voxel thick.
        assertEquals(0.03f, furnished.objects.floorTolerance, 0.001f)
    }

    @Test
    fun `a speck of depth noise gets no box and does not hide the furniture behind it`() {
        val top = Vec3(CUBE_X, CUBE_SIDE, CUBE_Z)
        val scene = Scene()
        scene.room(HALF_ROOM, HALF_ROOM, ROOM_HEIGHT)
        scene.box(CUBE_X, CUBE_Z, yawDeg = 0f, length = CUBE_SIDE, depth = CUBE_SIDE, height = CUBE_SIDE)
        // Hanging in the room, half-way between the eye and the cube: hundreds of voxels, a hand wide.
        scene.speck(EYE + (top - EYE) * 0.5f, SPECK_SIDE)
        val cloud = scene.cloud()
        val objects = ScanObjects.of(cloud)
        val first = objects.hit(EYE, toward(top))
        assertEquals("the speck is an object's stuff", ScanSurface.Object, objects.surfaceOf(first))
        assertTrue("and the first thing the ray meets", cloud.point(first).y > CUBE_SIDE + SPECK_SIDE)
        val box = (objects.pick(EYE, toward(top)) as ScanPick.Found).box
        assertEquals(CUBE_SIDE, box.length, VOXEL)
        assertEquals(CUBE_SIDE, box.depth, VOXEL)
        assertEquals(CUBE_SIDE, box.height, VOXEL)
        assertEquals(CUBE_X, box.centerX, VOXEL)
    }

    @Test
    fun `a speck of depth noise over the bare floor gives no box`() {
        val onTheFloor = Vec3(1.2f, 0f, -1.1f)
        val scene = Scene()
        scene.room(HALF_ROOM, HALF_ROOM, ROOM_HEIGHT)
        scene.speck(EYE + (onTheFloor - EYE) * 0.5f, SPECK_SIDE)
        val objects = ScanObjects.of(scene.cloud())
        assertEquals(ScanSurface.Object, objects.surfaceOf(objects.hit(EYE, toward(onTheFloor))))
        assertSame(ScanPick.Empty, objects.pick(EYE, toward(onTheFloor)))
        // The smallest thing that is furniture: a stool, just over the speck's size.
        scene.box(-1.2f, 1.1f, yawDeg = 0f, length = STOOL_SIDE, depth = STOOL_SIDE, height = STOOL_SIDE)
        val stool = ScanObjects.of(scene.cloud()).pick(EYE, toward(Vec3(-1.2f, STOOL_SIDE, 1.1f)))
        assertEquals(STOOL_SIDE, (stool as ScanPick.Found).box.height, VOXEL)
    }

    // ── One object, not its neighbours ────────────────────────────────────────

    @Test
    fun `two pieces of furniture a hand apart are two objects, stray points between them or not`() {
        val scene = Scene()
        scene.room(HALF_ROOM, HALF_ROOM, ROOM_HEIGHT)
        scene.box(-0.3f, 0f, yawDeg = 0f, length = 0.5f, depth = 0.5f, height = 0.5f)
        scene.box(0.3f, 0f, yawDeg = 0f, length = 0.5f, depth = 0.5f, height = 0.5f)
        // The thread of depth noise a scan throws across the 10 cm between them.
        scene.line(Vec3(-0.05f, 0.3f, 0.01f), Vec3(0.05f, 0.3f, 0.01f))
        val objects = ScanObjects.of(scene.cloud())
        for (x in floatArrayOf(-0.3f, 0.3f)) {
            val box = (objects.pick(EYE, toward(Vec3(x, 0.5f, 0f))) as ScanPick.Found).box
            assertEquals(0.5f, box.length, VOXEL)
            assertEquals(0.5f, box.depth, VOXEL)
            assertEquals(x, box.centerX, VOXEL)
        }
    }

    @Test
    fun `an object too long to be one piece of furniture says tap closer, never a box`() {
        val scene = Scene()
        scene.room(3f, HALF_ROOM, ROOM_HEIGHT)
        scene.box(0f, 0f, yawDeg = 0f, length = 3.6f, depth = 0.5f, height = 0.8f)
        val objects = ScanObjects.of(scene.cloud())
        assertSame(ScanPick.TapCloser, objects.pick(EYE, toward(Vec3(0.4f, 0.8f, 0f))))
    }

    @Test
    fun `a region that outgrows its budget says tap closer, never a box`() {
        val ray = toward(Vec3(CUBE_X, CUBE_SIDE, CUBE_Z))
        assertSame(ScanPick.TapCloser, furnished.objects.pick(EYE, ray, maxVoxels = 1_000))
        assertTrue(furnished.objects.pick(EYE, ray) is ScanPick.Found)
    }

    @Test
    fun `a replay picks only what it has drawn so far`() {
        // The room was scanned first, the furniture after: before it, the tap falls through to the floor.
        val ray = toward(Vec3(CUBE_X, CUBE_SIDE, CUBE_Z))
        assertSame(ScanPick.Empty, furnished.objects.pick(EYE, ray, limit = furnished.roomCount))
        assertTrue(furnished.objects.pick(EYE, ray, limit = furnished.cloud.count) is ScanPick.Found)
    }

    // ── The camera ────────────────────────────────────────────────────────────

    @Test
    fun `the tap camera casts through the pixel a point is drawn at`() {
        val camera = ScanCamera(Vec3(0.4f, 1.2f, 3f), Vec3(-0.2f, -0.3f, -1f), tan(PI / 8).toFloat(), 1080, 1920)
        val centre = camera.ray(540f, 960f)
        val forward = Vec3(-0.2f, -0.3f, -1f).normalized()
        assertEquals(1f, centre.dot(forward), 1e-5f)
        // Right of the screen is right of the view, the top is up.
        assertTrue(camera.ray(1080f, 960f).x > centre.x)
        assertTrue(camera.ray(540f, 0f).y > centre.y)
        for ((x, y) in listOf(0f to 0f, 1080f to 1920f, 200f to 1500f, 870f to 333f)) {
            val drawn = camera.project(camera.eye + camera.ray(x, y) * 2.5f)!!
            assertEquals(x, drawn.first, 0.05f)
            assertEquals(y, drawn.second, 0.05f)
        }
        assertNull("behind the camera", camera.project(camera.eye - forward))
    }

    // ── The app's own pipeline ────────────────────────────────────────────────

    @Test
    fun `in a room fused from depth frames, the cabinet and the sofa get their boxes and the room does not`() {
        val fusion = DenseFusion()
        RerunSyntheticRoom.frames().forEach { fusion.add(DepthBackProjection.project(it)) }
        val objects = ScanObjects.of(fusion.cloud())
        val eye = Vec3(0f, 1.4f, 0.2f)
        fun pick(at: Vec3) = objects.pick(eye, (at - eye).normalized())

        // The cabinet is 1.0 × 0.45 × 1.6 m against the right wall, the sofa 1.4 × 0.85 × 0.9 m against the back one.
        val cabinet = (pick(Vec3(1.55f, 0.8f, 1.1f)) as ScanPick.Found).box
        assertEquals(1f, cabinet.length, FUSED_M)
        assertEquals(0.45f, cabinet.depth, FUSED_M)
        assertEquals(1.6f, cabinet.height, FUSED_M)
        val sofa = (pick(Vec3(-1f, 0.45f, -1.2f)) as ScanPick.Found).box
        assertEquals(1.4f, sofa.length, FUSED_M)
        assertEquals(0.85f, sofa.depth, FUSED_M)
        assertEquals(0.9f, sofa.height, FUSED_M)

        assertSame("the floor", ScanPick.Empty, pick(Vec3(1.5f, 0f, -1.2f)))
        assertSame("the rug", ScanPick.Empty, pick(Vec3(-0.2f, 0f, 0.6f)))
        assertSame("the left wall", ScanPick.Empty, pick(Vec3(-2f, 1.5f, 0.3f)))
        assertSame("the ceiling", ScanPick.Empty, pick(Vec3(0.2f, 2.6f, -0.4f)))
    }

    /** A scan drawn by hand: every surface sampled finer than a voxel, with its normal. */
    private class Scene {
        private val positions = ArrayList<Float>()
        private val normals = ArrayList<Float>()
        val count: Int get() = positions.size / 3

        /** A room centred on the origin, floor at y = 0: every face looks in. */
        fun room(halfX: Float, halfZ: Float, height: Float) {
            floor(halfX, halfZ, 0f)
            rect(Vec3(-halfX, height, -halfZ), Vec3(2 * halfX, 0f, 0f), Vec3(0f, 0f, 2 * halfZ), Vec3(0f, -1f, 0f))
            rect(Vec3(-halfX, 0f, -halfZ), Vec3(2 * halfX, 0f, 0f), Vec3(0f, height, 0f), Vec3(0f, 0f, 1f))
            rect(Vec3(-halfX, 0f, halfZ), Vec3(2 * halfX, 0f, 0f), Vec3(0f, height, 0f), Vec3(0f, 0f, -1f))
            rect(Vec3(-halfX, 0f, -halfZ), Vec3(0f, 0f, 2 * halfZ), Vec3(0f, height, 0f), Vec3(1f, 0f, 0f))
            rect(Vec3(halfX, 0f, -halfZ), Vec3(0f, 0f, 2 * halfZ), Vec3(0f, height, 0f), Vec3(-1f, 0f, 0f))
        }

        fun floor(halfX: Float, halfZ: Float, y: Float) =
            rect(Vec3(-halfX, y, -halfZ), Vec3(2 * halfX, 0f, 0f), Vec3(0f, 0f, 2 * halfZ), Vec3.Up)

        /** A box standing on the floor: its top and four sides, as a scan sees it. */
        fun box(x: Float, z: Float, yawDeg: Float, length: Float, depth: Float, height: Float) {
            val yaw = Math.toRadians(yawDeg.toDouble()).toFloat()
            val u = Vec3(cos(yaw), 0f, sin(yaw))
            val v = Vec3(-sin(yaw), 0f, cos(yaw))
            val corner = Vec3(x, 0f, z) - u * (length / 2) - v * (depth / 2)
            val up = Vec3(0f, height, 0f)
            rect(corner + up, u * length, v * depth, Vec3.Up)
            rect(corner, u * length, up, v * -1f)
            rect(corner + v * depth, u * length, up, v)
            rect(corner, v * depth, up, u * -1f)
            rect(corner + u * length, v * depth, up, u)
        }

        fun line(from: Vec3, to: Vec3) {
            val steps = ((to - from).length() / STEP).toInt()
            for (k in 0..steps) add(from + (to - from) * (k.toFloat() / steps), Vec3.Up)
        }

        /** A clump of points round [center], [side] wide in every direction, filled through. */
        fun speck(center: Vec3, side: Float) {
            val n = (side / STEP).toInt()
            val corner = center - Vec3(side / 2, side / 2, side / 2)
            for (a in 0..n) for (b in 0..n) for (c in 0..n) {
                add(corner + Vec3(a.toFloat(), b.toFloat(), c.toFloat()) * (side / n), Vec3.Up)
            }
        }

        private fun rect(origin: Vec3, u: Vec3, v: Vec3, normal: Vec3) {
            val nu = (u.length() / STEP).toInt()
            val nv = (v.length() / STEP).toInt()
            for (a in 0..nu) for (b in 0..nv) add(origin + u * (a.toFloat() / nu) + v * (b.toFloat() / nv), normal)
        }

        private fun add(p: Vec3, normal: Vec3) {
            positions += p.x
            positions += p.y
            positions += p.z
            normals += normal.x
            normals += normal.y
            normals += normal.z
        }

        fun cloud() = DenseCloud(positions.toFloatArray(), IntArray(count) { GREY }, normals.toFloatArray())
    }

    private class Furnished(val cloud: DenseCloud, val roomCount: Int) {
        val objects: ScanObjects = ScanObjects.of(cloud)
    }

    private companion object {
        const val VOXEL = 0.02f
        const val STEP = 0.012f
        const val GREY = 0xFF808080.toInt()
        const val HALF_ROOM = 2f
        const val ROOM_HEIGHT = 2.6f

        const val CUBE_SIDE = 0.6f
        const val CUBE_X = -1f
        const val CUBE_Z = -0.9f

        const val SPECK_SIDE = 0.14f
        const val STOOL_SIDE = 0.36f

        const val TURNED_LENGTH = 0.9f
        const val TURNED_DEPTH = 0.5f
        const val TURNED_HEIGHT = 0.7f
        const val TURNED_X = 0.9f
        const val TURNED_Z = 0.8f
        const val TURNED_YAW_DEG = 30.0

        /** What depth at 160 × 90 leaves of a metre once fused: a few voxels. */
        const val FUSED_M = 0.08f

        /** Where the taps are cast from: head height, in the middle of the room. */
        val EYE = Vec3(0f, 1.5f, 0f)

        fun toward(point: Vec3): Vec3 = (point - EYE).normalized()

        fun DenseCloud.point(i: Int) = Vec3(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2])

        /** A 4 × 4 m room with a cube and a turned box in it, the room scanned first. */
        val furnished: Furnished by lazy {
            val scene = Scene()
            scene.room(HALF_ROOM, HALF_ROOM, ROOM_HEIGHT)
            val roomCount = scene.count
            scene.box(CUBE_X, CUBE_Z, yawDeg = 0f, length = CUBE_SIDE, depth = CUBE_SIDE, height = CUBE_SIDE)
            scene.box(TURNED_X, TURNED_Z, TURNED_YAW_DEG.toFloat(), TURNED_LENGTH, TURNED_DEPTH, TURNED_HEIGHT)
            Furnished(scene.cloud(), roomCount)
        }
    }
}
