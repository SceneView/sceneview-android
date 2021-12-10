package io.github.sceneview.ar

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.*
import com.google.ar.core.*
import com.google.ar.core.CameraConfig.FacingDirection
import com.google.ar.core.exceptions.DeadlineExceededException
import com.google.ar.core.exceptions.NotYetAvailableException
import com.google.ar.sceneform.ArCamera
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.TransformableNode
import com.gorisse.thomas.lifecycle.lifecycleScope
import com.gorisse.thomas.sceneview.Instructions
import io.github.sceneview.SceneLifecycle
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneLifecycleOwner
import io.github.sceneview.SceneView
import io.github.sceneview.ar.arcore.*
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.light.defaultMainLightIntensity
import io.github.sceneview.light.destroy
import io.github.sceneview.light.intensity
import io.github.sceneview.light.sunnyDayMainLightIntensity
import io.github.sceneview.model.await
import io.github.sceneview.node.Node
import io.github.sceneview.scene.exposureFactor
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

    override fun getLifecycle(): ArSceneLifecycle =
        _sceneLifecycle as? ArSceneLifecycle ?: ArSceneLifecycle(context, this).also {
            _sceneLifecycle = it
        }

    private val cameraTextureId = GLHelper.createCameraTexture()

    override val camera = ArCamera(this)

    override val arCore = ARCore(cameraTextureId, lifecycle)

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
    var estimatedEnvironmentLights: EnvironmentLightsEstimate? = null
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

    private var isProcessingFrame = false

    val instructions = Instructions(this, lifecycle)

    init {
    }

    override fun onArSessionCreated(session: ArSession) {
        super.onArSessionCreated(session)

        // Don't remove this code-block. It is important to correctly
        // set the DisplayGeometry for the ArCore-Session if four
        // example the permission Dialog is shown on the screen.
        // If we remove this part, the camera is flickering if returned
        // from the permission Dialog.
        if (renderer.desiredWidth != 0 && renderer.desiredHeight != 0) {
            session.setDisplayGeometry(
                display!!.rotation,
                renderer.desiredWidth,
                renderer.desiredHeight
            )
        }

        // Feature config, therefore facing direction, can only be configured once per session.
        if (session.cameraConfig.facingDirection == FacingDirection.FRONT) {
            renderer.isFrontFaceWindingInverted = true
        }

        // Set max frames per seconds here.
        maxFramesPerSeconds = session.cameraConfig.fpsRange.upper
    }

    override fun onArSessionConfigChanged(session: ArSession, config: Config) {
        // Set the correct Texture configuration on the camera stream
        cameraStream.checkIfDepthIsEnabled(session, config)

        mainLight?.intensity = when (config.lightEstimationMode) {
            Config.LightEstimationMode.DISABLED -> defaultMainLightIntensity
            else -> sunnyDayMainLightIntensity
        }
    }

    /**
     * Before the render call occurs, update the ARCore session to grab the latest frame and update
     * listeners.
     *
     * The super.onFrame() is called if the session updated successfully and a new frame was
     * obtained. Update the scene before rendering.
     */
    override fun doFrame(frameTime: FrameTime) {
        // TODO : Move to dedicated Lifecycle aware classes when Kotlined them
        val session = session?.takeIf { !isProcessingFrame } ?: return

        // No new frame, no drawing
        session.updateFrame(frameTime)?.let { frame ->
            isProcessingFrame = true
            doArFrame(frame)
            super.doFrame(frameTime)
            isProcessingFrame = false
        }
    }

    /**
     * ### Invoked once per [Frame] immediately before the Scene is updated.
     *
     * The listener will be called in the order in which they were added.
     */
    protected open fun doArFrame(arFrame: ArFrame) {
        // TODO : Move to dedicated Lifecycle aware classes when Kotlined them
        val (session, _, frame, arCamera) = arFrame
        if (arCamera.isTracking != session.previousFrame?.camera?.isTracking) {
            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            // You will say thanks when still have battery after a long day debugging an AR app.
            // ...and it's better for your users
            activity.setKeepScreenOn(arCamera.isTracking)
        }

        cameraStream.apply {
            // Setup Camera Stream if needed.
            if (!isTextureInitialized) {
                initializeTexture(arCamera)
            }
            // Recalculate camera Uvs if necessary.
            if (frame.hasDisplayGeometryChanged()) {
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
        this.camera.updateTrackedPose(arCamera)

        // Update the light estimate.
        if (session.config.lightEstimationMode != Config.LightEstimationMode.DISABLED) {
            estimatedEnvironmentLights = arFrame.environmentLightsEstimate(
                session.lightEstimationConfig,
                estimatedEnvironmentLights,
                environment,
                mainLight,
                renderer.camera.exposureFactor
            )
        }

        if (onAugmentedImageUpdate != null) {
            arFrame.updatedAugmentedImages.forEach(onAugmentedImageUpdate)
        }

        if (onAugmentedFaceUpdate != null) {
            arFrame.updatedAugmentedFaces.forEach(onAugmentedFaceUpdate)
        }

        onArFrameUpdated?.invoke(arFrame)
        lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
            onArFrame(arFrame)
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        estimatedEnvironmentLights?.destroy()
        estimatedEnvironmentLights = null
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
    fun configureSession(applyConfig: (Config) -> Unit) {
        lifecycle.doOnArSessionCreated { session ->
            session.configure(applyConfig)
        }
    }

    override fun onTouch(selectedNode: Node?, motionEvent: MotionEvent): Boolean {
        if (!super.onTouch(selectedNode, motionEvent) &&
            selectedNode == null
        ) {
            // TODO : Should be handled by the nodesTouchEventDispatcher
            nodeGestureRecognizer.selectNode(null)
            session?.let { session ->
                session.currentFrame?.hitTest(motionEvent)?.let { hitResult ->
                    onTouchAr(hitResult, motionEvent)
                    return true
                }
            }
        }
        return false
    }

    /**
     * ### Invoked when an ARCore error occurred
     *
     * Registers a callback to be invoked when the ARCore Session cannot be initialized because
     * ARCore is not available on the device or the camera permission has been denied.
     */
    var onARCoreException: ((exception: Exception) -> Unit)?
        get() = arCore.onException
        set(value) {
            arCore.onException = value
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
    var onArFrameUpdated: ((arFrame: ArFrame) -> Unit)? = null

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

    /**
     * Loads a monolithic binary glTF and add it to the fragment when the user tap on a detected
     * plane surface.
     *
     * Plays the animations automatically if the model has one.
     *
     * @param glbFileLocation Glb file source location can be come from the asset folder ("model.glb")
     * or an http source ("http://domain.com/model.glb")
     * @param onModelLoaded Called when an ARCore plane is tapped and the model is added to the
     * scene. The callback will only be invoked if no [com.google.ar.sceneform.Node] was tapped.
     * The [RenderableInstance] param is the added instance of the model.
     * @param onModelError An error occurred while loading the model from the source file location.
     * @see setOnTapArPlaneListener
     */
    fun setOnTapArPlaneGlbModel(
        glbFileLocation: String,
        onLoaded: ((renderable: Renderable) -> Unit)? = null,
        onAdded: ((node: ArNode, renderableInstance: RenderableInstance) -> Unit)? = null,
        onError: ((exception: Throwable) -> Unit)? = null,
    ) {
        lifecycleScope.launchWhenCreated {
            try {
                val model = ModelRenderable.builder()
                    .setSource(context, Uri.parse(glbFileLocation))
                    .setIsFilamentGltf(true)
                    .await()
                onLoaded?.invoke(model!!)
                onTouchAr = { hitResult, _ ->
                    addChild(ArNode(hitResult).apply {
                        // Create the transformable model and add it to the anchor
                        val modelNode = TransformableNode(nodeGestureRecognizer)
                        val renderableInstance = setRenderable(model)!!.apply {
                            animate(true).start()
                        }
                        addChild(modelNode)
                        onAdded?.invoke(this, renderableInstance)
                    })
                }
            } catch (exception: Exception) {
                onError?.invoke(exception)
            }
        }
    }
}

/**
 * ### A SurfaceView that integrates with ARCore and renders a scene.
 */
interface ArSceneLifecycleOwner : SceneLifecycleOwner {
    val arCore: ARCore
    val session get() = arCore.session
    val sessionConfig get() = session?.config
}

class ArSceneLifecycle(context: Context, override val owner: ArSceneLifecycleOwner) :
    SceneLifecycle(context, owner) {
    val arCore get() = owner.arCore
    val session get() = owner.session
    val sessionConfig get() = owner.sessionConfig

    /**
     * ### Performs the given action when ARCore session is created
     *
     * If the ARCore session is already created the action will be performed immediately, otherwise
     * the action will be performed after the ARCore session is next created.
     * The action will only be invoked once, and any listeners will then be removed.
     */
    fun doOnArSessionCreated(action: (session: ArSession) -> Unit) {
        session?.let(action) ?: addObserver(onArSessionCreated = {
            removeObserver(this)
            action(it)
        })
    }

    fun addObserver(
        onArSessionCreated: (ArSceneLifecycleObserver.(session: ArSession) -> Unit)? = null,
        onArSessionResumed: (ArSceneLifecycleObserver.(session: ArSession) -> Unit)? = null,
        onArSessionConfigChanged: (ArSceneLifecycleObserver.(session: ArSession, config: Config) -> Unit)? = null,
        onArFrameUpdated: (ArSceneLifecycleObserver.(arFrame: ArFrame) -> Unit)? = null
    ) {
        addObserver(object : ArSceneLifecycleObserver {
            override fun onArSessionCreated(session: ArSession) {
                onArSessionCreated?.invoke(this, session)
            }

            override fun onArSessionResumed(session: ArSession) {
                onArSessionResumed?.invoke(this, session)
            }

            override fun onArSessionConfigChanged(session: ArSession, config: Config) {
                onArSessionConfigChanged?.invoke(this, session, config)
            }

            override fun onArFrame(arFrame: ArFrame) {
                onArFrameUpdated?.invoke(this, arFrame)
            }
        })
    }
}

interface ArSceneLifecycleObserver : SceneLifecycleObserver {

    fun onArSessionCreated(session: ArSession) {
    }

    fun onArSessionResumed(session: ArSession) {
    }

    fun onArSessionConfigChanged(session: ArSession, config: Config) {
    }

    fun onArFrame(arFrame: ArFrame) {
    }
}