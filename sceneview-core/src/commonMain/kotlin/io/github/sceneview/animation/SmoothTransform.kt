package io.github.sceneview.animation

import io.github.sceneview.math.Transform
import io.github.sceneview.math.equals
import io.github.sceneview.math.slerp

/**
 * Platform-independent smooth transform interpolation state.
 *
 * Tracks a current transform being interpolated toward a target transform using
 * speed-scaled slerp/lerp. Pure data — no mutation, no side effects.
 *
 * @param current Current transform value.
 * @param target Target transform to interpolate toward (null = no active interpolation).
 * @param speed Interpolation speed multiplier. Higher = faster convergence.
 * @param convergenceDelta Threshold below which the transform is considered "arrived".
 */
data class SmoothTransformState(
    val current: Transform,
    val target: Transform? = null,
    val speed: Float = 10f,
    val convergenceDelta: Float = 0.001f
)

/**
 * Result of a smooth transform update step.
 *
 * @param state The updated smooth transform state.
 * @param arrived True if the interpolation just completed (target was reached).
 */
data class SmoothTransformResult(
    val state: SmoothTransformState,
    val arrived: Boolean
)

/**
 * Advance the smooth transform interpolation by [deltaSeconds].
 *
 * Pure function — no mutation, no side effects.
 *
 * - If no target is set, returns the state unchanged with `arrived = false`.
 * - If the interpolated transform is within [SmoothTransformState.convergenceDelta]
 *   of the target, snaps to the target and clears it (`arrived = true`).
 * - Otherwise, returns the slerp'd intermediate transform.
 *
 * @param state Current interpolation state.
 * @param deltaSeconds Time step in seconds since last frame.
 * @return Updated state and whether the target was just reached.
 */
fun updateSmoothTransform(
    state: SmoothTransformState,
    deltaSeconds: Double
): SmoothTransformResult {
    val target = state.target ?: return SmoothTransformResult(state, arrived = false)

    if (target == state.current) {
        return SmoothTransformResult(
            state = state.copy(target = null),
            arrived = true
        )
    }

    val interpolated = slerp(
        start = state.current,
        end = target,
        deltaSeconds = deltaSeconds,
        speed = state.speed
    )

    return if (interpolated.equals(state.current, delta = state.convergenceDelta)) {
        // Close enough — snap to target
        SmoothTransformResult(
            state = state.copy(current = target, target = null),
            arrived = true
        )
    } else {
        SmoothTransformResult(
            state = state.copy(current = interpolated),
            arrived = false
        )
    }
}

/**
 * Set a new target for smooth interpolation.
 *
 * @param state Current state.
 * @param target New target transform.
 * @param speed Optional new speed (keeps current speed if null).
 * @return Updated state with the new target.
 */
fun setSmoothTarget(
    state: SmoothTransformState,
    target: Transform,
    speed: Float? = null
): SmoothTransformState = state.copy(
    target = target,
    speed = speed ?: state.speed
)

/**
 * Cancel any active smooth interpolation without snapping to the target.
 */
fun cancelSmooth(state: SmoothTransformState): SmoothTransformState =
    state.copy(target = null)
