package io.github.sceneview.ar

import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.TextureView
import android.view.WindowManager as AndroidWindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.ar.core.Camera
import com.google.ar.core.CameraConfig
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.SceneNodeManager
import io.github.sceneview.SurfaceType
import io.github.sceneview.ar.arcore.configure
import io.github.sceneview.ar.arcore.getUpdatedTrackables
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.light.LightEstimator
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.ar.node.PoseNode
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.collision.HitResult
import io.github.sceneview.environment.Environment
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.drainFramePipeline
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.ViewNode2.WindowManager
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import io.github.sceneview.safeDestroyEnvironment
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * An ARCore session declared as Compose UI.
 *
 * `ARScene` is a `@Composable` that embeds a Filament + ARCore viewport. Its trailing [content]
 * block is an **[ARSceneScope]** DSL where AR-tracked nodes — anchors, augmented images, face
 * meshes, cloud anchors, hit-result cursors — are composable functions that follow the same
 * Compose lifecycle as any other UI element.
 *
 * Drive AR state with ordinary Compose `mutableStateOf`: when state changes, the composition
 * updates and the 3D scene reflects it on the next frame.
 *
 * ### Minimal usage
 * ```kotlin
 * var anchor by remember { mutableStateOf<Anchor?>(null) }
 *
 * ARScene(
 *     modifier = Modifier.fillMaxSize(),
 *     onSessionUpdated = { _, frame ->
 *         if (anchor == null) {
 *             anchor = frame.getUpdatedPlanes()
 *                 .firstOrNull { it.type == Plane.Type.HORIZONTAL_UPWARD_FACING }
 *                 ?.let { frame.createAnchorOrNull(it.centerPose) }
 *         }
 *     }
 * ) {
 *     anchor?.let { a ->
 *         AnchorNode(anchor = a) {
 *             ModelNode(
 *                 modelInstance = rememberModelInstance(modelLoader, "models/helmet.glb"),
 *                 scaleToUnits = 0.5f
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * When `anchor` is set, `AnchorNode` enters the composition and the model appears in AR.
 * When cleared, both are removed and destroyed automatically — no cleanup code needed.
 *
 * @param modifier                 Modifier for the underlying AR surface.
 * @param surfaceType              [SurfaceType.Surface] (SurfaceView, best GPU performance) or
 *                                 [SurfaceType.TextureSurface] (TextureView, supports alpha blending).
 * @param engine                   Shared Filament [Engine]. Use [rememberEngine].
 * @param modelLoader              Loader for glTF/GLB models. Use [rememberModelLoader].
 * @param materialLoader           Loader for Filament material templates. Use [rememberMaterialLoader].
 * @param environmentLoader        Loader for HDR environments. Use [rememberEnvironmentLoader].
 * @param sessionFeatures          ARCore [Session.Feature]s to enable (e.g. front camera).
 * @param sessionCameraConfig      Override for the ARCore camera configuration.
 * @param sessionConfiguration     Callback to configure the ARCore [Session] and [Config].
 * @param planeRenderer            Whether to render the AR plane grid overlay.
 * @param cameraStream             [ARCameraStream] for camera texture rendering and occlusion.
 * @param view                     Filament [View] for this scene. Use [rememberView].
 * @param isOpaque                 Whether the render target is opaque. Default `true`.
 * @param renderer                 Filament [Renderer]. Use [rememberRenderer].
 * @param scene                    Filament [Scene] graph. Use [rememberScene].
 * @param environment              IBL + skybox environment. Use [rememberAREnvironment].
 * @param mainLightNode            Primary directional light. Use [rememberMainLightNode].
 * @param cameraNode               AR camera node. Use [rememberARCameraNode].
 * @param collisionSystem          Hit-testing and collision system. Use [rememberCollisionSystem].
 * @param viewNodeWindowManager    Off-screen window manager required for [SceneScope.ViewNode].
 * @param onSessionCreated         Called once when the ARCore [Session] is ready.
 * @param onSessionResumed         Called each time the session is resumed.
 * @param onSessionPaused          Called each time the session is paused.
 * @param onSessionFailed          Called if ARCore fails to initialize (missing ARCore or permission).
 * @param onSessionUpdated         Called once per AR frame before the scene is updated.
 * @param onTrackingFailureChanged Called when the camera [TrackingFailureReason] changes.
 * @param onGestureListener        Gesture callbacks — tap, double-tap, drag, pinch, etc.
 * @param onTouchEvent             Raw touch event callback with optional hit result.
 * @param activity                 Host [ComponentActivity] (auto-resolved from [LocalContext]).
 * @param lifecycle                Lifecycle that binds the AR session resume/pause cycle.
 * @param content                  Declare AR scene content using the [ARSceneScope] composable DSL.
 */
@Composable
fun ARScene(
    modifier: Modifier = Modifier,
    /**
     * Selects whether the backing surface is SurfaceView-based ([SurfaceType.Surface], renders
     * behind Compose, best performance) or TextureView-based ([SurfaceType.TextureSurface],
     * renders inline, supports alpha blending).
     */
    surfaceType: SurfaceType = SurfaceType.Surface,
    /**
     * Provide your own instance if you want to share Filament resources between multiple views.
     */
    engine: Engine = rememberEngine(),
    /**
     * Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object,
     * which is a bundle of Filament textures, vertex buffers, index buffers, etc.
     */
    modelLoader: ModelLoader = rememberModelLoader(engine),
    /**
     * A Filament Material defines the visual appearance of an object.
     * Materials function as templates from which [MaterialInstance]s can be spawned.
     */
    materialLoader: MaterialLoader = rememberMaterialLoader(engine),
    /**
     * Utility for decoding an HDR file or consuming KTX1 files and producing Filament textures,
     * IBLs, and sky boxes.
     */
    environmentLoader: EnvironmentLoader = rememberEnvironmentLoader(engine),
    /**
     * Fundamental session features that can be requested.
     * @see Session.Feature
     */
    sessionFeatures: Set<Session.Feature> = setOf(),
    /**
     * Sets the camera config to use.
     * The config must be one returned by [Session.getSupportedCameraConfigs].
     */
    sessionCameraConfig: ((Session) -> CameraConfig)? = null,
    /**
     * Configures the session and verifies that the enabled features in the specified session
     * config are supported with the currently set camera config.
     */
    sessionConfiguration: ((session: Session, Config) -> Unit)? = null,
    /**
     * Enable the plane renderer.
     */
    planeRenderer: Boolean = true,
    /**
     * The [ARCameraStream] to render the camera texture.
     */
    cameraStream: ARCameraStream? = rememberARCameraStream(materialLoader),
    /**
     * Encompasses all the state needed for rendering a [Scene].
     */
    view: View = rememberView(engine),
    /**
     * Controls whether the render target is opaque or not. Default `true`.
     */
    isOpaque: Boolean = true,
    /**
     * A [Renderer] instance represents an operating system's window.
     */
    renderer: Renderer = rememberRenderer(engine),
    /**
     * Provide your own instance if you want to share [Node]s' scene between multiple views.
     */
    scene: Scene = rememberScene(engine),
    /**
     * Defines the lighting environment and the skybox of the scene.
     */
    environment: Environment = rememberAREnvironment(engine),
    /**
     * Always add a direct light source since it is required for shadowing.
     */
    mainLightNode: LightNode? = rememberMainLightNode(engine),
    cameraNode: ARCameraNode = rememberARCameraNode(engine),
    /**
     * Physics system to handle collision between nodes, hit testing on nodes, etc.
     */
    collisionSystem: CollisionSystem = rememberCollisionSystem(view),
    /**
     * Used for [io.github.sceneview.node.ViewNode2]s that can display an Android [android.view.View].
     */
    viewNodeWindowManager: WindowManager? = null,
    /**
     * The session is ready to be accessed.
     */
    onSessionCreated: ((session: Session) -> Unit)? = null,
    /**
     * The session has been resumed.
     */
    onSessionResumed: ((session: Session) -> Unit)? = null,
    /**
     * The session has been paused.
     */
    onSessionPaused: ((session: Session) -> Unit)? = null,
    /**
     * Invoked when an ARCore error occurred.
     */
    onSessionFailed: ((exception: Exception) -> Unit)? = null,
    /**
     * Updates of the state of the ARCore system.
     * Invoked once per [Frame] immediately before the Scene is updated.
     */
    onSessionUpdated: ((session: Session, frame: Frame) -> Unit)? = null,
    /**
     * Listen for camera tracking failure.
     */
    onTrackingFailureChanged: ((trackingFailureReason: TrackingFailureReason?) -> Unit)? = null,
    /**
     * The listener invoked for all the gesture detector callbacks.
     */
    onGestureListener: GestureDetector.OnGestureListener? = rememberOnGestureListener(),
    onTouchEvent: ((e: MotionEvent, hitResult: HitResult?) -> Boolean)? = null,
    activity: ComponentActivity? = LocalContext.current as? ComponentActivity,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    /**
     * DSL block for declaring AR nodes via [ARSceneScope].
     */
    content: (@Composable ARSceneScope.() -> Unit)? = null
) {
    if (LocalInspectionMode.current) {
        ARScenePreview(modifier)
        return
    }

    val context = LocalContext.current

    // ── AR subsystems ─────────────────────────────────────────────────────────────────────────────

    val arPlaneRenderer = remember(engine, materialLoader, scene) {
        PlaneRenderer(engine, materialLoader, scene)
    }
    val lightEstimator = remember(engine, environmentLoader) {
        LightEstimator(engine, environmentLoader.iblPrefilter)
    }
    DisposableEffect(arPlaneRenderer, lightEstimator) {
        onDispose {
            arPlaneRenderer.destroy()
            lightEstimator.destroy()
        }
    }

    // ── ARCore session lifecycle ──────────────────────────────────────────────────────────────────

    // Mutable refs for callbacks — updated each recomposition so lambdas are always fresh.
    val onSessionCreatedRef = remember { AtomicReference(onSessionCreated) }
    val onSessionResumedRef = remember { AtomicReference(onSessionResumed) }
    val onSessionPausedRef = remember { AtomicReference(onSessionPaused) }
    val onSessionFailedRef = remember { AtomicReference(onSessionFailed) }
    val onSessionUpdatedRef = remember { AtomicReference(onSessionUpdated) }
    val onTrackingFailureChangedRef = remember { AtomicReference(onTrackingFailureChanged) }
    val sessionConfigurationRef = remember { AtomicReference(sessionConfiguration) }
    val sessionCameraConfigRef = remember { AtomicReference(sessionCameraConfig) }

    SideEffect {
        onSessionCreatedRef.set(onSessionCreated)
        onSessionResumedRef.set(onSessionResumed)
        onSessionPausedRef.set(onSessionPaused)
        onSessionFailedRef.set(onSessionFailed)
        onSessionUpdatedRef.set(onSessionUpdated)
        onTrackingFailureChangedRef.set(onTrackingFailureChanged)
        sessionConfigurationRef.set(sessionConfiguration)
        sessionCameraConfigRef.set(sessionCameraConfig)
    }

    val prevTrackingFailureRef = remember { AtomicReference<TrackingFailureReason?>(null) }
    val isFrontFaceWindingInvertedRef = remember { AtomicBoolean(false) }

    val arCore = remember {
        ARCore(
            onSessionCreated = { session ->
                cameraStream?.let { session.setCameraTextureNames(it.cameraTextureIds) }
                sessionCameraConfigRef.get()?.let { session.cameraConfig = it(session) }
                session.configure { config ->
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    sessionConfigurationRef.get()?.invoke(session, config)
                }
                cameraStream?.let { scene.addEntity(it.entity) }
                onSessionCreatedRef.get()?.invoke(session)
            },
            onSessionResumed = { session ->
                session.configure { config -> config.focusMode = Config.FocusMode.AUTO }
                onSessionResumedRef.get()?.invoke(session)
            },
            onSessionPaused = { session ->
                onSessionPausedRef.get()?.invoke(session)
            },
            onArSessionFailed = { exception ->
                onSessionFailedRef.get()?.invoke(exception)
            },
            onSessionConfigChanged = { session, _ ->
                isFrontFaceWindingInvertedRef.set(
                    session.cameraConfig.facingDirection == CameraConfig.FacingDirection.FRONT
                )
            }
        )
    }

    DisposableEffect(lifecycle) {
        arCore.create(context, activity, sessionFeatures)

        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { arCore.resume(context, activity) }
            override fun onPause(owner: LifecycleOwner) { arCore.pause() }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            Thread { arCore.destroy() }.start()
        }
    }

    // ── Scene / camera / environment setup ───────────────────────────────────────────────────────

    val nodeManager = remember(scene, collisionSystem) { SceneNodeManager(scene, collisionSystem) }

    SideEffect {
        scene.indirectLight = environment.indirectLight
        scene.skybox = environment.skybox
        view.scene = scene
        view.camera = cameraNode.camera
        cameraNode.collisionSystem = collisionSystem
        cameraNode.setView(view)
    }

    // ── Main light node ───────────────────────────────────────────────────────────────────────────

    val prevMainLightRef = remember { AtomicReference<LightNode?>(null) }
    SideEffect {
        val prev = prevMainLightRef.get()
        if (prev != mainLightNode) {
            prev?.let { nodeManager.removeNode(it) }
            mainLightNode?.let { nodeManager.addNode(it) }
            prevMainLightRef.set(mainLightNode)
        }
    }

    // ── DSL nodes → Filament scene sync ──────────────────────────────────────────────────────────

    val scopeChildNodes: SnapshotStateList<Node> = remember { mutableStateListOf() }
    val childNodesRef = remember { AtomicReference(emptyList<Node>()) }

    LaunchedEffect(nodeManager) {
        var prevNodes = emptyList<Node>()
        snapshotFlow { scopeChildNodes.toList() }.collect { newNodes ->
            (prevNodes - newNodes.toSet()).forEach { nodeManager.removeNode(it) }
            (newNodes - prevNodes.toSet()).forEach { nodeManager.addNode(it) }
            prevNodes = newNodes
            childNodesRef.set(newNodes)
        }
    }

    // ── Camera stream lifecycle ───────────────────────────────────────────────────────────────────

    // Keep a thread-safe ref so the render loop always uses the latest camera stream instance,
    // even if it was recreated by a recomposition.
    val cameraStreamRef = remember { AtomicReference<ARCameraStream?>(cameraStream) }
    val prevCameraStreamRef = remember { AtomicReference<ARCameraStream?>(null) }
    SideEffect {
        cameraStreamRef.set(cameraStream)
        val prev = prevCameraStreamRef.get()
        if (prev != cameraStream) {
            prev?.let { scene.removeEntity(it.entity) }
            cameraStream?.let { stream ->
                arCore.session?.let {
                    it.setCameraTextureNames(stream.cameraTextureIds)
                    scene.addEntity(stream.entity)
                }
            }
            prevCameraStreamRef.set(cameraStream)
        }
    }

    // ── Plane renderer state ──────────────────────────────────────────────────────────────────────

    SideEffect {
        arPlaneRenderer.isEnabled = planeRenderer
    }

    // ── Lifecycle-aware rendering ─────────────────────────────────────────────────────────────────

    val isResumed = remember {
        AtomicBoolean(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) { isResumed.set(true) }
            override fun onPause(owner: LifecycleOwner) { isResumed.set(false) }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // ── Shared surface state ──────────────────────────────────────────────────────────────────────

    val swapChainRef = remember { AtomicReference<SwapChain?>(null) }
    val gestureDetector = remember(context) { GestureDetector(context = context, listener = null) }
    val cameraGestureDetectorRef = remember { AtomicReference<CameraGestureDetector?>(null) }
    val displayHelper = remember(context) { DisplayHelper(context) }

    SideEffect { gestureDetector.listener = onGestureListener }

    val touchDispatcher: (MotionEvent) -> Unit = { event ->
        val hitResult = collisionSystem.hitTest(event).firstOrNull { it.node.isTouchable }
        if (onTouchEvent?.invoke(event, hitResult) != true &&
            hitResult?.node?.onTouchEvent(event, hitResult) != true
        ) {
            gestureDetector.onTouchEvent(event, hitResult)
            cameraGestureDetectorRef.get()?.onTouchEvent(event)
        }
    }

    // ── Render loop ───────────────────────────────────────────────────────────────────────────────

    LaunchedEffect(engine, renderer, view, scene) {
        while (true) {
            if (!isResumed.get()) {
                delay(16)
                continue
            }
            withFrameNanos { frameTimeNanos ->
                val sc = swapChainRef.get() ?: return@withFrameNanos

                view.isFrontFaceWindingInverted = isFrontFaceWindingInvertedRef.get()

                val childNodes = childNodesRef.get()

                // AR frame update.
                arCore.session?.let { session ->
                    session.updateOrNull()?.let { frame ->
                        onARFrame(
                            engine = engine,
                            scene = scene,
                            view = view,
                            cameraNode = cameraNode,
                            cameraStream = cameraStreamRef.get(),
                            lightEstimator = lightEstimator,
                            mainLightNode = mainLightNode,
                            environment = environment,
                            arPlaneRenderer = arPlaneRenderer,
                            childNodes = childNodes,
                            prevTrackingFailureRef = prevTrackingFailureRef,
                            onTrackingFailureChangedRef = onTrackingFailureChangedRef,
                            onSessionUpdatedRef = onSessionUpdatedRef,
                            session = session,
                            frame = frame
                        )
                    }
                }

                modelLoader.updateLoad()
                childNodes.forEach { it.onFrame(frameTimeNanos) }

                if (renderer.beginFrame(sc, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }
    }

    // ── Surface view ──────────────────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    val display = remember(context) {
        (context.getSystemService(WINDOW_SERVICE) as AndroidWindowManager).defaultDisplay
    }

    fun applyARResize(width: Int, height: Int) {
        view.viewport = Viewport(0, 0, width, height)
        cameraNode.updateProjection()
        arCore.session?.setDisplayGeometry(display.rotation, width, height)
        arPlaneRenderer.viewSize = Size(width, height)
    }

    when (surfaceType) {
        SurfaceType.Surface -> AndroidView(
            modifier = modifier,
            factory = { ctx ->
                SurfaceView(ctx).also { sv ->
                    if (!isOpaque) sv.holder.setFormat(PixelFormat.TRANSLUCENT)
                    sv.holder.addCallback(object : SurfaceHolder.Callback {
                        override fun surfaceCreated(holder: SurfaceHolder) {}

                        override fun surfaceChanged(
                            holder: SurfaceHolder, format: Int, width: Int, height: Int
                        ) {
                            if (swapChainRef.get() == null) {
                                swapChainRef.set(engine.createSwapChain(holder.surface))
                                displayHelper.attach(renderer, display)
                                cameraGestureDetectorRef.set(
                                    CameraGestureDetector(
                                        viewHeight = { sv.height },
                                        cameraManipulator = null
                                    )
                                )
                            }
                            applyARResize(width, height)
                            engine.drainFramePipeline()
                        }

                        override fun surfaceDestroyed(holder: SurfaceHolder) {
                            cameraGestureDetectorRef.set(null)
                            swapChainRef.getAndSet(null)?.let {
                                runCatching { engine.destroySwapChain(it) }
                            }
                            engine.flushAndWait()
                            displayHelper.detach()
                        }
                    })
                    sv.setOnTouchListener { _, event -> touchDispatcher(event); true }
                }
            },
            update = {}
        )

        SurfaceType.TextureSurface -> AndroidView(
            modifier = modifier,
            factory = { ctx ->
                TextureView(ctx).also { tv ->
                    tv.isOpaque = isOpaque
                    var textureSurface: Surface? = null
                    tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            st: SurfaceTexture, width: Int, height: Int
                        ) {
                            textureSurface = Surface(st)
                            swapChainRef.set(engine.createSwapChain(textureSurface!!))
                            displayHelper.attach(renderer, display)
                            cameraGestureDetectorRef.set(
                                CameraGestureDetector(
                                    viewHeight = { tv.height },
                                    cameraManipulator = null
                                )
                            )
                            applyARResize(width, height)
                            engine.drainFramePipeline()
                        }

                        override fun onSurfaceTextureSizeChanged(
                            st: SurfaceTexture, width: Int, height: Int
                        ) {
                            applyARResize(width, height)
                            engine.drainFramePipeline()
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            cameraGestureDetectorRef.set(null)
                            swapChainRef.getAndSet(null)?.let {
                                runCatching { engine.destroySwapChain(it) }
                            }
                            engine.flushAndWait()
                            displayHelper.detach()
                            textureSurface?.release()
                            textureSurface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                    }
                    tv.setOnTouchListener { _, event -> touchDispatcher(event); true }
                }
            },
            update = {}
        )
    }

    // ── DSL content ───────────────────────────────────────────────────────────────────────────────

    if (content != null) {
        val scope = remember(engine, modelLoader, materialLoader, environmentLoader) {
            ARSceneScope(
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                environmentLoader = environmentLoader,
                _nodes = scopeChildNodes
            )
        }
        scope.content()
    }
}

// ── AR frame update helpers ───────────────────────────────────────────────────────────────────────

private fun onARFrame(
    engine: Engine,
    scene: Scene,
    view: View,
    cameraNode: ARCameraNode,
    cameraStream: ARCameraStream?,
    lightEstimator: LightEstimator?,
    mainLightNode: LightNode?,
    environment: Environment,
    arPlaneRenderer: PlaneRenderer,
    childNodes: List<Node>,
    prevTrackingFailureRef: AtomicReference<TrackingFailureReason?>,
    onTrackingFailureChangedRef: AtomicReference<((TrackingFailureReason?) -> Unit)?>,
    onSessionUpdatedRef: AtomicReference<((Session, Frame) -> Unit)?>,
    session: Session,
    frame: Frame
) {
    val camera = frame.camera
    val isCameraTracking = camera.isTracking

    cameraStream?.update(session, frame)
    cameraNode.update(session, frame)

    lightEstimator?.update(session, frame, cameraNode.camera)?.let { estimation ->
        mainLightNode?.let { light ->
            estimation.mainLightColor?.let { light.color = light.color * it }
            estimation.mainLightIntensity?.let { light.intensity = light.intensity * it }
            estimation.mainLightDirection?.let { light.lightDirection = it }
        }
        val indirectLight = environment.indirectLight
        IndirectLight.Builder().apply {
            estimation.irradiance?.let { irradiance(3, it) }
                ?: indirectLight?.irradianceTexture?.let { irradiance(it) }
            estimation.reflections?.let { reflections(it) }
                ?: indirectLight?.reflectionsTexture?.let { reflections(it) }
            indirectLight?.intensity?.let { intensity(it) }
            indirectLight?.getRotation(null)?.let { rotation(it) }
        }.build(engine).also { scene.indirectLight = it }
    }

    arPlaneRenderer.update(session, frame)

    childNodes.filterIsInstance<PoseNode>().forEach { it.update(session, frame) }

    val newTrackingFailure = if (!isCameraTracking) {
        camera.trackingFailureReason.takeIf { it != TrackingFailureReason.NONE }
    } else null

    if (prevTrackingFailureRef.get() != newTrackingFailure) {
        prevTrackingFailureRef.set(newTrackingFailure)
        onTrackingFailureChangedRef.get()?.invoke(newTrackingFailure)
    }

    onSessionUpdatedRef.get()?.invoke(session, frame)
}

// ── Remember helpers ──────────────────────────────────────────────────────────────────────────────

/**
 * Creates and remembers an [ARCameraNode] configured for AR rendering.
 *
 * Unlike the standard [rememberCameraNode], the AR camera node's transform and projection are
 * updated every frame by ARCore to match the physical device camera. Its exposure is set to
 * match ARCore's light estimation output so that virtual objects blend naturally with the
 * real world.
 *
 * Pass this to `ARScene(cameraNode = ...)` — it should not be used with a plain `Scene`.
 *
 * @param engine  The Filament [Engine] that owns the camera.
 * @param creator Factory for the AR camera node.
 * @return An [ARCameraNode] destroyed on disposal.
 */
@Composable
fun rememberARCameraNode(
    engine: Engine,
    creator: () -> ARCameraNode = {
        createARCameraNode(engine)
    }
) = rememberNode(creator)

/**
 * Creates and remembers an [ARCameraStream] for rendering the device camera feed.
 *
 * The camera stream owns the OpenGL external texture that receives frames from ARCore, and
 * the Filament renderable that draws that texture as the scene background. It also provides
 * depth occlusion when depth mode is enabled.
 *
 * Pass the result to `ARScene(cameraStream = ...)`. Without a camera stream the AR background
 * will be black instead of showing the live camera image.
 *
 * @param materialLoader The [MaterialLoader] used to create the camera background material.
 * @param creator        Factory for the camera stream.
 * @return An [ARCameraStream] destroyed on disposal.
 */
@Composable
fun rememberARCameraStream(
    materialLoader: MaterialLoader,
    creator: () -> ARCameraStream = {
        createARCameraStream(materialLoader)
    }
) = remember(materialLoader) { creator() }.also { cameraStream ->
    DisposableEffect(cameraStream) {
        onDispose {
            cameraStream.destroy()
        }
    }
}

/**
 * Creates and remembers an AR-optimised [Environment].
 *
 * Unlike the standard [rememberEnvironment], this produces an environment with no skybox
 * (transparent background so the camera feed shows through) and a neutral IBL that works
 * well before ARCore's light estimation has a chance to run.
 *
 * The environment's `IndirectLight` intensity is updated each frame by `ARScene` using
 * ARCore's `LightEstimator` when `ENVIRONMENTAL_HDR` mode is active.
 *
 * @param engine The Filament [Engine] that owns the IBL texture.
 * @param apply  Optional configuration block applied after creation.
 * @return An [Environment] destroyed on disposal.
 */
@Composable
fun rememberAREnvironment(
    engine: Engine,
    apply: Environment.() -> Unit = {}
) = remember(engine) {
    createAREnvironment(engine).apply(apply)
}.also { environment ->
    DisposableEffect(environment) {
        onDispose {
            engine.safeDestroyEnvironment(environment)
        }
    }
}

@Composable
private fun ARScenePreview(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.DarkGray)
    )
}
