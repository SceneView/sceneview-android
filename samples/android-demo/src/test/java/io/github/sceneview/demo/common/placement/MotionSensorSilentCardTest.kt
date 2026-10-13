package io.github.sceneview.demo.common.placement

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import com.dropbox.differ.SimpleImageComparator
import io.github.sceneview.demo.AssetSourceState
import io.github.sceneview.demo.DEMO_MODE_PILL_TAG
import io.github.sceneview.demo.DemoMode
import io.github.sceneview.demo.DemoModeSwitch
import io.github.sceneview.demo.DemoScaffold
import io.github.sceneview.demo.DemoScaffoldTestTags
import io.github.sceneview.demo.DockItem
import io.github.sceneview.demo.LocalDemoModeSwitch
import io.github.sceneview.demo.R
import io.github.sceneview.demo.common.ForcedTrackingFailure
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * The card an AR session shows once the phone's gyroscope is known to deliver nothing (#4522),
 * on the real screen: the real [DemoScaffold] inside AR Placement's mode switch, with the real
 * [TapToPlaceStatusOverlays] in its scene slot, in the state such a session is stuck in —
 * frames arriving, never tracking, phase `INITIALIZING`.
 *
 * What is held: the card replaces "Getting ready" rather than joining it, it carries no
 * button, and it stays clear of the chrome in portrait, sideways and at the largest text.
 *
 * Re-record with
 * `./gradlew :samples:android-demo:recordRoborazziDebug --tests '*MotionSensorSilentCardTest*'`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MotionSensorSilentCardTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val state = TapToPlaceState()

    @Before
    fun setUp() {
        ForcedTrackingFailure.override = null
        state.cameraReady = true
        state.isTracking = false
        state.trackingFailureReason = null
        state.phase = PlacementPhase.INITIALIZING
    }

    @After
    fun tearDown() {
        ForcedTrackingFailure.override = null
    }

    @Test
    fun withoutTheVerdict_theScreenSaysGettingReady_andShowsNoCard() {
        // The control: this is the screen the verdict replaces, and it must stay as it is.
        state.motionSensorSilent = false
        show()
        assertEquals(1, gettingReadyNodes())
        composeRule.onNodeWithTag(PlacementTestTags.PLACEMENT_CARD).assertDoesNotExist()
    }

    @Test
    fun portrait_light() = assertRestartCard("portrait-light")

    @Test
    fun portrait_dark() = assertRestartCard("portrait-dark", darkTheme = true)

    @Test
    fun portrait_dark_atMaximumFontScale() =
        assertRestartCard("portrait-dark-font-2", fontScale = 2.0f, darkTheme = true)

    @Test
    @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
    fun phoneHeldSideways_light() = assertRestartCard("landscape-light")

    @Test
    @Config(qualifiers = "w800dp-h360dp-land-xhdpi")
    fun phoneHeldSideways_dark_atMaximumFontScale() =
        assertRestartCard("landscape-dark-font-2", fontScale = 2.0f, darkTheme = true)

    private fun assertRestartCard(name: String, fontScale: Float = 1.0f, darkTheme: Boolean = false) {
        state.motionSensorSilent = true
        show(fontScale, darkTheme)

        composeRule.onRoot().captureRoboImage(
            "src/test/snapshots/motion_sensor_silent_$name.png",
            roborazziOptions = CROSS_PLATFORM_TOLERANT,
        )

        val resources = composeRule.activity.resources
        composeRule.onNodeWithText(resources.getString(R.string.ar_place_motion_sensor_title))
            .assertExists()
        composeRule.onNodeWithText(resources.getString(R.string.ar_place_motion_sensor_detail))
            .assertExists()
        assertEquals("\"Getting ready\" is still on screen next to the card", 0, gettingReadyNodes())
        // Nothing to press: no action of the app's brings a gyroscope back.
        composeRule.onNodeWithTag(PlacementTestTags.PLACEMENT_CARD_PRIMARY).assertDoesNotExist()
        composeRule.onNodeWithTag(PlacementTestTags.PLACEMENT_CARD_SECONDARY).assertDoesNotExist()

        composeRule.onNodeWithTag(PlacementTestTags.PLACEMENT_CARD).assertIsDisplayed()
        val window = composeRule.onRoot().getUnclippedBoundsInRoot()
        val card = bounds(PlacementTestTags.PLACEMENT_CARD)
        val pill = bounds(DEMO_MODE_PILL_TAG)
        val dock = bounds(DemoScaffoldTestTags.DOCK)
        val identity = bounds(DemoScaffoldTestTags.IDENTITY_ROW)
        val where = "card $card, mode pill $pill, dock $dock, identity row $identity, window $window"
        assertTrue("the card shares pixels with the mode pill — $where", !card.overlaps(pill))
        assertTrue("the card shares pixels with the dock — $where", !card.overlaps(dock))
        assertTrue("the card runs under the identity row — $where", !card.overlaps(identity))
        assertTrue(
            "the card leaves the window — $where",
            card.top >= window.top && card.bottom <= window.bottom &&
                card.left >= window.left && card.right <= window.right,
        )
    }

    /** The SDK's coaching card is one node whose description starts with its headline. */
    private fun gettingReadyNodes(): Int = composeRule
        .onAllNodes(hasContentDescription("Getting ready", substring = true))
        .fetchSemanticsNodes().size

    private fun show(fontScale: Float = 1.0f, darkTheme: Boolean = false) {
        composeRule.setContent {
            ScaledDensity(fontScale) {
                SceneViewDemoTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(
                        LocalDemoModeSwitch provides DemoModeSwitch(MODES, selected = 0, onSelect = {}),
                    ) {
                        // The chrome `ARPlacementDemo` declares: two dock items and the source pill.
                        DemoScaffold(
                            title = "AR Placement",
                            onBack = {},
                            assetSource = AssetSourceState.Bundled,
                            dock = listOf(
                                DockItem(Icons.Outlined.WbSunny, "Models", onClick = {}),
                                DockItem(Icons.Outlined.WbSunny, "Reset", onClick = {}),
                            ),
                        ) {
                            // A flat stand-in for the camera image the session keeps delivering.
                            Box(Modifier.fillMaxSize().background(CAMERA_STAND_IN))
                            TapToPlaceStatusOverlays(state = state, onViewIn3D = {}, onRestartSession = {})
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // Past the coaching card's 500 ms wait — counted on the system clock, which the compose
        // test clock does not move — and then past every enter transition.
        ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
        composeRule.mainClock.advanceTimeBy(2_000)
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String): DpRect = composeRule
        .onNodeWithTag(tag)
        .getUnclippedBoundsInRoot()

    private fun DpRect.overlaps(other: DpRect): Boolean =
        left < other.right - TOLERANCE && other.left < right - TOLERANCE &&
            top < other.bottom - TOLERANCE && other.top < bottom - TOLERANCE

    @Composable
    private fun ScaledDensity(fontScale: Float, content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, fontScale),
            content = content,
        )
    }

    private companion object {
        val TOLERANCE = 0.75.dp
        val CAMERA_STAND_IN = Color(0xFF7C7F86)

        val MODES = listOf(
            DemoMode("place", R.string.demo_mode_place),
            DemoMode("free-pose", R.string.demo_mode_free_pose),
            DemoMode("one-call", R.string.demo_mode_one_call),
        )

        /** The cross-OS bound `PlacementBottomAnchorSnapshotTest` measured for these surfaces. */
        val CROSS_PLATFORM_TOLERANT = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(
                imageComparator = SimpleImageComparator(maxDistance = 0.03f),
            ),
        )
    }
}
