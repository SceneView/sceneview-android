package io.github.sceneview.demo.demos.internal

import io.github.sceneview.demo.demos.internal.RescanZonesTest.Companion.filter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Lining a re-scan up on the scan it completes (#4736). The ray-cast room is scanned once with a
 * stretch of its back wall missed, then a second session, started somewhere else and facing some
 * other way, looks at that wall. Moved by a known turn and shift, with 5 mm of noise, the second
 * session must come back within 2 cm and 1° — or be refused, never merged wrong. Then what must be
 * refused: another room, one flat wall, a floor alone, a bare corner of a square room, a room that
 * looks the same turned round, the part alone, a cloud without normals.
 */
class RescanRegistrationTest {

    // ── Lining up ──────────────────────────────────────────────────────────────────────────────

    @Test
    fun `a second session moved by a known turn and shift lines up within 2 cm and 1 degree`() {
        for (offset in OFFSETS) {
            val found = RescanRegistrar.register(scan, session(offset), listOf(ZONE))
            assertWithin("offset $offset", offset, found)
        }
    }

    @Test
    fun `the scan read once lines up every session the same`() {
        val reference = requireNotNull(RescanReference.of(scan))
        for (offset in OFFSETS) assertWithin("offset $offset", offset, RescanRegistrar.register(reference, session(offset), listOf(ZONE)))
    }

    @Test
    fun `random turns and shifts line up within tolerance or are refused, never merged wrong`() {
        val random = Random(SEED)
        val reference = requireNotNull(RescanReference.of(scan))
        var lined = 0
        repeat(RANDOM_TRIALS) { trial ->
            val offset = randomOffset(random)
            val found = RescanRegistrar.register(reference, session(offset, seed = SEED + trial), listOf(ZONE))
            if (found is RescanAlignment.Lined) {
                assertWithin("trial $trial, offset $offset", offset, found)
                lined++
            }
        }
        assertTrue("only $lined of $RANDOM_TRIALS lined up", lined >= RANDOM_TRIALS - 1)
    }

    @Test
    fun `the moved session lands on the scan it completes`() {
        val offset = OFFSETS.first()
        val found = RescanRegistrar.register(scan, session(offset), listOf(ZONE)) as RescanAlignment.Lined
        assertTrue("overlap ${found.overlap}", found.overlap > MIN_GOOD_OVERLAP)
        assertTrue("rms ${found.rmsM}", found.rmsM < MAX_GOOD_RMS_M)
    }

    // ── Refusing ───────────────────────────────────────────────────────────────────────────────

    @Test
    fun `another room is refused wherever it starts`() {
        val bedroom = fuse(RerunSyntheticBedroom.scene, RerunSyntheticBedroom.poses())
        for (offset in OFFSETS) {
            val found = RescanRegistrar.register(scan, offset.inverse().moved(bedroom), listOf(ZONE))
            assertTrue("offset $offset: $found", found is RescanAlignment.Refused)
        }
    }

    @Test
    fun `one flat wall and the floor slide along the wall and are refused`() {
        val wall = rescan.keep { x, y, _, nx, ny, _ -> (y < FLOOR_BAND_M && ny > UP) || (x > WALL_X_M && abs(nx) > UP) }
        assertRefused(RescanRefusal.Unconstrained, RescanRegistrar.register(scan, OFFSETS.first().inverse().moved(wall), listOf(ZONE)))
    }

    @Test
    fun `a floor alone gives no turn and is refused`() {
        val floor = rescan.keep { _, y, _, _, ny, _ -> y < FLOOR_BAND_M && ny > UP }
        assertRefused(RescanRefusal.NoWalls, RescanRegistrar.register(scan, OFFSETS.first().inverse().moved(floor), listOf(ZONE)))
    }

    @Test
    fun `a bare corner of a square room fits all four corners and is refused`() {
        val square = SyntheticScene(listOf(SyntheticShape.Room(Vec3(-2f, 0f, -2f), Vec3(2f, 2.5f, 2f)) { _, _, _ -> PLAIN }))
        val whole = fuse(square, RerunSyntheticRoom.poses(stops = listOf(Vec3(0f, 1.4f, 0f), Vec3(0.5f, 1.3f, -0.4f))))
        val corner = fuse(square, aimed(listOf(Vec3(0.6f, 1.4f, 0.6f)), CORNER_YAW))
        assertRefused(RescanRefusal.Ambiguous, RescanRegistrar.register(whole, OFFSETS[2].inverse().moved(corner)))
    }

    @Test
    fun `a room that looks the same turned round, re-scanned around its missing corner, is refused`() {
        // Turned round, the re-scan lands whole on the far corner, which the scan has; lined up
        // right, its part drops into the hole. Both explain it all: no telling which.
        val hall = SyntheticScene(listOf(SyntheticShape.Room(Vec3(-2.5f, 0f, -1.75f), Vec3(2.5f, 2.5f, 1.75f)) { _, _, _ -> PLAIN }))
        val whole = fuse(hall, RerunSyntheticRoom.poses(stops = listOf(Vec3(0f, 1.4f, 0f), Vec3(0.5f, 1.3f, -0.4f))))
        val part = floatArrayOf(1.4f, 0f, 0.65f, 2.6f, 2.6f, 1.85f)
        val scan = whole.filter { x, y, z -> !RescanRegistrar.inside(part, x, y, z, 0f) }
        val around = whole.filter { x, _, z -> (x - TWIN_X_M) * (x - TWIN_X_M) + (z - TWIN_Z_M) * (z - TWIN_Z_M) <= TWIN_REACH_M * TWIN_REACH_M }
        val reference = requireNotNull(RescanReference.of(scan))
        for ((k, offset) in OFFSETS.withIndex()) {
            assertRefused(RescanRefusal.Ambiguous, RescanRegistrar.register(reference, noisy(offset.inverse().moved(around), SEED + k), listOf(part)))
        }
    }

    @Test
    fun `the part alone, with nothing the scan saw around it, is refused`() {
        val part = rescan.filter { x, y, z -> RescanRegistrar.inside(ZONE, x, y, z, RescanRegistrar.OPEN_MARGIN_M) }
        val found = RescanRegistrar.register(scan, OFFSETS.first().inverse().moved(part), listOf(ZONE))
        assertTrue("$found", found is RescanAlignment.Refused)
    }

    @Test
    fun `a cloud without normals is refused`() {
        val bare = DenseCloud(rescan.positions, rescan.colors, null, rescan.confidences, rescan.levels)
        assertRefused(RescanRefusal.NoNormals, RescanRegistrar.register(scan, bare, listOf(ZONE)))
        assertRefused(RescanRefusal.NoNormals, RescanRegistrar.register(DenseCloud(scan.positions, scan.colors, null, null, null), rescan))
    }

    // ── The move itself ────────────────────────────────────────────────────────────────────────

    @Test
    fun `a move and its inverse cancel out`() {
        for (offset in OFFSETS) {
            val back = offset.inverse()
            val x = 0.7f
            val y = 1.1f
            val z = -0.4f
            val ox = offset.x(x, z)
            val oz = offset.z(x, z)
            assertEquals(x, back.x(ox, oz), EPSILON)
            assertEquals(y, back.y(offset.y(y)), EPSILON)
            assertEquals(z, back.z(ox, oz), EPSILON)
        }
    }

    @Test
    fun `a move turns the box corners with it`() {
        val pose = RescanPose(Math.toRadians(90.0).toFloat(), 1f, 0.5f, 0f)
        val corners = pose.corners(floatArrayOf(0f, 0f, 0f, 1f, 2f, 3f))
        // Corner 7 is (1, 2, 3): a quarter turn takes it to (−3, 2, 1), then the shift.
        assertEquals(-2f, corners[7 * 3], EPSILON)
        assertEquals(2.5f, corners[7 * 3 + 1], EPSILON)
        assertEquals(1f, corners[7 * 3 + 2], EPSILON)
    }

    companion object {
        private const val SEED = 4736
        private const val RANDOM_TRIALS = 12
        private const val NOISE_M = 0.005f
        private const val MAX_TURN_DEG = 1f
        private const val MAX_GAP_M = 0.02f
        private const val MAX_TURN_RAD = PI / 6
        private const val MAX_SHIFT_M = 2f
        private const val MIN_GOOD_OVERLAP = 0.5f
        private const val MAX_GOOD_RMS_M = 0.01f
        private const val FLOOR_BAND_M = 0.1f
        private const val WALL_X_M = 1.95f
        private const val UP = 0.9f
        private const val EPSILON = 1e-4f
        private const val PLAIN = 0xFFDDDDDD.toInt()
        private val CORNER_YAW = (-3 * PI / 4).toFloat()
        private const val TWIN_X_M = 2f
        private const val TWIN_Z_M = 1.25f
        private const val TWIN_REACH_M = 2.5f

        /** The stretch of the back wall the first scan missed: what the second one is for. */
        val ZONE = floatArrayOf(-0.4f, 0f, 1.65f, 1.0f, 2.6f, 1.95f)

        /** Known moves between the two sessions: up to 30° and 2 m, and none. */
        val OFFSETS = listOf(
            RescanPose(Math.toRadians(25.0).toFloat(), 1.6f, 0.3f, -1.1f),
            RescanPose(Math.toRadians(-30.0).toFloat(), -1.2f, -0.2f, 1.5f),
            RescanPose(Math.toRadians(8.0).toFloat(), 0.5f, 0f, 0.4f),
            RescanPose.Identity,
        )

        /** The ray-cast room scanned with the back wall's stretch in [ZONE] missed. */
        val scan: DenseCloud by lazy {
            RescanZonesTest.room.filter { x, _, z -> !(z > ZONE[2] + MISSED_FROM_M && x > ZONE[0] && x < ZONE[3]) }
        }

        /** The second session, in the first one's frame: two stops facing the back wall. */
        val rescan: DenseCloud by lazy {
            fuse(RerunSyntheticRoom.scene, aimed(listOf(Vec3(0.3f, 1.4f, 0.3f), Vec3(-0.4f, 1.3f, 0.7f)), PI.toFloat()))
        }

        private const val MISSED_FROM_M = 0.05f
        private const val AIM_SPREAD_DEG = 60
        private const val AIM_STEP_DEG = 10
        private val PITCHES_DEG = listOf(-40, -12, 18)

        /** Poses from [stops] turning [AIM_SPREAD_DEG] either side of [yaw], at three pitches. */
        fun aimed(stops: List<Vec3>, yaw: Float): List<DebugPose> = buildList {
            for (stop in stops) {
                for (step in -AIM_SPREAD_DEG..AIM_SPREAD_DEG step AIM_STEP_DEG) {
                    for (pitch in PITCHES_DEG) {
                        add(RerunSyntheticRoom.lookPose(stop, yaw + Math.toRadians(step.toDouble()).toFloat(), Math.toRadians(pitch.toDouble()).toFloat()))
                    }
                }
            }
        }

        /** [scene] seen from [poses], fused as a recording fuses it. */
        fun fuse(scene: SyntheticScene, poses: List<DebugPose>): DenseCloud {
            val fusion = DenseFusion()
            for (pose in poses) fusion.add(DepthBackProjection.project(scene.render(pose)))
            return fusion.cloud(minViews = DenseFusion.MIN_VIEWS)
        }

        /** [rescan] as its own session saw it, [offset] away from the first, with noise. */
        fun session(offset: RescanPose, seed: Int = SEED): DenseCloud = noisy(offset.inverse().moved(rescan), seed)

        /** [cloud] with [NOISE_M] of Gaussian noise on every coordinate. */
        fun noisy(cloud: DenseCloud, seed: Int): DenseCloud {
            val random = java.util.Random(seed.toLong())
            val p = FloatArray(cloud.positions.size) { cloud.positions[it] + (random.nextGaussian() * NOISE_M).toFloat() }
            return DenseCloud(p, cloud.colors, cloud.normals, cloud.confidences, cloud.levels)
        }

        /** A turn within 30° and a shift within 2 m, at random. */
        fun randomOffset(random: Random): RescanPose {
            val turn = random.nextDouble(-MAX_TURN_RAD, MAX_TURN_RAD).toFloat()
            val length = random.nextDouble(0.0, MAX_SHIFT_M.toDouble()).toFloat()
            var x: Float
            var y: Float
            var z: Float
            do {
                x = random.nextFloat() * 2 - 1
                y = (random.nextFloat() * 2 - 1) * VERTICAL_SHARE
                z = random.nextFloat() * 2 - 1
            } while (x * x + y * y + z * z < MIN_DIRECTION)
            val l = sqrt(x * x + y * y + z * z)
            return RescanPose(turn, x / l * length, y / l * length, z / l * length)
        }

        private const val VERTICAL_SHARE = 0.3f
        private const val MIN_DIRECTION = 0.01f

        /** [this] with only the points [keep] keeps, read with their normals. */
        fun DenseCloud.keep(keep: (x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) -> Boolean): DenseCloud {
            val n = requireNotNull(normals)
            val kept = (0 until count).filter {
                keep(positions[it * 3], positions[it * 3 + 1], positions[it * 3 + 2], n[it * 3], n[it * 3 + 1], n[it * 3 + 2])
            }.toSet()
            var i = 0
            return filter { _, _, _ -> kept.contains(i++) }
        }

        /** [found] is lined up, and moves the session's points within 2 cm and 1° of where [truth] does. */
        fun assertWithin(label: String, truth: RescanPose, found: RescanAlignment) {
            assertTrue("$label: $found", found is RescanAlignment.Lined)
            val pose = (found as RescanAlignment.Lined).pose
            val turn = abs(pose.turnFrom(truth))
            assertTrue("$label: turned $turn° off ($pose)", turn <= MAX_TURN_DEG)
            val moved = truth.inverse().moved(rescan)
            var worst = 0f
            for (i in 0 until moved.count) {
                worst = maxOf(worst, pose.gap(truth, moved.positions[i * 3], moved.positions[i * 3 + 1], moved.positions[i * 3 + 2]))
            }
            assertTrue("$label: a point lands ${worst * MM} mm off ($pose)", worst <= MAX_GAP_M)
        }

        private const val MM = 1000f

        fun assertRefused(why: RescanRefusal, found: RescanAlignment) {
            assertTrue("expected refused $why, got $found", found is RescanAlignment.Refused && found.why == why)
        }
    }
}
