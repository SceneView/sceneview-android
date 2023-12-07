package io.github.sceneview.components

import com.google.android.filament.TransformManager
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import dev.romainguy.kotlin.math.lookAt
import dev.romainguy.kotlin.math.lookTowards
import dev.romainguy.kotlin.math.transform
import io.github.sceneview.SceneView
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.TransformProvider
import io.github.sceneview.gesture.transform
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Transform
import io.github.sceneview.math.quaternion
import io.github.sceneview.math.times
import io.github.sceneview.math.toColumnsFloatArray
import io.github.sceneview.math.toMatrix
import io.github.sceneview.math.toQuaternion
import io.github.sceneview.math.toTransform

/**
 * A Transform Component represents a transformation within the scene graph's hierarchy.
 *
 * [TransformManager] is used to add transform components to entities.
 *
 *  A transform component gives an entity a position and orientation in space in the coordinate
 *  space of its parent transform. The <code>TransformManager</code> takes care of computing the
 *  world-space transform of each component (i.e. its transform relative to the root).
 *  *
 *  * <h1>Creation and destruction</h1>
 *  *
 *  * A transform component is created using {@link TransformManager#create} and destroyed by calling
 *  * {@link TransformManager#destroy}.
 *
 * Each transform can have an arbitrary number of child components and one parent. The parent may be
 * another component or null.
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
 */
interface TransformComponent : Component, TransformProvider {

    val transformManager get() = engine.transformManager
    val transformInstance get() = transformManager.getInstance(entity)

    fun hasTransformComponent() = transformManager.hasComponent(entity)
    fun createTransformComponent() = transformManager.create(entity)

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
            transform = Transform(value, quaternion, scale)
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
            position = parentComponent?.getLocalPosition(value) ?: value
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
            quaternion = parentComponent?.getLocalQuaternion(value) ?: value
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
     * This is the composition of this component's local rotation with its parent's world rotation.
     *
     * @see worldTransform
     */
    var worldRotation: Rotation
        get() = worldTransform.rotation
        set(value) {
            worldQuaternion = Quaternion.fromEuler(value)
        }

    /**
     * Scale on each axis.
     *
     * Reduce (`scale < 1.0f`) / Increase (`scale > 1.0f`).
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
     * This is the composition of this component's local scale with its parent's world scale.
     *
     * @see worldTransform
     */
    var worldScale: Scale
        get() = worldTransform.scale
        set(value) {
            scale = parentComponent?.getLocalScale(value) ?: value
        }

    /**
     * The local transform of a transform component. (i.e. relative to the parent).
     *
     * This operation can be slow if the hierarchy of transform is too deep, and this will be
     * particularly bad when updating a lot of transforms. In that case, consider using
     * [TransformManager.openLocalTransformTransaction] /
     * [TransformManager.commitLocalTransformTransaction].
     *
     * @see TransformManager.getTransform
     * @see TransformManager.setTransform
     */
    var transform: Transform
        get() = FloatArray(16).apply {
            transformManager.getTransform(transformInstance, this)
        }.toTransform()
        set(value) {
            transformManager.setTransform(transformInstance, value.toColumnsFloatArray())
            onTransformChanged()
        }

    /**
     * The world transform of a transform component. (i.e. relative to the root).
     *
     * This is the composition of this component's local transform with its parent's world transform.
     * @see TransformManager.setTransform
     */
    var worldTransform: Transform
        get() = FloatArray(16).apply {
            transformManager.getWorldTransform(transformInstance, this)
        }.toTransform()
        set(value) {
            transform = parentComponent?.getLocalTransform(value) ?: value
        }

    /**
     * Transform from the world coordinate system to the coordinate system of this node.
     */
    val worldToLocal: Transform get() = inverse(worldTransform)

    /**
     * Re-parents an entity to a new one.
     *
     * It is an error to re-parent an entity to a descendant and will cause undefined behaviour.
     */
    var parentComponent: TransformComponent?
        get() = transformManager.getParent(transformInstance).takeIf { it != 0 }?.let { entity ->
            object : TransformComponent {
                override val engine get() = this@TransformComponent.engine
                override val entity get() = entity
            }
        }
        set(value) {
            transformManager.setParent(transformInstance, value?.transformInstance ?: 0)
            onParentChanged()
        }

