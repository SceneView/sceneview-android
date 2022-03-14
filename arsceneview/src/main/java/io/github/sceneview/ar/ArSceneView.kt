package io.github.sceneview.ar

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.*
import com.google.ar.core.*
import com.google.ar.core.CameraConfig.FacingDirection
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.ArCamera
import com.google.ar.sceneform.rendering.*
import io.github.sceneview.*
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.interaction.ArNodeManipulator
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.interaction.GestureHandler
import io.github.sceneview.light.destroy
import io.github.sceneview.node.Node
import io.github.sceneview.scene.exposureFactor
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

    /**
     * ### Sets the desired focus mode
     *
     * See [Config.FocusMode] for available options.
     */
    var focusMode: Config.FocusMode
        get() = arSession?.focusMode ?: ArSession.defaultFocusMode
        set(value) {
            configureSession { _, config ->
                config.focusMode = value
            }
        }

    /**
     * ### Sets the desired plane finding mode
     *
     * See the [Config.PlaneFindingMode] enum
     * for available options.
     */
    var planeFindingMode: Config.PlaneFindingMode
        get() = arSession?.planeFindingMode ?: ArSession.defaultPlaneFindingMode
        set(value) {
            configureSession { _, config ->
                config.planeFindingMode = value
            }
        }

    /**
     * ### Enable or disable the [Config.DepthMode.AUTOMATIC]
     *
     * Not all devices support all modes. Use [Session.isDepthModeSupported] to determine whether the
     * current device and the selected camera support a particular depth mode.
     */
    var depthEnabled: Boolean
        get() = arSession?.depthEnabled ?: ArSession.defaultDepthEnabled
        set(value) {
            configureSession { _, config ->
                config.depthEnabled = value
            }
        }

    /**
     * ### Enable or disable the [Config.InstantPlacementMode.LOCAL_Y_UP]
     *
     * // TODO : Doc
     */
    var instantPlacementEnabled: Boolean
        get() = arSession?.instantPlacementEnabled ?: ArSession.defaultInstantPlacementEnabled
        set(value) {
            configureSession { _, config ->
                config.instantPlacementEnabled = value
            }
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
     * LightEstimationConfig.SPECTACULAR vs LightEstimationConfig.REALISTIC mostly differs on the
     * reflections parts and you will mainly only see differences if your model has more metallic
     * than roughness material values.
     *
     * Adjust the based reference/factored lighting intensities and other values with:
     * - [io.github.sceneview.ar.ArSceneView.mainLight]
     * - [io.github.sceneview.ar.ArSceneView.environment][io.github.sceneview.environment.Environment.indirectLight]
     *
     * @see LightEstimationMode.REALISTIC
     * @see LightEstimationMode.SPECTACULAR
     * @see LightEstimationMode.AMBIENT_INTENSITY
     * @see LightEstimationMode.DISABLED
     */
    var lightEstimationMode: LightEstimationMode
        get() = arSession?.lightEstimationMode ?: ArSession.defaultLightEstimationMode
        set(value) {
            configureSession { session, _ ->
                session.lightEstimationMode = value
            }
        }

    override val sceneLifecycle: ArSceneLifecycle = ArSceneLifecycle(context, this)

    val cameraTextureId = GLHelper.createCameraTexture()

    //TODO : Move it to Lifecycle and NodeParent when Kotlined
    override val camera by lazy { ArCamera(this) }

    /**
     * ### Fundamental session features
     *
     * Must be set before session creation = before the [onResume] call
     */
    var arSessionFeatures = { ArSession.defaultFeatures }
    override val arCore = ARCore(
        cameraTextureId = cameraTextureId,
        lifecycle = lifecycle,
        features = arSessionFeatures
    )

    var currentFrame: ArFrame? = null

    override val renderer: Renderer by lazy {
        ArRenderer(this, camera).apply {
            enablePerformanceMode()
        }
    }

    /**
     * ### The [CameraStream] used to control if the occlusion should be enabled or disabled
     */
    val cameraStream = CameraStream(cameraTextureId, renderer)

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
    var estimatedLights: EnvironmentLightsEstimate? = null
        internal set(value) {
            //TODO: Move to Renderer when kotlined it
            val environment = value?.environment ?: environment
            if (renderer.getEnvironment() != environment) {
                if (field?.environment != environment) {
                    field?.environment?.destroy()
                }
                renderer.setEnvironment(environment)
            }
            val mainLight = value?.mainLight ?: mainLight
            if (renderer.getMainLight() != mainLight) {
                if (field?.mainLight != mainLight) {
                    field?.mainLight?.destroy()
                }
                renderer.setMainLight(mainLight)
            }
            field = value
        }

    val instructions = Instructions(this, lifecycle)

    var onArSessionCreated: ((session: ArSession) -> Unit)? = null

    override val gestureHandler: GestureHandler by lazy { ArNodeManipulator(this) }

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

    override fun onArSessionCreated(session: ArSession) {
        super.onArSessionCreated(session)

        // Feature config, therefore facing direction, can only be configured once per session.
        if (session.cameraConfig.facingDirection == FacingDirection.FRONT) {
            renderer.isFrontFaceWindingInverted = true
        }

        onArSessionCreated?.invoke(session)
    }

    override fun onArSessionFailed(exception: Exception) {
        super.onArSessionFailed(exception)

        onArSessionFailed?.invoke(exception)
    }

    override fun onArSessionResumed(session: ArSession) {
        super.onArSessionResumed(session)

        onArSessionResumed?.invoke(session)
    }

    override fun onArSessionConfigChanged(session: ArSession, config: Config) {
        super.onArSessionConfigChanged(session, config)

        // Set the correct Texture configuration on the camera stream
        //TODO: Move CameraStream to lifecycle aware
        cameraStream.checkIfDepthIsEnabled(session, config)

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

        cameraStream.apply {
            // Setup Camera Stream if needed.
            if (!isTextureInitialized) {
                initializeTexture(arFrame.camera)
            }
            // Recalculate camera Uvs if necessary.
            if (currentFrame == null || frame.hasDisplayGeometryChanged()) {
                recalculateCameraUvs(frame)
            }
            if (depthOcclusionMode == CameraStream.DepthOcclusionMode.DEPTH_OCCLUSION_ENABLED) {
                try {
                    when (depthMode) {
                        CameraStream.DepthMode.DEPTH -> frame.acquireDepthImage()
                        CameraStream.DepthMode.RAW_DEPTH -> frame.acquireRawDepthImage()
                        else -> null
                    }?.use { depthImage ->
                        recalculateOcclusion(depthImage)
                    }
                } catch (ignored: NotYetAvailableException) {
                } catch (ignored: DeadlineExceededException) {
                }
            }
        }

        // At the start of the frame, update the tracked pose of the camera
        // to use in any calculations during the frame.
        this.camera.updateTrackedPose(arFrame.camera)

        // Update the light estimate.
        estimatedLights =
            if (session.config.lightEstimationMode != Config.LightEstimationMode.DISABLED) {
                arFrame.environmentLightsEstimate(
                    session.lightEstimationMode,
                    estimatedLights,
                    environment,
                    mainLight,
                    renderer.camera.exposureFactor
                )
            } else null

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

    override fun onDestroy(owner: LifecycleOwner) {
        estimatedLights?.destroy()
        estimatedLights = null
    }

    override fun getLifecycle(): ArSceneLifecycle = sceneLifecycle

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
}

/**
 * ### A SurfaceView that integrates with ARCore and renders a scene.
 */
interface ArSceneLifecycleOwner : SceneLifecycleOwner {
    val arCore: ARCore
    val arSession get() = arCore.session
    val arSessionConfig get() = arSession?.config
}

class ArSceneLifecycle(context: Context, override val owner: ArSceneLifecycleOwner) :
    SceneLifecycle(context, owner) {
    val arCore get() = owner.arCore
    val arSession get() = owner.arSession
    val arSessionConfig get() = owner.arSessionConfig

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