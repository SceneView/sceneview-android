package io.github.sceneview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.*
import com.google.android.filament.View.AntiAliasing
import com.google.android.filament.View.QualityLevel
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.*
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.destroyGeometry
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.gesture.NodesManipulator
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.loaders.loadIndirectLight
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.managers.WindowViewManager
import io.github.sceneview.managers.destroyLight
import io.github.sceneview.managers.destroyRenderable
import io.github.sceneview.material.destroyMaterial
import io.github.sceneview.material.destroyMaterialInstance
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.model.Model
import io.github.sceneview.nodes.CameraNode
import io.github.sceneview.nodes.LightNode
import io.github.sceneview.nodes.ModelNode
import io.github.sceneview.nodes.Node
import io.github.sceneview.scene.*
import io.github.sceneview.texture.ViewStream
import io.github.sceneview.texture.destroyStream
import io.github.sceneview.texture.destroyTexture
import io.github.sceneview.texture.destroyViewStream
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.IBLPrefilter
import io.github.sceneview.utils.SurfaceMirorer
import io.github.sceneview.view.FilamentView
import io.github.sceneview.view.viewportToWorld
import io.github.sceneview.view.worldToViewport
import kotlinx.coroutines.Job

typealias Entity = Int
typealias EntityInstance = Int

typealias CameraGestureDetector = com.google.android.filament.utils.GestureDetector
typealias CameraManipulator = Manipulator

/**
 * A SurfaceView that manage rendering and interactions with the 3D scene
 *
 * Maintains the scene graph, a hierarchical organization of a scene's content.
 * A scene can have zero or more child nodes and each node can have zero or more child nodes.
 * The Scene also provides hit testing, a way to detect which node is touched by a MotionEvent or
 * Ray.
 */
