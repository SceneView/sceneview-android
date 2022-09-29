package io.github.sceneview.node

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
class LightNode(
    engine: Engine,
    nodeManager: NodeManager,
    type: LightManager.Type,
    apply: LightManager.Builder.() -> Unit
) : Node(engine, nodeManager, EntityManager.get().create()), LightComponent {

    init {
        LightManager.Builder(type)
            .apply(apply)
            .build(engine, entity)
    }

    constructor(
        sceneView: SceneView,
        type: LightManager.Type,
        apply: LightManager.Builder.() -> Unit
    ) : this(sceneView.engine, sceneView.nodeManager, type, apply)

    override fun destroy() {
        lightManager.destroy(entity)
        super.destroy()
    }
}