package io.github.sceneview.demo.ai.scene

import io.github.sceneview.demo.demos.RerunCaptureBuilder
import io.github.sceneview.demo.demos.internal.ArDebugTrace
import io.github.sceneview.demo.demos.internal.DenseFusion
import io.github.sceneview.demo.demos.internal.FakeRerunImageCodec
import io.github.sceneview.demo.demos.internal.ReplayGeometry
import io.github.sceneview.demo.demos.internal.ReplayManifest
import io.github.sceneview.demo.demos.internal.ScanDevice
import io.github.sceneview.demo.demos.internal.of
import io.github.sceneview.demo.demos.internal.parseArDebugLog
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Scan this room": a live scan reaches the AI's tools through the scan file a Rerun recording
 * writes. Built from the bundled session's own events, as a phone without raw depth would build
 * it, the file takes its floor height from ARCore's floor planes, and the tools read the same room
 * as from the bundled recording.
 */
class LiveScanSessionTest {

    private val showcase = FakeRerunImageCodec.showcase()
    private val events = parseArDebugLog(String(showcase.log).lineSequence())
    private val bundled = ReplayManifest.parse(String(showcase.manifest))!!

    private val live: SpatialSession by lazy {
        val device = ScanDevice("android", "Pixel 4a", ScanDevice.TIER_SPARSE, ScanDevice.SOURCE_FEATURE_POINTS)
        val pack = runBlocking {
            RerunCaptureBuilder.build(events, bundled.lens, emptyList(), device, null, DenseFusion.VOXEL_M, 0L)
        }
        // What `loadRerunSession` does with the file, minus the photos.
        val manifest = ReplayManifest.parse(String(pack.manifest))!!
        assertNotNull("a live scan takes its floor from ARCore's floor planes", manifest.floorY)
        val trace = ArDebugTrace.of(parseArDebugLog(String(pack.log).lineSequence()))
            .apply { keyframeSpacing = ReplayGeometry.KEYFRAME_SPACING_M }
        SpatialSession.of(trace, manifest.lens, manifest.floorY)
    }

    @Test
    fun `a live scan finds the floor from its planes, where the recording's manifest put it`() {
        val floor = bundled.floorY
        if (floor != null) assertEquals(floor, live.floorY, FLOOR_TOLERANCE_M)
        assertTrue(live.pointCount > 0)
        assertTrue(live.keyframes.isNotEmpty())
    }

    @Test
    fun `the tools read a live scan as they read the recording`() {
        val recorded = ShowcaseSession.session
        assertEquals(recorded.pointCount, live.pointCount)
        assertEquals(recorded.planes.size, live.planes.size)
        assertEquals(recorded.keyframes.size, live.keyframes.size)
    }

    @Test
    fun `dense capture supplies points colors and normals while retaining trace metadata`() {
        val trace = ArDebugTrace.of(events)
        val sparse = SpatialSession.of(trace, bundled.lens, bundled.floorY)
        val cloud = SyntheticScanSession.cloud
        val dense = SpatialSession.of(trace, bundled.lens, bundled.floorY, dense = cloud)
        assertSame(cloud.positions, dense.points)
        assertSame(cloud.colors, dense.colors)
        assertSame(cloud.normals, dense.normals)
        assertEquals(sparse.floorY, dense.floorY, 0f)
        assertEquals(sparse.planes, dense.planes)
        assertEquals(sparse.keyframes.map { it.path }, dense.keyframes.map { it.path })
    }

    private companion object {
        const val FLOOR_TOLERANCE_M = 0.05f
    }
}
