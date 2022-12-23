package io.github.sceneview.ar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.HardwareBuffer
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.SwapChain
import com.google.android.filament.android.UiHelper
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.CameraConfig.FacingDirection
import com.google.ar.core.Config.*
import com.google.ar.core.Config.LightEstimationMode
import com.google.ar.core.exceptions.UnavailableException
import io.github.sceneview.SceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.camera.ArCameraStream
import io.github.sceneview.ar.camera.CameraManager
import io.github.sceneview.ar.camera.SharedCamera
import io.github.sceneview.ar.nodes.ARCameraNode
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.ar.utils.PermissionHelper
import io.github.sceneview.gesture.NodesManipulator
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toTransform
import io.github.sceneview.nodes.Node
import io.github.sceneview.scene.setCustomProjection
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.setKeepScreenOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

typealias ARFrame = Frame

/**
 * A SurfaceView that integrates with ARCore and renders a scene
 */
open class ArSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    /**
     * Provide your own instance if you want to share Filament resources between multiple views.
     */
    sharedEngine: Engine? = null,
    /**
     * Provide your own instance if you want to share [Node]s instances between multiple views.
     */
    sharedNodeManager: NodeManager? = null,
    /**
     * Provide your own instance if you want to share [Node]s selection between multiple views.
     */
    sharedNodesManipulator: NodesManipulator? = null,
    /**
     * Provided by Filament to manage SurfaceView and SurfaceTexture.
     *
     * To choose a specific rendering resolution, add the following line:
     * `uiHelper.setDesiredSize(1280, 720)`
     */
    uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK),
    /**
     * Provide your own instance if you want to share ARCore session with your own system.
     *
     * The [Session] must support [Session.Feature.SHARED_CAMERA]
     * null if you want to use the default session creation.
     */
    private var sharedSession: Session? = null,
    /**
     * Select the camera config filters you need to enable/disable so that it can obtain the list of
     * camera configs that are supported on the device camera.
     */
    val cameraConfigFilter: CameraConfigFilter.() -> Unit = {
        facingDirection = FacingDirection.BACK
    },
    /**
     * Additional camera device stream [ImageReader]s
     *
     * Receive additional images directly coming from the camera device when ARCore is active.
     *
     * Those image readers can be used to render the camera stream, record the original camera
     * output or to link any AI/ML/CV that need access to the original camera source.
     *
     * It is the app's responsibility to ensure that the [CameraDevice] and ARCore both support the
     * full set of streams/surfaces/resolutions.
     *
     * A single additional surface, with a resolution equal to one of the CPU resolutions returned
     * by [Session.getSupportedCameraConfigs], is guaranteed to be supported by the [CameraDevice]
     * and ARCore. Additional surfaces beyond one, and resolutions not listed in
     * [Session.getSupportedCameraConfigs], might or might not be supported by the device and/or
     * ARCore. It is the app developer's responsibility to verify that ARCore and all targeted
     * devices support the requested app surfaces.
     */
    val cameraImageReaders: (session: Session) -> List<ImageReader> = { listOf() }
) : SceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
    sharedEngine,
    sharedNodeManager,
    sharedNodesManipulator,
    uiHelper,
    cameraNode = ARCameraNode(),
    cameraManipulator = null
) {
    var session: Session? = null
        private set

    /**
     * Gets the [SharedCamera] object of the session.
     *
     * `null` if session is not created for shared camera.
     */
    var sharedCamera: com.google.ar.core.SharedCamera? = null
        private set

    /**
     * Gets the camera identifier associated with the session config.
     *
     * This camera ID is the same ID as returned by Camera2 [CameraManager.getCameraIdList].
     */
    var cameraId: String? = null
        private set

    /**
     * Sets the desired [Config.FocusMode].
     *
     * The default focus mode varies by device and camera, and is set to optimize AR tracking.
     * Currently the default on most ARCore devices and cameras is [Config.FocusMode.FIXED],
     * although this default might change in the future.
     *
     * Note: On devices where ARCore does not support auto focus due to the use of a fixed focus
     * camera, setting [Config.FocusMode.AUTO] will be ignored.
     * Similarly, on devices where tracking requires auto focus, setting [Config.FocusMode.FIXED]
     * will be ignored.
     * See the ARCore supported devices: https://developers.google.com/ar/devices page for a list
     * of affected devices.
     *
     * To determine whether the configured ARCore camera supports auto focus, query
     * [android.hardware.camera2.CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES].
     *
     * @see Config.getFocusMode
     * @see Config.setFocusMode
     */
    var focusMode = FocusMode.AUTO
        get() = session?.config?.focusMode ?: field
        set(value) {
            field = value
            session?.configure { focusMode = value }
        }

    /**
     * Sets the desired lighting estimation mode.
     *
     * See the [Config.LightEstimationMode] enum for available options.
     *
     * @see Config.getLightEstimationMode
     * @see Config.setLightEstimationMode
     */
    var lightEstimationMode = LightEstimationMode.ENVIRONMENTAL_HDR
        get() = session?.config?.lightEstimationMode ?: field
        set(value) {
            field = value
            session?.configure { lightEstimationMode = value }
        }

    /**
     * Sets the desired plane finding mode.
     *
     * See the [Config.PlaneFindingMode] enum for available options.
     *
     * @see Config.getPlaneFindingMode
     * @see Config.setPlaneFindingMode
     */
    var planeFindingMode = PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        get() = session?.config?.planeFindingMode ?: field
        set(value) {
            field = value
            session?.configure { planeFindingMode = value }
        }

    /**
     * Sets the desired [Config.DepthMode].
     *
     * Notes:
     * - Not all devices support all modes. Use [Session.isDepthModeSupported] to determine whether
     * the current device and the selected camera support a particular depth mode.
     * - With depth enabled through this call, calls to [Frame.acquireDepthImage16Bits] and
     * [Frame.acquireRawDepthImage16Bits] can be made to acquire the latest computed depth image.
     *
     * @see Config.getDepthMode
     * @see Config.setDepthMode
     */
    var depthMode = DepthMode.AUTOMATIC
        get() = session?.config?.depthMode ?: field
        set(value) {
            field = value
            session?.configure { depthMode = value }
        }

    /**
     * Enable the depth occlusion material.
     *
     * This will process the incoming DepthImage to occlude virtual objects behind real world
     * objects.
     *
     * If the [Session] is not configured properly the standard camera material is used.
     * Valid [Session] configuration for the DepthMode are [Config.DepthMode.AUTOMATIC] and
     * [Config.DepthMode.RAW_DEPTH_ONLY]
     *
     * Disable this value to apply the standard camera material to the CameraStream.
     *
     * @see ArCameraStream.isDepthOcclusionEnabled
     */
    var isDepthOcclusionEnabled
        get() = cameraStream.isDepthOcclusionEnabled
        set(value) {
            if (depthMode == DepthMode.DISABLED) {
                depthMode = DepthMode.AUTOMATIC
            }
            cameraStream.isDepthOcclusionEnabled = value
        }


    /**
     * Sets the desired Instant Placement mode.
     *
     * See [Config.InstantPlacementMode] for available options.
     *
     * @see Config.getDepthMode
     * @see Config.setDepthMode
     */
    var instantPlacementMode = InstantPlacementMode.LOCAL_Y_UP
        get() = session?.config?.instantPlacementMode ?: field
        set(value) {
            field = value
            session?.configure { instantPlacementMode = value }
        }

    /**
     * Sets the cloud anchor mode of the [Session].
     *
     * @see Config.getCloudAnchorMode
     * @see Config.setCloudAnchorMode
     */
    var cloudAnchorMode = CloudAnchorMode.DISABLED
        get() = session?.config?.cloudAnchorMode ?: field
        set(value) {
            field = value
            session?.configure { cloudAnchorMode = value }
        }

    /**
     * Sets the desired Geospatial mode.
     *
     * See [Config.GeospatialMode] for available options.
     *
     * @see Config.getGeospatialMode
     * @see Config.setGeospatialMode
     */
    var geospatialMode = GeospatialMode.DISABLED
        get() = session?.config?.geospatialMode ?: field
        set(value) {
            field = value
            session?.configure { geospatialMode = value }
        }

    /**
     * The distance at which to create an InstantPlacementPoint in meters.
     *
     * Assumed distance in meters from the device camera to the surface on which user will try to
     * place models.
     *
     * This value affects the apparent scale of objects while the tracking method of the
     * Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE. Values in the
     * [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower values for AR
     * experiences where users are expected to place objects on surfaces close to the camera.
     * Use larger values for experiences where the user will likely be standing and trying to
     * place an object on the ground or floor in front of them.
     */
    var instantPlacementDistance = 2.0f

//    /**
//     * ### ARCore light estimation configuration
//     *
//     * ARCore estimate lighting to provide directional light, ambient spherical harmonics,
//     * and reflection cubemap estimation
//     *
//     * Light bounces off of surfaces differently depending on whether the surface has specular
//     * (highly reflective) or diffuse (not reflective) properties.
//     * For example, a metallic ball will be highly specular and reflect its environment, while
//     * another ball painted a dull matte gray will be diffuse. Most real-world objects have a
//     * combination of these properties — think of a scuffed-up bowling ball or a well-used credit
//     * card.
//     *
//     * Reflective surfaces also pick up colors from the ambient environment. The coloring of an
//     * object can be directly affected by the coloring of its environment. For example, a white ball
//     * in a blue room will take on a bluish hue.
//     *
//     * The main directional light API calculates the direction and intensity of the scene's
//     * main light source. This information allows virtual objects in your scene to show reasonably
//     * positioned specular highlights, and to cast shadows in a direction consistent with other
//     * visible real objects.
//     *
//     * @see LightEstimationMode.ENVIRONMENTAL_HDR
//     * @see LightEstimationMode.ENVIRONMENTAL_HDR_NO_REFLECTIONS
//     * @see LightEstimationMode.ENVIRONMENTAL_HDR_FAKE_REFLECTIONS
//     * @see LightEstimationMode.AMBIENT_INTENSITY
//     * @see LightEstimationMode.DISABLED
//     */
//    var lightEstimationMode: LightEstimationMode
//        get() = lightEstimator.mode
//        set(value) {
//            lightEstimator.mode = value
//        }

//    /**
//     * Sets camera facing direction filter.
//     *
//     * The default value is [CameraConfig.FacingDirection.BACK].
//     *
//     * Currently, a back-facing (world) camera is guaranteed to be available on all ARCore supported
//     * devices. Most ARCore supported devices also include support for a front-facing (selfie)
//     * camera. See [ARCore supported devices](https://developers.google.com/ar/devices) for
//     * available camera configs by device.
//     *
//     * Beginning with ARCore SDK 1.23.0, the default value is [CameraConfig.FacingDirection.FRONT]
//     * if the Session is created using the deprecated [Session.Feature.FRONT_CAMERA] feature.
//     *
//     * @see CameraConfig.getFacingDirection
//     * @see CameraConfigFilter.getFacingDirection
//     * @see CameraConfigFilter.setFacingDirection
//     */
//    var cameraFacingDirection = FacingDirection.BACK
//        get() = session?.cameraFacingDirection ?: field
//        set(value) {
//            field = value
//            isFrontFaceWindingInverted = value == FacingDirection.FRONT
//            session?.cameraFacingDirection = value
//        }

    /**
     * Camera GPU image reader used to render into the Filament stream
     */
    var cameraGPUImageReader: ImageReader? = null
        private set

    var cameraStream: ArCameraStream? = null
        private set

    /**
     * ### [PlaneRenderer] used to control plane visualization.
     */
    val planeRenderer = PlaneRenderer(lifecycle)

    /**
     * ### The environment and main light that are estimated by AR Core to render the scene.
     *
     * - Environment handles a reflections, indirect lighting and skybox.
     * - ARCore will estimate the direction, the intensity and the color of the light
     */
    val lightEstimator = LightEstimator(lifecycle, ::onLightEstimationUpdate)

    var mainLightEstimated: Light? = null
        private set(value) {
            if (field != value) {
                (field ?: mainLight)?.let { removeLight(it) }
                field = value
                (value ?: mainLight)?.let { addLight(it) }
            }
        }

    var environmentEstimated: Environment? = null
        private set(value) {
            field = value
            indirectLightEstimated = value?.indirectLight
        }

    var indirectLightEstimated: IndirectLight? = null
        private set(value) {
            if (field != value) {
                field = value
                scene.indirectLight = value ?: indirectLight
            }
        }

    val instructions = Instructions(lifecycle)

    var onSessionCreated: ((session: Session) -> Unit)? = null

    /**
     * ### Invoked when an ARCore error occurred
     *
     * Registers a callback to be invoked when the ARCore Session cannot be initialized because
     * ARCore is not available on the device or the camera permission has been denied.
     */
    var onArSessionFailed: ((exception: Exception) -> Unit)? = null
    var onArSessionResumed: ((session: ArSession) -> Unit)? = null
    var onArSessionConfigChanged: ((session: ArSession, config: Config) -> Unit)? = null

    /**
     * Invoked when an ARCore frame is processed
     *
     * Registers a callback to be invoked when a valid ARCore Frame is processing.
     *
     * The callback to be invoked once per frame **immediately before the scene is updated**.
     *
     * The callback will only be invoked if the Frame is considered as valid.
     */
    val onArFrame = mutableListOf<(arFrame: Frame) -> Unit>()

    /**
     * ### Invoked when an ARCore trackable is tapped
     *
     * Depending on the session configuration the [HitResult.getTrackable] can be:
     * - A [Plane] if [ArSession.planeFindingEnabled].
     * - An [InstantPlacementPoint] if [ArSession.instantPlacementEnabled].
     * - A [DepthPoint] and [Point] if [ArSession.depthEnabled].
     *
     * The listener is only invoked if no node is tapped.
     *
     * - `hitResult` - The ARCore hit result for the trackable that was tapped.
     * - `motionEvent` - The motion event that caused the tap.
     */
    var onTapAr: ((hitResult: HitResult, motionEvent: MotionEvent) -> Unit)? = null

//    /**
//     * Invoked when an ARCore AugmentedImage TrackingState/TrackingMethod is updated?
//     *
//     * Registers a callback to be invoked when an ARCore AugmentedImage TrackingState/TrackingMethod
//     * is updated. The callback will be invoked on each AugmentedImage update.
//     *
//     * @see AugmentedImage.getTrackingState
//     * @see AugmentedImage.getTrackingMethod
//     */
//    var onAugmentedImageUpdate = mutableListOf<(augmentedImage: AugmentedImage) -> Unit>()
//
//    /**
//     * Invoked when an ARCore AugmentedFace TrackingState is updated.
//     *
//     * Registers a callback to be invoked when an ARCore AugmentedFace TrackingState is updated. The
//     * callback will be invoked on each AugmentedFace update.
//     *
//     * @see AugmentedFace.getTrackingState
//     */
//    var onAugmentedFaceUpdate: ((augmentedFace: AugmentedFace) -> Unit)? = null

    lateinit var cameraManager: android.hardware.camera2.CameraManager

    override var swapChain: SwapChain? = null
        set(value) {
            field = value
            if (value != null) {
                // Open the camera when the surface is created
                openCamera()
            }
        }

    private var hasShownAppSettings = false

    /**
     * Set a listener to override the default camera permission denied behavior.
     *
     * This call may happen on each onResume().
     *
     * @see onARUnavailable
     */
    var onCameraPermissionDenied: (() -> Unit)? = {
        Toast.makeText(context, R.string.sceneview_camera_permission_required, Toast.LENGTH_LONG)
            .show()
        // Check to app settings screen already to avoid infinite orResume() callbacks
        if (!hasShownAppSettings) {
            // Launch application setting to manually grant permission
            activity?.apply {
                startActivity(
                    Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", packageName, null)
                    })
            }
            hasShownAppSettings = true
        } else {
            // We don't need to listen for result since the onResume() will check permission again
            onARUnavailable()
        }
    }

    /**
     * Set a listener to override the default camera permission denied behavior.
     *
     * This call may happen on each onResume().
     *
     * @see onARUnavailable
     */
    var onCameraError: ((error: Int?, exception: Exception?) -> Unit)? = { _, _ ->
        Toast.makeText(context, R.string.sceneview_camera_error, Toast.LENGTH_LONG)
            .show()
        onARUnavailable()
    }

    /**
     * Set a listener to override the default ARCore unavailable or error behavior.
     *
     * AR unavailability may occur in different case: device not supported, install
     * refused, version not updates. Have a look at the possible [Availability] values to handle
     * things depending on the case.
     *
     * This listener won't be called if the camera permission was previously denied. So you should
     * use it only if you nees more infos around the unavailability or error reason.
     *
     * This call may happen on each onResume().
     *
     * @see onARUnavailable
     */
    var onARCoreError: ((availability: Availability, error: Exception?) -> Unit)? = { _, _ ->
        Toast.makeText(context, R.string.sceneview_arcore_not_installed, Toast.LENGTH_LONG)
            .show()
        onARUnavailable()
    }

    /**
     * Set a listener to override the default AR unavailable behavior.
     *
     * AR unavailability may occur when camera permission was denied OR when ARCore is not
     * available.
     *
     * This is the good place to handle possible fallbacks to 3D only, dialogs or finish() when AR
     * is not available.
     *
     * This call may happen on each onResume().
     */
    var onARUnavailable: () -> Unit = {
    }

