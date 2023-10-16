package io.github.sceneview.components

import android.animation.ObjectAnimator
import com.google.android.filament.TransformManager
import dev.romainguy.kotlin.math.*
import io.github.sceneview.Entity
import io.github.sceneview.EntityInstance
import io.github.sceneview.SceneView
import io.github.sceneview.animation.TransformAnimator
import io.github.sceneview.managers.getAllChildren
import io.github.sceneview.managers.getChildren
import io.github.sceneview.math.*

const val defaultSmoothSpeed = 1.0f

interface TransformComponent : Component {

    val transformManager get() = engine.transformManager
    val transformInstance: EntityInstance get() = transformManager.getInstance(entity)

    /**
     * Returns whether a particular [Entity] is associated with a component of the
     * [TransformManager].
     *
     * @return true if this [Entity] has a component associated with this manager
     */
    val hasComponent get() = transformManager.hasComponent(entity)

    /**
     * Parent entity from this transform.
     *
     * It is an error to re-parent an entity to a descendant and will cause undefined behaviour.
     *
     * @see TransformManager.getParent
     * @see TransformManager.setTransform
     */
    var parentEntity: Entity?
        get() = transformManager.getParent(transformInstance).takeIf { it != 0 }
        set(value) {
            transformManager.setParent(transformInstance, value?.let {
                transformManager.getInstance(value)
            } ?: 0)
        }

    val parentInstance: EntityInstance?
        get() = parentEntity?.let { transformManager.getInstance(it) }

    /**
     * Returns the number of children of an [EntityInstance].
     *
     * @return The number of children of the queried component
     *
     * @see TransformManager.getChildCount
     */
    val childCount: Int
        get() = transformManager.getChildCount(transformInstance)

    /**
     * Gets a list of children for a transform component.
     *
     * @return Array of retrieved children [Entity]
     *
     * @see TransformManager.getChildren
     */
    val childEntities: List<Entity> get() = transformManager.getChildren(transformInstance)

    /**
     * Gets a flat list of all children within the hierarchy for a transform component.
     *
     * @return Array of retrieved children [Entity].
     *
     * @see childEntities
     */
    val allChildEntities: List<Entity> get() = transformManager.getAllChildren(transformInstance)

    /**
     * Position to locate within the coordinate system the parent.
     *
     * Default is `Position(x = 0.0f, y = 0.0f, z = 0.0f)`, indicating that the component is placed
     * at the origin of the parent component's coordinate system.
     *
     * **Horizontal (X):**
     * - left: x < 0.0f
     * - center horizontal: x = 0.0f
     * - right: x > 0.0f
     *
     * **Vertical (Y):**
     * - top: y > 0.0f
     * - center vertical : y = 0.0f
     * - bottom: y < 0.0f
     *
     * **Depth (Z):**
     * - forward: z < 0.0f
     * - origin/camera position: z = 0.0f
     * - backward: z > 0.0f
     *
     * ------- +y ----- -z
     *
     * ---------|----/----
     *
     * ---------|--/------
     *
     * -x - - - 0 - - - +x
     *
     * ------/--|---------
     *
     * ----/----|---------
     *
     * +z ---- -y --------
     *
     * @see transform
     */
    var position: Position
        get() = transform.position
        set(value) {
            this.transform = Transform(value, quaternion, scale)
        }

    /**
     * World-space position.
     *
     * The world position of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local position with its parent's world position.
     *
     * @see worldTransform
     */
    var worldPosition: Position
        get() = worldTransform.position
        set(value) {
            position = worldToParent * value
        }

    /**
     * Quaternion rotation.
     *
     * @see transform
     */
    var quaternion: Quaternion
        get() = transform.quaternion
        set(value) {
            transform = Transform(position, value, scale)
        }

    /**
     * The world-space quaternion.
     *
     * The world quaternion of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local quaternion with its parent's world
     * quaternion.
     *
     * @see worldTransform
     */
    var worldQuaternion: Quaternion
        get() = worldTransform.toQuaternion()
        set(value) {
            quaternion = worldToParent.toQuaternion() * value
        }

