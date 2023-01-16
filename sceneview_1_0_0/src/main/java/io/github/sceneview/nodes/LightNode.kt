package io.github.sceneview.nodes

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import io.github.sceneview.Entity
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
    entity: Entity
) : Node(engine, nodeManager, entity, false, false), LightComponent {

    constructor(
        engine: Engine,
        nodeManager: NodeManager,
        type: LightManager.Type,
        entity: Entity = EntityManager.get().create(),
        apply: LightManager.Builder.() -> Unit
    ) : this(engine, nodeManager, entity) {
        LightManager.Builder(type)
            .apply(apply)
            .build(engine, entity)
    }

    constructor(
        sceneView: SceneView,
        entity: Entity
    ) : this(sceneView.engine, sceneView.nodeManager, entity)

    constructor(
        sceneView: SceneView,
        type: LightManager.Type,
        entity: Entity = EntityManager.get().create(),
        apply: LightManager.Builder.() -> Unit
    ) : this(sceneView.engine, sceneView.nodeManager, type, entity, apply)

    override fun destroy() {
        lightManager.destroy(entity)
        super.destroy()
    }
}