open class SceneView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
    /**
     * Provide your own instance if you want to share Filament resources between multiple views.
     */
    val sharedEngine: Engine? = null,
    /**
     * Provide your own instance if you want to share [Node]s instances between multiple views.
     */
    val sharedNodeManager: NodeManager? = null,
    /**
     * Provide your own instance if you want to share [Node]s selection between multiple views.
     */
    sharedNodesManipulator: NodesManipulator? = null,
    /**
     * Provided by Filament to manage SurfaceView and SurfaceTexture.
     *
     * To choose a specific rendering resolution, add the following line:
     * `uiHelper.setDesiredSize(1280, 720)`
     */
    val uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK),
    cameraNode: CameraNode? = null,
    cameraManipulator: ((width: Int, height: Int) -> CameraManipulator)? = { width, height ->
        Manipulator.Builder()
            .targetPosition(DEFAULT_MODEL_POSITION)
            .viewport(width, height)
            .build(Manipulator.Mode.ORBIT)
    }
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * @param depth The value of the depth buffer at the picking query location
     */
    data class PickingResult internal constructor(
        val node: Node?,
        val worldPosition: Position,
        val depth: Float
    )

    /**
     * DisplayHelper is provided by Filament to manage the display
     */
    var displayHelper: DisplayHelper

    /**
     * An Engine instance main function is to keep track of all resources created by the user and
     * manage the rendering thread as well as the hardware renderer
     */
    lateinit var engine: Engine
        private set

    /**
     * Lens's focal length in millimeters
     *
     * @see [Camera.setLensProjection]
     */
    var cameraFocalLength = 28f
        set(value) {
            field = value
            updateCameraProjection()
        }

    lateinit var nodeManager: NodeManager
        private set

    /**
     * Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which
     * is a bundle of Filament textures, vertex buffers, index buffers, etc.
     * A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and
     * components.
     */
    lateinit var modelLoader: ModelLoader

    /**
     * A Filament Material defines the visual appearance of an object
     *
     * Materials function as a templates from which [MaterialInstance]s can be spawned.
     */
    lateinit var materialLoader: MaterialLoader

    /**
     * IBLPrefilter creates and initializes GPU state common to all environment map filters
     *
     * @see IBLPrefilterContext
     */
    lateinit var iblPrefilter: IBLPrefilter

    /**
     * A transform component gives an entity a position and orientation in space in the coordinate
     * space of its parent transform
     *
     * The [TransformManager] takes care of computing the world-space transform of each component
     * (i.e. its transform relative to the root).
     */
    val transformManager get() = engine.transformManager

    /**
     * Factory and manager for *renderables*, which are entities that can be drawn
     */
    val renderableManager get() = engine.renderableManager

    /**
     * LightManager allows you to create a light source in the scene, such as a sun or street lights
     */
    val lightManager get() = engine.lightManager

    /**
     * A [Scene] is a flat container of [RenderableManager] and [LightManager] components
     */
    lateinit var scene: Scene
        private set

    /**
     * Encompasses all the state needed for rendering a [Scene]
     */
    lateinit var filamentView: FilamentView
        private set

    /**
     * A [Renderer] instance represents an operating system's window.
     */
    lateinit var renderer: Renderer
        private set

    lateinit var surfaceMirorer: SurfaceMirorer
        private set

    /**
     * Helper that enables camera interaction similar to sketchfab or Google Maps
     */
    var cameraManipulator: CameraManipulator? = null
        private set

    lateinit var nodesManipulator: NodesManipulator
        private set

    /**
     * Responds to Android touch events with listeners and/or camera manipulator
     */
    lateinit var gestureDetector: GestureDetector

    /**
     * Get the world space size from the screen space size
     */
    val worldSize: Size get() = viewToWorld(width.toFloat(), height.toFloat())

    /**
     * Calculated number of pixels within a world space unit
     */
    val pxPerUnit: Size get() = Size(x = width.toFloat(), y = height.toFloat()) / worldSize

    /**
     * Associates the specified Camera with this View
     *
     * A Camera can be associated with several View instances.
     * To remove an existing association, simply pass null.
     * The View does not take ownership of the Scene pointer.
     */
    var cameraNode: CameraNode? = null
        set(value) {
            field = value
            filamentView.camera = value?.camera
        }

    /**
     * Always add a direct light source since it is required for shadowing.
     *
     * We highly recommend adding an [IndirectLight] as well.
     */
    var lightNode: LightNode? = null
        set(value) {
            field?.let { removeChildNode(it) }
            field = value
            value?.let { addChildNode(it) }
        }

    /**
     * IndirectLight is used to simulate environment lighting
     *
     * Environment lighting has a two components:
     * - irradiance
     * - reflections (specular component)
     *
     * Indirect light are usually captured as high-resolution HDR equirectangular images and
     * processed by the cmgen tool to generate the data needed.
     *
     * You can also process an hdr at runtime but this is more consuming
     *
     * Currently IndirectLight is intended to be used for "distant probes", that is, to represent
     * global illumination from a distant (i.e. at infinity) environment, such as the sky or distant
     * mountains.
     * Only a single IndirectLight can be used in a Scene. This limitation will be lifted in the
     * future.
     *
     * @see IndirectLight
     * @see Scene.setIndirectLight
     * @see KTX1Loader.loadIndirectLight
     * @see HDRLoader.loadIndirectLight
     */
    var indirectLight: IndirectLight?
        get() = scene.indirectLight
        set(value) {
            scene.indirectLight = value
        }

    /**
     * Image Based Light Spherical harmonics from the content of a KTX file.
     * The resulting array of 9 * 3 floats, or null on failure.
     */
    var sphericalHarmonics: FloatArray? = null

    /**
     * The Skybox is drawn last and covers all pixels not touched by geometry
     *
     * The skybox texture is rendered as though it were an infinitely large cube with the camera
     * inside it. This means that the cubemap which is mapped onto the cube's exterior
     * will appear mirrored. This follows the OpenGL conventions.
     *
     * When added to a [SceneView], the `Skybox` fills all untouched pixels.
     *
     * Set to null to unset the Skybox.
     *
     * @see HDRLoader
     * @see KTX1Loader
     * @see Scene.setSkybox
     */
    var skybox: Skybox?
        get() = scene.skybox
        set(value) {
            scene.skybox = value
        }

    /**
     * List of direct child entities that are within the [Scene] and has a node component within
     * this [SceneView.nodeManager]
     *
     * @see allChildNodes
     */
    val childNodes: List<Node> get() = allChildNodes.filter { it.parentNode == null }

    /**
     * Flat list of all children entities within the hierarchy that are within the [Scene] and
     * has a node component within this [SceneView.nodeManager]
     *
     * @see Scene.hasEntity
     * @see NodeManager.getNode
     */
    val allChildNodes: List<Node>
        get() = nodeManager.entities.filter { scene.hasEntity(it) }
            .mapNotNull { nodeManager.getNode(it) }

    /**
     * Inverts the winding order of front faces.
     *
     * By default front faces use a counter-clockwise winding order. When the winding order is
     * inverted, front faces are faces with a clockwise winding order.
     *
     * Changing the winding order will directly affect the culling mode in materials
     * (see [Material.getCullingMode]).
     *
     * Inverting the winding order of front faces is useful when rendering mirrored reflections
     * (water, mirror surfaces, front camera in AR, etc.).
     *
     * True to invert front faces, false otherwise.
     */
    var isFrontFaceWindingInverted: Boolean
        get() = filamentView.isFrontFaceWindingInverted
        set(value) {
            filamentView.isFrontFaceWindingInverted = value
        }

    /**
     * Invoked when an frame is processed
     *
     * Registers a callback to be invoked when a valid Frame is processing.
     */
    var onFrameListener: ((frameTime: FrameTime) -> Unit)? = null
    var onTapListener: ((e: MotionEvent, pickingResult: PickingResult) -> Unit)? = null

    /**
     * Choreographer is used to schedule new frames
     */
    private val choreographer: Choreographer

    /**
     * Performs the rendering and schedules new frames
     */
    private val frameScheduler = FrameCallback()

    /**
     * A swap chain is Filament's representation of a surface
     */
    open var swapChain: SwapChain? = null

    internal lateinit var windowViewManager: WindowViewManager

    private lateinit var colorGrading: ColorGrading

    private lateinit var selectionModel: Model

    private val pickingHandler by lazy { Handler(Looper.getMainLooper()) }

    private var currentFrameTime: FrameTime = FrameTime(0)

    private var lastTouchEvent: MotionEvent? = null

    val loadingJobs = mutableListOf<Job>()
    internal val cameras = mutableListOf<Camera>()
    internal val indirectLights = mutableListOf<IndirectLight>()
    internal val skyboxes = mutableListOf<Skybox>()
    internal val geometries = mutableListOf<Geometry>()
    internal val renderables = mutableListOf<Entity>()
    internal val lights = mutableListOf<Entity>()
    internal val materials = mutableListOf<Material>()
    internal val materialInstances = mutableListOf<MaterialInstance>()
    internal val textures = mutableListOf<Texture>()
    internal val streams = mutableListOf<Stream>()
    internal val viewStreams = mutableListOf<ViewStream>()

    init {
        displayHelper = DisplayHelper(context)
        setupWindowViewManager()

        choreographer = Choreographer.getInstance()

        val backgroundColor = (background as? ColorDrawable)?.let { Color(it) }
            ?: Color(android.graphics.Color.BLACK)

        if (!isInEditMode) {
            // Setup Filament
            engine = sharedEngine ?: Engine.create()
            nodeManager = sharedNodeManager ?: NodeManager(engine)
            modelLoader = ModelLoader(engine, context)
            materialLoader = MaterialLoader(engine, context)
            iblPrefilter = IBLPrefilter(engine)

            renderer = engine.createRenderer()
            renderer.clearOptions = renderer.clearOptions.apply {
                clear = !uiHelper.isOpaque
                if (backgroundColor.a == 1.0f) {
                    clearColor = backgroundColor.toFloatArray()
                }
            }

            scene = engine.createScene()
            filamentView = engine.createView().apply {
                // on mobile, better use lower quality color buffer
                renderQuality = renderQuality.apply {
                    hdrColorBuffer = QualityLevel.MEDIUM
                }
                // dynamic resolution often helps a lot
                dynamicResolutionOptions = dynamicResolutionOptions.apply {
                    enabled = true
                    quality = QualityLevel.MEDIUM
                }
                // MSAA is needed with dynamic resolution MEDIUM
                multiSampleAntiAliasingOptions = multiSampleAntiAliasingOptions.apply {
                    enabled = true
                }
                // FXAA is pretty cheap and helps a lot
                antiAliasing = AntiAliasing.FXAA
                // ambient occlusion is the cheapest effect that adds a lot of quality
                ambientOcclusionOptions = ambientOcclusionOptions.apply {
                    enabled = true
                }
                // bloom is pretty expensive but adds a fair amount of realism
                bloomOptions = bloomOptions.apply {
                    enabled = true
                }
            }
            // Change the ToneMapper to FILMIC to avoid some over saturated colors, for example
            // material orange 500.
//            colorGrading = ColorGrading.Builder()
//                .toneMapping(ColorGrading.ToneMapping.FILMIC)
//                .build(engine)
//            filamentView.colorGrading = colorGrading

            filamentView.scene = scene

            surfaceMirorer = SurfaceMirorer(engine, filamentView, renderer)

            this.cameraNode = cameraNode ?: CameraNode(engine, nodeManager) {
                // Set the exposure on the camera, this exposure follows the sunny f/16 rule
                // Since we define a light that has the same intensity as the sun, it guarantees a
                // proper exposure
                setExposure(16.0f, 1.0f / 125.0f, 100.0f)
            }

            this.cameraManipulator = cameraManipulator?.invoke(width, height)

            selectionModel = modelLoader.createModel("models/selection.glb")!!
            nodesManipulator = sharedNodesManipulator ?: NodesManipulator(engine) { nodeSize ->
                ModelNode(this@SceneView, modelLoader.createInstance(selectionModel)!!).apply {
                    isSelectable = false
                    size = size.apply {
                        xy = nodeSize.xy
                    }
                }
            }

            setupGestureDetector()

            // Taken from Filament ModelViewer
            val (r, g, b) = Colors.cct(6_500.0f)
            lightNode = LightNode(engine, nodeManager, LightManager.Type.DIRECTIONAL) {
                color(r, g, b)
                intensity(100_000.0f)
                direction(0.0f, -1.0f, 0.0f)
                castShadows(true)
            }
        }
        setupSurfaceView(backgroundColor)
    }

    private fun setupWindowViewManager() {
        windowViewManager = WindowViewManager(this)
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, cameraManipulator, nodesManipulator)
        gestureDetector.onSingleTapConfirmedListeners += { e, pickingResult ->
            onTapListener?.invoke(e, pickingResult)
        }
    }

    private fun setupSurfaceView(backgroundColor: Color) {
        // Setup SurfaceView
        uiHelper.renderCallback = SurfaceCallback()
        // Must be called before attachTo
        uiHelper.isOpaque = backgroundColor.a == 1.0f
        uiHelper.attachTo(this)
    }

    open fun onFrame(frameTime: FrameTime) {
        if (!uiHelper.isReadyToRender) {
            return
        }

        // Allow the resource loader to finalize textures that have become ready.
        modelLoader.onFrame(frameTime)

        // Extract the camera basis from the helper and push it to the Filament camera.
        cameraManipulator?.getLookAt()?.let { (eye, target, upward) ->
            cameraNode?.lookAt(eye, target, upward)
        }

        // Update child nodes
        allChildNodes.forEach {
            it.onFrame(frameTime)
        }

        // Call listeners
        onFrameListener?.invoke(frameTime)

        // Render the scene, unless the renderer wants to skip the frame
        // If beginFrame() returns false we skip the frame
        // This means we are sending frames too quickly to the GPU
        if (renderer.beginFrame(swapChain!!, frameTime.nanoseconds)) {
            renderer.render(filamentView)
            renderer.endFrame()
        }

        surfaceMirorer.onFrame()
    }

    /**
     * Add direct child [Node] and all its descendant child nodes to the [scene]
     *
     * Any future child add or remove inside the node hierarchy will be automatically applied to the
     * [scene].
     */
    fun addChildNode(node: Node) {
        (listOf(node) + node.allChildNodes).forEach { childNode ->
            childNode.onChildAdded += ::addChildNode
            childNode.onChildRemoved += ::removeChildNode
            scene.addEntities(childNode.sceneEntities.toIntArray())
        }
    }

    /**
     * Add multiple direct child [Node]s and all its descendant child nodes to the [scene]
     *
     * @see addChildNode
     */
    fun addChildNodes(nodes: List<Node>) = nodes.forEach { addChildNode(it) }

    /**
     * Remove a direct child [Node] and all its descendant child nodes from the [scene]
     */
    fun removeChildNode(node: Node) {
        (listOf(node) + node.allChildNodes).forEach { childNode ->
            childNode.onChildAdded -= ::addChildNode
            childNode.onChildRemoved -= ::removeChildNode
            scene.removeEntity(childNode.entity)
        }
    }

    /**
     * Picks a node at given coordinates
     *
     * Filament picking works with a small delay, therefore, a callback is used.
     * If no node is picked, the callback is invoked with a `null` value instead of a node.
     *
     * @param x The x coordinate within the `SceneView`.
     * @param y The y coordinate within the `SceneView`.
     * @param onResult Called when picking completes.
     */
    fun pickNode(x: Int, y: Int, onResult: (pickingResult: PickingResult) -> Unit) {
        // Invert the y coordinate since its origin is at the bottom
        //TODO: Should we remove the -1?
        val invertedY = height - 1 - y
        filamentView.pick(x, invertedY, pickingHandler) { result ->
            onResult(
                PickingResult(
                    node = nodeManager.getNode(result.renderable),
                    worldPosition = result.fragCoords.let { (x, y, z) ->
                        filamentView.viewportToWorld(x, y, z)
                    },
                    depth = result.depth
                )
            )
        }
    }

    /**
     * Picks a node at given motion event
     *
     * @see pickNode
     */
    fun pickNode(e: MotionEvent, onResult: (pickingResult: PickingResult) -> Unit) =
        pickNode(e.x.toInt(), e.y.toInt(), onResult)

    fun startMirroring(mediaRecorder: MediaRecorder) =
        surfaceMirorer.startMirroring(mediaRecorder.surface, width = width, height = height)

    fun stopMirroring(mediaRecorder: MediaRecorder) =
        surfaceMirorer.stopMirroring(mediaRecorder.surface)

    /**
     * Get a world space position from a screen space position
     *
     * @see viewportToWorld
     */
    fun viewToWorld(x: Float, y: Float, z: Float = 1.0f) = filamentView.viewportToWorld(
        // Invert Y because SceneView Y points down and Filament points up
        x = x, y = height - y, z = z
    )

    /**
     * Get a screen space position from a world space position
     *
     * @see worldToViewport
     */
    fun worldToView(worldPosition: Position) = filamentView.worldToViewport(worldPosition).apply {
        // Invert Y because Filament points up and screen Y points down.
        y = height - y
    }

    open fun resume() {
        windowViewManager.resume(this)
        choreographer.postFrameCallback(frameScheduler)
    }

    open fun pause() {
        choreographer.removeFrameCallback(frameScheduler)
        windowViewManager.pause()
    }

    open fun destroy() {
        loadingJobs.forEach { it.cancel() }
        loadingJobs.clear()

        // Stop any pending frame
        choreographer.removeFrameCallback(frameScheduler)

        if (nodeManager != sharedNodeManager) {
            // Destroys all the created nodes
            nodeManager.destroy()
        }

        // Always detach the surface before destroying the engine
        uiHelper.detach()

        windowViewManager.destroy()

        cameras.toList().forEach { runCatching { destroyCamera(it) } }
        cameras.clear()

        indirectLights.toList().forEach { runCatching { destroyIndirectLight(it) } }
        indirectLights.clear()

        skyboxes.toList().forEach { runCatching { destroySkybox(it) } }
        skyboxes.clear()

        geometries.toList().forEach { runCatching { destroyGeometry(it) } }
        geometries.clear()

        renderables.toList().forEach { runCatching { destroyRenderable(it) } }
        renderables.clear()

        lights.toList().forEach { runCatching { destroyLight(it) } }
        lights.clear()

        materialInstances.toList().forEach { runCatching { destroyMaterialInstance(it) } }
        materialInstances.clear()

        materials.toList().forEach { runCatching { destroyMaterial(it) } }
        materials.clear()

        textures.toList().forEach { runCatching { destroyTexture(it) } }
        textures.clear()

        streams.toList().forEach { runCatching { destroyStream(it) } }
        streams.clear()

        viewStreams.toList().forEach { runCatching { destroyViewStream(it) } }
        viewStreams.clear()

        modelLoader.destroy()
        materialLoader.destroy()
        iblPrefilter.destroy()

        engine.destroyRenderer(renderer)
        engine.destroyView(filamentView)
        engine.destroyColorGrading(colorGrading)
        engine.destroyScene(scene)

        cameraNode?.destroy()

        // Use runCatching because they should normally already been destroyed by the lifecycle and
        // Filament will throw an Exception when destroying them twice.
        lightNode?.destroy()

        indirectLight?.let { engine.destroyIndirectLight(it) }
        skybox?.let { engine.destroySkybox(it) }

        if (engine != sharedEngine) {
            engine.destroy()
        }
    }

    /**
     * Pass your lifecycle to the View in order to automatically handle the lifecycle states
     *
     * You can also handle it manually by calling the corresponding functions
     */
    fun setLifecycle(lifecycle: Lifecycle) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                resume()
            }

            override fun onPause(owner: LifecycleOwner) {
                pause()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                destroy()
            }
        })
    }

    open fun updateCameraProjection() {
        val width = filamentView.viewport.width
        val height = filamentView.viewport.height
        val aspect = width.toDouble() / height.toDouble()
        cameraNode?.setLensProjection(
            cameraFocalLength.toDouble(),
            aspect,
            NEAR_PLANE,
            FAR_PLANE
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        lastTouchEvent = event
        gestureDetector.onTouchEvent(event)

        return true
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            displayHelper.attach(renderer, display)
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            filamentView.viewport = Viewport(0, 0, width, height)
            cameraManipulator?.setViewport(width, height)
            windowViewManager.setSize(width, height)
            updateCameraProjection()
        }
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()

        /**
         * Callback that occurs for each display frame. Updates the scene and reposts itself to be
         * called by the choreographer on the next frame.
         */
        override fun doFrame(frameTimeNanos: Long) {
            // Always post the callback for the next frame.
            choreographer.postFrameCallback(this)

            currentFrameTime = FrameTime(frameTimeNanos, currentFrameTime.nanoseconds)
            onFrame(currentFrameTime)
        }
    }

    companion object {
        init {
            // Prevent isInEditMode Filament issue
            runCatching {
                // Load the library for the utility layer, which in turn loads gltfio and the
                // Filament core.
                Utils.init()
            }
        }

        const val NEAR_PLANE = 0.05 // 5 cm
        const val FAR_PLANE = 1000.0 // 1 km
        val DEFAULT_MODEL_POSITION = Position(0.0f, 0.0f, -4.0f)
    }
}
