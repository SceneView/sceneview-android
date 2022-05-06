package io.github.sceneview.ar

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.*
import com.google.ar.core.*
import com.google.ar.core.CameraConfig.FacingDirection
import com.google.ar.sceneform.ArCamera
import com.google.ar.sceneform.rendering.*
import io.github.sceneview.*
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.camera.ArCameraStream
import io.github.sceneview.ar.interaction.ArNodeManipulator
import io.github.sceneview.ar.interaction.ArSceneGestureDetector
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.interaction.SceneGestureDetector
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.setKeepScreenOn

/**
 * A SurfaceView that integrates with ARCore and renders a scene.
 */
open class ArSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : SceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes
), ArSceneLifecycleOwner, ArSceneLifecycleObserver {

    //TODO : Move it to Lifecycle and NodeParent when Kotlined
    override val camera = ArCamera(this)

    /**
     * ### Fundamental session features
     *
     * Must be set before session creation = before the [onResume] call
     */
    var arSessionFeatures = { setOf<Session.Feature>() }

    override val arCore = ARCore(
        lifecycle = lifecycle,
        features = arSessionFeatures
    )

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
        get() = lightEstimation.mode
        set(value) {
            lightEstimation.mode = value
        }

    var currentFrame: ArFrame? = null

    override val renderer: Renderer = ArRenderer(this, camera)

    /**
     * ### The [ArCameraStream] used to control if the occlusion should be enabled or disabled
     */
    val arCameraStream = ArCameraStream(lifecycle)

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
    val lightEstimation = LightEstimation(lifecycle)

    val instructions = Instructions(lifecycle)

    var onArSessionCreated: ((session: ArSession) -> Unit)? = null

    override var gestureListener: SceneGestureDetector.OnSceneGestureListener =
        DefaultArSceneGestureListener(this)

    override val gestureDetector: ArSceneGestureDetector by lazy {
        ArSceneGestureDetector(
            this,
            nodeManipulator = ArNodeManipulator(this),
            listener = gestureListener
        )
    }

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
     * ### Invoked when an ARCore plane is tapped
     *
     * Registers a callback to be invoked when an ARCore [Trackable] is tapped.
     * Depending on the session config you defined, the [HitResult.getTrackable] can be:
     * - a [Plane] if [ArSession.planeFindingEnabled]
     * - an [InstantPlacementPoint] if [ArSession.instantPlacementEnabled]
     * - a [DepthPoint] if [ArSession.depthEnabled]
     *
     * The callback will only be invoked if no [com.google.ar.sceneform.Node] was tapped.
     *
     * - hitResult: The ARCore hit result that occurred when tapping the plane
     * - motionEvent: the motion event that triggered the tap
     */
    var onTouchAr: ((hitResult: HitResult, motionEvent: MotionEvent) -> Unit)? =
        null

    /**
     * ### Invoked when an ARCore AugmentedImage TrackingState/TrackingMethod is updated
     *
     * Registers a callback to be invoked when an ARCore AugmentedImage TrackingState/TrackingMethod
     * is updated. The callback will be invoked on each AugmentedImage update.
     *
     * @see AugmentedImage.getTrackingState
     * @see AugmentedImage.getTrackingMethod
     */
    var onAugmentedImageUpdate: ((augmentedImage: AugmentedImage) -> Unit)? = null

    /**
     * ### Invoked when an ARCore AugmentedFace TrackingState is updated
     *
     * Registers a callback to be invoked when an ARCore AugmentedFace TrackingState is updated. The
     * callback will be invoked on each AugmentedFace update.
     *
     * @see AugmentedFace.getTrackingState
     */
    var onAugmentedFaceUpdate: ((augmentedFace: AugmentedFace) -> Unit)? = null

    override fun getLifecycle() =
        (sceneLifecycle ?: ArSceneLifecycle(this).also {
            sceneLifecycle = it
        }) as ArSceneLifecycle

    override fun onArSessionCreated(session: ArSession) {
        super.onArSessionCreated(session)

        session.configure { config ->
            // FocusMode must be changed after the session resume to work
//            config.focusMode = focusMode
            config.planeFindingMode = _planeFindingMode
            config.depthMode = _depthMode
            config.instantPlacementEnabled = _instantPlacementEnabled
            config.cloudAnchorEnabled = _cloudAnchorEnabled
            config.lightEstimationMode = _sessionLightEstimationMode
        }

        // Feature config, therefore facing direction, can only be configured once per session.
        if (session.cameraConfig.facingDirection == FacingDirection.FRONT) {
            renderer.isFrontFaceWindingInverted = true
        }

        arCameraStream?.let { addEntity(it.renderable) }

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
        // TODO : Move to dedicated Lifecycle aware classes when Kotlined them
        val (session, _, frame) = arFrame
        if (arFrame.camera.isTracking != currentFrame?.camera?.isTracking) {
            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            // You will say thanks when still have battery after a long day debugging an AR app.
            // ...and it's better for your users
            activity.setKeepScreenOn(arFrame.camera.isTracking)
        }

        // At the start of the frame, update the tracked pose of the camera
        // to use in any calculations during the frame.
        this.camera.updateTrackedPose(arFrame.camera)

        if (onAugmentedImageUpdate != null) {
            arFrame.updatedAugmentedImages.forEach(onAugmentedImageUpdate)
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

    override fun onTouch(selectedNode: Node?, motionEvent: MotionEvent): Boolean {
        // TODO : Should be handled by the nodesTouchEventDispatcher
        //nodeGestureRecognizer.onNodeTap(null)

        arSession?.let { session ->
            session.currentFrame?.hitTest(motionEvent)?.let { hitResult ->
                onTouchAr(hitResult, motionEvent)
                return true
            }
        }
        return false
    }

    /**
     * ### Invoked when an ARCore [Trackable] is tapped
     *
     * The callback will only be invoked if **no** [com.google.ar.sceneform.Node] was tapped.
     *
     * - hitResult: The ARCore hit result that occurred when tapping the plane
     * - plane: The ARCore Plane that was tapped
     * - motionEvent: the motion event that triggered the tap
     */
    protected open fun onTouchAr(hitResult: HitResult, motionEvent: MotionEvent) {
        onTouchAr?.invoke(hitResult, motionEvent)
    }

    open class DefaultArSceneGestureListener(sceneView: SceneView) :
        SceneView.DefaultSceneGestureListener(sceneView) {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return sceneView.onTouch(null, e)
        }
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