package io.github.sceneview.scene

import com.google.android.filament.Camera
import com.google.android.filament.EntityManager
import com.google.android.filament.utils.pow
import dev.romainguy.kotlin.math.Float4
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.math.*
import io.github.sceneview.view.*
import com.google.android.filament.utils.Manipulator as CameraManipulator

typealias ClipSpacePosition = Float4

/**
 * Creates and adds a [Camera] component to a given `entity`
 *
 * @param entity `entity` to add the camera component to
 *
 * @return A newly created [Camera]
 *
 * @exception IllegalStateException can be thrown if the {@link Camera} couldn't be created
 */
fun SceneView.createCamera(entity: Entity = EntityManager.get().create()): Camera =
    engine.createCamera(entity).also {
        cameras += it
    }

/**
 * Destroys the [Camera] component associated with the given entity
 */
fun SceneView.destroyCamera(camera: Camera) {
    engine.destroyCameraComponent(camera.entity)
    cameras -= camera
}

/**
 * Computes the camera's EV100 from exposure settings
 *
 * Exposure value (EV) is a number that represents a combination of a camera's shutter speed and
 * f-number, such that all combinations that yield the same exposure have the same EV
 * (for any fixed scene luminance)
 *
 * Reference: https://en.wikipedia.org/wiki/Exposure_value
 */
val Camera.ev100: Float
    get() = (aperture * aperture) / shutterSpeed * 100.0f / sensitivity

/**
 * Computes the exposure normalization factor from the camera's EV100
 *
 * Calculate a unit-less intensity scale from the actual Filament camera settings
 *
 * This method is useful when trying to match the lighting of other engines or tools.
 * Many engines/tools use unit-less light intensities, which can be matched by setting
 * the exposure manually.
 *
 * Filament uses a physically based camera model, with aperture, shutter speed, and sensitivity
 * (ISO).
 * This plays nicely with physical light units (for instance if you measure the sun outside on a
 * sunny day in California you'll get ~110,000 lux and if you set your camera settings to
 * ƒ/16 1/125s ISO 100, exposure will be correct). However many engines use "relative exposure"
 * instead, where exposure is just a float and set to 1.0 by default.
 *
 * ARCore's light estimation was designed to work with those engines.
 * So we scale light intensities estimations by multiplying the values provided by ARCore
 * by 1.0 / log2((aperture * aperture) / shutterSpeed * 100.0f / sensitivity).
 * This should raise ARCore's values to something that's physically plausible for the chosen
 * exposure.
 *
 * **Sources:**
 *  - [https://github.com/ThomasGorisse/SceneformMaintained/pull/156#issuecomment-911873565]
 *  - [https://github.com/google/filament/blob/main/filament/src/Exposure.cpp#L46]
 */
val Camera.exposureFactor get() = 1.0f / (1.2f * ev100)

val Camera.illuminance get() = illuminance(ev100)
fun Camera.illuminance(ev100: Float) = 2.5f * pow(2.0f, ev100)

val Camera.luminance get() = luminance(ev100)
fun Camera.luminance(ev100: Float) = pow(2.0f, ev100 - 3.0f)


/**
 * The camera's projection matrix
 *
 * The projection matrix used for rendering always has its far plane set to infinity.
 * This is why it may differ from the matrix set through setProjection() or setLensProjection().
 */
val Camera.projectionMatrix
    get() = DoubleArray(16).apply { getProjectionMatrix(this) }.toTransformTransposed()

fun Camera.setCustomProjection(transform: Transform, near: Float, far: Float) =
    setCustomProjection(
        transform.toFloatArray().map { it.toDouble() }.toDoubleArray(),
        near.toDouble(),
        far.toDouble()
    )

/**
 * The camera's view matrix. The view matrix is the inverse of the model matrix
 */
val Camera.viewMatrix get() = FloatArray(16).apply { getViewMatrix(this) }.toTransformTransposed()

/**
 * The camera's model matrix. The model matrix encodes the camera position and orientation, or pose
 */
var Camera.modelMatrix: Transform
    get() = FloatArray(16).apply { getModelMatrix(this) }.toTransform()
    set(value) {
        setModelMatrix(value.toFloatArray())
    }

/**
 * @see viewPortToClipSpace
 * @see viewSpaceToWorld
 */
fun Camera.clipSpaceToViewSpace(clipSpacePosition: ClipSpacePosition): Position =
    inverse(projectionMatrix) * clipSpacePosition.xyz * clipSpacePosition.w

/**
 * @see worldToViewSpace
 * @see clipSpaceToViewPort
 */
fun Camera.viewSpaceToClipSpace(viewSpacePosition: Position): ClipSpacePosition {
    val clipSpacePosition = projectionMatrix * ClipSpacePosition(viewSpacePosition.xyz, 1.0f)
    return ClipSpacePosition(clipSpacePosition.xyz / clipSpacePosition.w, w = clipSpacePosition.w)
}

/**
 * @see viewPortToClipSpace
 * @see clipSpaceToViewSpace
 */
fun Camera.viewSpaceToWorld(viewSpacePosition: Position): Position =
    inverse(viewMatrix) * viewSpacePosition

/**
 * @see viewSpaceToClipSpace
 * @see clipSpaceToViewPort
 */
fun Camera.worldToViewSpace(worldPosition: Position): Position =
    viewMatrix * worldPosition


/**
 * Sets the camera's model matrix.
 *
 * @param eye position of the camera in world space
 * @param center the point in world space the camera is looking at
 * @param up coordinate of a unit vector denoting the camera's "up" direction
 */
fun Camera.lookAt(eye: Position, center: Position, up: Direction) {
    lookAt(
        eye.x.toDouble(), eye.y.toDouble(), eye.z.toDouble(),
        center.x.toDouble(), center.y.toDouble(), center.z.toDouble(),
        up.x.toDouble(), up.y.toDouble(), up.z.toDouble()
    )
}

/**
 * The current orthonormal basis. This is usually called once per frame
 */
fun CameraManipulator.getLookAt(): LookAt =
    (List(3) { FloatArray(3) }).apply {
        getLookAt(this[0], this[1], this[2])
    }.let { (eye, target, upward) ->
        LookAt(eye.toPosition(), target.toPosition(), upward.toDirection())
    }

/**
 * Sets world-space position of interest, which defaults to (0,0,0)
 */
fun CameraManipulator.Builder.targetPosition(position: Position) =
    targetPosition(position.x, position.y, position.z)