//    /**
//     * Whether the Android surface has been created
//     */
//    private val surfaceCreated = false

    override fun resume() {
        super.resume()

        // Start background handler thread, used to run callbacks without blocking UI thread.
        backgroundThread = HandlerThread("sharedCameraBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread.looper)

        // When the activity starts and resumes for the first time, openCamera() will be called
        // from onSurfaceCreated(). In subsequent resumes we call openCamera() here.
        if (swapChain != null) {
            openCamera()
        }

        if (session == null && PermissionHelper.hasCameraPermission(context)) {
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
                        session = createSession(lifecycle, features)
                        session?.let {
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

    private var activity: ComponentActivity? = null
    private var cameraPermissionLauncher: ActivityResultLauncher<String>? = null

    private var requestedARCoreInstall = false

    /**
     * Define the Activity for lifecycle and automate camera permissions and ARCore checks.
     *
     * If you're not under a [Fragment] context, please handle lifecycle, permissions and
     * ARCore on your side.
     *
     * Must be called before onResume()
     */
    fun setFragment(fragment: Fragment) {
        activity = fragment.activity
        // Must be called before on resume
        cameraPermissionLauncher = fragment.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // We don't need to listen for onGranted since the onResume will call the launcher
            if (!isGranted && !fragment.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                onCameraPermissionDenied?.invoke()
            }
        }
        setLifecycle(fragment.lifecycle)
    }

    /**
     * Define the Activity for lifecycle and automate camera permissions and ARCore checks.
     *
     * - If you're under a [Fragment] context, please use [setFragment] instead in order to give
     * link the view to the fragment lifecycle instead of the activity one.
     * - If you're not under a [ComponentActivity] context, please handle lifecycle, permissions and
     * ARCore on your side.
     *
     * Must be called before onResume()
     */
    fun setActivity(activity: ComponentActivity) {
        this.activity = activity
        // Must be called before on resume
        cameraPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            // We don't need to listen for onGranted since the onResume will call the launcher
            if (!isGranted && !activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                onCameraPermissionDenied?.invoke()
            }
        }
        setLifecycle(activity.lifecycle)
    }

    // Whether ARCore is currently active.
    val isARCoreActive = MutableStateFlow(false)

    /**
     * Prevent any changes to camera capture session after CameraManager.openCamera() is called, but
     * before camera device becomes active.
     */
    private var cameraActivation = Mutex(true)

    private val shouldUpdateSurfaceTexture = AtomicBoolean(false)

    inner class CameraDeviceCallback(private val surfaces: List<Surface>) :
        CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            try {
//            sharedSession!!.setCameraTextureName(backgroundRenderer.getTextureId())
//            sharedCamera!!.surfaceTexture.setOnFrameAvailableListener(this)

                // Create an ARCore compatible capture request using `TEMPLATE_RECORD`.
                val captureRequestBuilder =
                    camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)

                // Build surfaces list, starting with ARCore provided surfaces.
                // Add app images readers surfaces.
                // On devices that don't support CPU image access, the image may arrive
                // significantly later, or not arrive at all.
                val surfaceList = (sharedCamera!!.arCoreSurfaces + surfaces).apply {
                    // Add ARCore surfaces and app images surfaces targets.
                    forEach { captureRequestBuilder.addTarget(it) }
                }

                // Create camera capture session for camera preview using ARCore wrapped callback.
                cameraDevice!!.createCaptureSession(
                    surfaceList,
                    // Wrap our callback in a shared camera callback.
                    sharedCamera!!.createARSessionStateCallback(
                        CameraCaptureSessionCallback(captureRequestBuilder),
                        backgroundHandler
                    ),
                    backgroundHandler
                )
            } catch (e: Exception) {
                onCameraError?.invoke(null, e)
            }
        }

        override fun onClosed(camera: CameraDevice) {
            cameraDevice = null
        }

        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
            cameraDevice = null
        }

        override fun onError(camera: CameraDevice, error: Int) {
            camera.close()
            cameraDevice = null
            onCameraError?.invoke(error, Exception("Failed to open Camera: Error $error."))
        }
    }

    inner class CameraCaptureSessionCallback(val captureRequestBuilder: CaptureRequest.Builder) :
        CameraCaptureSession.StateCallback() {
        // Called when the camera capture session is first configured after the app
        // is initialized, and again each time the activity is resumed.
        override fun onConfigured(captureSession: CameraCaptureSession) {
            cameraCaptureSession = captureSession.apply {
                setRepeatingRequest(
                    captureRequestBuilder.build(),
                    object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession,
                            request: CaptureRequest,
                            result: TotalCaptureResult
                        ) {
                            shouldUpdateSurfaceTexture.set(true)
                        }
                    },
                    backgroundHandler
                )
            }
        }

        override fun onActive(captureSession: CameraCaptureSession) {
            // To avoid flicker when resuming ARCore mode inform the renderer to not suppress rendering
            // of the frames with zero timestamp.
            backgroundRenderer.suppressTimestampZeroRendering(false)
            // Resume ARCore.
            try {
                session?.takeIf { !isSessionResumed }?.apply {
                    resume()
                    isARCoreActive.value = true
                }
            } catch (e: Exception) {
                onARCoreError?.invoke(Availability.SUPPORTED_INSTALLED, e)
            }
            cameraActivation.unlock(this@ArSceneView)
        }

        override fun onConfigureFailed(captureSession: CameraCaptureSession) {
            onCameraError?.invoke(
                null,
                Exception("Failed to configure camera capture session.")
            )
        }
    }

    /**
     * Perform various checks, then open camera device and create image readers.
     */
    open fun openCamera() {
        // Don't open camera if already opened.
        if (sharedCamera.isOpened ||
            // Check for Camera permission
            !checkCameraPermission() ||
            // Check for ARCore installed and updated
            !checkARCoreInstall()
        ) {
            return
        }

        if (session == null) {
            session = sharedSession ?: createSession()
        }

        // Store the ARCore shared camera reference.
        sharedCamera = session?.sharedCamera
        // Store the ID of the camera used by ARCore.
        cameraId = session?.cameraConfig?.cameraId

//        val session = session ?: return

        session?.let { session ->
            cameraGPUImageReader?.close()
            // Gets the dimensions of the image frames for texture that are sent on GPU stream.
            val textureSize = session.cameraConfig.textureSize
            cameraGPUImageReader = ImageReader.newInstance(
                textureSize.width, textureSize.height,
                ImageFormat.PRIVATE,
                ArCameraStream.IMAGE_READER_MAX_IMAGES,
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
            )
        }
        cameraStream = session?.let {
            ArCameraStream(it.cameraConfig.textureSize, backgroundHandler)
        }

        try {
            // Store a reference to the camera system service.
            cameraManager =
                context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager

            val appSurfaces = (listOfNotNull(cameraGPUImageReader) + cameraImageReaders(session!!))
                .map { it.surface }

            // When ARCore is running, make sure it also updates our CPU image surface.
            sharedCamera!!.setAppSurfaces(cameraId, appSurfaces)

            lockCameraActivation {
                // Open the camera device using the ARCore wrapped callback.
                cameraManager.openCamera(
                    cameraId!!,
                    // Wrap our callback in a shared camera callback.
                    sharedCamera!!.createARDeviceStateCallback(
                        CameraDeviceCallback(appSurfaces),
                        backgroundHandler
                    ),
                    backgroundHandler
                )
            }
        } catch (e: Exception) {
            onCameraError?.invoke(null, e)
        }
    }

    var onGPUCameraImageAvailable: ((imageReader: ImageReader) -> Unit)? = null

    /**
     * Listen for acquired images from the camera GPU image reader
     */
    var onGPUCameraImage: ((image: Image) -> Unit)? = null

    /**
     * Callback that is called when a new image is available from camera ImageReader.
     */
    open fun onGPUCameraImageAvailable(imageReader: ImageReader) {
        onGPUCameraImageAvailable?.invoke(imageReader)
        imageReader.acquireLatestImage()?.let { image ->
            filamentStream.setAcquiredImage(image.hardwareBuffer, Handler(Looper.getMainLooper())) {
                onGPUCameraImage?.invoke(image)
                image.close()
            }
        }
    }

    /**
     * Prevent app crashes due to quick operations on camera open / close by waiting for the capture
     * session's onActive() callback to be triggered.
     */
    private fun lockCameraActivation(block: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            cameraActivation.withLock(this@ArSceneView, block)
        }
    }

    // Close the camera device.
    private fun closeCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null
        cameraDevice?.apply {
            lockCameraActivation {
                close()
            }
        }
        cameraImageReaders
        if (cpuImageReader != null) {
            cpuImageReader.close()
            cpuImageReader = null
        }
    }

    /**
     * Creates the ARCore session that supports camera sharing.
     */
    open fun createSession(): Session? = try {
        ARSession(context, EnumSet.of(Session.Feature.SHARED_CAMERA)).apply {
            cameraFacingDirection = this@ArSceneView.cameraFacingDirection
            configure {
                focusMode = this@ArSceneView.focusMode
                lightEstimationMode = this@ArSceneView.lightEstimationMode
                planeFindingMode = this@ArSceneView.planeFindingMode
                depthMode = this@ArSceneView.depthMode
                instantPlacementMode = this@ArSceneView.instantPlacementMode
                cloudAnchorMode = this@ArSceneView.cloudAnchorMode
                geospatialMode = this@ArSceneView.geospatialMode
            }
        }.also {
            onSessionCreated?.invoke(it)
        }
    } catch (e: Exception) {
        onARCoreError?.invoke(Availability.SUPPORTED_INSTALLED, e)
        null
    }

    /**
     * Check for user Camera permission.
     *
     * Make sure you have called [setFragment] or [setActivity] before calling this function.
     * Handle the permission on your side if you aren't under a [Fragment] or [ComponentActivity]
     * context (Flutter, ReactNative,...)
     */
    open fun checkCameraPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED).also { hasPermission ->
            if (!hasPermission) {
                cameraPermissionLauncher?.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Make sure ARCore is installed and supported on this device.
     *
     * Request ARCore installation or update if needed.
     */
    open fun checkARCoreInstall(): Boolean {
        return when (val availability = ArCoreApk.getInstance().checkAvailability(context)) {
            Availability.SUPPORTED_INSTALLED -> true
            Availability.SUPPORTED_APK_TOO_OLD,
            Availability.SUPPORTED_NOT_INSTALLED -> try {
                activity?.let { activity ->
                    when (ArCoreApk.getInstance()
                        .requestInstall(activity, requestedARCoreInstall)) {
                        InstallStatus.INSTALLED -> true
                        InstallStatus.INSTALL_REQUESTED -> {
                            requestedARCoreInstall = true
                            false
                        }
                    }
                } ?: false
            } catch (e: UnavailableException) {
                onARCoreError?.invoke(availability, e)
                false
            }
            Availability.UNKNOWN_ERROR,
            Availability.UNKNOWN_CHECKING,
            Availability.UNKNOWN_TIMED_OUT,
            Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                onARCoreError?.invoke((availability, null)
                false
            }
        }
    }

    fun <T> arFrameFlow(callback: (frame: Frame) -> T) = callbackFlow {
        onArFrame += { frame: Frame ->
            try {
                trySend(callback(frame))
            } catch (e: Exception) {
                close(e)
            }
            Unit
        }.also {
            awaitClose {
                onArFrame -= it
            }
        }
    }

//    fun checkPermissionAndARCore(activity: ComponentActivity) {
//        if (!PermissionHelper.hasCameraPermission(activity)) {
//            PermissionHelper.requestCameraPermission(activity) {
//                cameraManager.openCamera()
//            }
//        } else {
//            cameraManager.openCamera()
//        }
//    }
//
//    fun checkPermissionAndARCore(fragment: Fragment) {
//        if (!PermissionHelper.hasCameraPermission(fragment.requireContext())) {
//            PermissionHelper.requestCameraPermission(fragment) {
//                cameraManager.openCamera()
//            }
//        } else {
//            cameraManager.openCamera()
//        }
//    }


    override fun onArSessionCreated(session: ArSession) {
        super.onArSessionCreated(session)

        session.configure { config ->
            // FocusMode must be changed after the session resume to work
            // config.focusMode = focusMode
            config.planeFindingMode = _planeFindingMode
            config.depthMode = _depthMode
            config.instantPlacementEnabled = _instantPlacementEnabled
            config.cloudAnchorEnabled = _cloudAnchorEnabled
            config.lightEstimationMode = _sessionLightEstimationMode
            config.geospatialEnabled = _geospatialEnabled
        }

        addEntity(cameraStream.renderable)

        onArSessionCreated?.invoke(session)
    }

    override fun onArSessionFailed(exception: Exception) {
        super.onArSessionFailed(exception)

        onArSessionFailed?.invoke(exception)
    }

    override fun onArSessionResumed(session: ArSession) {
        super.onArSessionResumed(session)

        session.configure { config ->
            // FocusMode must be changed after the session resume to work
            config.focusMode = _focusMode
        }

        onArSessionResumed?.invoke(session)
    }

    override fun onArSessionConfigChanged(session: ArSession, config: Config) {
        super.onArSessionConfigChanged(session, config)

        // Feature config, therefore facing direction, can only be configured once per session.
        isFrontFaceWindingInverted = session.cameraConfig.facingDirection == FacingDirection.FRONT

        onArSessionConfigChanged?.invoke(session, config)
    }

    override fun onFrame(frameTime: FrameTime) {
        // Perform ARCore per-frame update.

        // Perform ARCore per-frame update.
        val frame = session?.update()?.let { frame ->
            if (frame.timestamp != 0 &&)
                onARFrame(frameTime, frame)
        }
        // Check if no frame or same timestamp, no drawing.
        return super.update()?.takeIf {
            it.timestamp != (currentFrame?.frame?.timestamp ?: 0) && it.camera != null
        }

        if (uiHelper.isReadyToRender) {
            // Fetches the latest image (if any) from ImageReader and passes its HardwareBuffer to
            // Filament.
            cameraGPUImageReader?.acquireLatestImage()?.let { image ->
                cameraStream?.setCameraAcquiredImage(image) {
                    image.close()
                }
            }
        }

        super.onFrame(frameTime)
    }

    fun onARFrame(frameTime: FrameTime, frame: Frame) {

    }

    /**
     * Before the render call occurs, update the ARCore session to grab the latest frame and update
     * listeners.
     *
     * The super.onFrame() is called if the session updated successfully and a new frame was
     * obtained. Update the scene before rendering.
     */
    override fun doFrame(frameTime: FrameTime) {
        arSession?.update(frameTime)?.let { frame ->
            doArFrame(frame)
        }
        super.doFrame(frameTime)
    }

    /**
     * ### Invoked once per [Frame] immediately before the Scene is updated.
     *
     * The listener will be called in the order in which they were added.
     */
    protected open fun doArFrame(arFrame: ArFrame) {
        if (arFrame.camera.isTracking != currentFrame?.camera?.isTracking) {
            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            // You will say thanks when still have battery after a long day debugging an AR app.
            // ...and it's better for your users
            activity.setKeepScreenOn(arFrame.camera.isTracking)
        }

        // At the start of the frame, update the tracked pose of the camera
        // to use in any calculations during the frame.
        // TODO : Move to dedicated Lifecycle aware classes when Kotlined them
        cameraNode.updateTrackedPose(arFrame.camera)

        if (onAugmentedImageUpdate.isNotEmpty()) {
            arFrame.updatedAugmentedImages.forEach { augmentedImage ->
                onAugmentedImageUpdate.forEach {
                    it(augmentedImage)
                }
            }
        }

        if (onAugmentedFaceUpdate != null) {
            arFrame.updatedAugmentedFaces.forEach(onAugmentedFaceUpdate)
        }

        currentFrame = arFrame
        lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
            onArFrame(arFrame)
        }
        onArFrame?.invoke(arFrame)
    }


    var nearPlane = 0.01f
    var farPlane = 30.0f

    fun updateCamera(frame: Frame) {
        val arCamera = frame.camera ?: return
        camera?.apply {
            setCustomProjection(
                arCamera.getProjectionMatrix(nearPlane, farPlane),
                nearPlane,
                farPlane
            )
            setModelMatrix(arCamera.displayOrientedPose.transform)
        }
        val arProjectionMatrix = FloatArray(16).apply {
            arCamera.getProjectionMatrix(this, 0, nearPlane, farPlane)
        }
        val arViewMatrix = FloatArray(16).apply {
            arCamera.getViewMatrix(this, 0)
        }
    }


    open fun onLightEstimationUpdate(lightEstimation: LightEstimator) {
        mainLightEstimated = lightEstimation.mainLight
        environmentEstimated = lightEstimation.environment
    }

    /**
     * ### Define the session config used by ARCore
     *
     * Prefer calling this method before the global (Activity or Fragment) onResume() cause the session
     * base configuration in made there.
     * Any later calls (after onSessionResumed()) to this function are not completely sure be taken in
     * account by ARCore (even if most of them will work)
     *
     * Please check that all your Session Config parameters are taken in account by ARCore at
     * runtime.
     *
     * @param applyConfig the apply block for the new config
     */
    fun configureSession(applyConfig: (ArSession, Config) -> Unit) {
        lifecycle.doOnArSessionCreated { session ->
            session.configure { config ->
                applyConfig.invoke(session, config)
            }
        }
    }

    override fun onTap(motionEvent: MotionEvent, node: Node?, renderable: Renderable?) {
        super.onTap(motionEvent, node, renderable)

        if (node == null) {
            arSession?.currentFrame?.hitTest(motionEvent)?.let { hitResult ->
                onTapAr(hitResult, motionEvent)
            }
        }
    }

    /**
     * ### Invoked when an ARCore trackable is tapped
     *
     * Calls the `onTapAr` listener if it is available.
     *
     * @param hitResult The ARCore hit result for the trackable that was tapped.
     * @param motionEvent The motion event that caused the tap.
     */
    open fun onTapAr(hitResult: HitResult, motionEvent: MotionEvent) {
        onTapAr?.invoke(hitResult, motionEvent)
    }

    /**
     * Camera capture session
     */
    var cameraCaptureSession: CameraCaptureSession? = null
        private set

    /**
     * Camera device
     */
    var cameraDevice: CameraDevice? = null
        private set

    /**
     * Looper handler thread
     */
    private lateinit var backgroundThread: HandlerThread

    /**
     * Looper handler
     */
    private lateinit var backgroundHandler: Handler

    /**
     * The most recent ARCore Frame if it is available.
     *
     * The frame is updated at the beginning of each drawing frame.
     * Callers of this method should not retain a reference to the return value, since it will be
     * invalid to use the ARCore frame starting with the next frame.
     */
    var currentFrame: ARFrame? = null
        private set

    /**
     * Returns the current ambient light estimate, if light estimation was enabled.
     *
     * If lighting estimation is not enabled in the session configuration, the returned
     * Lighting Estimate will always return [LightEstimate.State.NOT_VALID] from
     * [LightEstimate.getState].
     *
     * @see Frame.lightEstimate
     */
    val lightEstimateFlow: Flow<LightEstimate> by lazy {
        arFrameFlow { frame ->
            frame.lightEstimate
        }
    }

    /**
     * Returns the [Trackable]'s that were changed by the [Session.update] that returned this Frame.
     *
     * @see Frame.getUpdatedTrackables
     */
    val updatedTrackablesFlow: Flow<List<Point>> by lazy {
        arFrameFlow { frame ->
            frame.getUpdatedTrackables(Point::class.java).toList()
        }
    }

    /**
     * Returns the [Plane]'s that were changed by the [Session.update] that returned this Frame.
     *
     * @see Frame.getUpdatedTrackables
     */
    val updatedPlanesFlow: Flow<List<Plane>> by lazy {
        arFrameFlow { frame ->
            frame.getUpdatedTrackables(Plane::class.java).toList()
        }
    }

    /**
     * Returns the [Point]'s that were changed by the [Session.update] that returned this Frame.
     *
     * @see Frame.getUpdatedTrackables
     */
    val updatedPointsFlow: Flow<List<Point>> by lazy {
        arFrameFlow { frame ->
            frame.getUpdatedTrackables(Point::class.java).toList()
        }
    }

    /**
     * Returns the [AugmentedImage]'s that were changed by the [Session.update] that returned this
     * Frame.
     *
     * @see Frame.getUpdatedTrackables
     */
    val updatedAugmentedImagesFlow: Flow<List<AugmentedImage>> by lazy {
        arFrameFlow { frame ->
            frame.getUpdatedTrackables(AugmentedImage::class.java).toList()
        }
    }

    /**
     * Returns the [AugmentedFace]'s that were changed by the [Session.update] that returned this
     * Frame.
     *
     * @see Frame.getUpdatedTrackables
     */
    val updatedAugmentedFacesFlow: Flow<List<AugmentedFace>> by lazy {
        arFrameFlow { frame ->
            frame.getUpdatedTrackables(AugmentedFace::class.java).toList()
        }
    }

    /**
     * Attempts to acquire a depth [Android Image object][Image] that corresponds to the current
     * frame.
     *
     * @return an [Android Image object][Image] that contains the image data from the camera.
     * The returned image object format is [IMAGE_FORMAT_YUV_420_888](https://developer.android.com/ndk/reference/group/media#group___media_1gga9c3dace30485a0f28163a882a5d65a19aea9797f9b5db5d26a2055a43d8491890)}</a>.
     * @throws java.lang.NullPointerException if `session` or `frame` is null.
     * @throws com.google.ar.core.exceptions.DeadlineExceededException if the input frame is not the
     * current frame.
     * @throws com.google.ar.core.exceptions.ResourceExhaustedException if the caller app has
     * exceeded maximum number of images that it can hold without releasing.
     * @throws com.google.ar.core.exceptions.NotYetAvailableException if the image with the
     * timestamp of the input frame did not become available within a bounded amount of time, or if
     * the camera failed to produce the image.
     *
     * @see Frame.acquireCameraImage
     */
    val cameraImageFlow: Flow<Image> by lazy {
        arFrameFlow { frame ->
            frame.acquireCameraImage()
        }
    }

    /**
     * Attempts to acquire a depth [Android Image object][Image] that corresponds to the current
     * frame.
     *
     * @return The depth image corresponding to the frame.
     * @throws com.google.ar.core.exceptions.NotYetAvailableException if the number of observed
     * frames is not yet sufficient for depth estimation; or depth estimation was not possible due
     * to poor lighting, camera occlusion, or insufficient motion observed.
     * @throws com.google.ar.core.exceptions.NotTrackingException if the [Session] is not in the
     * [TrackingState.TRACKING] state, which is required to acquire depth images.
     * @throws java.lang.IllegalStateException if a supported depth mode was not enabled in Session
     * configuration.
     * @throws com.google.ar.core.exceptions.ResourceExhaustedException if the caller app has
     * exceeded maximum number of depth images that it can hold without releasing.
     * @throws com.google.ar.core.exceptions.DeadlineExceededException if the method is called on
     * not the current frame.
     *
     * @see Frame.acquireDepthImage16Bits
     */
    val depthImage16BitsFlow: Flow<Image> by lazy {
        arFrameFlow { frame ->
            frame.acquireDepthImage16Bits()
        }
    }

    /**
     * Attempts to acquire a "raw", mostly unfiltered, depth [Android Image](Image) object that
     * corresponds to the current frame.
     *
     * @return The raw depth image corresponding to the frame.
     * @throws com.google.ar.core.exceptions.NotYetAvailableException if the number of observed
     * frames is not yet sufficient for depth estimation; or depth estimation was not possible due
     * to poor lighting, camera occlusion, or insufficient motion observed.
     * @throws com.google.ar.core.exceptions.NotTrackingException if the [Session] is not in the
     * [TrackingState.TRACKING] state, which is required to acquire depth images.
     * @throws java.lang.IllegalStateException if a supported [Config.DepthMode] was not enabled in
     * Session configuration.
     * @throws com.google.ar.core.exceptions.ResourceExhaustedException if the caller app has
     * exceeded maximum number of depth images that it can hold without releasing.
     * @throws com.google.ar.core.exceptions.DeadlineExceededException if the method is called on
     * not the current frame.
     *
     * @see Frame.acquireRawDepthImage16Bits
     */
    val rawDepthImage16BitsFlow: Flow<Image> by lazy {
        arFrameFlow { frame ->
            frame.acquireRawDepthImage16Bits()
        }
    }

    /**
     * Attempts to acquire the confidence [Android Image object][Image] corresponding to the raw
     * depth image of the current frame.
     *
     * @return The confidence image corresponding to the raw depth of the frame.
     * @throws com.google.ar.core.exceptions.NotYetAvailableException if the number of observed
     * frames is not yet sufficient for depth estimation; or depth estimation was not possible due
     * to poor lighting, camera occlusion, or insufficient motion observed.
     * @throws com.google.ar.core.exceptions.NotTrackingException if the [Session] is not in the
     * [TrackingState.TRACKING] state, which is required to acquire depth images.
     * @throws java.lang.IllegalStateException if a supported [Config.DepthMode] was not enabled in
     * Session configuration.
     * @throws com.google.ar.core.exceptions.ResourceExhaustedException if the caller app has
     * exceeded maximum number of depth images that it can hold without releasing.
     * @throws com.google.ar.core.exceptions.DeadlineExceededException if the method is called on
     * not the current frame.
     *
     * @see Frame.acquireRawDepthConfidenceImage
     */
    val rawDepthConfidenceImageFlow: Flow<Image> by lazy {
        arFrameFlow { frame ->
            frame.acquireRawDepthConfidenceImage()
        }
    }

    /**
     * Performs a ray cast to retrieve the hit trackables
     *
     * Performs a ray cast from the user's device in the direction of the given location in the
     * camera view. Intersections with detected scene geometry are returned, sorted by distance from
     * the device; the nearest intersection is returned first.
     *
     * This function will succeed only if the ARCore session tracking state is
     * [TrackingState.TRACKING], and there are sufficient feature points to track the point in
     * screen space.
     *
     * When using:
     * - Plane and/or Depth: Significant geometric leeway is given when returning hit results.
     * For example, a plane hit may be generated if the ray came close, but did not actually hit
     * within the plane extents or plane bounds [Plane.isPoseInExtents] and [Plane.isPoseInPolygon].
     * A point (point cloud) hit is generated when a point is roughly within one finger-width of the
     * provided screen coordinates.
     *
     * - Instant Placement: Ray cast can return a result before ARCore establishes full tracking.
     * The pose and apparent scale of attached objects depends on the [InstantPlacementPoint]
     * tracking method and the provided approximateDistance.
     * A discussion of the different tracking methods and the effects of apparent object scale are
     * described in [InstantPlacementPoint].
     *
     * - [Session.Feature.FRONT_CAMERA]:  The returned hit result list will always be empty, as the
     * camera is not [TrackingState.TRACKING]. Hit testing against tracked faces is not currently
     * supported.
     *
     * Note: In ARCore 1.24.0 or later on supported devices, if depth is enabled by calling
     * [Config.setDepthMode] with the value [Config.DepthMode.AUTOMATIC], the returned list includes
     * [DepthPoint] values sampled from the latest computed depth image.
     *
     * @param xPx x view coordinate in pixels
     * @param yPx y view coordinate in pixels
     * @param approximateDistance the distance at which to create an [InstantPlacementPoint].
     * This is only used while the tracking method for the returned point is
     * [InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE]
     * @param plane enable plane results
     * @param depth enable depth results
     * @param instantPlacement enable instant placement results
     *
     * @return an ordered list of intersections with scene geometry, nearest hit first.
     * In case of instant placement result, if successful, a list containing a single [HitResult],
     * otherwise an empty list. The [HitResult] will have a trackable of type
     * [InstantPlacementPoint]
     */
    fun hitTests(
        frame: ARFrame? = currentFrame,
        xPx: Float = width / 2.0f,
        yPx: Float = height / 2.0f,
        plane: Boolean = planeFindingMode != PlaneFindingMode.DISABLED,
        depth: Boolean = depthMode != DepthMode.DISABLED,
        instantPlacement: Boolean = instantPlacementMode != InstantPlacementMode.DISABLED,
        approximateDistance: Float = instantPlacementDistance
    ): List<HitResult> {
        if (frame?.camera?.isTracking == true) {
            if (plane || depth) {
                frame.hitTest(xPx, yPx).takeIf { it.isNotEmpty() }?.let {
                    return it
                }
            }
            if (instantPlacement) {
                return frame.hitTestInstantPlacement(xPx, yPx, approximateDistance)
            }
        }
        return listOf()
    }

    /**
     * Performs a ray cast to retrieve the hit trackable and return the more accurate result.
     *
     * Performs a ray cast from the user's device in the direction of the given location in the
     * camera view. Intersections with detected scene geometry are returned, sorted by distance from
     * the device; the nearest intersection is returned first.
     *
     * Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, Depth Point,
     * or Instant Placement Point.
     *
     * @see hitTests
     */
    fun hitTest(
        frame: ARFrame? = currentFrame,
        xPx: Float = width / 2.0f,
        yPx: Float = height / 2.0f,
        plane: Boolean = planeFindingMode != PlaneFindingMode.DISABLED,
        depth: Boolean = depthMode != DepthMode.DISABLED,
        instantPlacement: Boolean = instantPlacementMode != InstantPlacementMode.DISABLED,
        approximateDistance: Float = instantPlacementDistance
    ): HitResult? = frame?.let {
        hitTests(frame, xPx, yPx, plane, depth, instantPlacement, approximateDistance)
            .firstOrNull { hitResult ->
                when (val trackable = hitResult.trackable!!) {
                    is Plane -> plane && trackable.isPoseInPolygon(hitResult.hitPose) &&
                            hitResult.hitPose.calculateDistanceToPlane(frame.camera.pose) > 0.0f
                    is Point -> depth && trackable.orientationMode ==
                            Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
                    is InstantPlacementPoint -> instantPlacement
                    is DepthPoint -> depth
                    else -> false
                }
            }
    }

    var isSessionResumed = false
        private set
    var hasAugmentedImageDatabase = false
        private set

    inner class ArSession() : Session(context, sessionFeatures) {

        override fun resume() {
            super.resume()
            isSessionResumed = true
        }

        override fun pause() {
            super.pause()
            isSessionResumed = false
        }

        override fun close() {
            super.close()
            isSessionResumed = false
        }

        override fun setCameraConfig(cameraConfig: CameraConfig) {
            super.setCameraConfig(cameraConfig.apply { checkFrontFacing(cameraConfig = this) })
        }

        override fun configure(config: Config) {
            super.configure(config.apply {
                if (depthMode != DepthMode.DISABLED && !isDepthModeSupported(depthMode)) {
                    depthMode = DepthMode.DISABLED
                }
                hasAugmentedImageDatabase = (augmentedImageDatabase?.numImages ?: 0) > 0
                checkFrontFacing(config = this)
            })
        }

        private fun checkFrontFacing(
            config: Config = this.config,
            cameraConfig: CameraConfig = this.cameraConfig
        ) {
            val isFrontFacing = cameraConfig.facingDirection == FacingDirection.FRONT
            // Light estimation is not usable with front camera
            if (isFrontFacing && config.lightEstimationMode != LightEstimationMode.DISABLED) {
                config.lightEstimationMode = LightEstimationMode.DISABLED
            }
            // Checks Filament isFrontFaceWindingInverted
            if (isFrontFaceWindingInverted != isFrontFacing) {
                isFrontFaceWindingInverted = isFrontFacing
            }
        }

        fun update(frameTime: FrameTime): ArFrame? {
            // Check if no frame or same timestamp, no drawing.
            return super.update()?.takeIf {
                it.timestamp != (currentFrame?.frame?.timestamp ?: 0) && it.camera != null
            }?.let { frame ->
                ArFrame(this, frameTime, frame).also {
                    currentFrame = it
                }
            }
        }
    }
}

