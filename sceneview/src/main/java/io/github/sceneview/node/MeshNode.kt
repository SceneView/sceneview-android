package io.github.sceneview.node

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.managers.material
import io.github.sceneview.math.toVector3Box

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
 * To modify the state of an existing renderable, clients should first use RenderableManager to get
 * a temporary handle called an <em>instance</em>. The instance can then be used to get or set the
 * renderable's state. Please note that instances are ephemeral; clients should store entities,
 * not instances.
 *
 * @see Geometry
 */
open class MeshNode(
    engine: Engine,
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer,
    val boundingBox: Box? = null,
    materialInstance: MaterialInstance? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : RenderableNode(engine) {

    init {
        RenderableManager.Builder(1)
            .geometry(
                0,
                RenderableManager.PrimitiveType.TRIANGLES,
                vertexBuffer,
                indexBuffer
            )
            .apply {
                boundingBox?.let { }
                culling(boundingBox != null)
                materialInstance?.let { material(it) }
            }.apply(renderableApply)
            .build(engine, entity)
        updateCollisionShape()
    }

    fun updateCollisionShape() {
        collisionShape = axisAlignedBoundingBox.toVector3Box()
    }
}