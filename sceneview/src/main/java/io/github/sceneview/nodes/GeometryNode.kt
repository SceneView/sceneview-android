package io.github.sceneview.nodes

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.managers.geometry

/**
 * Mesh are bundles of primitives, each of which has its own geometry and material.
 *
 * All primitives in a particular renderable share a set of rendering attributes, such as whether
 * they cast shadows or use vertex skinning. Kotlin usage example:
 *
 * ```
 * val entity = EntityManager.get().create()
 *
 * RenderableManager.Builder(1)
 *         .boundingBox(Box(0.0f, 0.0f, 0.0f, 9000.0f, 9000.0f, 9000.0f))
 *         .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)
 *         .material(0, material)
 *         .build(engine, entity)
 *
 * scene.addEntity(renderable)
 * ```
 *
 * To modify the state of an existing renderable, clients should first use RenderableManager
 * to get a temporary handle called an <em>instance</em>. The instance can then be used to get or
 * set the renderable's state. Please note that instances are ephemeral; clients should store
 * entities, not instances.
 *
 * @see Geometry
 */
open class GeometryNode(
    engine: Engine,
    nodeManager: NodeManager,
    geometry: Geometry,
    materials: List<MaterialInstance?> = listOf(),
    entity: Entity = EntityManager.get().create(),
    apply: RenderableManager.Builder.() -> Unit = {}
) : RenderableNode(engine, nodeManager, entity), RenderableComponent {

    init {
        RenderableManager.Builder(geometry.submeshes.size)
            .geometry(geometry)
            .apply {
                materials.forEachIndexed { index, material ->
                    material?.let { material(index, it) }
                }
            }
            .apply(apply)
            .build(engine, entity)
    }

    constructor(
        engine: Engine,
        nodeManager: NodeManager,
        geometry: Geometry,
        material: MaterialInstance,
        entity: Entity = EntityManager.get().create(),
        apply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        nodeManager,
        geometry,
        geometry.submeshes.map { material },
        entity,
        apply
    )

    constructor(
        sceneView: SceneView,
        geometry: Geometry,
        materials: List<MaterialInstance?> = listOf(),
        entity: Entity = EntityManager.get().create(),
        apply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        sceneView.engine,
        sceneView.nodeManager,
        geometry,
        materials,
        entity,
        apply
    )

    constructor(
        sceneView: SceneView,
        geometry: Geometry,
        material: MaterialInstance,
        entity: Entity = EntityManager.get().create(),
        apply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        sceneView.engine,
        sceneView.nodeManager,
        geometry,
        material,
        entity,
        apply
    )

    override fun getBoundingBox() = axisAlignedBoundingBox
}