///**
// * ### A SurfaceView that integrates with ARCore and renders a scene.
// */
//interface ArSceneLifecycleOwner : SceneLifecycleOwner {
//    val arCore: ARCore
//    val arSession get() = arCore.session
//    val arSessionConfig get() = arSession?.config
//}

class ArSceneLifecycle(sceneView: ArSceneView) : SceneLifecycle(sceneView) {
    override val sceneView get() = super.sceneView as ArSceneView
    val context get() = sceneView.context
    val arCore get() = sceneView.arCore
    val arSession get() = sceneView.arSession

    /**
     * ### Performs the given action when ARCore session is created
     *
     * If the ARCore session is already created the action will be performed immediately, otherwise
     * the action will be performed after the ARCore session is next created.
     * The action will only be invoked once, and any listeners will then be removed.
     */
    fun doOnArSessionCreated(action: (session: ArSessionOld) -> Unit) {
        arSession?.let(action) ?: addObserver(onArSessionCreated = {
            removeObserver(this)
            action(it)
        })
    }

    fun addObserver(
        onArSessionCreated: (ArSceneLifecycleObserver.(session: ArSessionOld) -> Unit)? = null,
        onArSessionFailed: (ArSceneLifecycleObserver.(exception: Exception) -> Unit)? = null,
        onArSessionResumed: (ArSceneLifecycleObserver.(session: ArSessionOld) -> Unit)? = null,
        onArSessionConfigChanged: (ArSceneLifecycleObserver.(session: ArSessionOld, config: Config) -> Unit)? = null,
        onArFrame: (ArSceneLifecycleObserver.(arFrame: ArFrame) -> Unit)? = null
    ) {
        addObserver(object : ArSceneLifecycleObserver {
            override fun onArSessionCreated(session: ArSessionOld) {
                onArSessionCreated?.invoke(this, session)
            }

            override fun onArSessionFailed(exception: Exception) {
                onArSessionFailed?.invoke(this, exception)
            }

            override fun onArSessionResumed(session: ArSessionOld) {
                onArSessionResumed?.invoke(this, session)
            }

            override fun onArSessionConfigChanged(session: ArSessionOld, config: Config) {
                onArSessionConfigChanged?.invoke(this, session, config)
            }

            override fun onArFrame(arFrame: ArFrame) {
                onArFrame?.invoke(this, arFrame)
            }
        })
    }
}