    /**
     * Orientation in Euler Angles Degrees per axis from `0.0f` to `360.0f`.
     *
     * The three-component rotation vector specifies the direction of the rotation axis in degrees.
     * Rotation is applied relative to the component's origin property.
     *
     * Default is `Rotation(x = 0.0f, y = 0.0f, z = 0.0f)`, specifying no rotation.
     *
     * Note that modifying the individual components of the returned rotation doesn't have any
     * effect.
     *
     * ------- +y ----- -z
     *
     * ---------|----/----
     *
     * ---------|--/------
     *
     * -x - - - 0 - - - +x
     *
     * ------/--|---------
     *
     * ----/----|---------
     *
     * +z ---- -y --------
     *
     * @see transform
     */
    var rotation: Rotation
        get() = quaternion.toEulerAngles()
        set(value) {
            quaternion = Quaternion.fromEuler(value)
        }

    /**
     * World-space rotation.
     *
     * The world rotation of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local rotation with its parent's world
     * rotation.
     *
     * @see worldTransform
     */
    var worldRotation: Rotation
        get() = worldTransform.rotation
        set(value) {
            quaternion = worldToParent.toQuaternion() * Quaternion.fromEuler(value)
        }

    /**
     * Scale on each axis.
     *
     * Reduce (`scale < 1.0f`) / Increase (`scale > 1.0f`)
     *
     * @see transform
     */
    var scale: Scale
        get() = transform.scale
        set(value) {
            transform = Transform(position, quaternion, value)
        }

    /**
     * World-space scale.
     *
     * The world scale of this component (i.e. relative to the [SceneView]).
     * This is the composition of this component's local scale with its parent's world
     * scale.
     *
     * @see worldTransform
     */
    var worldScale: Scale
        get() = worldTransform.scale
        set(value) {
            scale = (worldToParent * scale(value)).scale
        }

    /**
     * Local transform of the transform component (i.e. relative to the parent).
     *
     * @see TransformManager.getTransform
     * @see TransformManager.setTransform
     */
    var transform: Transform
        get() = transpose(FloatArray(16).apply {
            transformManager.getTransform(transformInstance, this)
        }.toTransform())
        set(value) {
            transformManager.setTransform(transformInstance, transpose(value).toFloatArray())
        }

    /**
     * World transform of a transform component (i.e. relative to the root).
     *
     * @see TransformManager.getWorldTransform
     */
    val worldTransform: Transform
        get() = transpose(FloatArray(16).apply {
            transformManager.getWorldTransform(transformInstance, this)
        }.toTransform())

    /**
     * Transform from the world coordinate system to the coordinate system of the parent.
     *
     * @see TransformManager.getWorldTransform
     */
    val worldToParent: Transform
        get() = parentInstance?.let { parentInstance ->
            inverse(Transform.of(*FloatArray(16).apply {
                transformManager.getWorldTransform(parentInstance, this)
            }))
        } ?: Transform()

    /**
     * Creates a transform component and associates it with the given entity. The component is
     * initialized with the identity transform.
     * If this component already exists on the given entity, it is first
     * destroyed as if [.destroy] was called.
     *
     * @see TransformManager.create
     */
    fun createComponent(): EntityInstance = transformManager.create(entity)

    /**
     * Apply a scale to all axis.
     *
     * - reduce size: scale < 1.0f
     * - same size: scale = 1.0f
     * - increase size: scale > 1.0f
     *
     * @see scale
     */
    fun setScale(scale: Float) {
        this.scale.xyz = Scale(scale)
    }

    /**
     * Apply multiple transforms at at same time.
     *
     * @see position
     * @see quaternion
     * @see scale
     * @see smooth
     */
    fun transform(
        position: Position = this.position,
        quaternion: Quaternion = this.quaternion,
        scale: Scale = this.scale,
        smooth: Boolean = false,
        smoothSpeed: Float = defaultSmoothSpeed
    ) {
        if (smooth) {
            smooth(position, quaternion, scale, smoothSpeed)
        } else {
            this.position = position
            this.quaternion = quaternion
            this.scale = scale
        }
    }

