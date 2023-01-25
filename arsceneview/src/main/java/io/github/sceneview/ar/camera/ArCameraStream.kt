package io.github.sceneview.ar.camera

import com.google.android.filament.*
import com.google.android.filament.IndexBuffer.Builder.IndexType
import com.google.android.filament.Texture.PixelBufferDescriptor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.Renderable.RENDER_PRIORITY_LAST
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.material.*
import io.github.sceneview.math.Transform
import io.github.sceneview.renderable.*
import io.github.sceneview.texture.build
import io.github.sceneview.texture.destroy
import io.github.sceneview.texture.setImage
import io.github.sceneview.utils.OpenGL
import io.github.sceneview.utils.clone
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

/**
 * ### Displays the Camera stream using Filament.
 */
class ArCameraStream(
    private val lifecycle: ArSceneLifecycle,
    val flatMaterialLocation: String = "sceneview/materials/camera_stream_flat.filamat",
    val depthOcclusionMaterialLocation: String = "sceneview/materials/camera_stream_depth.filamat"
) : ArSceneLifecycleObserver {

    private val sceneView get() = lifecycle.sceneView

    /**
     * ### Changes the coarse-level camera draw ordering
     */
    var priority: Int = RENDER_PRIORITY_LAST
        set(value) {
            field = value
            renderable.setPriority(value)
        }

    private var _flatMaterial: MaterialInstance? = null

    /**
     * ### Flat camera material
     */
    var flatMaterial: MaterialInstance
        get() = _flatMaterial ?: MaterialLoader.createMaterial(
            context = sceneView.context,
            lifecycle = lifecycle,
            filamatFileLocation = flatMaterialLocation
        ).apply {
            setParameter("uvTransform", Transform())
            _flatMaterial = this
        }
        set(value) {
            _flatMaterial = value
        }

    private var _depthOcclusionMaterial: MaterialInstance? = null

    /**
     * ### Depth occlusion material
     */
    var depthOcclusionMaterial: MaterialInstance
        get() = _depthOcclusionMaterial ?: MaterialLoader.createMaterial(
            context = sceneView.context,
            lifecycle = lifecycle,
            filamatFileLocation = depthOcclusionMaterialLocation
        ).apply {
            setParameter("uvTransform", Transform())
            _depthOcclusionMaterial = this
        }
        set(value) {
            _depthOcclusionMaterial = value
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
                if (value && sceneView.depthMode == Config.DepthMode.DISABLED) {
                    sceneView.depthEnabled = true
                }
                updateMaterial()
            }
        }

    private val vertexBuffer: VertexBuffer = VertexBuffer.Builder()
        .vertexCount(VERTEX_COUNT)
        .bufferCount(2)
        .attribute(
            VertexBuffer.VertexAttribute.POSITION,
            POSITION_BUFFER_INDEX,
            VertexBuffer.AttributeType.FLOAT3,
            0,
            CAMERA_VERTICES.size / VERTEX_COUNT * FLOAT_SIZE_IN_BYTES
        )
        .attribute(
            VertexBuffer.VertexAttribute.UV0,
            UV_BUFFER_INDEX,
            VertexBuffer.AttributeType.FLOAT2,
            0,
            CAMERA_UVS.size / VERTEX_COUNT * FLOAT_SIZE_IN_BYTES
        )
        .build(lifecycle)
        .apply {
            setBufferAt(POSITION_BUFFER_INDEX, FloatBuffer.wrap(CAMERA_VERTICES))
        }

    /**
     * ### The quad renderable (leave off the aabb)
     */
    val renderable: Renderable = RenderableManager.Builder(1)
        .castShadows(false)
        .receiveShadows(false)
        // Always draw the camera feed last to avoid overdraw
        .culling(false)
        .priority(priority)
        .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer,
            IndexBuffer.Builder()
                .indexCount(INDICES.size)
                .bufferType(IndexType.USHORT)
                .build(lifecycle)
                .apply {
                    // Create screen quad geometry to camera stream to
                    setBuffer(ShortBuffer.wrap(INDICES))
                })
        .material(0, flatMaterial)
        .build(lifecycle)

    /**
     * ### The applied material
     *
     * Depending on [isDepthOcclusionEnabled] and device Depth compatibility
     */
    var materialInstance: MaterialInstance = renderable.getMaterial()
        set(value) {
            field = value
            renderable.setMaterial(value)
        }

    /**
     * Passing multiple textures allows for a multithreaded rendering pipeline
     */
    val cameraTextureIds = IntArray(6) { OpenGL.createExternalTextureId() }

    val flatTextures = cameraTextureIds.associateWith {
        Texture.Builder()
            .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            .format(Texture.InternalFormat.RGB8)
            .importTexture(it.toLong())
            .build(lifecycle)
    }

    /**
     * ### Extracted texture from the session depth image
     */
    var depthTexture: Texture? = null
        set(value) {
            field = value
            value?.let { depthOcclusionMaterial.setTexture(MATERIAL_DEPTH_TEXTURE, value) }
        }

    private val uvCoordinates: FloatBuffer =
        ByteBuffer.allocateDirect(CAMERA_UVS.size * FLOAT_SIZE_IN_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(CAMERA_UVS)
                rewind()
            }

    // Note: ARCore expects the UV buffers to be direct or will assert in transformDisplayUvCoords
    private var transformedUvCoordinates: FloatBuffer? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onArSessionCreated(session: ArSession) {
        super.onArSessionCreated(session)

        session.setCameraTextureNames(cameraTextureIds)
    }

    override fun onArSessionConfigChanged(session: ArSession, config: Config) {
        super.onArSessionConfigChanged(session, config)

        updateMaterial()
    }

    override fun onArFrame(arFrame: ArFrame) {
        super.onArFrame(arFrame)

        val frame = arFrame.frame

//        // Texture names should only be set once on a GL thread unless they change.
//        // This is done during updateFrame rather than init since the session is
//        // not guaranteed to have been initialized during the execution of init.
//        if (!hasSetTextureNames) {
//            arFrame.session.setCameraTextureNames(cameraTextureIds)
//
//            hasSetTextureNames = true
//        }

        // Retrieve the actual ARCore frame texture for next draw
        materialInstance.setExternalTexture(
            MATERIAL_CAMERA_TEXTURE,
            flatTextures[frame.cameraTextureName]!!
        )

        // Recalculate camera Uvs if necessary.
        if (transformedUvCoordinates == null || frame.hasDisplayGeometryChanged()) {
            val transformedUvCoordinates = transformedUvCoordinates
                ?: uvCoordinates.clone().also {
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
            vertexBuffer.setBufferAt(UV_BUFFER_INDEX, transformedUvCoordinates)
        }

        if (isDepthOcclusionEnabled) {
            when (sceneView.depthMode) {
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
                val depthTexture = depthTexture?.takeIf {
                    it.getWidth(0) == depthImage.width &&
                            it.getHeight(0) == depthImage.height
                } ?: Texture.Builder()
                    .width(depthImage.width)
                    .height(depthImage.height)
                    .sampler(Texture.Sampler.SAMPLER_2D)
                    .format(Texture.InternalFormat.RG8)
                    .levels(1)
                    .build(lifecycle).also {
                        depthTexture?.destroy()
                        depthTexture = it
                    }
                // To solve a problem with a to early released DepthImage the ByteBuffer which holds
                // all necessary data is cloned. The cloned ByteBuffer is unaffected of a released
                // DepthImage and therefore produces not a flickering result.
                val buffer = depthImage.planes[0].buffer//.clone()
                depthTexture.setImage(
                    0,
                    PixelBufferDescriptor(
                        buffer,
                        Texture.Format.RG,
                        Texture.Type.UBYTE,
                        1, 0, 0, 0, null
                    ) {
                        // Close the image only after the execution
                        depthImage.close()
                        buffer.clear()
                    }
                )
            }
        }
    }

    private fun updateMaterial() {
        materialInstance = if (isDepthOcclusionEnabled && sceneView.depthEnabled) {
            depthOcclusionMaterial
        } else {
            flatMaterial
        }
    }

    fun destroy() {
        lifecycle.removeObserver(this)
        _flatMaterial?.destroy()
        _depthOcclusionMaterial?.destroy()
        vertexBuffer.destroy()
        renderable.destroyRenderable()
        flatTextures.values.forEach { it.destroy() }
        depthTexture?.destroy()
        uvCoordinates.clear()
        transformedUvCoordinates?.clear()
    }

    companion object {
        const val MATERIAL_CAMERA_TEXTURE = "cameraTexture"
        const val MATERIAL_DEPTH_TEXTURE = "depthTexture"

        private const val VERTEX_COUNT = 3
        private const val POSITION_BUFFER_INDEX = 0
        private val CAMERA_VERTICES = floatArrayOf(
            -1.0f, 1.0f,
            1.0f, -1.0f,
            -3.0f, 1.0f,
            3.0f, 1.0f,
            1.0f
        )
        private const val UV_BUFFER_INDEX = 1
        private val CAMERA_UVS = floatArrayOf(
            0.0f, 0.0f,
            0.0f, 2.0f,
            2.0f, 0.0f
        )

        private val INDICES = shortArrayOf(0, 1, 2)
        private const val FLOAT_SIZE_IN_BYTES = java.lang.Float.SIZE / 8
    }
}
