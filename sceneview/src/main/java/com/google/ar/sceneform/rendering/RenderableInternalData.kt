package com.google.ar.sceneform.rendering

import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityInstance
import com.google.android.filament.IndexBuffer
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import io.github.sceneview.collision.Vector3
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.Collections

/** Represents the data used to render each mesh of a renderable. */
class MeshData {
    // The start index into the triangle indices buffer for this mesh.
    var indexStart: Int = 0
    // The end index into the triangle indices buffer for this mesh.
    var indexEnd: Int = 0
}

/**
 * Represents the data used by a [Renderable] for rendering
 */
internal class RenderableInternalData : IRenderableInternalData {

    // Geometry data.
    private val centerAabb: Vector3 = Vector3.zero()
    private val extentsAabb: Vector3 = Vector3.zero()

    // Transform data.
    private var transformScale: Float = 1f
    private val transformOffset: Vector3 = Vector3.zero()

    // Raw buffers.
    private var rawIndexBuffer: IntBuffer? = null
    private var rawPositionBuffer: FloatBuffer? = null
    private var rawTangentsBuffer: FloatBuffer? = null
    private var rawUvBuffer: FloatBuffer? = null
    private var rawColorBuffer: FloatBuffer? = null

    // Filament Geometry buffers.
    private var indexBuffer: IndexBuffer? = null
    private var vertexBuffer: VertexBuffer? = null

    // Represents the set of meshes to render.
    private val meshes = ArrayList<MeshData>()

    override fun setCenterAabb(minAabb: Vector3) {
        this.centerAabb.set(minAabb)
    }

    override fun getCenterAabb(): Vector3 = Vector3(centerAabb)

    override fun setExtentsAabb(maxAabb: Vector3) {
        this.extentsAabb.set(maxAabb)
    }

    override fun getExtentsAabb(): Vector3 = Vector3(extentsAabb)

    override fun getSizeAabb(): Vector3 = extentsAabb.scaled(2.0f)

    override fun setTransformScale(scale: Float) {
        this.transformScale = scale
    }

    override fun getTransformScale(): Float = transformScale

    override fun setTransformOffset(offset: Vector3) {
        this.transformOffset.set(offset)
    }

    override fun getTransformOffset(): Vector3 = Vector3(transformOffset)

    override fun getMeshes(): ArrayList<MeshData> = meshes

    override fun setIndexBuffer(indexBuffer: IndexBuffer?) {
        this.indexBuffer = indexBuffer
    }

    override fun getIndexBuffer(): IndexBuffer? = indexBuffer

    override fun setVertexBuffer(vertexBuffer: VertexBuffer?) {
        this.vertexBuffer = vertexBuffer
    }

    override fun getVertexBuffer(): VertexBuffer? = vertexBuffer

    override fun setRawIndexBuffer(rawIndexBuffer: IntBuffer?) {
        this.rawIndexBuffer = rawIndexBuffer
    }

    override fun getRawIndexBuffer(): IntBuffer? = rawIndexBuffer

    override fun setRawPositionBuffer(rawPositionBuffer: FloatBuffer?) {
        this.rawPositionBuffer = rawPositionBuffer
    }

    override fun getRawPositionBuffer(): FloatBuffer? = rawPositionBuffer

    override fun setRawTangentsBuffer(rawTangentsBuffer: FloatBuffer?) {
        this.rawTangentsBuffer = rawTangentsBuffer
    }

    override fun getRawTangentsBuffer(): FloatBuffer? = rawTangentsBuffer

    override fun setRawUvBuffer(rawUvBuffer: FloatBuffer?) {
        this.rawUvBuffer = rawUvBuffer
    }

    override fun getRawUvBuffer(): FloatBuffer? = rawUvBuffer

    override fun setRawColorBuffer(rawColorBuffer: FloatBuffer?) {
        this.rawColorBuffer = rawColorBuffer
    }

    override fun getRawColorBuffer(): FloatBuffer? = rawColorBuffer

    private fun setupSkeleton(builder: RenderableManager.Builder) { return }

    override fun buildInstanceData(engine: Engine, instance: RenderableInstance, @Entity renderedEntity: Int) {
        val renderable = instance.getRenderable()
        val renderableData = renderable.getRenderableData()
        val materialBindings: ArrayList<MaterialInstance> = renderable.getMaterialBindings()
        val renderableManager = engine.getRenderableManager()
        @EntityInstance val renderableInstance = renderableManager.getInstance(renderedEntity)

        // Determine if a new filament Renderable needs to be created.
        val meshCount = renderableData.getMeshes().size
        if (renderableInstance == 0
            || renderableManager.getPrimitiveCount(renderableInstance) != meshCount
        ) {
            // Destroy the old one if it exists.
            if (renderableInstance != 0) {
                try {
                    renderableManager.destroy(renderedEntity)
                } catch (e: Exception) { }
            }

            // Build the filament renderable.
            val builder = RenderableManager.Builder(meshCount)
                .priority(renderable.getRenderPriority())
                .castShadows(renderable.isShadowCaster())
                .receiveShadows(renderable.isShadowReceiver())

            setupSkeleton(builder)
            builder.build(engine, renderedEntity)

            @EntityInstance val newInstance = renderableManager.getInstance(renderedEntity)
            if (newInstance == 0) {
                throw AssertionError("Unable to create RenderableInstance.")
            }
        } else {
            renderableManager.setPriority(renderableInstance, renderable.getRenderPriority())
            renderableManager.setCastShadows(renderableInstance, renderable.isShadowCaster())
            renderableManager.setReceiveShadows(renderableInstance, renderable.isShadowReceiver())
        }

        @EntityInstance val finalInstance = renderableManager.getInstance(renderedEntity)

        // Update the bounding box.
        val extents = renderableData.getExtentsAabb()
        val center = renderableData.getCenterAabb()
        val filamentBox = Box(center.x, center.y, center.z, extents.x, extents.y, extents.z)
        renderableManager.setAxisAlignedBoundingBox(finalInstance, filamentBox)

        if (materialBindings.size != meshCount) {
            throw AssertionError("Material Bindings are out of sync with meshes.")
        }

        // Update the geometry and material instances.
        val primitiveType = RenderableManager.PrimitiveType.TRIANGLES
        for (mesh in 0 until meshCount) {
            val meshData = renderableData.getMeshes()[mesh]
            val vb = renderableData.getVertexBuffer()
            val ib = renderableData.getIndexBuffer()
            if (vb == null || ib == null) {
                throw AssertionError("Internal Error: Failed to get vertex or index buffer")
            }
            renderableManager.setGeometryAt(
                finalInstance,
                mesh,
                primitiveType,
                vb,
                ib,
                meshData.indexStart,
                meshData.indexEnd - meshData.indexStart
            )

            val material = materialBindings[mesh]
            renderableManager.setMaterialInstanceAt(finalInstance, mesh, material)
        }
    }

    override fun setAnimationNames(animationNames: List<String>) {}

    override fun getAnimationNames(): List<String> = Collections.emptyList()

    companion object {
        private val TAG = RenderableInternalData::class.java.simpleName
    }
}
