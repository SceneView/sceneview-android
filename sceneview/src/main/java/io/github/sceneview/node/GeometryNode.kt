package io.github.sceneview.node

import com.google.android.filament.Box
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.build
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.geometry
import io.github.sceneview.model.collisionShape
import io.github.sceneview.model.model
import io.github.sceneview.renderable.axisAlignedBoundingBox
import io.github.sceneview.renderable.collisionShape

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
    geometry: Geometry,
    materials: List<MaterialInstance?> = listOf(),
    apply: RenderableManager.Builder.() -> Unit = {}
) : RenderableNode(EntityManager.get().create()) {

    init {
        RenderableManager.Builder(geometry.submeshes.size)
            .geometry(geometry)
            .apply {
                materials.forEachIndexed { index, material ->
                    material?.let { material(index, it) }
                }
            }
            .apply(apply)
            .build(renderable!!)
        updateCollisionShape()
    }

    fun updateCollisionShape() {
        collisionShape = renderable?.collisionShape
    }
}