package io.github.sceneview.ar

import android.content.Context
import android.opengl.EGLContext
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfig.FacingDirection
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.ar.arcore.LightEstimator
import io.github.sceneview.ar.arcore.configure
import io.github.sceneview.ar.arcore.getUpdatedTrackables
import io.github.sceneview.ar.arcore.hitTest
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.node.PoseNode
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.utils.setKeepScreenOn

/**
 * A SurfaceView that integrates with ARCore and renders a scene
 *
 * @param sessionFeatures Fundamental session features
 */
open class ARSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    sharedActivity: ComponentActivity? = null,
    sharedLifecycle: Lifecycle? = null,
    /**
     * Provide your own instance if you want to share Filament resources between multiple views.
     */
    sharedEngine: Engine? = null,
    /**
     * Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which is
     * a bundle of Filament textures, vertex buffers, index buffers, etc.
     *
     * A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and components.
     */
    sharedModelLoader: ModelLoader? = null,
    /**
     * A Filament Material defines the visual appearance of an object.
     *
     * Materials function as a templates from which [MaterialInstance]s can be spawned.
     */
    sharedMaterialLoader: MaterialLoader? = null,
    /**
     * Provide your own instance if you want to share [Node]s' scene between multiple views.
     */
    sharedScene: Scene? = null,
    /**
     * Encompasses all the state needed for rendering a {@link Scene}.
     *
     * [View] instances are heavy objects that internally cache a lot of data needed for
     * rendering. It is not advised for an application to use many View objects.
     *
     * For example, in a game, a [View] could be used for the main scene and another one for the
     * game's user interface. More <code>View</code> instances could be used for creating special
     * effects (e.g. a [View] is akin to a rendering pass).
     */
    sharedView: View? = null,
    /**
     * A [Renderer] instance represents an operating system's window.
     *
     * Typically, applications create a [Renderer] per window. The [Renderer] generates drawing
     * commands for the render thread and manages frame latency.
     */
    sharedRenderer: Renderer? = null,
    /**
     * Represents a virtual camera, which determines the perspective through which the scene is
     * viewed.
     *
     * All other functionality in Node is supported. You can access the position and rotation of the
     * camera, assign a collision shape to it, or add children to it.
     */
    sharedCameraNode: ARCameraNode? = null,
    /**
     * Always add a direct light source since it is required for shadowing.
     *
     * We highly recommend adding an [IndirectLight] as well.
     */
    sharedMainLightNode: LightNode? = null,
    /**
     * IndirectLight is used to simulate environment lighting.
     *
     * Environment lighting has a two components:
     * - irradiance
     * - reflections (specular component)
     *
     * @see IndirectLight
     * @see Scene.setIndirectLight
     */
    sharedIndirectLight: IndirectLight? = null,
    /**
     * The Skybox is drawn last and covers all pixels not touched by geometry.
     *
     * When added to a [SceneView], the `Skybox` fills all untouched pixels.
     *
     * The Skybox to use to fill untouched pixels, or null to unset the Skybox.
     *
     * @see Skybox
     * @see Scene.setSkybox
     */
    sharedSkybox: Skybox? = null,
    var sessionFeatures: Set<Session.Feature> = setOf(),
    var cameraConfig: ((Session) -> CameraConfig)? = null,
    /**
     * The [ARCameraStream] to render the camera texture.
     *
     * Use it to control if the occlusion should be enabled or disabled
     */
    sharedCameraStream: ARCameraStream? = null
) : SceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
    sharedActivity,
    sharedLifecycle,
    sharedEngine,
    sharedModelLoader,
    sharedMaterialLoader,
    sharedScene,
    sharedView,
    sharedRenderer,
    sharedCameraNode,
    sharedMainLightNode,
    sharedIndirectLight,
    sharedSkybox
) {
    open val arCore = ARCore(
        onSessionCreated = ::onSessionCreated,
        onSessionResumed = ::onSessionResumed,
        onArSessionFailed = ::onSessionFailed,
        onSessionConfigChanged = ::onSessionConfigChanged,
    )

    val session get() = arCore.session
    var frame: Frame? = null

    private var defaultCameraNode: ARCameraNode? = null

    /**
     * Represents a virtual camera, which determines the perspective through which the scene is
     * viewed.
     *
     * All other functionality in Node is supported. You can access the position and rotation of the
     * camera, assign a collision shape to it, or add children to it. Disabling the camera turns off
     * rendering.
     */
    override val cameraNode: ARCameraNode get() = _cameraNode as ARCameraNode

    private var defaultCameraStream: ARCameraStream? = null

    /**
     * The [ARCameraStream] to render the camera texture.
     *
     * Use it to control if the occlusion should be enabled or disabled
     */
    var cameraStream: ARCameraStream? =
        sharedCameraStream ?: createCameraStream(engine, materialLoader).also {
            defaultCameraStream = it
        }
        set(value) {
            if (field != value) {
                field?.let { removeEntity(it.entity) }
                field = value
                value?.let {
                    session?.setCameraTextureNames(it.cameraTextureIds)
                    addEntity(it.entity)
                }
            }
        }

    /**
     * [PlaneRenderer] used to control plane visualization.
     */
    val planeRenderer = PlaneRenderer(engine, modelLoader, materialLoader, scene)

    final override var indirectLight: IndirectLight? = super.indirectLight
        set(value) {
            super.indirectLight = value
            field = value
        }

    var indirectLightEstimated: IndirectLight?
        get() = super.indirectLight
        private set(value) {
            super.indirectLight = value
        }

    final override var mainLightNode: LightNode? = super.mainLightNode
        set(value) {
            super.mainLightNode = value
            field = value
        }

    var mainLightEstimatedNode: LightNode?
        get() = super.mainLightNode
        set(value) {
            super.mainLightNode = value
        }

    /**
     * The environment and main light that are estimated by AR Core to render the scene.
     *
     * - Environment handles a reflections, indirect lighting and skybox.
     * - ARCore will estimate the direction, the intensity and the color of the light
     */
    var lightEstimator: LightEstimator? = LightEstimator(engine, iblPrefilter)

    var lightEstimation: LightEstimator.Estimation? = null
        private set(value) {
            field = value
            if (value != null) {
                mainLightNode?.let { mainLightNode ->
                    value.mainLightColor?.let {
                        mainLightEstimatedNode?.color = mainLightNode.color * it
                    }
                    value.mainLightIntensity?.let {
                        mainLightEstimatedNode?.intensity = mainLightNode.intensity * it
                    }
                    value.mainLightDirection?.let {
                        mainLightEstimatedNode?.lightDirection = it
                    }
                }

                indirectLightEstimated = IndirectLight.Builder().apply {
                    value.irradiance?.let {
                        irradiance(3, it)
                    } ?: indirectLight?.irradianceTexture?.let { irradiance(it) }
                    value.reflections?.let {
                        reflections(it)
                    } ?: indirectLight?.reflectionsTexture?.let { reflections(it) }
                    indirectLight?.intensity?.let { intensity(it) }
                    indirectLight?.getRotation(null)?.let { rotation(it) }
                }.build(engine)
            }
            onLightEstimationUpdated?.invoke(value)
        }

    var trackingFailureReason: TrackingFailureReason? = null
        private set(value) {
            if (field != value) {
                field = value
                onTrackingFailureChanged?.invoke(value)
            }
        }

    var onSessionConfiguration: ((session: Session, Config) -> Unit)? = null
    var onSessionCreated: ((session: Session) -> Unit)? = null

    /**
     * Updates of the state of the ARCore system.
     *
     * Callback for [onSessionUpdated].
     *
     * This includes: receiving a new camera frame, updating the location of the device, updating
     * the location of tracking anchors, updating detected planes, etc.
     *
     * This call may update the pose of all created anchors and detected planes. The set of updated
     * objects is accessible through [Frame.getUpdatedTrackables].
     *
     * Invoked once per [Frame] immediately before the Scene is updated.
     */
    var onSessionUpdated: ((session: Session, frame: Frame) -> Unit)? = null
    var onSessionResumed: ((session: Session) -> Unit)? = null

    /**
     * Invoked when an ARCore error occurred.
     *
     * Registers a callback to be invoked when the ARCore Session cannot be initialized because
     * ARCore is not available on the device or the camera permission has been denied.
     */
    var onSessionFailed: ((exception: Exception) -> Unit)? = null
    var onSessionConfigChanged: ((session: Session, config: Config) -> Unit)? = null

    /**
     * Invoked when an ARCore trackable is tapped.
     *
     * Depending on the session configuration the [HitResult.getTrackable] can be:
     * - A [Plane] if [Config.setPlaneFindingMode] is enable.
     * - An [InstantPlacementPoint] if [Config.setInstantPlacementMode] is enable.
     * - A [DepthPoint] and [Point] if [Config.setDepthMode] is enable.
     */
    var onTapAR: ((
        /** The motion event that caused the tap. */
        motionEvent: MotionEvent,
        /** The ARCore hit result for the trackable that was tapped. */
        hitResult: HitResult
    ) -> Unit)? = null

    val onLightEstimationUpdated: ((estimation: LightEstimator.Estimation?) -> Unit)? = null

    var onTrackingFailureChanged: ((trackingFailureReason: TrackingFailureReason?) -> Unit)? =
        null

    override val cameraGestureDetector = null
    override val cameraManipulator = null

    private val lifecycleObserver = LifeCycleObserver()
    override var lifecycle: Lifecycle?
        get() = super.lifecycle
        set(value) {
            super.lifecycle?.removeObserver(lifecycleObserver)
            super.lifecycle = value
            value?.addObserver(lifecycleObserver)
        }

    private var _onSessionCreated = mutableListOf<(session: Session) -> Unit>()

    init {
        setCameraNode(sharedCameraNode ?: createCameraNode(engine).also {
            defaultCameraNode = it
        })
        sharedLifecycle?.addObserver(lifecycleObserver)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        session?.setDisplayGeometry(display.rotation, width, height)
    }

    fun onSessionCreated(session: Session) {
        cameraStream?.let {
            session.setCameraTextureNames(it.cameraTextureIds)
        }

        cameraConfig?.let { session.cameraConfig = it(session) }

        session.configure { config ->
            // getting ar frame doesn't block and gives last frame
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            // FocusMode must be changed after the session resume to work
//            config.focusMode = Config.FocusMode.AUTO

            onSessionConfiguration?.invoke(session, config)
        }

        cameraStream?.let { addEntity(it.entity) }

        _onSessionCreated.toList().forEach { it(session) }
        onSessionCreated?.invoke(session)
    }

    fun onSessionFailed(exception: Exception) {
        onSessionFailed?.invoke(exception)
    }

    fun onSessionResumed(session: Session) {
        session.configure { config ->
            // FocusMode must be changed after the session resume to work
            config.focusMode = Config.FocusMode.AUTO
        }

        onSessionResumed?.invoke(session)
    }

    fun onSessionConfigChanged(session: Session, config: Config) {
        // Feature config, therefore facing direction, can only be configured once per session.
        isFrontFaceWindingInverted = session.cameraConfig.facingDirection == FacingDirection.FRONT

        onSessionConfigChanged?.invoke(session, config)
    }

    /**
     * Before the render call occurs, update the ARCore session to grab the latest frame and update
     * listeners.
     */
    override fun onFrame(frameTimeNanos: Long) {
        session?.let { session ->
            session.updateOrNull()?.let { frame ->
                onSessionUpdated(session, frame)
            }
        }
        super.onFrame(frameTimeNanos)
    }

    /**
     * Updates the state of the ARCore system.
     *
     * This includes: receiving a new camera frame, updating the location of the device, updating
     * the location of tracking anchors, updating detected planes, etc.
     *
     * This call may update the pose of all created anchors and detected planes. The set of updated
     * objects is accessible through [Frame.getUpdatedTrackables].
     *
     * Invoked once per [Frame] immediately before the Scene is updated.
     *
     * @param session The running session
     * @param frame The most recent Frame received
     */
    protected open fun onSessionUpdated(session: Session, frame: Frame) {
        this.frame = frame

        val camera = frame.camera
        val isCameraTracking = camera.isTracking

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        // You will say thanks when still have battery after a long day debugging an AR app.
        // ...and it's better for your users
        activity?.setKeepScreenOn(isCameraTracking)

        cameraStream?.update(session, frame)
        cameraNode.update(session, frame)

        lightEstimation = lightEstimator?.update(session, frame, cameraNode.camera)

        planeRenderer.update(session, frame)

        childNodes.filterIsInstance<PoseNode>().forEach { poseNode ->
            poseNode.update(session, frame)
        }

        trackingFailureReason = if (!isCameraTracking) {
            camera.trackingFailureReason.takeIf { it != TrackingFailureReason.NONE }
        } else null

        onSessionUpdated?.invoke(session, frame)
    }

    /**
     * Define the session config used by ARCore.
     *
     * Prefer calling this method before the global (Activity or Fragment) onResume() cause the
     * session base configuration in made there.
     * Any later calls (after onSessionResumed()) to this function are not completely sure be taken
     * in account by ARCore (even if most of them will work)
     *
     * Please check that all your Session Config parameters are taken in account by ARCore at
     * runtime.
     *
     * @param applyConfig the apply block for the new config
     */
    fun configureSession(applyConfig: (Session, Config) -> Unit) {
        _onSessionCreated += object : (Session) -> Unit {
            override fun invoke(session: Session) {
                _onSessionCreated -= this
                session.configure { config ->
                    applyConfig.invoke(session, config)
                }
            }
        }
    }

    override fun onTap(motionEvent: MotionEvent, node: Node?) {
        super.onTap(motionEvent, node)

        if (node == null) {
            frame?.hitTest(
                xPx = motionEvent.x,
                yPx = motionEvent.y
            )?.firstOrNull()?.let { hitResult ->
                onTapAR(motionEvent, hitResult)
            }
        }
    }

    /**
     * Invoked when an ARCore trackable is tapped.
     *
     * Calls the `onTapAr` listener if it is available.
     *
     * @param motionEvent The motion event that caused the tap.
     * @param hitResult The ARCore hit result for the trackable that was tapped.
     */
    open fun onTapAR(motionEvent: MotionEvent, hitResult: HitResult) {
        onTapAR?.invoke(motionEvent, hitResult)
    }

    override fun onResized(width: Int, height: Int) {
        super.onResized(width, height)

        planeRenderer.viewSize = Size(width, height)
    }

    /**
     * Performs a ray cast to retrieve the ARCore info at this camera point.
     *
     * By default the latest session frame is used.
     *
     * @return the hitResult or null if no info is retrieved
     */
    fun hitTestAR(
        xPx: Float = width / 2.0f,
        yPx: Float = height / 2.0f,
        plane: Boolean = true,
        depth: Boolean = true,
        instant: Boolean = true,
        instantDistance: Float = kDefaultHitTestInstantDistance,
        planePoseInPolygon: Boolean = true,
        minCameraDistance: Float = 0.0f,
        pointOrientationModes: Set<Point.OrientationMode> = setOf(
            Point.OrientationMode.ESTIMATED_SURFACE_NORMAL
        )
    ) = frame?.hitTest(
        xPx, yPx, plane = plane,
        depth = depth,
        instant = instant,
        instantDistance = instantDistance,
        planePoseInPolygon = planePoseInPolygon,
        minCameraDistance = minCameraDistance,
        pointOrientationModes = pointOrientationModes
    )

    override fun destroy() {
        if (!isDestroyed) {
            arCore.destroy()

            defaultCameraNode?.destroy()
            defaultCameraStream?.destroy()

            lightEstimator?.destroy()
            planeRenderer.destroy()
        }

        super.destroy()
    }

    class DefaultARCameraNode(engine: Engine) : ARCameraNode(engine) {
        init {
            // Set the exposure on the camera, this exposure follows the sunny f/16 rule
            // Since we define a light that has the same intensity as the sun, it guarantees a
            // proper exposure
            setExposure(16.0f, 1.0f / 125.0f, 100.0f)
        }
    }


    private inner class LifeCycleObserver : DefaultLifecycleObserver {
        override fun onCreate(owner: LifecycleOwner) {
            arCore.create(context, activity, sessionFeatures)
        }

        override fun onResume(owner: LifecycleOwner) {
            arCore.resume(context, activity)
        }

        override fun onPause(owner: LifecycleOwner) {
            arCore.pause()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            arCore.destroy()
        }
    }

    companion object {
        fun createEglContext() = SceneView.createEglContext()
        fun createEngine(eglContext: EGLContext) = SceneView.createEngine(eglContext)
        fun createScene(engine: Engine) = SceneView.createScene(engine)
        fun createView(engine: Engine) = SceneView.createView(engine).apply {
            // Dynamic resolutions has issues with the AR Camera stream: Lags, green screens,...
            dynamicResolutionOptions = dynamicResolutionOptions.apply {
                enabled = false
            }
        }

        fun createRenderer(engine: Engine) = SceneView.createRenderer(engine)
        fun createModelLoader(engine: Engine, context: Context) =
            SceneView.createModelLoader(engine, context)

        fun createMaterialLoader(engine: Engine, context: Context) =
            SceneView.createMaterialLoader(engine, context)

        fun createCameraNode(engine: Engine): ARCameraNode = DefaultARCameraNode(engine)

        fun createCameraStream(engine: Engine, materialLoader: MaterialLoader) =
            ARCameraStream(engine, materialLoader)

        fun createMainLight(engine: Engine): LightNode = SceneView.createMainLightNode(engine)
        fun createIndirectLight(engine: Engine): IndirectLight? = null
        fun createSkybox(engine: Engine) = SceneView.createSkybox(engine)
    }
}