interface ArSceneLifecycleObserver : SceneLifecycleObserver {

    fun onArSessionCreated(session: ArSessionOld) {
    }

    fun onArSessionFailed(exception: Exception) {
    }

    fun onArSessionResumed(session: ArSessionOld) {
    }

    fun onArSessionConfigChanged(session: ArSessionOld, config: Config) {
    }

    fun onArFrame(arFrame: ArFrame) {
    }
}

//var Session.cameraFacingDirection
//    get() = cameraConfig.facingDirection
//    set(value) {
//        cameraConfig = getSupportedCameraConfigs(CameraConfigFilter(this).apply {
//            facingDirection = value
//        }
//        )[0]
//    }

/**
 * Sets the camera config to use. The config must be one returned by
 * [Session.getSupportedCameraConfigs]
 *
 * The new camera config will be applied once the session is resumed.
 *
 * The session must be paused. All previously acquired frame images must be released via
 * [Image.close] before resuming. Failure to do so will cause [Session.resume] to throw
 * [IllegalStateException].
 *
 * Changing the camera config for an existing session may affect which ARCore features can
 * be used. Unsupported session features are silently disabled when the session is resumed.
 * Call [Session.configure] after setting a camera config to verify that all configured
 * session features are supported with the new camera config.
 *
 * @param cameraConfig Reference to one of the [CameraConfig] that was obtained by calling
 * [Session.getSupportedCameraConfigs] method.
 */
fun Session.cameraConfig(cameraConfig: CameraConfig.() -> Unit) {
    setCameraConfig(this.cameraConfig.apply(cameraConfig))
}

/**
 * Configures the session and verifies that the enabled features in the specified session
 * config are supported with the currently set camera config.
 *
 * Should be called after [Session.setCameraConfig] to verify that all requested session
 * config features are supported. Features not supported with the current camera config will
 * otherwise be silently disabled when the session is resumed by calling [Session.resume].
 *
 * @param config The new configuration setting for the session.
 *
 * @see Session.configure
 */
fun Session.configure(config: Config.() -> Unit) {
    configure(this.config.apply(config))
}

fun Camera.getProjectionMatrix(near: Float, far: Float): Transform =
    FloatArray(16).apply { getProjectionMatrix(this, 0, near, far) }.toTransform()