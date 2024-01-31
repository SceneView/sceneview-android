package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import io.github.sceneview.Entity
import io.github.sceneview.components.LightComponent

/**
 * Light source [Node] in the scene such as a sun or street lights.
 *
 * At least one light must be added to a scene in order to see anything (unless the
 * [Material.Shading.UNLIT] is used).
 *
 * Creation and destruction:
 * - A Light component is created using the [LightManager.Builder] and destroyed by calling
 * [LightManager.destroy].
 *
 * @see LightManager
 */
open class LightNode(
    engine: Engine,
    entity: Entity
) : Node(engine, entity), LightComponent {

    override var isTouchable = false
    override var isEditable = false

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        builder: LightManager.Builder
    ) : this(engine, entity) {
        builder.build(engine, entity)
    }

    constructor(
        engine: Engine,
        type: LightManager.Type,
        entity: Entity = EntityManager.get().create(),
        apply: LightManager.Builder.() -> Unit
    ) : this(engine, entity, LightManager.Builder(type).apply(apply))

    override fun destroy() {
        lightManager.destroy(entity)
        super.destroy()
    }
}