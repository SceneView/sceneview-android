package io.github.sceneview.animation

import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.mix
import dev.romainguy.kotlin.math.normalize
import dev.romainguy.kotlin.math.dot
import kotlin.math.acos
import kotlin.math.exp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ── Easing functions ────────────────────────────────────────────────────────────
// Each takes a fraction in [0..1] and returns a curved fraction in [0..1].

object Easing {
    /** No curve — constant speed. */
    val Linear: (Float) -> Float = { it }

    /** Cubic ease-in — slow start, fast end. */
    val EaseIn: (Float) -> Float = { it * it * it }

    /** Cubic ease-out — fast start, slow end. */
    val EaseOut: (Float) -> Float = { val t = 1f - it; 1f - t * t * t }

    /** Cubic ease-in-out — slow start and end, fast middle. */
    val EaseInOut: (Float) -> Float = { t ->
        if (t < 0.5f) 4f * t * t * t
        else 1f - (-2f * t + 2f).let { it * it * it } / 2f
    }

    /**
     * Spring-based easing using a damped harmonic oscillator.
     *
     * @param dampingRatio 1.0 = critically damped, <1.0 = underdamped (bouncy), >1.0 = overdamped
     * @param stiffness Controls the speed of the oscillation (higher = faster)
     */
    fun spring(dampingRatio: Float = 0.5f, stiffness: Float = 500f): (Float) -> Float = { t ->
        val omega = sqrt(stiffness) // natural frequency
        val zeta = dampingRatio
        if (zeta < 1f) {
            // Underdamped
            val dampedOmega = omega * sqrt(1f - zeta * zeta)
            val envelope = exp(-zeta * omega * t)
            1f - envelope * (cos(dampedOmega * t) + (zeta * omega / dampedOmega) * sin(dampedOmega * t))
        } else {
            // Critically damped or overdamped — simple exponential approach
            1f - (1f + omega * t) * exp(-omega * t)
        }
    }
}

// ── Lerp helpers ────────────────────────────────────────────────────────────────

/** Linearly interpolate between [start] and [end] by [fraction]. */
fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction

/** Linearly interpolate between two [Float4] values. Uses kotlin-math [mix]. */
fun lerp(start: Float4, end: Float4, fraction: Float): Float4 =
    mix(start, end, fraction)

/**
 * Spherical linear interpolation between two [Quaternion] values.
 *
 * Handles the shortest-path case (dot < 0 flips the target) and falls back to
 * linear interpolation for nearly-parallel quaternions.
 */
fun slerp(start: Quaternion, end: Quaternion, fraction: Float): Quaternion {
    // Compute cosine of angle between quaternions
    var cosTheta = dot(start, end)

    // If the dot product is negative, negate one to take the short path
    val adjustedEnd = if (cosTheta < 0f) {
        cosTheta = -cosTheta
        Quaternion(-end.x, -end.y, -end.z, -end.w)
    } else {
        end
    }

    // If quaternions are very close, use linear interpolation to avoid division by zero
    return if (cosTheta > 0.9995f) {
        normalize(
            Quaternion(
                lerp(start.x, adjustedEnd.x, fraction),
                lerp(start.y, adjustedEnd.y, fraction),
                lerp(start.z, adjustedEnd.z, fraction),
                lerp(start.w, adjustedEnd.w, fraction)
            )
        )
    } else {
        val theta = acos(cosTheta)
        val sinTheta = sin(theta)
        val w0 = sin((1f - fraction) * theta) / sinTheta
        val w1 = sin(fraction * theta) / sinTheta
        Quaternion(
            w0 * start.x + w1 * adjustedEnd.x,
            w0 * start.y + w1 * adjustedEnd.y,
            w0 * start.z + w1 * adjustedEnd.z,
            w0 * start.w + w1 * adjustedEnd.w
        )
    }
}
