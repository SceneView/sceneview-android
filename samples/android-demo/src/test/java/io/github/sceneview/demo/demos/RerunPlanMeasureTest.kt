package io.github.sceneview.demo.demos

import androidx.compose.ui.test.junit4.createComposeRule
import io.github.sceneview.demo.demos.internal.ArDebugTrace
import io.github.sceneview.demo.demos.internal.DebugMesh
import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.PlanOffer
import io.github.sceneview.demo.demos.internal.ReplayLens
import io.github.sceneview.demo.demos.internal.ReplayManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The export sheet's "Floor plan" row ends: on the plan or on why there is none, never on
 * "Measuring the room…" for good — even when the measure cannot run, as when every thread it
 * shares is busy building the scan's model.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RerunPlanMeasureTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** A scan with a dense cloud: what the export sheet measures for its plan. */
    private fun scan() = RerunReplayMedia(
        trace = ArDebugTrace(),
        manifest = ReplayManifest(ReplayLens.Default, frameRate = 1f, frameCount = 0, floorY = null, textures = emptyList()),
        planeBitmaps = emptyMap(),
        thumbnails = emptyMap(),
        archive = ByteArray(0),
        dense = ReplayDenseLayer(
            cloud = DenseCloud(floatArrayOf(0f, 0f, 0f), intArrayOf(0), normals = floatArrayOf(0f, 1f, 0f)),
            voxelM = VOXEL_M,
            mesh = DebugMesh(),
            atlas = ByteArray(0),
        ),
    )

    /**
     * Holds every thread of [Dispatchers.Default] until the returned latch is released (or
     * [HOLD_S] pass): blockers are added until one no longer starts, so the pool is full.
     */
    private fun occupyDefault(): CountDownLatch {
        val release = CountDownLatch(1)
        val scope = CoroutineScope(Dispatchers.Default)
        while (true) {
            val started = CountDownLatch(1)
            scope.launch {
                started.countDown()
                release.await(HOLD_S, TimeUnit.SECONDS)
            }
            if (!started.await(START_MS, TimeUnit.MILLISECONDS)) return release
        }
    }

    @Test
    fun `the floor plan row leaves Measuring within a minute when the measure cannot run`() {
        val release = occupyDefault()
        try {
            val media = scan()
            var offer: PlanOffer? = null
            composeRule.mainClock.autoAdvance = false
            composeRule.setContent { offer = PlanOffer.of(rememberRoomSurvey(media, wanted = true)) }
            composeRule.mainClock.advanceTimeBy(BOUND_MS)
            composeRule.mainClock.advanceTimeByFrame()
            assertNotEquals("still measuring after ${BOUND_MS / 1000} s", PlanOffer.Measuring, offer)
        } finally {
            release.countDown()
        }
    }

    private companion object {
        const val VOXEL_M = 0.05f

        /** The longest the row may say "Measuring the room…". */
        const val BOUND_MS = 60_000L

        /** How long a blocker gets to start before the pool counts as full. */
        const val START_MS = 500L

        /** The longest a blocker holds its thread, should the test never release it. */
        const val HOLD_S = 30L
    }
}
