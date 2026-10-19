package io.github.sceneview.demo.labs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.sceneview.demo.ALL_DEMOS
import io.github.sceneview.demo.DemoEntry
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import io.github.sceneview.demo.theme.SceneViewTokens
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi goldens for the SceneView Labs card, in the states the Home grid golden does not
 * show: joined with nothing behind the switch yet ("New experiments land here soon"), joined
 * with one experiment (a stand-in demo, the real registry is empty), and both in dark.
 *
 * Re-record: `./gradlew :samples:android-demo:recordRoborazziDebug --tests '*LabsCardSnapshotTest*'`
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LabsCardSnapshotTest {

    @Test
    fun labs_joined_empty_light() = capture("labs_joined_empty_light", LabsAccess.On, emptyList(), dark = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xhdpi")
    fun labs_joined_empty_dark() = capture("labs_joined_empty_dark", LabsAccess.On, emptyList(), dark = true)

    @Test
    fun labs_joined_one_light() = capture("labs_joined_one_light", LabsAccess.On, STAND_IN, dark = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xhdpi")
    fun labs_not_joined_dark() = capture("labs_not_joined_dark", LabsAccess.NotJoined, emptyList(), dark = true)

    @Test
    @Config(fontScale = 1.5f)
    fun labs_not_joined_large_font() =
        capture("labs_not_joined_large_font", LabsAccess.NotJoined, emptyList(), dark = false)

    private fun capture(name: String, access: LabsAccess, demos: List<DemoEntry>, dark: Boolean) {
        captureRoboImage("src/test/snapshots/$name.png", roborazziOptions = HOST_TOLERANT) {
            SceneViewDemoTheme(darkTheme = dark) {
                Card(access, demos)
            }
        }
    }

    @Composable
    private fun Card(access: LabsAccess, demos: List<DemoEntry>) {
        Surface {
            LabsHomeCard(
                access = access,
                labsDemos = demos,
                onJoinClick = {},
                onEnabledChange = {},
                onDemoClick = {},
                onOpenCollectiveClick = {},
                modifier = Modifier.padding(SceneViewTokens.Home.contentPadding),
            )
        }
    }

    private companion object {
        val STAND_IN: List<DemoEntry> = listOf(ALL_DEMOS.first())

        val HOST_TOLERANT = RoborazziOptions(
            compareOptions = RoborazziOptions.CompareOptions(
                changeThreshold = 0f,
                imageComparator = SimpleImageComparator(maxDistance = 0.02f),
            ),
        )
    }
}