    /**
     * Gets a list of children for a transform component.
     *
     * @return Array of retrieved children [TransformComponent].
     *
     * @see TransformManager.getChildren
     */
    var childComponents: Set<TransformComponent>
        get() = transformManager.getChildren(transformInstance, null).map { entity ->
            object : TransformComponent {
                override val engine get() = this@TransformComponent.engine
                override val entity get() = entity
            }
        }.toSet()
        set(value) {
            value.forEach { it.parentComponent = this }
            onTransformChanged()
        }

    /**
     * Converts a position in the world-space to a local-space of this node.
     *
     * @param worldPosition the position in world-space to convert.
     * @return a new position that represents the world position in local-space.
     */
    fun getLocalPosition(worldPosition: Position) = worldToLocal * worldPosition

    /**
     * Converts a position in the local-space of this node to world-space.
     *
     * @param localPosition the position in local-space to convert.
     * @return a new position that represents the local position in world-space.
     */
    fun getWorldPosition(localPosition: Position) = worldTransform * localPosition

    /**
     * Converts a quaternion in the world-space to a local-space of this node.
     *
     * @param worldQuaternion the quaternion in world-space to convert.
     * @return a new quaternion that represents the world quaternion in local-space.
     */
    fun getLocalQuaternion(worldQuaternion: Quaternion) =
        worldToLocal.toQuaternion() * worldQuaternion

    /**
     * Converts a quaternion in the local-space of this node to world-space.
     *
     * @param quaternion the quaternion in local-space to convert.
     * @return a new quaternion that represents the local quaternion in world-space.
     */
    fun getWorldQuaternion(quaternion: Quaternion) = worldTransform.toQuaternion() * quaternion

    /**
     * Converts a rotation in the world-space to a local-space of this node.
     *
     * @param worldRotation the rotation in world-space to convert.
     * @return a new rotation that represents the world rotation in local-space.
     */
    fun getLocalRotation(worldRotation: Rotation) =
        getLocalQuaternion(Quaternion.fromEuler(worldRotation)).toEulerAngles()

    /**
     * Converts a rotation in the local-space of this node to world-space.
     *
     * @param rotation the rotation in local-space to convert.
     * @return a new rotation that represents the local rotation in world-space.
     */
    fun getWorldRotation(rotation: Rotation) =
        getWorldQuaternion(Quaternion.fromEuler(rotation)).toEulerAngles()

    /**
     * Converts a scale in the world-space to a local-space of this node.
     *
     * @param worldScale the transform in world-space to convert.
     * @return a new scale that represents the world scale in local-space.
     */
//    fun getLocalScale(worldScale: Scale) = scale(worldToLocal) * worldScale
//    fun getLocalScale(worldScale: Scale) = (worldToLocal * scale(worldScale)).scale
    fun getLocalScale(worldScale: Scale) = worldToLocal * worldScale

    /**
     * Converts a scale in the local-space of this node to world-space.
     *
     * @param scale the scale in local-space to convert.
     * @return a new scale that represents the local scale in world-space.
     */
//    fun getWorldScale(scale: Scale) = (worldTransform * scale(scale)).scale
//    fun getWorldScale(scale: Scale) = scale(worldTransform) * scale
    fun getWorldScale(scale: Scale) = worldTransform * scale

    /**
     * Converts a node transform in the world-space to a local-space of this node.
     *
     * @param node the node in world-space to convert.
     * @return a new transform that represents the world transform in local-space.
     */
    fun getLocalTransform(component: TransformComponent) =
        getLocalTransform(component.worldTransform)

    /**
     * Converts a transform in the world-space to a local-space of this node.
     *
     * @param worldTransform the transform in world-space to convert.
     * @return a new transform that represents the world transform in local-space.
     */
    fun getLocalTransform(worldTransform: Transform) = worldToLocal * worldTransform

    /**
     * Converts a node transform in the local-space of this node to world-space.
     *
     * @param node the node in local-space to convert.
     * @return a new transform that represents the local transform in world-space.
     */
    fun getWorldTransform(component: TransformComponent) = getWorldTransform(component.transform)

    /**
     * Converts a transform in the local-space of this node to world-space.
     *
     * @param localTransform the transform in local-space to convert.
     * @return a new transform that represents the local transform in world-space.
     */
    fun getWorldTransform(localTransform: Transform) = worldTransform * localTransform

