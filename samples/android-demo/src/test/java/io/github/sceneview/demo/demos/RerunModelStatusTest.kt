package io.github.sceneview.demo.demos

import io.github.sceneview.demo.demos.internal.RerunScanModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the model screen says under the model, from building it to the raw surface left in its place. */
class RerunModelStatusTest {
    @Test
    fun `a big scan's raw surface shown meanwhile says the model is being finished`() {
        assertEquals(ModelStatus.Creating, ModelStatus.of(model = null, settled = false))
        assertEquals(ModelStatus.Finishing, ModelStatus.of(mesh(finished = false), settled = false))
        assertTrue(ModelStatus.Creating.showsProgress)
        assertTrue(ModelStatus.Finishing.showsProgress)
    }

    @Test
    fun `the raw surface left in the finished model's place never goes without a word`() {
        val outOfMemory = mesh(finished = false, outOfMemoryInHeap = HEAP)
        assertEquals(ModelStatus.RawOutOfMemory, ModelStatus.of(outOfMemory, settled = true))
        assertEquals(ModelStatus.RawUnfinished, ModelStatus.of(mesh(finished = false), settled = true))
        assertFalse(ModelStatus.RawOutOfMemory.showsProgress)
    }

    @Test
    fun `a finished model needs no word, a failure or the points do`() {
        assertNull(ModelStatus.of(mesh(finished = true), settled = true))
        assertEquals(ModelStatus.Failed, ModelStatus.of(model = null, settled = true))
        val points = RerunScanModel(RerunScanModel.Kind.Points, ByteArray(0), FloatArray(BOUNDS), 1, 3, finished = false)
        assertEquals(ModelStatus.Partial, ModelStatus.of(points, settled = true))
    }

    private fun mesh(finished: Boolean, outOfMemoryInHeap: Long? = null) = RerunScanModel(
        RerunScanModel.Kind.Mesh,
        ByteArray(0),
        FloatArray(BOUNDS),
        1,
        3,
        finished = finished,
        outOfMemoryInHeap = outOfMemoryInHeap,
    )

    private companion object {
        const val BOUNDS = 6
        const val HEAP = 192L * 1024 * 1024
    }
}
