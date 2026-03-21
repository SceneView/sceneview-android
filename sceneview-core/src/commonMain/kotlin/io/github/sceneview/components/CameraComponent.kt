package io.github.sceneview.components

import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.Ray
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform

/**
 * Cross-platform camera component interface.
 *
 * Defines camera projection, view transforms, and coordinate conversions
 * using pure kotlin-math types. Platform modules implement this with
 * Filament (Android), SceneKit (iOS), or other backends.
 */
interface CameraComponent : Component {

    /** Camera projection type. */
    enum class ProjectionType {
        PERSPECTIVE,
        ORTHO
    }

    /** Near clipping plane distance. */
    val near: Float

    /** Far clipping plane distance. */
    val far: Float

    /** The camera's model (world) transform. */
    var modelTransform: Transform

    /** The camera's view transform (inverse of model). */
    val viewTransform: Transform

    /** The camera's projection transform. */
    val projectionTransform: Transform

    /** Forward direction vector in world space. */
    val forwardDirection: Direction

    /** Up direction vector in world space. */
    val upDirection: Direction

    /** Right direction vector in world space. */
    val rightDirection: Direction

    /** Current world-space position. */
    val worldPosition: Position

    /** Current world-space quaternion. */
    val worldQuaternion: Quaternion

    /**
     * Sets a perspective projection.
     *
     * @param fovInDegrees full field of view (in degrees)
     * @param aspect aspect ratio (width/height)
     * @param near near plane distance
     * @param far far plane distance
     */
    fun setProjection(fovInDegrees: Double, aspect: Double, near: Double, far: Double)

    /**
     * Sets a custom projection from explicit frustum planes.
     */
    fun setProjection(
        projection: ProjectionType,
        left: Double, right: Double,
        bottom: Double, top: Double,
        near: Double, far: Double
    )

    /**
     * Sets the camera's look-at parameters.
     *
     * @param eye camera position
     * @param center target to look at
     * @param up up direction
     */
    fun lookAt(eye: Position, center: Position, up: Direction = Direction(y = 1.0f))

    /**
     * Sets camera exposure.
     */
    fun setExposure(aperture: Float, shutterSpeed: Float, sensitivity: Float)

    /**
     * Converts a normalized view coordinate to a world position.
     *
     * @param viewPosition x: (0=left, 1=right), y: (0=bottom, 1=top)
     * @param z depth (0=near, 1=far)
     */
    fun viewToWorld(viewPosition: Float2, z: Float = 1.0f): Position

    /**
     * Converts a world position to a normalized view coordinate.
     */
    fun worldToView(worldPosition: Position): Float2

    /**
     * Creates a ray from a normalized view coordinate.
     */
    fun viewToRay(viewPosition: Float2): Ray
}
