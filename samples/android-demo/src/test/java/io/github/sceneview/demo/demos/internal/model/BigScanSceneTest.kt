package io.github.sceneview.demo.demos.internal.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The finish of a scan as big as the longest recordings mesh ([BigScanScene]). */
class BigScanSceneTest {
    @Test
    fun `a big scan over the size cap keeps its photo texture, its faces cut first`() {
        // A dim room's model stays well over its budget, and its atlas compresses as badly as
        // noisy photos do: together over the cap. The faces pay first, then the atlas's
        // resolution; the photos' texture goes only when neither is left to give.
        val result = CleanModel.build(BigScanScene.raw(BigScanScene.DIM), BigScanScene.photos(1), HEAVY_ATLAS)
        val stats = result.stats
        val report = "big dim scan: raw ${stats.rawTriangles} triangles -> ${stats.simplifiedTriangles} simplified" +
            ", ${stats.finalTriangles} final, glb ${result.glb.size / KB} KB, guarded ${stats.glbGuarded}" +
            ", halved ${stats.atlasHalvings}, textured ${stats.texture?.texturedShare ?: "none"}, dropped at ${stats.droppedTextureShare}"
        println(report)
        assertTrue("the size guard never ran | $report", stats.glbGuarded)
        assertNotNull("the photo texture was dropped | $report", stats.texture)
        assertTrue("the .glb holds no texture | $report", ModelGlb.isTextured(result.glb))
        assertEquals("the atlas was halved before the faces were cut | $report", 0, stats.atlasHalvings)
        assertTrue(
            "faces cut under the textured budget | $report",
            stats.finalTriangles >= CleanModel.TEXTURED_TARGET_TRIANGLES,
        )
        assertTrue("glb ${result.glb.size} over the cap | $report", result.glb.size <= CleanModel.MAX_GLB_BYTES)
        assertTrue("glb: ${GlbCheck.problems(result.glb)} | $report", GlbCheck.problems(result.glb).isEmpty())
    }

    private companion object {
        const val KB = 1024

        /** What the photos of a dim room add to an atlas's JPEG: about half the cap. */
        const val HEAVY_ATLAS_PADDING = 3_900 * KB

        val HEAVY_ATLAS = AtlasEncoder { argb, width, height ->
            JvmAtlasEncoder.jpeg(argb, width, height) + ByteArray(HEAVY_ATLAS_PADDING)
        }
    }
}
