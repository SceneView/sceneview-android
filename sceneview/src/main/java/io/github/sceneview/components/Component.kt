package io.github.sceneview.components

import com.google.android.filament.Engine
import io.github.sceneview.Entity

/**
 * Base interface for Filament ECS components.
 *
 * Every SceneView component (camera, renderable, light) is attached to an [entity] managed by a
 * Filament [Engine]. Implementations expose typed accessors that delegate to the appropriate
 * Filament manager (RenderableManager, LightManager, etc.).
 */
interface Component {
    /** The Filament engine that owns this component's resources. */
    val engine: Engine
    /** The entity this component is attached to. */
    val entity: Entity
}