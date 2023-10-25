package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.managers.geometry
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
    /**
     * Binds a material instance to all primitives.
     */
    materialInstance: MaterialInstance? = null,
    /**
     * Binds a material instance to the specified primitive.
     *
     * If no material is specified for a given primitive, Filament will fall back to a basic
     * default material.
     *
     * Should return the material to bind for the zero-based index of the primitive, must be less
     * than the [Geometry.submeshes] size passed to constructor.
     */
    materialInstances: (index: Int) -> MaterialInstance? = { materialInstance },
    /**
     * The parent node.
     *
     * If set to null, this node will not be attached.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     */
    parent: Node? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : BaseGeometryNode<Geometry>(
    engine = engine,
    geometry = Geometry.Builder()
        .vertices(vertices)
        .submeshes(submeshes)
        .build(engine),
    materialInstance = materialInstance,
    materialInstances = materialInstances,
    parent = parent,
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
    /**
     * Binds a material instance to all primitives.
     */
    materialInstance: MaterialInstance? = null,
    /**
     * Binds a material instance to the specified primitive.
     *
     * If no material is specified for a given primitive, Filament will fall back to a basic
     * default material.
     *
     * Should return the material to bind for the zero-based index of the primitive, must be less
     * than the [Geometry.submeshes] size passed to constructor.
     */
    materialInstances: (index: Int) -> MaterialInstance? = { materialInstance },
    /**
     * The parent node.
     *
     * If set to null, this node will not be attached.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     */
    parent: Node? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : RenderableNode(engine = engine, parent = parent) {

    init {
        RenderableManager.Builder(geometry.submeshes.size)
            .geometry(geometry)
            .apply {
                geometry.submeshes.forEachIndexed { index, _ ->
                    materialInstances(index)?.let { material(index, it) }
                }
            }
            .apply(renderableApply)
            .build(engine, entity)
        updateCollisionShape()
    }

    fun updateCollisionShape() {
        collisionShape = axisAlignedBoundingBox.toVector3Box()
    }
}