    /**
     * The node scale.
     *
     * - reduce size: scale < 1.0f
     * - same size: scale = 1.0f
     * - increase size: scale > 1.0f
     */
    fun setScale(scale: Float) {
        this.scale.xyz = Scale(scale)
    }

    /**
     * Change the local transform. (i.e. relative to the parent).
     *
     * This operation can be slow if the hierarchy of transform is too deep, and this will be
     * particularly bad when updating a lot of transforms. In that case, consider using
     * [TransformManager.openLocalTransformTransaction] /
     * [TransformManager.commitLocalTransformTransaction].
     */
    fun transform(
        position: Position = this.position,
        quaternion: Quaternion = this.quaternion,
        scale: Scale = this.scale
    ) {
        this.transform = Transform(position, quaternion, scale)
    }

    /**
     * Change the local transform. (i.e. relative to the parent).
     *
     * This operation can be slow if the hierarchy of transform is too deep, and this will be
     * particularly bad when updating a lot of transforms. In that case, consider using
     * [TransformManager.openLocalTransformTransaction] /
     * [TransformManager.commitLocalTransformTransaction].
     */
    fun transform(
        position: Position = this.position,
        rotation: Rotation,
        scale: Scale = this.scale
    ) {
        this.transform = Transform(position, rotation, scale)
    }

    /**
     * Change the node world transform. (i.e. relative to the root).
     *
     * This is the composition of this component's local transform with its parent's world
     * transform.
     */
    fun worldTransform(
        worldPosition: Position = this.worldPosition,
        worldQuaternion: Quaternion = this.worldQuaternion,
        worldScale: Scale = this.worldScale
    ) {
        this.worldTransform = Transform(worldPosition, worldQuaternion, worldScale)
    }

    /**
     * Change the node world transform. (i.e. relative to the root).
     *
     * This is the composition of this component's local transform with its parent's world
     * transform.
     */
    fun worldTransform(
        worldPosition: Position = this.worldPosition,
        worldRotation: Rotation,
        worldScale: Scale = this.worldScale
    ) {
        this.worldTransform = Transform(worldPosition, worldRotation, worldScale)
    }

    /**
     * Rotates the node to face another node.
     *
     * @param targetComponent The target node to look at
     * @param upDirection The up direction will determine the orientation of the node around the
     * direction
     */
    fun lookAt(
        targetComponent: TransformComponent,
        upDirection: Direction = Direction(y = 1.0f)
    ) = lookAt(targetComponent.worldPosition, upDirection)

    /**
     * Rotates the node to face a point in world-space.
     *
     * @param targetPosition The target position to look at in world space
     * @param upDirection The up direction will determine the orientation of the node around the
     * direction
     */
    fun lookAt(
        targetPosition: Position,
        upDirection: Direction = Direction(y = 1.0f)
    ) {
        quaternion = lookAt(
            targetPosition,
            worldPosition,
            upDirection
        ).toQuaternion()
    }

    /**
     * Rotates the node to face a direction in world-space.
     *
     * The look direction and up direction cannot be coincident (parallel) or the orientation will
     * be invalid.
     *
     * @param lookDirection The desired look direction in world-space.
     * @param upDirection The up direction will determine the orientation of the node around the
     * look direction.
     */
    fun lookTowards(
        lookDirection: Direction,
        upDirection: Direction = Direction(y = 1.0f)
    ) {
        quaternion = lookTowards(
            worldPosition,
            -lookDirection,
            upDirection
        ).toQuaternion()
    }

    /**
     * The transformation (position, rotation or scale) of the [Node] has changed.
     *
     * If node's position is changed, then that will trigger [onWorldTransformChanged] to be called
     * for all of it's descendants.
     */
    fun onTransformChanged() {
        onWorldTransformChanged()
    }

    /**
     * The transformation (position, rotation or scale) of the [Node] has changed.
     *
     * If node's position is changed, then that will trigger [onWorldTransformChanged] to be called
     * for all of it's descendants.
     */
    fun onWorldTransformChanged() {
        childComponents.forEach { it.onWorldTransformChanged() }
    }

    fun onParentChanged() {
        onTransformChanged()
    }

    /**
     * Detach and destroy the node and all its children.
     */
    fun destroy() {
        transformManager.destroy(entity)
    }

    // TODO : Remove this when full Kotlined Math
    override fun getTransformationMatrix(): Matrix {
        return worldTransform.toMatrix()
    }
}