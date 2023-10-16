package io.github.sceneview.ar

import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.view.MotionEvent
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
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
import io.github.sceneview.environment.Environment
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.Node
import io.github.sceneview.utils.setKeepScreenOn

/**
 * ### A SurfaceView that integrates with ARCore and renders a scene
 *
 * @param sessionFeatures Fundamental session features
 */
open class ARSceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    sharedEngine: Engine? = null,
    sharedScene: Scene? = null,
    sharedView: View? = null,
    sharedRenderer: Renderer? = null,
    sharedModelLoader: ModelLoader? = null,
    sharedMaterialLoader: MaterialLoader? = null,
    var sessionFeatures: Set<Session.Feature> = setOf(),
    var cameraConfig: ((Session) -> CameraConfig)? = null,
    cameraNode: (engine: Engine, viewSize: Size) -> ARCameraNode = { engine, viewSize ->
        ARCameraNode(engine, viewSize)
    },
    var onSessionConfiguration: ((session: Session, Config) -> Unit)? = null,
    var onSessionCreated: ((session: Session) -> Unit)? = null,
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
    var onSessionUpdated: ((session: Session, frame: Frame) -> Unit)? = null,
    var onSessionResumed: ((session: Session) -> Unit)? = null,
    /**
     * Invoked when an ARCore error occurred.
     *
     * Registers a callback to be invoked when the ARCore Session cannot be initialized because
     * ARCore is not available on the device or the camera permission has been denied.
     */
    var onSessionFailed: ((exception: Exception) -> Unit)? = null,
    var onSessionConfigChanged: ((session: Session, config: Config) -> Unit)? = null,
    /**
     * Invoked when the `SceneView` is tapped.
     *
     * Only nodes with renderables or their parent nodes can be tapped since Filament picking is
     * used to find a touched node. The ID of the Filament renderable can be used to determine what
     * part of a model is tapped.
     */
    onTap: ((
        /** The motion event that caused the tap. **/
        motionEvent: MotionEvent,
        /** The node that was tapped or `null`. **/
        node: Node?
    ) -> Unit)? = null,
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
) : SceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes,
    sharedEngine,
    sharedScene,
    sharedView,
    sharedRenderer,
    sharedModelLoader,
    sharedMaterialLoader,
    cameraNode = cameraNode,
    onTap = onTap
) {
    open val arCore = ARCore(
        onSessionCreated = ::onSessionCreated,
        onSessionResumed = ::onSessionResumed,
        onArSessionFailed = ::onSessionFailed,
        onSessionConfigChanged = ::onSessionConfigChanged,
    )

    val session get() = arCore.session
    var frame: Frame? = null

    override val cameraNode: ARCameraNode = super.cameraNode as ARCameraNode

    /**
     * The [ARCameraStream] used to control if the occlusion should be enabled or disabled
     */
    val arCameraStream = ARCameraStream(engine, materialLoader)

    /**
     * [PlaneRenderer] used to control plane visualization.
     */
    val planeRenderer = PlaneRenderer(this)

    /**
     * The environment and main light that are estimated by AR Core to render the scene.
     *
     * - Environment handles a reflections, indirect lighting and skybox.
     * - ARCore will estimate the direction, the intensity and the color of the light
     */
    var lightEstimator: LightEstimator? = LightEstimator(engine, iblPrefilter)

    var lightEstimation: LightEstimator.Estimation? = null
        private set(value) {
            value?.mainLightColorFactor?.let {
                mainLight?.color = kDefaultMainLightColor * it
            }
            value?.mainLightIntensityFactor?.let {
                mainLight?.intensity = kDefaultMainLightIntensity * it
            }
            value?.mainLightDirection?.let {
                mainLight?.lightDirection = it
            }
            value?.environment?.let {
                environment = it
                // Delete previous environment only in case of new one
                if (field?.environment != it) {
                    field?.environment?.destroy()
                }
            }
            field = value

            onLightEstimationUpdated?.invoke(value)
        }

    override var environment: Environment?
        get() = super.environment
        set(value) {
            super.environment = value
            lightEstimator?.baseSphericalHarmonics = value?.sphericalHarmonics
        }

    override var indirectLight: IndirectLight?
        get() = super.indirectLight
        set(value) {
            super.indirectLight = value
            lightEstimator?.setBaseIndirectLight(value)
        }

    var trackingFailureReason: TrackingFailureReason? = null
        private set(value) {
            if (field != value) {
                field = value
                onTrackingFailureChanged?.invoke(value)
            }
        }

    private var _onSessionCreated = mutableListOf<(session: Session) -> Unit>()

    val onLightEstimationUpdated: ((estimation: LightEstimator.Estimation?) -> Unit)? = null

    var onTrackingFailureChanged: ((trackingFailureReason: TrackingFailureReason?) -> Unit)? =
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
        session?.setDisplayGeometry(display.rotation, width, height)
    }

    fun onSessionCreated(session: Session) {
        session.setCameraTextureNames(arCameraStream.cameraTextureIds)

        cameraConfig?.let { session.cameraConfig = it(session) }

        session.configure { config ->
            // getting ar frame doesn't block and gives last frame
            config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            // FocusMode must be changed after the session resume to work
//            config.focusMode = Config.FocusMode.AUTO

            onSessionConfiguration?.invoke(session, config)
        }

        addEntity(arCameraStream.entity)

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

        arCameraStream.update(session, frame)
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
            session?.frame?.hitTest(
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

            arCameraStream.destroy()

            lightEstimator?.destroy()
            planeRenderer.destroy()
        }

        super.destroy()
    }
}