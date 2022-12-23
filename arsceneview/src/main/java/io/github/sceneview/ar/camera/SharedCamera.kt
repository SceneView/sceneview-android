package io.github.sceneview.ar.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import android.hardware.camera2.*
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import com.google.android.filament.*
import com.google.android.filament.IndexBuffer.Builder.IndexType
import com.google.android.filament.Texture.PixelBufferDescriptor
import com.google.ar.core.Config
import com.google.ar.core.Coordinates2d
import com.google.ar.core.Session
import com.google.ar.sceneform.rendering.GLHelper
import com.google.ar.sceneform.rendering.Renderable.RENDER_PRIORITY_LAST
import io.github.sceneview.ar.ArSceneLifecycleObserver
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSessionOld
import io.github.sceneview.ar.utils.PermissionHelper
import io.github.sceneview.light.destroy
import io.github.sceneview.material.*
import io.github.sceneview.math.Transform
import io.github.sceneview.renderable.*
import io.github.sceneview.texture.destroy
import io.github.sceneview.utils.clone
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.*

typealias ARCoreSharedCamera = com.google.ar.core.SharedCamera

/**
 *
 */
class SharedCamera(
    /**
     * ARCore session that supports camera sharing.
     */
    private val arSession: Session,
    /**
     * Camera custom ImageReaders
     *
     * Custom image readers can be added here in order to receive the camera updated image when
     * required.
     * You can define the required resolution at the [ImageReader] level and retrieve each new
     * camera image at the needed frequency.
     * Those image readers can be used to render the camera stream, record the original camera
     * output or to link any AI/ML/CV that need access to the original camera image.
     */
    val imageReaders: List<ImageReader> = listOf()
) {

    /**
     * ARCore shared camera instance
     *
     * Obtained from ARCore session that supports sharing.
     */
    val sessionCamera = arSession.sharedCamera

    /**
     * Camera device.
     *
     * Used by both non-AR and AR modes.
     */
    var device: CameraDevice? = null
        private set

    val isOpened get() = device != null

    private var isResumed = false

    /**
     * Looper handler thread
     */
    private lateinit var backgroundThread: HandlerThread

    /**
     * Looper handler
     */
    private lateinit var backgroundHandler: Handler

    /**
     * Prevent any changes to camera capture session after CameraManager.openCamera() is called, but
     * before camera device becomes active.
     */
    private val captureSessionChangesPossible = Mutex(true)

    /**
     * Camera capture session. Used by both non-AR and AR modes.
     */
    private var captureSession: CameraCaptureSession? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val channel = Channel<Boolean>()
    val channel2 = Deferred<Boolean>()

    /**
     * Whether the surface has been created.
     */
    private val surfaceCreated = false

    private var activatingCamera: CompletableDeferred<Boolean>? = null

    //    private val activatingCamera = MutableStateFlow(false)
    private val activatingCamera2 = Mutex(false)

    fun waitForCameraActivationEnd(block: () -> Unit) {
        coroutineScope.launch {
            if (activatingCamera.value) {
                activatingCamera.first { !it }
            }
            block()
        }
    }

    fun resume() {
        if (isResumed) return
        isResumed = true

        coroutineScope.launch {
            activatingCamera?.await()

            // Start background handler thread, used to run callbacks without blocking UI thread.
            backgroundThread = HandlerThread("sharedCameraBackground").apply { start() }
            backgroundHandler = Handler(backgroundThread.looper)

            // When the activity starts and resumes for the first time, openCamera() will be called
            // from onSurfaceCreated(). In subsequent resumes we call openCamera() here.
            if (surfaceCreated) {
                openCamera()
            }

            if (arSession == null && PermissionHelper.hasCameraPermission(context)) {
                // Camera Permission
                if (checkCameraPermission && !cameraPermissionRequested &&
                    !checkCameraPermission(activity, cameraPermissionLauncher)
                ) {
                    cameraPermissionRequested = true
                } else if (!appSettingsRequested) {
                    // In case of Camera permission previously denied, the allow popup won't show but
                    // the onResume will be called anyway.
                    // So if we launch the app settings screen, the onResume will be called twice.
                    // In order to avoid multiple session creation failing because camera permission is
                    // still not granted, we check if the app settings screen is displayed.
                    // In last case, if the camera permission is still not granted a SecurityException
                    // will be thrown when trying to create session.
                    try {
                        // ARCore install and update if camera permission is granted.
                        // For now, ARCore session will throw an exception if the camera is not
                        // accessible (ARCore cannot be used without camera.
                        // Request installation if necessary
                        if (checkAvailability && !installRequested &&
                            !checkInstall(activity, installRequested)
                        ) {
                            // Session will be created if everything is ok on next onResume(), so we
                            // return for now
                            installRequested = true
                        } else {
                            // Create a session if Google Play Services for AR is installed and up to
                            // date.
                            arSession = createSession(lifecycle, features)
                            arSession?.let {
                                lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
                                    onArSessionCreated(it)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        onException(e)
                    }
                }
            }
        }
    }

    /**
     * Called when starting non-AR mode or switching to non-AR mode.
     *
     * Also called when app starts or resumes.
     */
    private fun setRepeatingCaptureRequest() {

    }

    /**
     * Camera device state callback.
     *
     * A callback objects for receiving updates about the state of a camera device.
     */
    inner class CameraDeviceCallback : CameraDevice.StateCallback() {
        /**
         * @throws CameraAccessException
         */
        override fun onOpened(device: CameraDevice) {
            this@SharedCamera.device = device

            // Create an ARCore compatible capture request using `TEMPLATE_RECORD`.
            val captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

            // Build surfaces list, starting with ARCore provided surfaces.
            val surfaceList = sessionCamera.arCoreSurfaces +
                    // Add the Filament Camera Stream CPU image reader surface.
                    // On devices that don't support CPU image access, the image may arrive
                    // significantly later, or not arrive at all.
                    arCameraStream.imageReader.surface +
                    // Add custom images readers if any
                    customCameraImageReaders.map { it.surface }


            // Surface list should now contain three surfaces:
            // 0. sharedCamera.getSurfaceTexture()
            // 1. …
            // 2. arCameraStream.mageReader.getSurface()
            // 3. customCameraImageReaders.getSurface()
            // 4. …

            // Add ARCore surfaces and CPU image surface targets.
            surfaceList.forEach { captureRequestBuilder.addTarget(it) }

            // Wrap our callback in a shared camera callback.
            val wrappedCallback: CameraCaptureSession.StateCallback =
                sharedCamera!!.createARSessionStateCallback(
                    CameraSessionStateCallback(),
                    backgroundHandler
                )

            // Create camera capture session for camera preview using ARCore wrapped callback.
            device.createCaptureSession(
                surfaceList,
                wrappedCallback,
                backgroundHandler
            )
        }

        override fun onClosed(cameraDevice: CameraDevice) {
            Log.d(
                com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                "Camera device ID " + cameraDevice.id + " closed."
            )
            this@SharedCameraActivity.cameraDevice = null
            safeToExitApp.open()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.w(
                com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                "Camera device ID " + cameraDevice.id + " disconnected."
            )
            cameraDevice.close()
            this@SharedCameraActivity.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            Log.e(
                com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                "Camera device ID " + cameraDevice.id + " error " + error
            )
            cameraDevice.close()
            this@SharedCameraActivity.cameraDevice = null
            // Fatal error. Quit application.
            finish()
        }
    }

    /**
     * Repeating camera capture session state callback.
     */
    inner class CameraSessionStateCallback : CameraCaptureSession.StateCallback() {
        /**
         * Called when the camera capture session is first configured after the app is initialized,
         * and again each time the activity is resumed.
         */
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            session.setRepeatingRequest(
                previewCaptureRequestBuilder.build(), cameraCaptureCallback, backgroundHandler
            )
            // Note, resumeARCore() must be called in onActive(), not here.
        }

        override fun onActive(session: CameraCaptureSession) {
            if (!arcoreActive) {
                resumeARCore()
            }
            synchronized(this@SharedCamera) {
                captureSessionChangesPossible.value = true
            }
            updateSnackbarMessage()
        }
    }

    // Repeating camera capture session capture callback.
    private val cameraCaptureCallback: CameraCaptureSession.CaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                shouldUpdateSurfaceTexture.set(true)
            }

            override fun onCaptureBufferLost(
                session: CameraCaptureSession,
                request: CaptureRequest,
                target: Surface,
                frameNumber: Long
            ) {
                Log.e(
                    com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                    "onCaptureBufferLost: $frameNumber"
                )
            }

            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure
            ) {
                Log.e(
                    com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                    "onCaptureFailed: " + failure.frameNumber + " " + failure.reason
                )
            }

            override fun onCaptureSequenceAborted(
                session: CameraCaptureSession, sequenceId: Int
            ) {
                Log.e(
                    com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                    "onCaptureSequenceAborted: $sequenceId $session"
                )
            }
        }


    val imageReader = ImageReader.newInstance(
        resolution.width,
        resolution.height,
        ImageFormat.PRIVATE,
        IMAGE_READER_MAX_IMAGES,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
    )

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

    override fun onArSessionCreated(session: ArSessionOld) {
        super.onArSessionCreated(session)

        session.setCameraTextureNames(cameraTextureIds)
    }

    override fun onArSessionConfigChanged(session: ArSessionOld, config: Config) {
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


    // Perform various checks, then open camera device and create CPU image reader.
    private fun openCamera() {
        // Don't open camera if already opened.
        if (device != null) return

        // Verify CAMERA_PERMISSION has been granted.
        if (!PermissionHelper.hasCameraPermission(this)) {
            PermissionHelper.requestCameraPermission(this)
            return
        }

        // Make sure that ARCore is installed, up to date, and supported on this device.
        if (!isARCoreSupportedAndUpToDate()) {
            return
        }
        if (sharedSession == null) {
            try {
                // Create ARCore session that supports camera sharing.
                sharedSession = Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA))
            } catch (e: Exception) {
                errorCreatingSession = true
                messageSnackbarHelper.showError(
                    this, "Failed to create ARCore session that supports camera sharing"
                )
                Log.e(
                    com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                    "Failed to create ARCore session that supports camera sharing",
                    e
                )
                return
            }
            errorCreatingSession = false

            // Enable auto focus mode while ARCore is running.
            val config: Config = sharedSession.getConfig()
            config.setFocusMode(Config.FocusMode.AUTO)
            sharedSession.configure(config)
        }

        // Store the ARCore shared camera reference.
        sharedCamera = sharedSession.getSharedCamera()

        // Store the ID of the camera used by ARCore.
        cameraId = sharedSession.getCameraConfig().getCameraId()

        // Use the currently configured CPU image size.
        val desiredCpuImageSize: Size = sharedSession.getCameraConfig().getImageSize()
        cpuImageReader = ImageReader.newInstance(
            desiredCpuImageSize.width,
            desiredCpuImageSize.height,
            ImageFormat.YUV_420_888,
            2
        )
        cpuImageReader.setOnImageAvailableListener(this, backgroundHandler)

        // When ARCore is running, make sure it also updates our CPU image surface.
        sharedCamera.setAppSurfaces(
            this.cameraId,
            Arrays.asList<Surface>(cpuImageReader.getSurface())
        )
        try {

            // Wrap our callback in a shared camera callback.
            val wrappedCallback =
                sharedCamera.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler)

            // Store a reference to the camera system service.
            cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            // Get the characteristics for the ARCore camera.
            val characteristics: CameraCharacteristics =
                cameraManager.getCameraCharacteristics(this.cameraId)

            // On Android P and later, get list of keys that are difficult to apply per-frame and can
            // result in unexpected delays when modified during the capture session lifetime.
            if (Build.VERSION.SDK_INT >= 28) {
                keysThatCanCauseCaptureDelaysWhenModified = characteristics.availableSessionKeys
                if (keysThatCanCauseCaptureDelaysWhenModified == null) {
                    // Initialize the list to an empty list if getAvailableSessionKeys() returns null.
                    keysThatCanCauseCaptureDelaysWhenModified = ArrayList<CaptureRequest.Key<*>>()
                }
            }

            // Prevent app crashes due to quick operations on camera open / close by waiting for the
            // capture session's onActive() callback to be triggered.
            captureSessionChangesPossible = false

            // Open the camera device using the ARCore wrapped callback.
            cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e(
                com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                "Failed to open camera",
                e
            )
        } catch (e: IllegalArgumentException) {
            Log.e(
                com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                "Failed to open camera",
                e
            )
        } catch (e: SecurityException) {
            Log.e(
                com.google.ar.core.examples.java.sharedcamera.SharedCameraActivity.TAG,
                "Failed to open camera",
                e
            )
        }
    }


    companion object {
        // This seems a little high, but lower values cause occasional "client tried to acquire
        // more than maxImages buffers" on a Pixel 3
        var IMAGE_READER_MAX_IMAGES = 7

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