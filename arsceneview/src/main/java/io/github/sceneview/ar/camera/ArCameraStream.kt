package io.github.sceneview.ar.camera

import com.google.android.filament.*
import com.google.android.filament.IndexBuffer.Builder.IndexType
import com.google.android.filament.Texture.PixelBufferDescriptor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.GLHelper
import com.google.ar.sceneform.rendering.Renderable.RENDER_PRIORITY_LAST
import io.github.sceneview.ar.ArSceneLifecycle
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.light.destroy
import io.github.sceneview.material.*
import io.github.sceneview.math.Transform
import io.github.sceneview.renderable.*
import io.github.sceneview.texture.build
import io.github.sceneview.texture.destroy
import io.github.sceneview.texture.setImage
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
    val standardMaterialLocation: String = "sceneview/materials/camera_stream_standard.filamat",
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

    private var _standardMaterial: MaterialInstance? = null

    /**
     * ### Flat camera material
     */
    var standardMaterial: MaterialInstance
        get() = _standardMaterial ?: MaterialLoader.createMaterial(
            context = sceneView.context,
            lifecycle = lifecycle,
            filamatFileLocation = standardMaterialLocation
        ).apply {
            setParameter("uvTransform", Transform())
            _standardMaterial = this
        }
        set(value) {
            _standardMaterial = value
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
        .material(0, standardMaterial)
        .build(lifecycle)

    /**
     * ### The applied material
     *
     * Depending on [isDepthOcclusionEnabled] and device Depth compatibility
     */
    var material: MaterialInstance = renderable.getMaterial()
        set(value) {
            field = value
            cameraTexture?.let { value.setExternalTexture(MATERIAL_CAMERA_TEXTURE, it) }
            renderable.setMaterial(value)
        }

    private var hasSetTextureNames = false

    /**
     * Passing multiple textures allows for a multithreaded rendering pipeline
     */
    val cameraTextureIds = IntArray(6) { GLHelper.createCameraTexture() }

    /**
     * The init is done when we have the session frame size
     */
    var cameraTextures: List<Texture>? = null

    /**
     * We apply the multithreaded actual rendering texture
     */
    var cameraTexture: Texture? = null
        set(value) {
            field = value
            value?.let { material.setExternalTexture(MATERIAL_CAMERA_TEXTURE, it) }
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

        // Texture names should only be set once on a GL thread unless they change.
        // This is done during updateFrame rather than init since the session is
        // not guaranteed to have been initialized during the execution of init.
        if (!hasSetTextureNames) {
            arFrame.session.setCameraTextureNames(cameraTextureIds)
            hasSetTextureNames = true
        }

        // Setup External Camera Texture if needed
        val (width, height) = arFrame.camera.textureIntrinsics.imageDimensions
        // The ExternalTexture can't be created until we receive the first AR Core Frame so
        // that we can access the width and height of the camera texture. Return early if
        // the External Texture hasn't been created yet so we don't start rendering until we
        // have a valid texture. This will be called again when the ExternalTexture is
        // created.
        val cameraTextures = cameraTextures?.takeIf {
            it[0].getWidth(0) == width && it[0].getHeight(0) == height
        } ?: cameraTextureIds.map { cameraTextureId ->
            Texture.Builder()
                .width(width)
                .height(height)
                .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                .format(Texture.InternalFormat.RGB8)
                .importTexture(cameraTextureId.toLong())
                .build(lifecycle)
        }.also { textures ->
            cameraTextures?.forEach { it.destroy() }
            cameraTextures = textures
        }
        cameraTexture = cameraTextures.getOrNull(cameraTextureIds.indexOf(frame.cameraTextureName))

        // Recalculate camera Uvs if necessary.
        if (transformedUvCoordinates == null || frame.hasDisplayGeometryChanged()) {
            val transformedUvCoordinates = transformedUvCoordinates
                ?: uvCoordinates.clone().also {
                    transformedUvCoordinates = it
                }
            // Recalculate Camera Uvs
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

        when (sceneView.depthMode) {
            Config.DepthMode.AUTOMATIC -> {
                runCatching {
                    frame.acquireDepthImage()
                }.getOrNull()
            }
            Config.DepthMode.RAW_DEPTH_ONLY -> {
                runCatching {
                    frame.acquireRawDepthImage()
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

    private fun updateMaterial() {
        material = if (isDepthOcclusionEnabled && sceneView.depthEnabled) {
            depthOcclusionMaterial
        } else {
            standardMaterial
        }
    }

    fun destroy() {
        lifecycle.removeObserver(this)
        _standardMaterial?.destroy()
        _depthOcclusionMaterial?.destroy()
        vertexBuffer.destroy()
        renderable.destroy()
        cameraTextures?.forEach { it.destroy() }
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