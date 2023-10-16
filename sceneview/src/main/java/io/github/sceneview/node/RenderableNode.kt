package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import io.github.sceneview.Entity
import io.github.sceneview.FilamentEntity
import io.github.sceneview.SceneView
import io.github.sceneview.components.RenderableComponent

/**
 * A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a renderable model for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the [SceneView]
 * .
 */
open class RenderableNode(
    engine: Engine,
    @FilamentEntity entity: Entity = EntityManager.get().create()
) : Node(engine, entity), RenderableComponent {

    override fun updateVisibility() {
        super.updateVisibility()

        setLayerVisible(isVisible)
    }
}