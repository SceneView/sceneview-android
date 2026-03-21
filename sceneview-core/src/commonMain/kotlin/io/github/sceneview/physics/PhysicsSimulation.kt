package io.github.sceneview.physics

import io.github.sceneview.math.Position
import kotlin.math.abs

/**
 * Pure, platform-independent rigid-body physics state.
 *
 * Uses simple Euler integration — no external physics library dependencies.
 * Supports gravity, floor collision with restitution (bounce), and sleep detection.
 *
 * Physics coordinate system: +Y is up, gravity pulls in -Y direction.
 *
 * @param position Current world-space position.
 * @param velocity Current linear velocity in m/s (world space).
 * @param restitution Coefficient of restitution [0..1]. 0 = inelastic, 1 = perfectly elastic.
 * @param floorY World-space Y coordinate of the floor plane.
 * @param radius Collision radius in meters (bottom of sphere = position.y - radius).
 * @param isAsleep True once the body has come to rest.
 */
data class PhysicsState(
    val position: Position = Position(),
    val velocity: Position = Position(),
    val restitution: Float = 0.6f,
    val floorY: Float = 0f,
    val radius: Float = 0f,
    val isAsleep: Boolean = false
)

/** Gravitational acceleration in m/s² (downward along -Y). */
const val GRAVITY = -9.8f

/** Velocities below this threshold are zeroed to stop micro-bouncing. */
const val SLEEP_THRESHOLD = 0.05f

/**
 * Advance the physics simulation by [deltaSeconds].
 *
 * Pure function — no mutation, no side effects. Returns a new [PhysicsState].
 *
 * @param state Current physics state.
 * @param deltaSeconds Time step in seconds (clamped to 0..0.05 internally to avoid instability).
 * @return Updated physics state after one integration step.
 */
fun simulateStep(state: PhysicsState, deltaSeconds: Float): PhysicsState {
    if (state.isAsleep) return state

    val dt = deltaSeconds.coerceIn(0f, 0.05f)

    // Apply gravity to vertical velocity
    val newVelocity = Position(
        x = state.velocity.x,
        y = state.velocity.y + GRAVITY * dt,
        z = state.velocity.z
    )

    // Integrate position
    var newPosition = Position(
        x = state.position.x + newVelocity.x * dt,
        y = state.position.y + newVelocity.y * dt,
        z = state.position.z + newVelocity.z * dt
    )

    // Floor collision
    val contactY = state.floorY + state.radius
    if (newPosition.y < contactY) {
        newPosition = Position(newPosition.x, contactY, newPosition.z)
        val reboundVy = -newVelocity.y * state.restitution

        // Sleep when rebound speed is negligible
        return if (abs(reboundVy) < SLEEP_THRESHOLD) {
            state.copy(
                position = newPosition,
                velocity = Position(newVelocity.x, 0f, newVelocity.z),
                isAsleep = true
            )
        } else {
            state.copy(
                position = newPosition,
                velocity = Position(newVelocity.x, reboundVy, newVelocity.z)
            )
        }
    }

    return state.copy(position = newPosition, velocity = newVelocity)
}

/**
 * Apply an instantaneous impulse to the physics state.
 *
 * @param state Current physics state.
 * @param impulse Velocity change in m/s (world space).
 * @return Updated state with modified velocity, woken up if sleeping.
 */
fun applyImpulse(state: PhysicsState, impulse: Position): PhysicsState {
    return state.copy(
        velocity = Position(
            x = state.velocity.x + impulse.x,
            y = state.velocity.y + impulse.y,
            z = state.velocity.z + impulse.z
        ),
        isAsleep = false
    )
}
