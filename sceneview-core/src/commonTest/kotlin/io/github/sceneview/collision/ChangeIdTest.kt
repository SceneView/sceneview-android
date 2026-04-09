package io.github.sceneview.collision

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChangeIdTest {

    @Test
    fun newChangeIdIsNotEmpty() {
        // The init block calls update(), so a new ChangeId is not empty
        val id = ChangeId()
        id.update()
        assertFalse(id.isEmpty())
    }

    @Test
    fun emptyIdConstant() {
        assertEquals(0, ChangeId.EMPTY_ID)
    }

    @Test
    fun updateIncrementsId() {
        val id = ChangeId()
        id.update()
        val first = id.get()
        id.update()
        val second = id.get()
        assertTrue(second > first, "Id should increment: first=$first, second=$second")
    }

    @Test
    fun checkChangedReturnsTrueWhenDifferent() {
        val id = ChangeId()
        id.update()
        val snapshot = id.get()
        id.update()
        assertTrue(id.checkChanged(snapshot))
    }

    @Test
    fun checkChangedReturnsFalseWhenSame() {
        val id = ChangeId()
        id.update()
        val snapshot = id.get()
        assertFalse(id.checkChanged(snapshot))
    }

    @Test
    fun multipleUpdatesTrackCorrectly() {
        val id = ChangeId()
        id.update()
        val start = id.get()
        repeat(10) { id.update() }
        assertTrue(id.checkChanged(start))
        assertFalse(id.checkChanged(id.get()))
    }
}
