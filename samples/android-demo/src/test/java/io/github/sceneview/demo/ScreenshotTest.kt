package io.github.sceneview.demo

import androidx.compose.material3.Surface
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.sceneview.demo.about.AboutScreen
import io.github.sceneview.demo.samples.SamplesScreen
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi) — s'exécutent sur JVM, sans émulateur.
 *
 * Générer les goldens :  ./gradlew :samples:android-demo:recordRoborazziDebug
 * Vérifier les goldens : ./gradlew :samples:android-demo:verifyRoborazziDebug
 *
 * Les goldens sont dans : src/test/snapshots/
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    // ── About ─────────────────────────────────────────────────────────────────

    @Test
    fun aboutScreen_lightMode() {
        captureRoboImage("src/test/snapshots/about_light.png") {
            SceneViewDemoTheme(darkTheme = false) {
                Surface { AboutScreen() }
            }
        }
    }

    @Test
    fun aboutScreen_darkMode() {
        captureRoboImage("src/test/snapshots/about_dark.png") {
            SceneViewDemoTheme(darkTheme = true) {
                Surface { AboutScreen() }
            }
        }
    }

    @Test
    @Config(fontScale = 1.5f)
    fun aboutScreen_largeFont() {
        captureRoboImage("src/test/snapshots/about_large_font.png") {
            SceneViewDemoTheme(darkTheme = false) {
                Surface { AboutScreen() }
            }
        }
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-xhdpi")
    fun aboutScreen_tablet() {
        captureRoboImage("src/test/snapshots/about_tablet.png") {
            SceneViewDemoTheme(darkTheme = false) {
                Surface { AboutScreen() }
            }
        }
    }

    // ── Samples ───────────────────────────────────────────────────────────────

    @Test
    fun samplesScreen_lightMode() {
        captureRoboImage("src/test/snapshots/samples_light.png") {
            SceneViewDemoTheme(darkTheme = false) {
                Surface { SamplesScreen() }
            }
        }
    }

    @Test
    fun samplesScreen_darkMode() {
        captureRoboImage("src/test/snapshots/samples_dark.png") {
            SceneViewDemoTheme(darkTheme = true) {
                Surface { SamplesScreen() }
            }
        }
    }
}
