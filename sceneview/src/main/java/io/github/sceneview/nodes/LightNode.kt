package io.github.sceneview.nodes

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import io.github.sceneview.SceneView
import io.github.sceneview.components.LightComponent
import io.github.sceneview.managers.NodeManager

/**
 * Light source [Node] in the scene such as a sun or street lights
 *
 * At least one light must be added to a scene in order to see anything
 * (unless the [Material.Shading.UNLIT] is used).
 *
 * Creation and destruction
 * - A Light component is created using the [LightManager.Builder] and destroyed by calling
 * [LightManager.destroy].
 *
 * @see LightManager
 */
open class LightNode(
    engine: Engine,
    nodeManager: NodeManager,
    type: LightManager.Type,
    builder: LightManager.Builder.() -> Unit
) : Node(
    engine = engine,
    nodeManager = nodeManager,
    entity = EntityManager.get().create(),
    isSelectable = false,
    isEditable = false
), LightComponent {

    init {
        LightManager.Builder(type)
            .apply(builder)
            .build(engine, entity)
    }

    constructor(
        sceneView: SceneView,
        type: LightManager.Type,
        builder: LightManager.Builder.() -> Unit
    ) : this(sceneView.engine, sceneView.nodeManager, type, builder)

    override fun destroy() {
        lightManager.destroy(entity)
        super.destroy()
    }
}