    /**
     * Apply multiple transforms at at same time.
     *
     * @see position
     * @see rotation
     * @see scale
     * @see smooth
     */
    fun transform(
        position: Position = this.position,
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale,
        smooth: Boolean = false,
        smoothSpeed: Float = defaultSmoothSpeed
    ) = transform(position, rotation.toQuaternion(), scale, smooth, smoothSpeed)

    /**
     * Smooth move, rotate and scale at a specified speed.
     *
     * @see position
     * @see quaternion
     * @see scale
     * @see speed
     */
    fun smooth(
        position: Position = this.position,
        quaternion: Quaternion = this.quaternion,
        scale: Scale = this.scale,
        speed: Float = defaultSmoothSpeed
    ) = smooth(Transform(position, quaternion, scale), speed)

    /**
     * Smooth move, rotate and scale at a specified speed.
     *
     * @see position
     * @see quaternion
     * @see scale
     * @see speed
     */
    fun smooth(
        position: Position = this.position,
        rotation: Rotation = this.rotation,
        scale: Scale = this.scale,
        speed: Float = defaultSmoothSpeed
    ) = smooth(Transform(position, rotation, scale), speed)

    /**
     * Smooth move, rotate and scale at a specified speed.
     *
     * @see transform
     */
    fun smooth(transform: Transform, speed: Float = defaultSmoothSpeed) {
        val maxLength = floatArrayOf(
            distance(this.position, transform.position),
            length(this.quaternion - transform.quaternion),
            distance(this.scale, transform.scale)
        ).maxOrNull()!!
        TransformAnimator.ofTransform(this, transform).apply {
            setAutoCancel(true)
            duration = (maxLength * speed).toLong()
        }.start()
    }

    fun animatePositions(vararg positions: Position): ObjectAnimator =
        TransformAnimator.ofPosition(this, *positions)

    fun animateQuaternions(vararg quaternions: Quaternion): ObjectAnimator =
        TransformAnimator.ofQuaternion(this, *quaternions)

    fun animateRotations(vararg rotations: Rotation): ObjectAnimator =
        TransformAnimator.ofRotation(this, *rotations)

    fun animateScales(vararg scales: Scale): ObjectAnimator =
        TransformAnimator.ofScale(this, *scales)

    fun animateTransforms(vararg transforms: Transform): ObjectAnimator =
        TransformAnimator.ofTransform(this, *transforms)

    /**
     * Rotate the transform to face a point in world-space.
     *
     * @param targetPosition The target position to look at in world space
     * @param smooth Whether the rotation should happen smoothly
     */
    fun lookAt(
        eye: Position = worldPosition,
        targetPosition: Position,
        smooth: Boolean = false
    ) {
        if (eye != worldPosition) {
            worldPosition = eye
        }
        val newQuaternion = lookAt(
            worldPosition,
            targetPosition,
            Direction(y = 1.0f)
        ).toQuaternion()
        if (smooth) {
            smooth(quaternion = newQuaternion)
        } else {
            transform(quaternion = newQuaternion)
        }
    }

    /**
     * Rotates the transform to face another [TransformComponent].
     *
     * @param targetComponent The target [TransformComponent] to look at
     * @param smooth Whether the rotation should happen smoothly
     */
    fun lookAt(
        eye: Position = worldPosition,
        targetComponent: TransformComponent,
        smooth: Boolean = false
    ) = lookAt(eye, targetComponent.worldPosition, smooth)

    /**
     * Rotates the transform to face a direction in world-space.
     *
     * The look direction and up direction cannot be coincident (parallel) or the orientation will
     * be invalid.
     *
     * @param lookDirection The desired look direction in world-space
     * @param smooth Whether the rotation should happen smoothly
     */
    fun lookTowards(lookDirection: Direction, smooth: Boolean = false) {
        val newQuaternion =
            lookTowards(worldPosition, -lookDirection, Direction(y = 1.0f)).toQuaternion()
        if (smooth) {
            smooth(quaternion = newQuaternion)
        } else {
            transform(quaternion = newQuaternion)
        }
    }
}