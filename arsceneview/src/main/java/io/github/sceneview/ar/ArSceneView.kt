package io.github.sceneview.ar

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.filament.IndirectLight
import com.google.ar.core.*
import com.google.ar.core.CameraConfig.FacingDirection
import com.google.ar.sceneform.ArCameraNode
import io.github.sceneview.SceneLifecycle
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneLifecycleOwner
import io.github.sceneview.SceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.camera.ArCameraStream
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.environment.Environment
import io.github.sceneview.light.Light
import io.github.sceneview.math.Position
import io.github.sceneview.node.Node
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.setKeepScreenOn

/**
 * ### A SurfaceView that integrates with ARCore and renders a scene
 *
 * @param sessionFeatures Fundamental session features
 */
open class ArSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    sessionFeatures: Set<Session.Feature> = setOf(),
    override val cameraNode: ArCameraNode = ArCameraNode(),
) : SceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
    cameraNode
), ArSceneLifecycleOwner, ArSceneLifecycleObserver {

    override val arCore = ARCore(activity, lifecycle, sessionFeatures)

    override var frameRate: FrameRate = FrameRate.Half

    private var _focusMode = Config.FocusMode.AUTO

    /**
     * ### Sets the desired focus mode
     *
     * See [Config.FocusMode] for available options.
     */
    var focusMode: Config.FocusMode
        get() = arSession?.focusMode ?: _focusMode
        set(value) {
            _focusMode = value
            arSession?.focusMode = value
        }

    private var _planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL

    /**
     * ### Sets the desired plane finding mode
     *
     * See the [Config.PlaneFindingMode] enum
     * for available options.
     */
    var planeFindingMode: Config.PlaneFindingMode
        get() = arSession?.planeFindingMode ?: _planeFindingMode
        set(value) {
            _planeFindingMode = value
            arSession?.planeFindingMode = value
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
    var isDepthOcclusionEnabled
        get() = arCameraStream.isDepthOcclusionEnabled
        set(value) {
            arCameraStream.isDepthOcclusionEnabled = value
        }

    /**
     * ### Enable or disable the [Config.DepthMode.AUTOMATIC]
     *
     * Not all devices support all modes. Use [Session.isDepthModeSupported] to determine whether
     * the current device and the selected camera support a particular depth mode.
     */
    var depthEnabled: Boolean
        get() = depthMode != Config.DepthMode.DISABLED
        set(value) {
            depthMode = if (value) {
                Config.DepthMode.AUTOMATIC
            } else {
                Config.DepthMode.DISABLED
            }
        }

    private var _depthMode = Config.DepthMode.DISABLED

    /**
     * ### Sets the desired [Config.DepthMode]
     *
     * Not all devices support all modes. Use [Session.isDepthModeSupported] to determine whether
     * the current device and the selected camera support a particular depth mode.
     */
    var depthMode: Config.DepthMode
        get() = arSession?.depthMode ?: _depthMode
        set(value) {
            _depthMode = value
            arSession?.depthMode = value
        }

    private var _instantPlacementEnabled = true

    /**
     * ### Enable or disable the [Config.InstantPlacementMode.LOCAL_Y_UP]
     */
    var instantPlacementEnabled: Boolean
        get() = arSession?.instantPlacementEnabled ?: _instantPlacementEnabled
        set(value) {
            _instantPlacementEnabled = value
            arSession?.instantPlacementEnabled = value
        }

    private var _cloudAnchorEnabled = false

    /**
     * ### Cloud anchor mode
     */
    var cloudAnchorEnabled: Boolean
        get() = arSession?.cloudAnchorEnabled ?: _cloudAnchorEnabled
        set(value) {
            _cloudAnchorEnabled = value
            arSession?.cloudAnchorEnabled = value
        }

    private var _geospatialEnabled = false

    /**
     * ### Geospatial mode
     */
    var geospatialEnabled: Boolean
        get() = arSession?.geospatialEnabled ?: _geospatialEnabled
        set(value) {
            _geospatialEnabled = value
            arSession?.geospatialEnabled = value
        }

    private var _sessionLightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR

    /**
     * ### The behavior of the lighting estimation subsystem
     *
     * These modes consist of separate APIs that allow for granular and realistic lighting
     * estimation for directional lighting, shadows, specular highlights, and reflections.
     */
    var sessionLightEstimationMode: Config.LightEstimationMode
        get() = arSession?.lightEstimationMode ?: _sessionLightEstimationMode
        set(value) {
            _sessionLightEstimationMode = value
            arSession?.lightEstimationMode = value
        }

    /**
     * ### ARCore light estimation configuration
     *
     * ARCore estimate lighting to provide directional light, ambient spherical harmonics,
     * and reflection cubemap estimation
     *
     * Light bounces off of surfaces differently depending on whether the surface has specular
     * (highly reflective) or diffuse (not reflective) properties.
     * For example, a metallic ball will be highly specular and reflect its environment, while
     * another ball painted a dull matte gray will be diffuse. Most real-world objects have a
     * combination of these properties — think of a scuffed-up bowling ball or a well-used credit
     * card.
     *
     * Reflective surfaces also pick up colors from the ambient environment. The coloring of an
     * object can be directly affected by the coloring of its environment. For example, a white ball
     * in a blue room will take on a bluish hue.
     *
     * The main directional light API calculates the direction and intensity of the scene's
     * main light source. This information allows virtual objects in your scene to show reasonably
     * positioned specular highlights, and to cast shadows in a direction consistent with other
     * visible real objects.
     *
     * @see LightEstimationMode.ENVIRONMENTAL_HDR
     * @see LightEstimationMode.ENVIRONMENTAL_HDR_NO_REFLECTIONS
     * @see LightEstimationMode.ENVIRONMENTAL_HDR_FAKE_REFLECTIONS
     * @see LightEstimationMode.AMBIENT_INTENSITY
     * @see LightEstimationMode.DISABLED
     */
    var lightEstimationMode: LightEstimationMode
        get() = lightEstimator.mode
        set(value) {
            lightEstimator.mode = value
        }

    /**
     * ### Camera facing direction filter
     *
     * Currently, a back-facing (world) camera is guaranteed to be available on all ARCore supported
     * devices. Most ARCore supported devices also include support for a front-facing (selfie)
     * camera.
     * See [ARCore supported devices](https://developers.google.com/ar/devices) for available camera
     * configs by device.
     *
     * The default value is [CameraConfig.FacingDirection.BACK]
     */
    var cameraFacingDirection: FacingDirection = FacingDirection.BACK
        set(value) {
            field = value
            configureSession { arSession, config ->
                arSession.cameraConfig = arSession.getSupportedCameraConfigs(
                    CameraConfigFilter(arSession).apply {
                        facingDirection = value
                    }
                )[0]
            }
        }

    var currentFrame: ArFrame? = null

    /**
     * ### The [ArCameraStream] used to control if the occlusion should be enabled or disabled
     */
    val arCameraStream = ArCameraStream(this)

    /**
     * ### [PlaneRenderer] used to control plane visualization.
     */
    val planeRenderer = PlaneRenderer(this)

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

    open var trackingFailureReason: TrackingFailureReason? = null
        set(value) {
            if (field != value) {
                field = value
                onArTrackingFailureChanged?.invoke(value)
            }
        }

    var onArSessionCreated: ((session: ArSession) -> Unit)? = null

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
     * ### Invoked when an ARCore frame is processed
     *
     * Registers a callback to be invoked when a valid ARCore Frame is processing.
     *
     * The callback to be invoked once per frame **immediately before the scene
     * is updated**.
     *
     * The callback will only be invoked if the Frame is considered as valid.
     */
    var onArFrame: ((arFrame: ArFrame) -> Unit)? = null

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

    /**
     * ### Invoked when an ARCore AugmentedImage TrackingState/TrackingMethod is updated
     *
     * Registers a callback to be invoked when an ARCore AugmentedImage TrackingState/TrackingMethod
     * is updated. The callback will be invoked on each AugmentedImage update.
     *
     * @see AugmentedImage.getTrackingState
     * @see AugmentedImage.getTrackingMethod
     */
    var onAugmentedImageUpdate = mutableListOf<(augmentedImage: AugmentedImage) -> Unit>()

    /**
     * ### Invoked when an ARCore AugmentedFace TrackingState is updated
     *
     * Registers a callback to be invoked when an ARCore AugmentedFace TrackingState is updated. The
     * callback will be invoked on each AugmentedFace update.
     *
     * @see AugmentedFace.getTrackingState
     */
    var onAugmentedFaceUpdate: ((augmentedFace: AugmentedFace) -> Unit)? = null

    var onArTrackingFailureChanged: ((trackingFailureReason: TrackingFailureReason?) -> Unit)? =
        null

    override val cameraGestureDetector = null
    override val cameraManipulator = null

    override fun getLifecycle() =
        (sceneLifecycle ?: ArSceneLifecycle(this).also {
            sceneLifecycle = it
        }) as ArSceneLifecycle

    override fun onArSessionCreated(session: ArSession) {
        super.onArSessionCreated(session)

        session.setCameraTextureNames(arCameraStream.cameraTextureIds)

        session.configure { config ->
            // getting ar frame doesn't block and gives last frame
            //config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            // FocusMode must be changed after the session resume to work
            // config.focusMode = focusMode
            config.planeFindingMode = _planeFindingMode
            config.depthMode = _depthMode
            config.instantPlacementEnabled = _instantPlacementEnabled
            config.cloudAnchorEnabled = _cloudAnchorEnabled
            config.lightEstimationMode = _sessionLightEstimationMode
            config.geospatialEnabled = _geospatialEnabled
        }

        addEntity(arCameraStream.renderable)

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

    /**
     * Before the render call occurs, update the ARCore session to grab the latest frame and update
     * listeners.
     *
     * The super.onFrame() is called if the session updated successfully and a new frame was
     * obtained. Update the scene before rendering.
     */
    override fun doFrame(frameTime: FrameTime) {
        arSession?.update(frameTime)?.let { arFrame ->
            // During startup the camera system may not produce actual images immediately.
            // In this common case, a frame with timestamp = 0 will be returned.
            if (arFrame.frame.timestamp != 0L && arFrame.time != currentFrame?.time) {
                currentFrame = arFrame
                doArFrame(arFrame)
                super.doFrame(frameTime)
            }
        }
    }

    /**
     * ### Invoked once per [Frame] immediately before the Scene is updated.
     *
     * The listener will be called in the order in which they were added.
     */
    protected open fun doArFrame(arFrame: ArFrame) {
        val camera = arFrame.camera

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        // You will say thanks when still have battery after a long day debugging an AR app.
        // ...and it's better for your users
        activity.setKeepScreenOn(camera.isTracking)

        cameraNode.updateTrackedPose(camera)

        arCameraStream.update(arFrame)

        planeRenderer.update(arFrame)

        trackingFailureReason = if (!camera.isTracking) {
            camera.trackingFailureReason.takeIf { it != TrackingFailureReason.NONE }
        } else null

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

        lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
            onArFrame(arFrame)
        }
        onArFrame?.invoke(arFrame)
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

    fun hitTest(
        position: Position,
        plane: Boolean,
        depth: Boolean,
        instant: Boolean
    ) = currentFrame?.hitTest(position, plane, depth, instant)

    /**
     * ### Performs a ray cast to retrieve the ARCore info at this camera point
     *
     * @param frame the [ArFrame] from where we take the [HitResult]
     * By default the latest session frame if any exist
     * @param xPx x view coordinate in pixels
     * @property yPx y view coordinate in pixels
     *
     * @return the hitResult or null if no info is retrieved
     *
     * @see ArFrame.hitTest
     */
    fun hitTest(
        xPx: Float,
        yPx: Float,
        plane: Boolean,
        depth: Boolean,
        instant: Boolean,
        approximateDistance: Float = 2.0f
    ) = currentFrame?.hitTest(xPx, yPx, plane, depth, instant, approximateDistance)

    override fun destroy() {
        super.destroy()

        arCameraStream.destroy()
        planeRenderer.destroy()
    }
}

/**
 * ### A SurfaceView that integrates with ARCore and renders a scene.
 */
interface ArSceneLifecycleOwner : SceneLifecycleOwner {
    val arCore: ARCore
    val arSession get() = arCore.session
    val arSessionConfig get() = arSession?.config
}

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
    fun doOnArSessionCreated(action: (session: ArSession) -> Unit) {
        arSession?.let(action) ?: addObserver(onArSessionCreated = {
            removeObserver(this)
            action(it)
        })
    }

    fun addObserver(
        onArSessionCreated: (ArSceneLifecycleObserver.(session: ArSession) -> Unit)? = null,
        onArSessionFailed: (ArSceneLifecycleObserver.(exception: Exception) -> Unit)? = null,
        onArSessionResumed: (ArSceneLifecycleObserver.(session: ArSession) -> Unit)? = null,
        onArSessionConfigChanged: (ArSceneLifecycleObserver.(session: ArSession, config: Config) -> Unit)? = null,
        onArFrame: (ArSceneLifecycleObserver.(arFrame: ArFrame) -> Unit)? = null
    ) {
        addObserver(object : ArSceneLifecycleObserver {
            override fun onArSessionCreated(session: ArSession) {
                onArSessionCreated?.invoke(this, session)
            }

            override fun onArSessionFailed(exception: Exception) {
                onArSessionFailed?.invoke(this, exception)
            }

            override fun onArSessionResumed(session: ArSession) {
                onArSessionResumed?.invoke(this, session)
            }

            override fun onArSessionConfigChanged(session: ArSession, config: Config) {
                onArSessionConfigChanged?.invoke(this, session, config)
            }

            override fun onArFrame(arFrame: ArFrame) {
                onArFrame?.invoke(this, arFrame)
            }
        })
    }
}

interface ArSceneLifecycleObserver : SceneLifecycleObserver {

    fun onArSessionCreated(session: ArSession) {
    }

    fun onArSessionFailed(exception: Exception) {
    }

    fun onArSessionResumed(session: ArSession) {
    }

    fun onArSessionConfigChanged(session: ArSession, config: Config) {
    }

    fun onArFrame(arFrame: ArFrame) {
    }
}