package io.github.sceneview.math

import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.length
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplinesTest {

    // --- Catmull-Rom ---

    @Test
    fun catmullRomAtT0ReturnsP1() {
        val p0 = Float3(0f, 0f, 0f)
        val p1 = Float3(1f, 0f, 0f)
        val p2 = Float3(2f, 0f, 0f)
        val p3 = Float3(3f, 0f, 0f)
        val result = catmullRom(p0, p1, p2, p3, 0f)
        assertEquals(1f, result.x, 1e-5f)
        assertEquals(0f, result.y, 1e-5f)
    }

    @Test
    fun catmullRomAtT1ReturnsP2() {
        val p0 = Float3(0f, 0f, 0f)
        val p1 = Float3(1f, 0f, 0f)
        val p2 = Float3(2f, 0f, 0f)
        val p3 = Float3(3f, 0f, 0f)
        val result = catmullRom(p0, p1, p2, p3, 1f)
        assertEquals(2f, result.x, 1e-5f)
    }

    @Test
    fun catmullRomMidpointOnLine() {
        val p0 = Float3(0f)
        val p1 = Float3(1f, 0f, 0f)
        val p2 = Float3(2f, 0f, 0f)
        val p3 = Float3(3f, 0f, 0f)
        val mid = catmullRom(p0, p1, p2, p3, 0.5f)
        assertEquals(1.5f, mid.x, 1e-4f)
    }

    @Test
    fun catmullRomSplineReturnsCorrectPointCount() {
        val points = listOf(
            Float3(0f), Float3(1f, 0f, 0f),
            Float3(2f, 1f, 0f), Float3(3f, 0f, 0f)
        )
        val spline = catmullRomSpline(points, segments = 10)
        // 1 span * 10 segments + 1 final point = 11
        assertEquals(11, spline.size)
    }

    @Test
    fun catmullRomSplineMultiSpan() {
        val points = listOf(
            Float3(0f), Float3(1f, 0f, 0f),
            Float3(2f, 0f, 0f), Float3(3f, 0f, 0f),
            Float3(4f, 0f, 0f)
        )
        val spline = catmullRomSpline(points, segments = 8)
        // 2 spans * 8 segments + 1 = 17
        assertEquals(17, spline.size)
    }

    @Test
    fun catmullRomSplineRequiresFourPoints() {
        var threw = false
        try { catmullRomSpline(listOf(Float3(0f), Float3(1f, 0f, 0f), Float3(2f, 0f, 0f))) }
        catch (_: IllegalArgumentException) { threw = true }
        assertTrue(threw)
    }

    // --- Cubic Bezier ---

    @Test
    fun cubicBezierAtT0ReturnsP0() {
        val p0 = Float3(0f)
        val p3 = Float3(1f, 0f, 0f)
        val result = cubicBezier(p0, Float3(0.3f, 0.5f, 0f), Float3(0.7f, 0.5f, 0f), p3, 0f)
        assertEquals(0f, result.x, 1e-5f)
    }

    @Test
    fun cubicBezierAtT1ReturnsP3() {
        val p0 = Float3(0f)
        val p3 = Float3(1f, 0f, 0f)
        val result = cubicBezier(p0, Float3(0.3f, 0.5f, 0f), Float3(0.7f, 0.5f, 0f), p3, 1f)
        assertEquals(1f, result.x, 1e-5f)
    }

    @Test
    fun cubicBezierStraightLine() {
        val p0 = Float3(0f)
        val p1 = Float3(1f / 3f, 0f, 0f)
        val p2 = Float3(2f / 3f, 0f, 0f)
        val p3 = Float3(1f, 0f, 0f)
        val mid = cubicBezier(p0, p1, p2, p3, 0.5f)
        assertEquals(0.5f, mid.x, 1e-4f)
    }

    @Test
    fun cubicBezierTangentNonZero() {
        val tan = cubicBezierTangent(
            Float3(0f), Float3(0f, 1f, 0f), Float3(1f, 1f, 0f), Float3(1f, 0f, 0f), 0.5f
        )
        assertTrue(length(tan) > 0.01f, "Tangent should be non-zero")
    }

    @Test
    fun cubicBezierSplinePointCount() {
        val spline = cubicBezierSpline(
            Float3(0f), Float3(0f, 1f, 0f), Float3(1f, 1f, 0f), Float3(1f, 0f, 0f),
            segments = 20
        )
        assertEquals(21, spline.size)
    }

    // --- Quadratic Bezier ---

    @Test
    fun quadraticBezierEndpoints() {
        val p0 = Float3(0f)
        val p1 = Float3(0.5f, 1f, 0f)
        val p2 = Float3(1f, 0f, 0f)
        val start = quadraticBezier(p0, p1, p2, 0f)
        val end = quadraticBezier(p0, p1, p2, 1f)
        assertEquals(0f, start.x, 1e-5f)
        assertEquals(1f, end.x, 1e-5f)
    }
}
