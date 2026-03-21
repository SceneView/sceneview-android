package io.github.sceneview.components

import io.github.sceneview.rendering.Entity

/**
 * Base interface for all scene graph components.
 *
 * Components are attached to entities and provide specific functionality
 * (rendering, lighting, camera). This interface is platform-agnostic —
 * platform modules provide concrete implementations.
 */
interface Component {
    /**
     * The entity this component is attached to.
     */
    val entity: Entity
}
