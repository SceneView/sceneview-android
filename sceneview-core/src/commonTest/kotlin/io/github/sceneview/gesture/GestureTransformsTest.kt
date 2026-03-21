package io.github.sceneview.gesture

import io.github.sceneview.math.Scale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GestureTransformsTest {

    private val epsilon = 0.001f

    // --- applyScaleGesture ---

    @Test
    fun scaleFactorOneNoChange() {
        val scale = Scale(1f)
        val result = applyScaleGesture(scale, scaleFactor = 1f)
        assertNotNull(result)
        assertEquals(1f, result.x, epsilon)
        assertEquals(1f, result.y, epsilon)
        assertEquals(1f, result.z, epsilon)
    }

    @Test
    fun scaleUpWithFullSensitivity() {
        val scale = Scale(1f)
        val result = applyScaleGesture(scale, scaleFactor = 2f, sensitivity = 1f)
        assertNotNull(result)
        assertEquals(2f, result.x, epsilon)
    }

    @Test
    fun scaleUpWithDamping() {
        val scale = Scale(1f)
        val result = applyScaleGesture(scale, scaleFactor = 2f, sensitivity = 0.5f)
        assertNotNull(result)
        // damped = 1 + (2-1)*0.5 = 1.5
        assertEquals(1.5f, result.x, epsilon)
    }

    @Test
    fun scaleDownWithDamping() {
        val scale = Scale(2f)
        val result = applyScaleGesture(scale, scaleFactor = 0.5f, sensitivity = 0.5f)
        assertNotNull(result)
        // damped = 1 + (0.5-1)*0.5 = 0.75
        // newScale = 2 * 0.75 = 1.5
        assertEquals(1.5f, result.x, epsilon)
    }

    @Test
    fun scaleRejectsOutOfRange() {
        val scale = Scale(1f)
        // Try scaling way up beyond range
        val result = applyScaleGesture(scale, scaleFactor = 20f, sensitivity = 1f, range = 0.1f..10f)
        assertNull(result, "Scale exceeding range should return null")
    }

    @Test
    fun scaleRejectsBelowRange() {
        val scale = Scale(0.2f)
        // Try scaling down below range
        val result = applyScaleGesture(scale, scaleFactor = 0.1f, sensitivity = 1f, range = 0.1f..10f)
        assertNull(result, "Scale below range should return null")
    }

    @Test
    fun scaleAppliesUniformly() {
        val scale = Scale(x = 1f, y = 2f, z = 3f)
        val result = applyScaleGesture(scale, scaleFactor = 1.5f, sensitivity = 1f, range = 0.1f..10f)
        assertNotNull(result)
        assertEquals(1.5f, result.x, epsilon)
        assertEquals(3f, result.y, epsilon)
        assertEquals(4.5f, result.z, epsilon)
    }

    @Test
    fun zeroSensitivityNoEffect() {
        val scale = Scale(1f)
        val result = applyScaleGesture(scale, scaleFactor = 5f, sensitivity = 0f)
        assertNotNull(result)
        // damped = 1 + (5-1)*0 = 1
        assertEquals(1f, result.x, epsilon)
    }

    // --- applyRotationGesture ---

    @Test
    fun rotationWithFullSensitivity() {
        val result = applyRotationGesture(currentAngle = 0f, deltaAngle = 90f, sensitivity = 1f)
        assertEquals(90f, result, epsilon)
    }

    @Test
    fun rotationWithDamping() {
        val result = applyRotationGesture(currentAngle = 45f, deltaAngle = 90f, sensitivity = 0.5f)
        assertEquals(90f, result, epsilon) // 45 + 90*0.5 = 90
    }

    @Test
    fun rotationNegativeDelta() {
        val result = applyRotationGesture(currentAngle = 180f, deltaAngle = -90f, sensitivity = 1f)
        assertEquals(90f, result, epsilon)
    }
}
