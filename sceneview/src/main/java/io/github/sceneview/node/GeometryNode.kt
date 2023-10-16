package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.managers.geometry
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
open class GeometryNode(
    engine: Engine,
    vertices: List<Geometry.Vertex> = listOf(),
    submeshes: List<Geometry.Submesh> = listOf(),
    materialInstance: MaterialInstance? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : BaseGeometryNode<Geometry>(
    engine = engine,
    geometry = Geometry.Builder()
        .vertices(vertices)
        .submeshes(submeshes)
        .build(engine),
    materialInstance = materialInstance,
    renderableApply = renderableApply
) {
    val vertexBuffer get() = geometry.vertexBuffer
    val indexBuffer get() = geometry.indexBuffer

    fun setVertices(vertices: List<Geometry.Vertex>) = geometry.setVertices(vertices)
    fun setIndices(submeshes: List<Geometry.Submesh>) = geometry.setSubmeshes(submeshes)
}

open class BaseGeometryNode<T : Geometry>(
    engine: Engine,
    val geometry: T,
    materialInstance: MaterialInstance? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : RenderableNode(engine) {

    init {
        RenderableManager.Builder(geometry.submeshes.size)
            .geometry(geometry)
            .apply {
                materialInstance?.let { material(it) }
            }
            .apply(renderableApply)
            .build(engine, entity)
        updateCollisionShape()
    }

    fun updateCollisionShape() {
        collisionShape = axisAlignedBoundingBox.toVector3Box()
    }
}