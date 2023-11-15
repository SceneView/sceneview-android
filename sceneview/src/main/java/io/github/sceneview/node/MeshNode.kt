package io.github.sceneview.node

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer
import io.github.sceneview.geometries.Geometry

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
    primitiveType: PrimitiveType,
    val vertexBuffer: VertexBuffer,
    val indexBuffer: IndexBuffer,
    val boundingBox: Box? = null,
    /**
     * Binds a material instance.
     *
     * If no material is specified, Filament will fall back to a basic default material.
     */
    materialInstance: MaterialInstance? = null,
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
    builder: RenderableManager.Builder.() -> Unit = {}
) : RenderableNode(engine = engine, parent = parent) {

    init {
        RenderableManager.Builder(1)
            .geometry(
                0,
                primitiveType,
                vertexBuffer,
                indexBuffer
            )
            .apply {
                boundingBox?.let { boundingBox(it) }
                culling(boundingBox != null)
                materialInstance?.let { materialInstance ->
                    material(0, materialInstance)
                }
            }.apply(builder)
            .build(engine, entity)
        updateCollisionShape()
    }
}