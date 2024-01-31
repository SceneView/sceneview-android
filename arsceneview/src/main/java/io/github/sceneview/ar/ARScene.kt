package io.github.sceneview.ar

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.View
import com.google.ar.core.Camera
import com.google.ar.core.CameraConfig
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.arcore.configure
import io.github.sceneview.ar.arcore.getUpdatedTrackables
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.environment.Environment
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberHitTestGestureDetector
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberView
import io.github.sceneview.safeDestroyEnvironment

@Composable
fun ARScene(
    modifier: Modifier = Modifier,
    /**
     * Provide your own instance if you want to share Filament resources between multiple views.
     */
    engine: Engine = rememberEngine(),
    /**
     * Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which is
     * a bundle of Filament textures, vertex buffers, index buffers, etc.
     *
     * A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and components.
     */
    modelLoader: ModelLoader = rememberModelLoader(engine),
    /**
     * A Filament Material defines the visual appearance of an object.
     *
     * Materials function as a templates from which [MaterialInstance]s can be spawned.
     */
    materialLoader: MaterialLoader = rememberMaterialLoader(engine),
    /**
     * Utility for decoding an HDR file or consuming KTX1 files and producing Filament textures,
     * IBLs, and sky boxes.
     *
     * KTX is a simple container format that makes it easy to bundle miplevels and cubemap faces
     * into a single file.
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
     * Provides details of a camera configuration such as size of the CPU image and GPU texture.
     *
     * @see Session.setCameraConfig
     */
    sessionCameraConfig: ((Session) -> CameraConfig)? = null,
    /**
     * Configures the session and verifies that the enabled features in the specified session config
     * are supported with the currently set camera config.
     *
     * @see Session.configure
     */
    sessionConfiguration: ((session: Session, Config) -> Unit)? = null,
    /**
     * Enable the plane renderer.
     */
    planeRenderer: Boolean = true,
    /**
     * The [ARCameraStream] to render the camera texture.
     *
     * Use it to control if the occlusion should be enabled or disabled
     */
    cameraStream: ARCameraStream? = rememberARCameraStream(materialLoader),
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
    view: View = rememberView(engine),
    /**
     * Controls whether the render target (SurfaceView) is opaque or not.
     * The render target is considered opaque by default.
     */
    isOpaque: Boolean = true,
    /**
     * A [Renderer] instance represents an operating system's window.
     *
     * Typically, applications create a [Renderer] per window. The [Renderer] generates drawing
     * commands for the render thread and manages frame latency.
     */
    renderer: Renderer = rememberRenderer(engine),
    /**
     * Provide your own instance if you want to share [Node]s' scene between multiple views.
     */
    scene: Scene = rememberScene(engine),
    /**
     * Defines the lighting environment and the skybox of the scene.
     *
     * Environments are usually captured as high-resolution HDR equirectangular images and processed
     * by the cmgen tool to generate the data needed by IndirectLight.
     *
     * You can also process an hdr at runtime but this is more consuming.
     *
     * - Currently IndirectLight is intended to be used for "distant probes", that is, to represent
     * global illumination from a distant (i.e. at infinity) environment, such as the sky or distant
     * mountains.
     * Only a single IndirectLight can be used in a Scene. This limitation will be lifted in the
     * future.
     *
     * - When added to a Scene, the Skybox fills all untouched pixels.
     *
     * @see [EnvironmentLoader]
     */
    environment: Environment = rememberAREnvironment(engine),
    /**
     * Always add a direct light source since it is required for shadowing.
     *
     * We highly recommend adding an [IndirectLight] as well.
     */
    mainLightNode: LightNode? = rememberMainLightNode(engine),
    cameraNode: ARCameraNode = rememberARCameraNode(engine),
    /**
     * List of the scene's nodes that can be linked to a `mutableStateOf<List<Node>>()`
     */
    childNodes: List<Node> = rememberNodes(),
    /**
     * Physics system to handle collision between nodes, hit testing on a nodes,...
     */
    collisionSystem: CollisionSystem = rememberCollisionSystem(view),
    /**
     * Detects various gestures and events.
     *
     * The gesture listener callback will notify users when a particular motion event has occurred.
     * Responds to Android touch events with listeners.
     */
    gestureDetector: GestureDetector = rememberHitTestGestureDetector(
        LocalContext.current,
        collisionSystem
    ),
    /**
     * The session is ready to be accessed.
     */
    onSessionCreated: ((session: Session) -> Unit)? = null,
    /**
     * The session has been resumed
     */
    onSessionResumed: ((session: Session) -> Unit)? = null,
    /**
     * The session has been paused
     */
    onSessionPaused: ((session: Session) -> Unit)? = null,
    /**
     * Invoked when an ARCore error occurred.
     *
     * Registers a callback to be invoked when the ARCore Session cannot be initialized because
     * ARCore is not available on the device or the camera permission has been denied.
     */
    onSessionFailed: ((exception: Exception) -> Unit)? = null,
    /**
     * Updates of the state of the ARCore system.
     *
     * This includes: receiving a new camera frame, updating the location of the device, updating
     * the location of tracking anchors, updating detected planes, etc.
     *
     * This call may update the pose of all created anchors and detected planes. The set of updated
     * objects is accessible through [Frame.getUpdatedTrackables].
     *
     * Invoked once per [Frame] immediately before the Scene is updated.
     */
    onSessionUpdated: ((session: Session, frame: Frame) -> Unit)? = null,
    /**
     * Listen for camera tracking failure.
     *
     * The reason that [Camera.getTrackingState] is [TrackingState.PAUSED] or `null` if it is
     * [TrackingState.TRACKING]
     */
    onTrackingFailureChanged: ((trackingFailureReason: TrackingFailureReason?) -> Unit)? = null,
    /**
     * Represents a virtual camera, which determines the perspective through which the scene is
     * viewed.
     *
     * All other functionality in Node is supported. You can access the position and rotation of the
     * camera, assign a collision shape to it, or add children to it.
     */
    /**
     * The listener invoked for all the gesture detector callbacks.
     */
    onGestureListener: GestureDetector.OnGestureListener? = rememberOnGestureListener(),
    activity: ComponentActivity? = LocalContext.current as? ComponentActivity,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    onViewUpdated: (ARSceneView.() -> Unit)? = null,
    onViewCreated: (ARSceneView.() -> Unit)? = null
) {
    if (LocalInspectionMode.current) {
        ARScenePreview(modifier)
    } else {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                ARSceneView(
                    context,
                    null,
                    0,
                    0,
                    activity,
                    lifecycle,
                    engine,
                    modelLoader,
                    materialLoader,
                    environmentLoader,
                    scene,
                    view,
                    renderer,
                    cameraNode,
                    mainLightNode,
                    environment,
                    isOpaque,
                    collisionSystem,
                    gestureDetector,
                    onGestureListener,
                    cameraStream,
                    sessionFeatures,
                    sessionCameraConfig,
                    sessionConfiguration,
                    onSessionCreated,
                    onSessionResumed,
                    onSessionPaused,
                    onSessionFailed,
                    onTrackingFailureChanged,
                    onSessionUpdated
                ).also {
                    onViewCreated?.invoke(it)
                }
            },
            update = { sceneView ->
                sceneView.childNodes = childNodes
                sceneView.scene = scene
                sceneView.setCameraNode(cameraNode)
                sceneView.mainLightNode = mainLightNode
                sceneView.environment = environment
                sceneView.gestureDetector = gestureDetector
                sceneView.onGestureListener = onGestureListener

                sceneView.planeRenderer.isEnabled = planeRenderer

                sceneView.onSessionCreated = onSessionCreated
                sceneView.onSessionResumed = onSessionResumed
                sceneView.onSessionPaused = onSessionPaused
                sceneView.onSessionFailed = onSessionFailed
                sceneView.onTrackingFailureChanged = onTrackingFailureChanged
                sceneView.onSessionUpdated = onSessionUpdated

                onViewUpdated?.invoke(sceneView)
            },
            onReset = {},
            onRelease = { sceneView -> sceneView.destroy() }
        )
    }
}

@Composable
fun rememberARCameraNode(
    engine: Engine,
    creator: () -> ARCameraNode = {
        ARSceneView.createARCameraNode(engine)
    }
) = rememberNode(creator)

@Composable
fun rememberARCameraStream(
    materialLoader: MaterialLoader,
    creator: () -> ARCameraStream = {
        ARSceneView.createARCameraStream(materialLoader)
    }
) = remember(materialLoader, creator).also { cameraStream ->
    DisposableEffect(cameraStream) {
        onDispose {
            cameraStream.destroy()
        }
    }
}

@Composable
fun rememberAREnvironment(
    engine: Engine,
    apply: Environment.() -> Unit = {}
) = remember(engine) {
    ARSceneView.createAREnvironment(engine).apply(apply)
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