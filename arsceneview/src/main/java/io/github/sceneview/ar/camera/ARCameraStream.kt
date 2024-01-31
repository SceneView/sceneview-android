package io.github.sceneview.ar.camera

import android.util.Log
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.IndexBuffer.Builder.IndexType
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Texture
import com.google.android.filament.Texture.PixelBufferDescriptor
import com.google.android.filament.VertexBuffer
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.Renderable.RENDER_PRIORITY_LAST
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.managers.safeDestroy
import io.github.sceneview.material.setExternalTexture
import io.github.sceneview.material.setParameter
import io.github.sceneview.material.setTexture
import io.github.sceneview.math.Transform
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.safeDestroyVertexBuffer
import io.github.sceneview.utils.OpenGL
import io.github.sceneview.utils.clone
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

const val kUVTransformParameter = "uvTransform"
const val kCameraTextureParameter = "cameraTexture"
const val kDepthTextureParameter = "depthTexture"

/**
 * Displays the Camera stream using Filament.
 */
open class ARCameraStream(
    private val materialLoader: MaterialLoader,
    standardMaterialFile: String = "materials/camera_stream_flat.filamat",
    depthOcclusionMaterialFile: String = "materials/camera_stream_depth.filamat",
) : RenderableComponent {

    final override val engine: Engine = materialLoader.engine

//    /**
//     * Changes the coarse-level camera draw ordering
//     */
//    var priority: Int = RENDER_PRIORITY_LAST
//        set(value) {
//            field = value
//            renderable.setPriority(value)
//        }

    final override val entity = EntityManager.get().create()

    /**
     * Passing multiple textures allows for a multithreaded rendering pipeline
     */
    val cameraTextureIds = IntArray(6) { OpenGL.createExternalTextureId() }

    /**
     * Textures buffer
     */
    var cameraTextures: Map<Int, Texture> = cameraTextureIds.associateWith { cameraTextureId ->
        Texture.Builder().sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            .format(Texture.InternalFormat.RGB16F)
            .importTexture(cameraTextureId.toLong())
            .build(engine)
    }

    /**
     * We apply the multithreaded actual rendering (Frame) texture
     */
    var cameraTexture: Texture = cameraTextures[cameraTextureIds[0]]!!
        set(value) {
            if (field != value) {
                field = value
                materialInstance.setExternalTexture(kCameraTextureParameter, value)
            }
        }

    /**
     * Extracted texture from the session depth image
     */
    val depthTexture =
        Texture.Builder().sampler(Texture.Sampler.SAMPLER_2D).format(Texture.InternalFormat.RG8)
            .levels(1).build(engine)

    /**
     * ### Flat camera material
     */
    val standardMaterial = materialLoader.createMaterial(standardMaterialFile).apply {
        defaultInstance.apply {
            setParameter(kUVTransformParameter, Transform())
            setExternalTexture(kCameraTextureParameter, cameraTexture)
        }
    }

    /**
     * ### Depth occlusion material
     */
    var depthOcclusionMaterial = materialLoader.createMaterial(depthOcclusionMaterialFile).apply {
        defaultInstance.apply {
            setParameter(kUVTransformParameter, Transform())
            setExternalTexture(kCameraTextureParameter, cameraTexture)
            setTexture(kDepthTextureParameter, depthTexture)
        }
    }

    /**
     * Depending on [isDepthOcclusionEnabled] and device Depth compatibility
     */
    override fun setMaterialInstances(materialInstance: MaterialInstance) {
        materialInstance.setExternalTexture(kCameraTextureParameter, cameraTexture)
        super.setMaterialInstances(materialInstance)
    }

    /**
     * ### Enable the depth occlusion material
     *
     * This will process the incoming DepthImage to occlude virtual objects behind real world
     * objects.
     *
     * If the [Session] is not configured properly the standard camera material is used.
     * Valid [Session] configuration for the DepthMode are [Config.DepthMode.AUTOMATIC] and
     * [Config.DepthMode.RAW_DEPTH_ONLY]
     *
     * Disable this value to apply the standard camera material to the CameraStream.
     */
    var isDepthOcclusionEnabled = false
        set(value) {
            if (field != value) {
                field = value
                setMaterialInstances(
                    if (value) {
                        depthOcclusionMaterial.defaultInstance
                    } else {
                        standardMaterial.defaultInstance
                    }
                )
            }
        }

    private val vertexBuffer: VertexBuffer =
        VertexBuffer.Builder().vertexCount(VERTEX_COUNT).bufferCount(2).attribute(
            VertexBuffer.VertexAttribute.POSITION,
            POSITION_BUFFER_INDEX,
            VertexBuffer.AttributeType.FLOAT3,
            0,
            CAMERA_VERTICES.size / VERTEX_COUNT * FLOAT_SIZE_IN_BYTES
        ).attribute(
            VertexBuffer.VertexAttribute.UV0,
            UV_BUFFER_INDEX,
            VertexBuffer.AttributeType.FLOAT2,
            0,
            CAMERA_UVS.size / VERTEX_COUNT * FLOAT_SIZE_IN_BYTES
        ).build(engine).apply {
            setBufferAt(engine, POSITION_BUFFER_INDEX, FloatBuffer.wrap(CAMERA_VERTICES))
        }

    private val uvCoordinates: FloatBuffer =
        ByteBuffer.allocateDirect(CAMERA_UVS.size * FLOAT_SIZE_IN_BYTES)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(CAMERA_UVS)
                rewind()
            }

    // Note: ARCore expects the UV buffers to be direct or will assert in transformDisplayUvCoords
    private var transformedUvCoordinates: FloatBuffer? = null

    init {
        RenderableManager.Builder(4)
            .castShadows(false)
            .receiveShadows(false)
            // Always draw the camera feed last to avoid overdraw
            .culling(false)
            .priority(RENDER_PRIORITY_LAST)
            .geometry(0,
                RenderableManager.PrimitiveType.TRIANGLES,
                vertexBuffer,
                IndexBuffer.Builder()
                    .indexCount(INDICES.size)
                    .bufferType(IndexType.USHORT)
                    .build(engine)
                    .apply {
                        // Create screen quad geometry to camera stream to
                        setBuffer(engine, ShortBuffer.wrap(INDICES))
                    })
            .material(0, standardMaterial.defaultInstance)
            .build(engine, entity)
    }

    fun update(session: Session, frame: Frame) {
        cameraTextures[frame.cameraTextureName]?.let {
            cameraTexture = it
        }

        // Recalculate camera Uvs if necessary.
        if (transformedUvCoordinates == null || frame.hasDisplayGeometryChanged()) {
            val transformedUvCoordinates = transformedUvCoordinates ?: uvCoordinates.clone().also {
                transformedUvCoordinates = it
            }

            // If display rotation changed (also includes view size change), we need to re-query the UV
            // coordinates for the screen rect, as they may have changed as well.
            frame.transformCoordinates2d(
                Coordinates2d.VIEW_NORMALIZED,
                uvCoordinates,
                Coordinates2d.TEXTURE_NORMALIZED,
                transformedUvCoordinates
            )
            // Adjust Camera Uvs for OpenGL
            for (i in 1 until (VERTEX_COUNT * 2) step 2) {
                // Correct for vertical coordinates to match OpenGL
                transformedUvCoordinates.put(i, 1.0f - transformedUvCoordinates[i])
            }
            vertexBuffer.setBufferAt(engine, UV_BUFFER_INDEX, transformedUvCoordinates)
        }

        if (isDepthOcclusionEnabled) {
            when (session.config.depthMode) {
                Config.DepthMode.AUTOMATIC -> {
                    runCatching {
                        frame.acquireDepthImage16Bits()
                    }.getOrNull()
                }

                Config.DepthMode.RAW_DEPTH_ONLY -> {
                    runCatching {
                        frame.acquireRawDepthImage16Bits()
                    }.getOrNull()
                }

                else -> null
            }?.let { depthImage ->
                // Recalculate Occlusion
                // To solve a problem with a to early released DepthImage the ByteBuffer which holds
                // all necessary data is cloned. The cloned ByteBuffer is unaffected of a released
                // DepthImage and therefore produces not a flickering result.
                val buffer = depthImage.planes[0].buffer//.clone()
                depthTexture.setImage(engine, 0, PixelBufferDescriptor(
                    buffer, Texture.Format.RG, Texture.Type.UBYTE, 1, 0, 0, 0, null
                ) {
                    // Close the image only after the execution
                    depthImage.close()
                    buffer.clear()
                })
            }
        }
    }

    fun destroy() {
        materialLoader.destroyMaterialInstance(standardMaterial.defaultInstance)
        materialLoader.destroyMaterial(standardMaterial)
        materialLoader.destroyMaterialInstance(depthOcclusionMaterial.defaultInstance)
        materialLoader.destroyMaterial(depthOcclusionMaterial)
        engine.safeDestroyVertexBuffer(vertexBuffer)
        renderableManager.safeDestroy(entity)
        cameraTextures.values.forEach { engine.safeDestroyTexture(it) }
        engine.safeDestroyTexture(depthTexture)
        uvCoordinates.clear()
        transformedUvCoordinates?.clear()
        Log.d("Sceneview", "CameraStream destroyed")
    }

    companion object {
        private const val VERTEX_COUNT = 3
        private const val POSITION_BUFFER_INDEX = 0
        private val CAMERA_VERTICES = floatArrayOf(
            -1.0f, 1.0f, 1.0f, -1.0f, -3.0f, 1.0f, 3.0f, 1.0f, 1.0f
        )
        private const val UV_BUFFER_INDEX = 1
        private val CAMERA_UVS = floatArrayOf(
            0.0f, 0.0f, 0.0f, 2.0f, 2.0f, 0.0f
        )

        private val INDICES = shortArrayOf(0, 1, 2)
        private const val FLOAT_SIZE_IN_BYTES = java.lang.Float.SIZE / 8
    }
}