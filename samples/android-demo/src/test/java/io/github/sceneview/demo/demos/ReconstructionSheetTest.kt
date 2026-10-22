package io.github.sceneview.demo.demos

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.sceneview.demo.R
import io.github.sceneview.demo.demos.internal.DenseCloud
import io.github.sceneview.demo.demos.internal.ReconstructionMethod
import io.github.sceneview.demo.demos.internal.RerunTsdf
import io.github.sceneview.demo.theme.SceneViewDemoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The end of a scan's choice of reconstruction (#4630): the two methods the phone runs and
 * nothing else, "As scanned" picked first, "Filled" only when the scan has its surfels. Each run
 * leaves the sheet's content, light and dark, in `build/reconstruction-sheet-renders/` (not
 * committed).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReconstructionSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var finished: ReconstructionMethod? = null

    private fun show(canFill: Boolean) {
        composeRule.setContent {
            SceneViewDemoTheme(darkTheme = false) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    ReconstructionChoices(canFill, ReconstructionMethod.Fused, onFinish = { finished = it })
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `only the two methods the phone runs are offered, as scanned first`() {
        show(canFill = true)
        val choices = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        composeRule.onAllNodes(choices).assertCountEquals(ReconstructionMethod.entries.size)
        composeRule.onNodeWithTag(choiceTag(ReconstructionMethod.Fused)).assertIsSelected()
        composeRule.onNodeWithTag(choiceTag(ReconstructionMethod.Poisson)).assertIsNotSelected()
    }

    @Test
    fun `filled can be picked and the model finishes with it`() {
        show(canFill = true)
        composeRule.onNodeWithTag(choiceTag(ReconstructionMethod.Poisson)).performClick().assertIsSelected()
        composeRule.onNodeWithTag(choiceTag(ReconstructionMethod.Fused)).assertIsNotSelected()
        composeRule.onNodeWithTag(FINISH_TAG).performClick()
        assertEquals(ReconstructionMethod.Poisson, finished)
    }

    @Test
    fun `without surfels filled is shown, says why, and cannot be picked`() {
        show(canFill = false)
        val unavailable = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.scan_finish_method_filled_unavailable)
        composeRule.onNodeWithText(unavailable).assertExists()
        composeRule.onNodeWithTag(choiceTag(ReconstructionMethod.Poisson)).assertIsNotEnabled().performClick()
        composeRule.onNodeWithTag(choiceTag(ReconstructionMethod.Fused)).assertIsSelected()
        composeRule.onNodeWithTag(FINISH_TAG).performClick()
        assertEquals(ReconstructionMethod.Fused, finished)
    }

    @Test
    fun `a scan can be filled when it kept surfels with normals`() {
        val withNormals = DenseCloud(FloatArray(3), IntArray(1), normals = floatArrayOf(0f, 1f, 0f))
        val withoutNormals = DenseCloud(FloatArray(3), IntArray(1))
        assertTrue(canFill(RerunModelSource.Surfels(withNormals)))
        assertFalse(canFill(RerunModelSource.Surfels(withoutNormals)))
        assertTrue(canFill(RerunModelSource.Live(RerunTsdf(), withNormals)))
        assertFalse(canFill(RerunModelSource.Live(RerunTsdf(), null)))
        assertTrue(canFill(RerunModelSource.Synthetic()))
    }

    @Test
    fun `the sheet renders in light and dark`() {
        var look by mutableStateOf(LOOKS.first())
        composeRule.setContent {
            key(look) {
                SceneViewDemoTheme(darkTheme = look.dark) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                        ReconstructionChoices(look.canFill, look.initial, onFinish = {}, Modifier.testTag(SHEET_TAG))
                    }
                }
            }
        }
        for (each in LOOKS) {
            look = each
            composeRule.waitForIdle()
            composeRule.onAllNodesWithTag(SHEET_TAG).assertCountEquals(1)
            val image = composeRule.onNodeWithTag(SHEET_TAG).captureToImage().asAndroidBitmap()
            save(image, each.name)
            val ground = Color(image.getPixel(image.width - 1, image.height - 1)).luminance()
            assertTrue("${each.name}: ground $ground", if (each.dark) ground < DARK_GROUND else ground > LIGHT_GROUND)
        }
    }

    private fun save(image: Bitmap, name: String) {
        val out = File("build/reconstruction-sheet-renders").apply { mkdirs() }.resolve("$name.png")
        out.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private data class Look(val name: String, val dark: Boolean, val canFill: Boolean, val initial: ReconstructionMethod)

    private companion object {
        const val SHEET_TAG = "reconstruction-sheet"
        const val DARK_GROUND = 0.2f
        const val LIGHT_GROUND = 0.5f
        val LOOKS = listOf(
            Look("light", dark = false, canFill = true, initial = ReconstructionMethod.Fused),
            Look("light-filled", dark = false, canFill = true, initial = ReconstructionMethod.Poisson),
            Look("light-unavailable", dark = false, canFill = false, initial = ReconstructionMethod.Fused),
            Look("dark", dark = true, canFill = true, initial = ReconstructionMethod.Fused),
            Look("dark-filled", dark = true, canFill = true, initial = ReconstructionMethod.Poisson),
        )
    }
}
