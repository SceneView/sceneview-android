package io.github.sceneview.demo.demos.internal

import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.cross
import dev.romainguy.kotlin.math.dot
import dev.romainguy.kotlin.math.length
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import kotlin.math.abs
import kotlin.math.exp

/**
 * The paint, a little behind the finger.
 *
 * A touch screen does not report a curve. It reports the finger 60 to 240 times a second,
 * and Android then resamples those reports onto its frame clock: between two real samples it
 * adds points on the straight line that joins them, and past the last one it extrapolates.
 * Drawn as they come, a fast circle is a polygon — the sides are the real samples, and the
 * interpolated points pin the curve to each side so no spline can round the corners.
 *
 * So the paint does not sit under the finger: it follows it, closing the gap at a rate that
 * makes it [LAG_SECONDS] late. A finger turning a corner pulls the paint through a curve. The
 * lag is a time, not a distance — a slow, careful line is followed to the pixel, a fast sweep
 * trails by a finger's width, under the finger where it cannot be seen.
 *
 * @param paint  Where the stroke is now.
 * @param finger Where the finger was at the last sample.
 */
internal data class SteadyPen(val paint: Position, val finger: Position) {

    /**
     * The pen once the finger has moved to [to], [dtSeconds] after the last sample.
     *
     * This is the exact answer of `paint' = (finger - paint) / LAG_SECONDS` for a finger that
     * went straight from its last position to [to] — not one step of it — so the result is
     * the same curve whether the samples come 1 ms or 30 ms apart. Two samples with one
     * timestamp move the finger and leave the paint where it is.
     */
    fun follow(to: Position, dtSeconds: Float): SteadyPen {
        if (dtSeconds <= 0f) return copy(finger = to)
        val lag = (to - finger) * (LAG_SECONDS / dtSeconds)
        val decay = exp(-dtSeconds / LAG_SECONDS)
        return SteadyPen(paint = to - lag + (paint - finger + lag) * decay, finger = to)
    }

    companion object {
        /**
         * How late the paint is. Thirty milliseconds spans a few touch samples at any rate a
         * phone reports them, which is what rounds the polygon; at a brisk 600 dp/s it is
         * 18 dp, less than the finger covers.
         */
        const val LAG_SECONDS: Float = 0.03f

        /** A pen put down at [point]: the paint starts under the finger. */
        fun at(point: Position) = SteadyPen(paint = point, finger = point)
    }
}

/**
 * Where the *Finger Drawing* demo puts its paint: which surface a touch lands on, how big a
 * dp is there, and — in AR — which anchor a stroke hangs from. [FingerDrawing] turns the
 * points this object finds into a mesh.
 */
internal object FingerDrawingPlacement {

    /** How far in front of the phone the AR mode paints, in metres. */
    const val AR_PAINT_DEPTH_METERS: Float = 0.35f

    /** An AR stroke starting within this distance of an anchor hangs from it. */
    const val AR_ANCHOR_REACH_METERS: Float = 0.75f

    private const val EPSILON = 1e-6f

    /**
     * Where the ray from the eye through a touch point meets the plane through [planePoint]
     * facing [planeNormal], or `null` when the ray runs along the plane or away from it.
     *
     * The 3D mode paints on the plane through the orbit target facing the camera; the AR mode
     * on the plane [AR_PAINT_DEPTH_METERS] in front of the phone. Both are this one function.
     */
    fun planeHit(
        rayOrigin: Position,
        rayDirection: Direction,
        planePoint: Position,
        planeNormal: Direction,
    ): Position? {
        val denominator = dot(rayDirection, planeNormal)
        if (abs(denominator) < EPSILON) return null
        val t = dot(planePoint - rayOrigin, planeNormal) / denominator
        if (t <= 0f) return null
        return rayOrigin + rayDirection * t
    }

    /**
     * Length of one dp of screen on the painted surface: the distance between the hits of
     * two touch points [dpApart] dp apart, divided by [dpApart]. It converts the thickness
     * slider — a screen size, which is what a finger chooses — into a radius in the scene.
     */
    fun unitsPerDp(hitA: Position, hitB: Position, dpApart: Float): Float =
        if (dpApart <= 0f) 0f else length(hitB - hitA) / dpApart

    /**
     * The point the 3D mode's painting plane goes through: [pivot] — the point the camera
     * orbits — as long as it is at least [minDepth] in front of the camera, so every angle
     * paints through the same centre and the strokes cross there. A camera panned or zoomed
     * past the pivot paints [fallbackDepth] in front of itself instead of on a plane behind
     * its back, where no touch could land.
     */
    fun paintPlanePoint(
        cameraPosition: Position,
        forward: Direction,
        pivot: Position,
        minDepth: Float,
        fallbackDepth: Float,
    ): Position =
        if (dot(pivot - cameraPosition, forward) >= minDepth) {
            pivot
        } else {
            cameraPosition + forward * fallbackDepth
        }

    /**
     * Index of the anchor a stroke starting at [point] should hang from: the nearest of
     * [anchorPositions] within [reach], or `null` when the stroke starts somewhere new and
     * needs its own.
     *
     * One anchor per stroke would be the simplest rule, and ARCore pays for every live anchor
     * on every frame. Strokes drawn in one spot share one, so a detailed doodle costs the
     * same as its first line, while a line drawn at the other end of the room still gets an
     * anchor of its own and stays put there.
     */
    fun anchorToReuse(point: Position, anchorPositions: List<Position>, reach: Float): Int? {
        var best: Int? = null
        var bestDistance = reach
        anchorPositions.forEachIndexed { index, anchor ->
            val distance = length(point - anchor)
            if (distance <= bestDistance) {
                best = index
                bestDistance = distance
            }
        }
        return best
    }

    /**
     * [point] expressed in the frame of an anchor at [anchorPosition] with rotation
     * [anchorRotation]. AR strokes are stored relative to their anchor, so when ARCore
     * corrects its map the anchor moves and the paint moves with it.
     */
    fun worldToAnchor(
        point: Position,
        anchorPosition: Position,
        anchorRotation: Quaternion,
    ): Position = rotate(conjugate(anchorRotation), point - anchorPosition)

    /** A world direction in an anchor's frame — [worldToAnchor] without the translation. */
    fun worldDirectionToAnchor(direction: Direction, anchorRotation: Quaternion): Direction =
        rotate(conjugate(anchorRotation), direction)

    private fun conjugate(q: Quaternion) = Quaternion(-q.x, -q.y, -q.z, q.w)

    /** `q · v · q⁻¹` for a unit quaternion, expanded so it needs no matrix. */
    private fun rotate(q: Quaternion, v: Position): Position {
        val u = Position(q.x, q.y, q.z)
        val uv = cross(u, v)
        val uuv = cross(u, uv)
        return v + (uv * q.w + uuv) * 2f
    }
}
