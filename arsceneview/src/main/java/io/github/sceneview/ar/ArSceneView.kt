package io.github.sceneview.ar

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.ar.core.*
import com.google.ar.core.CameraConfig.FacingDirection
import io.github.sceneview.*
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.camera.ArCameraStream
import io.github.sceneview.ar.node.ArCameraNode
import io.github.sceneview.ar.node.ArNode
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
    engine: Engine = Filament.retain(),
    val sessionFeatures: Set<Session.Feature> = setOf(),
    override val cameraNode: ArCameraNode = ArCameraNode(engine),
) : SceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
    engine,
    cameraNode
) {

    open val arCore = ARCore(
        onSessionCreated = ::onArSessionCreated,
        onSessionResumed = ::onArSessionResumed,
        onArSessionFailed = ::onArSessionFailed,
        onSessionConfigChanged = ::onArSessionConfigChanged,
    )

    val arSession get() = arCore.session

//    override var frameRate: FrameRate = FrameRate.Half

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
     * Enable or disable the [Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL]
     */
    var planeFindingEnabled: Boolean
        get() = planeFindingMode != Config.PlaneFindingMode.DISABLED
        set(value) {
            planeFindingMode = if (value) {
                Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            } else {
                Config.PlaneFindingMode.DISABLED
            }
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

    /**
     * ### The behavior of the lighting estimation subsystem
     *
     * These modes consist of separate APIs that allow for granular and realistic lighting
     * estimation for directional lighting, shadows, specular highlights, and reflections.
     */
    var lightEstimationMode = Config.LightEstimationMode.DISABLED
        get() = arSession?.lightEstimationMode ?: field
        set(value) {
            field = value
            arSession?.lightEstimationMode = value
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
    var lightEstimator: LightEstimator? = LightEstimator(engine)

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

    private var _onArSessionCreated = mutableListOf<(session: ArSession) -> Unit>()
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

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        arCore.create(context, activity, sessionFeatures)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        arCore.resume(context, activity)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        arCore.pause()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        arSession?.setDisplayGeometry(display.rotation, width, height)
    }

    fun onArSessionCreated(session: ArSession) {
        session.setCameraTextureNames(arCameraStream.cameraTextureIds)

        session.configure { config ->
            // getting ar frame doesn't block and gives last frame
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            // FocusMode must be changed after the session resume to work
            // config.focusMode = focusMode
            config.planeFindingMode = _planeFindingMode
            config.depthMode = _depthMode
            config.instantPlacementEnabled = _instantPlacementEnabled
            config.cloudAnchorEnabled = _cloudAnchorEnabled
            config.lightEstimationMode = lightEstimationMode
            config.geospatialEnabled = _geospatialEnabled
        }

        addEntity(arCameraStream.renderable)

        _onArSessionCreated.toList().forEach { it(session) }
        onArSessionCreated?.invoke(session)
    }

    fun onArSessionFailed(exception: Exception) {
        onArSessionFailed?.invoke(exception)
    }

    fun onArSessionResumed(session: ArSession) {
        session.configure { config ->
            // FocusMode must be changed after the session resume to work
            config.focusMode = _focusMode
        }

        onArSessionResumed?.invoke(session)
    }

    fun onArSessionConfigChanged(session: ArSession, config: Config) {
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
//            if (arFrame.frame.timestamp != 0L
            // && arFrame.time != currentFrame?.time
//            ) {
            currentFrame = arFrame
            doArFrame(arFrame)
//            }
        }
        super.doFrame(frameTime)
    }

    /**
     * ### Invoked once per [Frame] immediately before the Scene is updated.
     *
     * The listener will be called in the order in which they were added.
     */
    protected open fun doArFrame(arFrame: ArFrame) {
        val camera = arFrame.camera
        val isCameraTracking = camera.isTracking

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        // You will say thanks when still have battery after a long day debugging an AR app.
        // ...and it's better for your users
        activity?.setKeepScreenOn(isCameraTracking)

        arCameraStream.update(this, arFrame)
        cameraNode.updateTrackedPose(camera)

        children.filterIsInstance<ArNode>().forEach { it.onArFrame(arFrame, isCameraTracking) }

        lightEstimator?.update(this, arFrame)?.let { (environment, mainLight) ->
            environmentEstimated = environment
            mainLightEstimated = mainLight
        }

        planeRenderer.update(arFrame)

        trackingFailureReason = if (isCameraTracking) {
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

        onArFrame?.invoke(arFrame)
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
        _onArSessionCreated += object : (ArSession) -> Unit {
            override fun invoke(session: ArSession) {
                _onArSessionCreated -= this
                session.configure { config ->
                    applyConfig.invoke(session, config)
                }
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
        plane: Boolean = planeFindingEnabled,
        depth: Boolean = depthEnabled,
        instant: Boolean = instantPlacementEnabled,
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
        xPx: Float = width / 2.0f,
        yPx: Float = height / 2.0f,
        plane: Boolean = planeFindingEnabled,
        depth: Boolean = depthEnabled,
        instant: Boolean = instantPlacementEnabled,
        approximateDistance: Float = 2.0f
    ) = currentFrame?.hitTest(xPx, yPx, plane, depth, instant, approximateDistance)

    override fun destroy() {
        if (!isDestroyed) {
            arCore.destroy()

            arCameraStream.destroy()

            lightEstimator?.destroy()
            planeRenderer.destroy()
        }

        super.destroy()
    }
}