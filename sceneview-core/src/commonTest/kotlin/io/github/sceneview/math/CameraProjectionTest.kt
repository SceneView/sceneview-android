package io.github.sceneview.math

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Mat4
import dev.romainguy.kotlin.math.inverse
import kotlin.math.abs
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertTrue

class CameraProjectionTest {

    /** Build a simple symmetric perspective projection matrix. */
    private fun perspectiveMatrix(
        fovYDegrees: Float,
        aspect: Float,
        near: Float,
        far: Float
    ): Mat4 {
        val fovRad = fovYDegrees * (kotlin.math.PI.toFloat() / 180f)
        val f = 1f / tan(fovRad / 2f)
        val rangeInv = 1f / (near - far)
        return Mat4(
            Float4(f / aspect, 0f, 0f, 0f),
            Float4(0f, f, 0f, 0f),
            Float4(0f, 0f, (far + near) * rangeInv, -1f),
            Float4(0f, 0f, 2f * far * near * rangeInv, 0f)
        )
    }

    /** Build a simple look-at view matrix. */
    private fun lookAtMatrix(eye: Float3, center: Float3, up: Float3): Mat4 {
        val f = run {
            val d = center - eye
            val len = kotlin.math.sqrt(d.x * d.x + d.y * d.y + d.z * d.z)
            Float3(d.x / len, d.y / len, d.z / len)
        }
        val s = run {
            val c = Float3(f.y * up.z - f.z * up.y, f.z * up.x - f.x * up.z, f.x * up.y - f.y * up.x)
            val len = kotlin.math.sqrt(c.x * c.x + c.y * c.y + c.z * c.z)
            Float3(c.x / len, c.y / len, c.z / len)
        }
        val u = Float3(s.y * f.z - s.z * f.y, s.z * f.x - s.x * f.z, s.x * f.y - s.y * f.x)
        return Mat4(
            Float4(s.x, u.x, -f.x, 0f),
            Float4(s.y, u.y, -f.y, 0f),
            Float4(s.z, u.z, -f.z, 0f),
            Float4(
                -(s.x * eye.x + s.y * eye.y + s.z * eye.z),
                -(u.x * eye.x + u.y * eye.y + u.z * eye.z),
                f.x * eye.x + f.y * eye.y + f.z * eye.z,
                1f
            )
        )
    }

    @Test
    fun viewToWorldCenterWithIdentity() {
        val worldPos = viewToWorld(Float2(0.5f, 0.5f), 1.0f, Mat4.identity(), Mat4.identity())
        assertTrue(abs(worldPos.x) < 0.01f, "x should be near 0, got ${worldPos.x}")
        assertTrue(abs(worldPos.y) < 0.01f, "y should be near 0, got ${worldPos.y}")
    }

    @Test
    fun worldToViewCenterProjectsToCenter() {
        val proj = perspectiveMatrix(60f, 1f, 0.1f, 100f)
        val view = lookAtMatrix(Float3(0f, 0f, 5f), Float3(0f, 0f, 0f), Float3(0f, 1f, 0f))

        val viewPos = worldToView(Float3(0f, 0f, 0f), proj, view)
        assertTrue(abs(viewPos.x - 0.5f) < 0.1f, "Origin should project near center x, got ${viewPos.x}")
        assertTrue(abs(viewPos.y - 0.5f) < 0.1f, "Origin should project near center y, got ${viewPos.y}")
    }

    @Test
    fun viewToRayDirectionIsNonZero() {
        val proj = perspectiveMatrix(60f, 1f, 0.1f, 100f)
        val view = lookAtMatrix(Float3(0f, 0f, 5f), Float3(0f, 0f, 0f), Float3(0f, 1f, 0f))

        val ray = viewToRay(Float2(0.5f, 0.5f), proj, view)
        val dirLen = kotlin.math.sqrt(
            ray.direction.x * ray.direction.x +
            ray.direction.y * ray.direction.y +
            ray.direction.z * ray.direction.z
        )
        assertTrue(dirLen > 0.01f, "Ray direction should be non-zero, got length $dirLen")
    }

    @Test
    fun viewToRayEdgePointsDiffer() {
        val proj = perspectiveMatrix(60f, 1f, 0.1f, 100f)
        val view = lookAtMatrix(Float3(0f, 0f, 5f), Float3(0f, 0f, 0f), Float3(0f, 1f, 0f))

        val leftRay = viewToRay(Float2(0f, 0.5f), proj, view)
        val rightRay = viewToRay(Float2(1f, 0.5f), proj, view)

        // Left and right rays should have different X directions
        assertTrue(
            abs(leftRay.direction.x - rightRay.direction.x) > 0.01f,
            "Left and right rays should differ in X direction"
        )
    }

    @Test
    fun exposureEV100SunnyDay() {
        // f/16, 1/125s, ISO 100 → sunny day, EV ~15
        val ev = exposureEV100(aperture = 16f, shutterSpeed = 1f / 125f, sensitivity = 100f)
        assertTrue(ev > 14f && ev < 16f, "EV100 should be ~15 for sunny day settings, got $ev")
    }

    @Test
    fun exposureFactorIsInverseOfEV() {
        val ev = 10f
        val factor = exposureFactor(ev)
        assertTrue(abs(factor - 0.1f) < 0.001f, "Factor should be 1/10, got $factor")
    }
}
