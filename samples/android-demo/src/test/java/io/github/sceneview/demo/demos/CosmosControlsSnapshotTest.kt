package io.github.sceneview.demo.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.sceneview.demo.demos.internal.CosmosScene
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import io.github.sceneview.demo.theme.SceneViewTokens
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi goldens for Cosmos's controls over the dock (#4601): the scene switch on each of
 * the four scenes, and the Star scene's Starlight | Spacetime pill above it.
 *
 * The ground is a black → white-blue ramp, the range the scenes put behind the controls (the
 * black sky of Galaxy and Burst, the lit currents of Flow). The pills are the fixed
 * `mode-pill-*` palette, so the light and dark goldens are expected to be identical: a change
 * that reaches for `MaterialTheme.colorScheme` here would split them.
 *
 * Re-record after a deliberate UI change:
 *   `./gradlew :samples:android-demo:recordRoborazziDebug --tests '*CosmosControlsSnapshotTest*'`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CosmosControlsSnapshotTest {

    @Test
    fun controls_lightTheme() {
        captureRoboImage("src/test/snapshots/cosmos_controls-light.png", roborazziOptions = HOST_TOLERANT) {
            Controls(darkTheme = false)
        }
    }

    @Test
    fun controls_darkTheme() {
        captureRoboImage("src/test/snapshots/cosmos_controls-dark.png", roborazziOptions = HOST_TOLERANT) {
            Controls(darkTheme = true)
        }
    }

    @Composable
    private fun Controls(darkTheme: Boolean) {
        SceneViewDemoTheme(darkTheme = darkTheme) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF0B2A5C), Color(0xFFDCE8FF)))),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(SceneViewTokens.Space.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                        SceneViewTokens.Space.xl,
                    ),
                ) {
                    CosmosBottomControls(CosmosScene.Galaxy, spacetime = false, onSpacetime = {}, onPick = {})
                    CosmosBottomControls(CosmosScene.Star, spacetime = false, onSpacetime = {}, onPick = {})
                    CosmosBottomControls(CosmosScene.Burst, spacetime = false, onSpacetime = {}, onPick = {})
                    CosmosBottomControls(CosmosScene.Flow, spacetime = false, onSpacetime = {}, onPick = {})
                }
            }
        }
    }

    private companion object {
        // Native graphics anti-aliases text a little differently from host to host.
        val HOST_TOLERANT = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(
                changeThreshold = 0f,
                imageComparator = SimpleImageComparator(maxDistance = 0.02f),
            ),
        )
    }
}
