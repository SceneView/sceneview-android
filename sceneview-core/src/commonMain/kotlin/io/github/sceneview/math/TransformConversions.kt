package io.github.sceneview.math

import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse

/**
 * Platform-independent local↔world coordinate conversions.
 *
 * These functions convert positions, rotations, scales, and full transforms
 * between a node's local space and world space using the node's world transform
 * matrix. No renderer dependency — works with any engine that provides a
 * 4×4 world transform.
 *
 * Usage pattern:
 * ```
 * val worldToLocal = inverse(node.worldTransform)
 * val localPos = worldToLocalPosition(worldPos, worldToLocal)
 * ```
 */

// --- Position ---

/**
 * Convert a world-space position to this node's local space.
 *
 * @param worldPosition Position in world space.
 * @param worldToLocal Inverse of the node's world transform (`inverse(worldTransform)`).
 */
fun worldToLocalPosition(worldPosition: Position, worldToLocal: Transform): Position =
    worldToLocal * worldPosition

/**
 * Convert a local-space position to world space.
 *
 * @param localPosition Position in this node's local space.
 * @param worldTransform The node's world transform matrix.
 */
fun localToWorldPosition(localPosition: Position, worldTransform: Transform): Position =
    worldTransform * localPosition

// --- Quaternion ---

/**
 * Convert a world-space quaternion to this node's local space.
 *
 * @param worldQuaternion Quaternion in world space.
 * @param worldToLocal Inverse of the node's world transform.
 */
fun worldToLocalQuaternion(
    worldQuaternion: Quaternion,
    worldToLocal: Transform
): Quaternion = worldToLocal.quaternion * worldQuaternion

/**
 * Convert a local-space quaternion to world space.
 *
 * @param localQuaternion Quaternion in this node's local space.
 * @param worldTransform The node's world transform matrix.
 */
fun localToWorldQuaternion(
    localQuaternion: Quaternion,
    worldTransform: Transform
): Quaternion = worldTransform.quaternion * localQuaternion

// --- Rotation (Euler angles) ---

/**
 * Convert a world-space rotation (Euler angles) to this node's local space.
 *
 * @param worldRotation Rotation in degrees (world space).
 * @param worldToLocal Inverse of the node's world transform.
 */
fun worldToLocalRotation(worldRotation: Rotation, worldToLocal: Transform): Rotation =
    worldToLocalQuaternion(worldRotation.toQuaternion(), worldToLocal).toRotation()

/**
 * Convert a local-space rotation (Euler angles) to world space.
 *
 * @param localRotation Rotation in degrees (local space).
 * @param worldTransform The node's world transform matrix.
 */
fun localToWorldRotation(localRotation: Rotation, worldTransform: Transform): Rotation =
    localToWorldQuaternion(localRotation.toQuaternion(), worldTransform).toRotation()

// --- Scale ---

/**
 * Convert a world-space scale to this node's local space.
 *
 * @param worldScale Scale in world space.
 * @param worldToLocal Inverse of the node's world transform.
 */
fun worldToLocalScale(worldScale: Scale, worldToLocal: Transform): Scale =
    worldToLocal * worldScale

/**
 * Convert a local-space scale to world space.
 *
 * @param localScale Scale in this node's local space.
 * @param worldTransform The node's world transform matrix.
 */
fun localToWorldScale(localScale: Scale, worldTransform: Transform): Scale =
    worldTransform * localScale

// --- Full Transform ---

/**
 * Convert a world-space transform to this node's local space.
 *
 * @param worldTransformToConvert Transform matrix in world space to convert.
 * @param worldToLocal Inverse of the node's world transform.
 */
fun worldToLocalTransform(
    worldTransformToConvert: Transform,
    worldToLocal: Transform
): Transform = worldToLocal * worldTransformToConvert

/**
 * Convert a local-space transform to world space.
 *
 * @param localTransform Transform matrix in this node's local space.
 * @param worldTransform The node's world transform matrix.
 */
fun localToWorldTransform(
    localTransform: Transform,
    worldTransform: Transform
): Transform = worldTransform * localTransform

// --- Convenience: compute worldToLocal from worldTransform ---

/**
 * Compute the inverse world transform (world-to-local matrix).
 *
 * @param worldTransform The node's world transform matrix.
 * @return The inverse matrix for converting world→local coordinates.
 */
fun computeWorldToLocal(worldTransform: Transform): Transform = inverse(worldTransform)
