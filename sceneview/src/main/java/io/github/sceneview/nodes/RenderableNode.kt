package io.github.sceneview.nodes

import com.google.android.filament.Engine
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.managers.NodeManager

open class RenderableNode(
    engine: Engine,
    nodeManager: NodeManager,
    entity: Entity,
    isSelectable: Boolean = true,
    isEditable: Boolean = true
) : Node(engine, nodeManager, entity, isSelectable, isEditable), RenderableComponent {

    constructor(
        sceneView: SceneView,
        entity: Entity,
        isSelectable: Boolean = true,
        isEditable: Boolean = true
    ) : this(
        sceneView.engine,
        sceneView.nodeManager,
        entity,
        isSelectable,
        isEditable
    )

    override fun destroy() {
        renderableManager.destroy(entity)
        super.destroy()
    }
}