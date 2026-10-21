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
    fun `the finished model replacing a big scan's raw surface fades the line out instead of removing it`() {
        // While the raw surface finishes, the line shows over it.
        val finishing = checkNotNull(ModelStatus.slot(mesh(finished = false), settled = false, rawShownFirst = false))
        assertEquals(ModelStatus.Finishing, finishing.status)
        assertTrue(finishing.visible)
        // The swap: the same line keeps its room, faded. The model is framed above this slot, and
        // a line that went would move the finished model the moment it takes the raw one's place.
        val swapped = checkNotNull(ModelStatus.slot(mesh(finished = true), settled = true, rawShownFirst = true))
        assertEquals(ModelStatus.Finishing, swapped.status)
        assertFalse(swapped.visible)
        // A finish that fails after the raw surface was shown still says why.
        val failed = checkNotNull(ModelStatus.slot(mesh(finished = false), settled = true, rawShownFirst = true))
        assertEquals(ModelStatus.RawUnfinished, failed.status)
        assertTrue(failed.visible)
    }

    @Test
    fun `a small scan or a reopened model never had a line under it, so none is laid out`() {
        assertNull(ModelStatus.slot(mesh(finished = true), settled = true, rawShownFirst = false))
        val creating = checkNotNull(ModelStatus.slot(model = null, settled = false, rawShownFirst = false))
        assertEquals(ModelStatus.Creating, creating.status)
        assertTrue(creating.visible)
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
