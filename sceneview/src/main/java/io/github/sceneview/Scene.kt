package io.github.sceneview

import android.content.Context
import android.opengl.EGLContext
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
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
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.utils.destroy

@Composable
fun Scene(
    modifier: Modifier = Modifier,
    activity: ComponentActivity? = LocalContext.current as? ComponentActivity,
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    /**
     * List of the scene's nodes that can be linked to a `mutableStateOf<List<Node>>()`
     */
    childNodes: List<Node> = listOf(),
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
    cameraNode: CameraNode = rememberCameraNode(engine),
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
    onFrame: ((frameTimeNanos: Long) -> Unit)? = null,
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
    onViewCreated: (SceneView.() -> Unit)? = null,
    onViewUpdated: (SceneView.() -> Unit)? = null
) {
    if (LocalInspectionMode.current) {
        ScenePreview(modifier)
    } else {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                SceneView(
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
                    skybox
                ).apply {
                    onViewCreated?.invoke(this)
                }
            },
            update = { sceneView ->
                sceneView.childNodes = childNodes
                sceneView.setCameraNode(cameraNode)
                sceneView.mainLightNode = mainLightNode
                sceneView.indirectLight = indirectLight
                sceneView.skybox = skybox
                sceneView.onFrame = onFrame
                sceneView.onTap = onTap

                onViewUpdated?.invoke(sceneView)
            },
            onReset = {},
            onRelease = { sceneView ->
                sceneView.destroy()
            }
        )
    }
}

@Composable
fun rememberEngine(
    eglContextCreator: () -> EGLContext = { SceneView.createEglContext() },
    engineCreator: (eglContext: EGLContext) -> Engine = { SceneView.createEngine(it) }
): Engine {
    val eglContext = remember(eglContextCreator)
    val engine = remember(eglContext) { engineCreator(eglContext) }
    DisposableEffect(eglContext, engine) {
        onDispose {
            engine.destroy()
            eglContext.destroy()
        }
    }
    return engine
}

@Composable
inline fun <reified T : Node> rememberNode(crossinline creator: () -> T) =
    remember(creator).also { node ->
        DisposableEffect(node) {
            onDispose {
                node.destroy()
            }
        }
    }

@Composable
fun rememberNodes(creator: MutableList<Node>.() -> Unit = {}) = remember {
    buildList(creator).toMutableStateList()
}.also { nodes ->
    DisposableEffect(nodes) {
        onDispose {
            nodes.forEach { it.destroy() }
        }
    }
}

@Composable
fun rememberScene(engine: Engine, creator: () -> Scene = { SceneView.createScene(engine) }) =
    remember(engine, creator).also { scene ->
        DisposableEffect(scene) {
            onDispose {
                engine.safeDestroyScene(scene)
            }
        }
    }

@Composable
fun rememberView(engine: Engine, creator: () -> View = { SceneView.createView(engine) }) =
    remember(engine, creator).also { view ->
        DisposableEffect(view) {
            onDispose {
                engine.safeDestroyView(view)
            }
        }
    }

@Composable
fun rememberRenderer(
    engine: Engine,
    creator: () -> Renderer = { SceneView.createRenderer(engine) }
) = remember(engine, creator).also { renderer ->
    DisposableEffect(renderer) {
        onDispose {
            engine.safeDestroyRenderer(renderer)
        }
    }
}

@Composable
fun rememberModelLoader(
    engine: Engine,
    context: Context = LocalContext.current,
    creator: () -> ModelLoader = {
        SceneView.createModelLoader(engine, context)
    }
) = remember(engine, context, creator).also { modelLoader ->
    DisposableEffect(modelLoader) {
        onDispose {
            engine.safeDestroyModelLoader(modelLoader)
        }
    }
}

@Composable
fun rememberMaterialLoader(
    engine: Engine,
    context: Context = LocalContext.current,
    creator: () -> MaterialLoader = {
        SceneView.createMaterialLoader(engine, context)
    }
) = remember(engine, context, creator).also { materialLoader ->
    DisposableEffect(materialLoader) {
        onDispose {
            engine.safeDestroyMaterialLoader(materialLoader)
        }
    }
}

@Composable
fun rememberCameraNode(
    engine: Engine,
    creator: () -> CameraNode = {
        SceneView.createCameraNode(engine)
    }
) = rememberNode(creator)

@Composable
fun rememberMainLightNode(
    engine: Engine,
    creator: () -> LightNode = {
        SceneView.createMainLightNode(engine)
    }
) = rememberNode(creator)

@Composable
fun rememberIndirectLight(
    engine: Engine,
    creator: () -> IndirectLight? = {
        SceneView.createIndirectLight(engine)
    }
) = remember(engine, creator)?.also { indirectLight ->
    DisposableEffect(indirectLight) {
        onDispose {
            engine.safeDestroyIndirectLight(indirectLight)
        }
    }
}

@Composable
fun rememberSkybox(
    engine: Engine,
    creator: () -> Skybox = {
        SceneView.createSkybox(engine)
    }
) = remember(engine, creator).also { skybox ->
    DisposableEffect(skybox) {
        onDispose {
            engine.safeDestroySkybox(skybox)
        }
    }
}

@Composable
private fun ScenePreview(modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.DarkGray)
    )
}