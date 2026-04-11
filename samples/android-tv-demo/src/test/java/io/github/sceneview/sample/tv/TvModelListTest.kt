package io.github.sceneview.sample.tv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Asserts that every model entry in [models] — the list TvModelViewerActivity
 * renders in its D-pad gallery — resolves to a bundled asset in
 * `src/main/assets/`.
 *
 * This is the JVM-level backstop for the session-34 fix where
 * [TvModelViewerActivity] was shipping five "models/…glb" paths that were
 * never bundled (`space_helmet.glb`, `toy_car.glb`, `geisha_mask.glb`,
 * `iridescence_lamp.glb`, `sheen_chair.glb`). Before this test, the only
 * protection was `.claude/scripts/validate-demo-assets.sh`, which is bash
 * and has to be invoked explicitly.
 *
 * With this test, `./gradlew :samples:android-tv-demo:testDebugUnitTest`
 * fails fast on any PR that adds a model entry without bundling the file.
 */
class TvModelListTest {

    /**
     * Gradle sets the unit-test working directory to the module's `projectDir`
     * (i.e. `samples/android-tv-demo/`). Assets live at `src/main/assets/`
     * relative to that.
     */
    private val assetsDir = File("src/main/assets")

    @Test
    fun `models list is not empty`() {
        assertTrue(
            "TvModelViewerActivity.models is empty — the TV demo would have nothing to render",
            models.isNotEmpty(),
        )
    }

    @Test
    fun `every model entry resolves to a bundled asset`() {
        val missing = models.filter { entry ->
            !File(assetsDir, entry.assetPath).isFile
        }

        assertTrue(
            "TvModelViewerActivity.models references assets that are not bundled:\n" +
                missing.joinToString("\n") { "  - ${it.label}: ${it.assetPath}" } +
                "\n\nFix: either bundle the missing file into src/main/assets/, " +
                "or replace the entry with a model that already is.",
            missing.isEmpty(),
        )
    }

    @Test
    fun `every model entry has a positive scale`() {
        val bad = models.filter { it.scale <= 0f }
        assertTrue(
            "TvModelViewerActivity.models entries must have a positive scale (> 0). " +
                "Offenders: ${bad.joinToString { "${it.label}=${it.scale}" }}",
            bad.isEmpty(),
        )
    }

    @Test
    fun `every model entry has a non-blank label`() {
        val blank = models.filter { it.label.isBlank() }
        assertTrue(
            "TvModelViewerActivity.models entries must have non-blank labels. " +
                "Offenders (by assetPath): ${blank.joinToString { it.assetPath }}",
            blank.isEmpty(),
        )
    }

    @Test
    fun `no duplicate asset paths`() {
        val duplicates = models
            .groupBy { it.assetPath }
            .filterValues { it.size > 1 }
            .keys
        assertFalse(
            "TvModelViewerActivity.models has duplicate asset paths: $duplicates",
            duplicates.isNotEmpty(),
        )
    }
}
