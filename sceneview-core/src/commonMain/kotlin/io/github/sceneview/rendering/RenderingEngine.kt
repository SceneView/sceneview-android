package io.github.sceneview.rendering

import io.github.sceneview.math.Transform

/**
 * Platform-agnostic handle for a scene graph entity.
 *
 * On Android/Filament this maps to a Filament Entity (Int).
 * On iOS this could map to a SceneKit SCNNode or a custom Metal entity.
 */
typealias Entity = Int

/**
 * Platform-agnostic handle for a transform instance.
 */
typealias TransformInstance = Int

/**
 * Cross-platform transform manager interface.
 *
 * Manages the scene graph hierarchy and per-entity transforms.
 * On Android this wraps Filament's TransformManager.
 * On iOS this wraps SceneKit's node transform system or a custom Metal backend.
 */
interface TransformManagerBridge {

    /**
     * Returns the transform instance for the given entity.
     */
    fun getInstance(entity: Entity): TransformInstance

    /**
     * Returns true if the entity has a transform component.
     */
    fun hasComponent(entity: Entity): Boolean

    /**
     * Creates a transform component for the given entity.
     */
    fun create(entity: Entity): TransformInstance

    /**
     * Creates a transform component for the given entity with a parent.
     */
    fun create(entity: Entity, parent: TransformInstance): TransformInstance

    /**
     * Sets the local transform of the given instance.
     */
    fun setTransform(instance: TransformInstance, transform: Transform)

    /**
     * Gets the local transform of the given instance.
     */
    fun getTransform(instance: TransformInstance): Transform

    /**
     * Gets the world transform of the given instance (accumulated from root).
     */
    fun getWorldTransform(instance: TransformInstance): Transform

    /**
     * Sets the parent of the given instance.
     */
    fun setParent(instance: TransformInstance, parent: TransformInstance)

    /**
     * Gets the parent of the given instance, or null if root.
     */
    fun getParent(instance: TransformInstance): TransformInstance?

    /**
     * Destroys the transform component for the given entity.
     */
    fun destroy(entity: Entity)
}
