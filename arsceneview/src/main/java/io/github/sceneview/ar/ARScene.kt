package io.github.sceneview.ar

import android.view.MotionEvent
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
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.ar.core.CameraConfig
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.InstantPlacementPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import io.github.sceneview.Scene
import io.github.sceneview.SceneView
import io.github.sceneview.ar.arcore.getUpdatedTrackables
import io.github.sceneview.ar.camera.ARCameraStream
import io.github.sceneview.ar.node.ARCameraNode
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberIndirectLight
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberRenderer
import io.github.sceneview.rememberScene
import io.github.sceneview.rememberSkybox
import io.github.sceneview.rememberView

@Composable
fun ARScene(
    modifier: Modifier = Modifier,
    activity: ComponentActivity? = LocalContext.current as? ComponentActivity,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    /**
     * List of the scene's nodes that can be linked to a `mutableStateOf<List<Node>>()`
     */
    childNodes: List<Node> = rememberNodes(),
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
     * Provide your own instance if you want to share [Node]s' scene between multiple views.
     */
    scene: Scene = rememberScene(engine),
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
     * A [Renderer] instance represents an operating system's window.
     *
     * Typically, applications create a [Renderer] per window. The [Renderer] generates drawing
     * commands for the render thread and manages frame latency.
     */
    renderer: Renderer = rememberRenderer(engine),
    /**
     * Represents a virtual camera, which determines the perspective through which the scene is
     * viewed.
     *
     * All other functionality in Node is supported. You can access the position and rotation of the
     * camera, assign a collision shape to it, or add children to it.
     */
    cameraNode: ARCameraNode = rememberARCamera(engine),
    /**
     * Always add a direct light source since it is required for shadowing.
     *
     * We highly recommend adding an [IndirectLight] as well.
     */
    mainLightNode: LightNode? = rememberMainLightNode(engine),
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
    indirectLight: IndirectLight? = rememberIndirectLight(engine),
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
    skybox: Skybox? = rememberSkybox(engine),
    /**
     * Invoked when an frame is processed.
     *
     * Registers a callback to be invoked when a valid Frame is processing.
     *
     * The callback to be invoked once per frame **immediately before the scene is updated.
     *
     * The callback will only be invoked if the Frame is considered as valid.
     */
    sessionFeatures: Set<Session.Feature> = setOf(),
    cameraConfig: ((Session) -> CameraConfig)? = null,
    planeRenderer: Boolean = true,
    /**
     * The [ARCameraStream] to render the camera texture.
     *
     * Use it to control if the occlusion should be enabled or disabled
     */
    cameraStream: ARCameraStream? = rememberCameraStream(engine, materialLoader),
    onSessionConfiguration: ((session: Session, Config) -> Unit)? = null,
    onSessionCreated: ((session: Session) -> Unit)? = null,
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
    onSessionUpdated: ((session: Session, frame: Frame) -> Unit)? = null,
    onSessionResumed: ((session: Session) -> Unit)? = null,
    /**
     * Invoked when an ARCore error occurred.
     *
     * Registers a callback to be invoked when the ARCore Session cannot be initialized because
     * ARCore is not available on the device or the camera permission has been denied.
     */
    onSessionFailed: ((exception: Exception) -> Unit)? = null,
    onSessionConfigChanged: ((session: Session, config: Config) -> Unit)? = null,
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
    onTapAR: ((
        /** The motion event that caused the tap. */
        motionEvent: MotionEvent,
        /** The ARCore hit result for the trackable that was tapped. */
        hitResult: HitResult
    ) -> Unit)? = null,
    onCreate: ((ARSceneView) -> Unit)? = null
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
                    scene,
                    view,
                    renderer,
                    cameraNode,
                    mainLightNode,
                    indirectLight,
                    skybox,
                    sessionFeatures,
                    cameraConfig,
                    cameraStream
                ).also {
                    onViewCreated?.invoke(it)
                }
            },
            update = { sceneView ->
                sceneView.childNodes = childNodes
                sceneView.setCameraNode(cameraNode)
                sceneView.mainLightNode = mainLightNode
                sceneView.indirectLight = indirectLight
                sceneView.skybox = skybox

                sceneView.planeRenderer.isEnabled = planeRenderer

                sceneView.onSessionConfiguration = onSessionConfiguration
                sceneView.onSessionCreated = onSessionCreated
                sceneView.onSessionUpdate = onSessionUpdate
                sceneView.onSessionResumed = onSessionResumed
                sceneView.onSessionFailed = onSessionFailed
                sceneView.onSessionConfigChanged = onSessionConfigChanged
                sceneView.onTap = onTap
                sceneView.onTapAR = onTapAR
            },
            onReset = {},
            onRelease = { sceneView -> sceneView.destroy() }
        )
    }
}

@Composable
fun rememberARCamera(
    engine: Engine,
    creator: () -> ARCameraNode = {
        ARSceneView.createCameraNode(engine)
    }
) = rememberNode(creator)

@Composable
fun rememberCameraStream(
    engine: Engine,
    materialLoader: MaterialLoader,
    creator: () -> ARCameraStream = {
        ARSceneView.createCameraStream(engine, materialLoader)
    }
) = remember(engine, creator).also { cameraStream ->
    DisposableEffect(cameraStream) {
        onDispose {
            cameraStream.destroy()
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