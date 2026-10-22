package io.github.sceneview.demo.demos.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The plane bake's decoded photos: each one decoded once, on the first plane that paints from it,
 * and let go after the last, so a room's bake holds a handful of photos at a time instead of every
 * photo it decoded, until the build ends.
 */
class BakePhotoCacheTest {

    @Test
    fun `a photo two planes paint from is decoded once`() {
        val decoded = ArrayList<Int>()
        val cache = BakePhotoCache(listOf(listOf(0, 1), listOf(1, 2))) { decoded += it; "photo $it" }
        assertEquals(listOf("photo 0", "photo 1"), cache.photosFor(0))
        assertEquals(listOf("photo 1", "photo 2"), cache.photosFor(1))
        assertEquals(listOf(0, 1, 2), decoded)
        assertEquals(3, cache.loads)
    }

    @Test
    fun `a photo is let go after the last plane that paints from it`() {
        val cache = BakePhotoCache(listOf(listOf(0, 1), listOf(1, 2), listOf(3))) { "photo $it" }
        cache.photosFor(0)
        assertEquals(setOf(1), cache.held)
        cache.photosFor(1)
        assertEquals(emptySet<Int>(), cache.held)
        cache.photosFor(2)
        assertEquals(emptySet<Int>(), cache.held)
        // Two photos at once at most, never all four the bake decodes.
        assertEquals(2, cache.peak)
        assertEquals(4, cache.loads)
    }

    @Test
    fun `a photo that does not decode is not decoded again, and does not count as held`() {
        val decoded = ArrayList<Int>()
        val cache = BakePhotoCache(listOf(listOf(0, 1), listOf(1), listOf(1, 2))) {
            decoded += it
            if (it == 1) null else "photo $it"
        }
        assertEquals(listOf("photo 0"), cache.photosFor(0))
        assertEquals(emptyList<String>(), cache.photosFor(1))
        assertEquals(listOf("photo 2"), cache.photosFor(2))
        assertEquals(listOf(0, 1, 2), decoded)
        assertEquals(1, cache.peak)
    }

    @Test
    fun `a plane that paints from no photo decodes nothing`() {
        val cache = BakePhotoCache(listOf(emptyList(), listOf(4))) { "photo $it" }
        assertEquals(emptyList<String>(), cache.photosFor(0))
        assertEquals(0, cache.loads)
        assertEquals(listOf("photo 4"), cache.photosFor(1))
    }

    @Test
    fun `the log line gives the most photos held at once and how many were decoded`() {
        val cache = BakePhotoCache(listOf(listOf(0, 1), listOf(2))) { "photo $it" }
        cache.photosFor(0)
        cache.photosFor(1)
        assertEquals("photos held at once: at most 2 of 3 decoded", cache.logLine())
    }

    /**
     * The furnished synthetic room, walked the way a scan is (270 photo poses, near the scan's cap
     * of 300): its floor, four walls, the table and the sofa seat, each painted from the photos
     * [PlaneBake.pickPhotos] picks. Holding every decoded photo until the build ends kept all of
     * them in memory at once; letting each go after its last plane keeps only the most any one
     * plane, plus the photos it shares with planes still to come, needs.
     */
    @Test
    fun `the synthetic room's bake holds far fewer photos at once than it decodes`() {
        val poses = RerunSyntheticRoom.poses()
        val stops = RerunSyntheticRoom.STOPS
        val viewpoint = Vec3(
            stops.map { it.x }.average().toFloat(),
            stops.map { it.y }.average().toFloat(),
            stops.map { it.z }.average().toFloat(),
        )
        val picks = roomPlanes().map { plane ->
            PlaneBake.rectOf(plane, viewpoint)?.let { PlaneBake.pickPhotos(it, poses, ReplayLens.Default) }.orEmpty()
        }
        val decodedBefore = picks.flatten().distinct().size
        val cache = BakePhotoCache(picks) { it }
        picks.indices.forEach { cache.photosFor(it) }
        val mb = { photos: Int -> photos * BAKE_PHOTO_BYTES / (1024.0 * 1024.0) }
        println(
            "synthetic room bake: ${poses.size} photos, ${picks.size} planes; held at once " +
                "before $decodedBefore (%.1f MB), after ${cache.peak} (%.1f MB)".format(mb(decodedBefore), mb(cache.peak)),
        )
        assertEquals(decodedBefore, cache.loads)
        assertTrue("every plane paints from photos", picks.all { it.isNotEmpty() })
        assertTrue("held ${cache.peak} of $decodedBefore", cache.peak * 2 <= decodedBefore)
        assertEquals(emptySet<Int>(), cache.held)
    }

    private fun roomPlanes(): List<DebugPlane> {
        val lo = RerunSyntheticRoom.MIN
        val hi = RerunSyntheticRoom.MAX
        fun plane(id: Int, kind: DebugPlaneKind, vararg corners: Float) = DebugPlane(id, kind, corners)
        return listOf(
            plane(1, DebugPlaneKind.Floor, lo.x, 0f, lo.z, hi.x, 0f, lo.z, hi.x, 0f, hi.z, lo.x, 0f, hi.z),
            plane(2, DebugPlaneKind.Wall, lo.x, 0f, lo.z, hi.x, 0f, lo.z, hi.x, hi.y, lo.z, lo.x, hi.y, lo.z),
            plane(3, DebugPlaneKind.Wall, hi.x, 0f, lo.z, hi.x, 0f, hi.z, hi.x, hi.y, hi.z, hi.x, hi.y, lo.z),
            plane(4, DebugPlaneKind.Wall, hi.x, 0f, hi.z, lo.x, 0f, hi.z, lo.x, hi.y, hi.z, hi.x, hi.y, hi.z),
            plane(5, DebugPlaneKind.Wall, lo.x, 0f, hi.z, lo.x, 0f, lo.z, lo.x, hi.y, lo.z, lo.x, hi.y, hi.z),
            // The table top and the sofa seat, as ARCore finds them.
            plane(6, DebugPlaneKind.Floor, 0.3f, 0.76f, -0.3f, 1.3f, 0.76f, -0.3f, 1.3f, 0.76f, 0.4f, 0.3f, 0.76f, 0.4f),
            plane(7, DebugPlaneKind.Floor, -1.7f, 0.45f, -1.55f, -0.3f, 0.45f, -1.55f, -0.3f, 0.45f, -0.95f, -1.7f, 0.45f, -0.95f),
        )
    }

    private companion object {
        /** A photo decoded for the bake: 270 × 480 ARGB, the sharp photos' read size. */
        const val BAKE_PHOTO_BYTES = 270 * 480 * 4
    }
}
