package io.github.sceneview.rendering

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for material default constants — ensures they stay within valid PBR ranges.
 */
class MaterialDefaultsTest {

    @Test
    fun metallicDefaultIsZero() {
        assertEquals(0.0f, kMaterialDefaultMetallic)
    }

    @Test
    fun roughnessDefaultInValidRange() {
        assertTrue(kMaterialDefaultRoughness in 0f..1f,
            "Roughness must be between 0 and 1, got $kMaterialDefaultRoughness")
    }

    @Test
    fun reflectanceDefaultInValidRange() {
        assertTrue(kMaterialDefaultReflectance in 0f..1f,
            "Reflectance must be between 0 and 1, got $kMaterialDefaultReflectance")
    }

    @Test
    fun priorityDefaultIsBetweenFirstAndLast() {
        assertTrue(PRIORITY_DEFAULT in PRIORITY_FIRST..PRIORITY_LAST,
            "Default priority must be between first ($PRIORITY_FIRST) and last ($PRIORITY_LAST)")
    }

    @Test
    fun priorityFirstIsLessThanLast() {
        assertTrue(PRIORITY_FIRST < PRIORITY_LAST)
    }

    @Test
    fun priorityFirstIsZero() {
        assertEquals(0, PRIORITY_FIRST)
    }

    @Test
    fun priorityLastIs7() {
        assertEquals(7, PRIORITY_LAST)
    }
}
