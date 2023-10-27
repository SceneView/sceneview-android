package io.github.sceneview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.opengl.EGLContext
import android.util.AttributeSet
import android.view.*
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.*
import com.google.android.filament.*
import com.google.android.filament.View
import com.google.android.filament.View.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.Manipulator
import com.google.android.filament.utils.Utils
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.IBLPrefilter
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.gesture.NodeMotionEvent
import io.github.sceneview.gesture.orbitHomePosition
import io.github.sceneview.gesture.targetPosition
import io.github.sceneview.gesture.transform
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.loaders.loadEnvironment
import io.github.sceneview.managers.color
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import io.github.sceneview.math.colorOf
import io.github.sceneview.math.toColor
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.utils.*
import com.google.android.filament.utils.KTX1Loader as KTXLoader

typealias Entity = Int
typealias EntityInstance = Int
typealias FilamentEntity = com.google.android.filament.Entity
typealias FilamentEntityInstance = com.google.android.filament.EntityInstance

/**
 * A SurfaceView that manages rendering and interactions with the 3D scene.
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
    sharedCameraNode: CameraNode? = null,
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
    sharedSkybox: Skybox? = null
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes),
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    private var defaultEngine: Engine? = null
    val engine = sharedEngine ?: createEglContext().let {
        defaultEglContext = it
        createEngine(it).also { defaultEngine = it }
    }
    private var defaultScene: Scene? = null
    val scene = sharedScene ?: createScene(engine).also { defaultScene = it }
    private var defaultView: View? = null
    val view = sharedView ?: createView(engine).also { defaultView = it }
    private var defaultRenderer: Renderer? = null
    val renderer = sharedRenderer ?: createRenderer(engine).also { defaultRenderer = it }
    private var defaultModelLoader: ModelLoader? = null
    val modelLoader = sharedModelLoader ?: createModelLoader(engine, context).also {
        defaultModelLoader = it
    }
    private var defaultMaterialLoader: MaterialLoader? = null
    val materialLoader = sharedMaterialLoader ?: createMaterialLoader(engine, context).also {
        defaultMaterialLoader = it
    }
    val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        renderCallback = SurfaceCallback()
        attachTo(this@SceneView)
    }
    val iblPrefilter = IBLPrefilter(engine)

    private var defaultCameraNode: CameraNode? = null
    protected var _cameraNode: CameraNode? = null

    /**
     * Represents a virtual camera, which determines the perspective through which the scene is
     * viewed.
     *
     * All other functionality in Node is supported. You can access the position and rotation of the
     * camera, assign a collision shape to it, or add children to it. Disabling the camera turns off
     * rendering.
     */
    open val cameraNode: CameraNode get() = _cameraNode!!

    private var defaultMainLight: LightNode? = null
    private var _mainLightNode: LightNode? =
        sharedMainLightNode ?: createMainLightNode(engine).also { defaultMainLight = it }

    /**
     * Always add a direct light source since it is required for shadowing.
     *
     * We highly recommend adding an [IndirectLight] as well.
     */
    open var mainLightNode: LightNode?
        get() = _mainLightNode
        set(value) {
            if (_mainLightNode != value) {
                _mainLightNode?.let { removeNode(it) }
                _mainLightNode = value
                value?.let { addNode(it) }
            }
        }

    private var defaultIndirectLight: IndirectLight? = null

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
    open var indirectLight: IndirectLight?
        get() = scene.indirectLight
        set(value) {
            if (scene.indirectLight != value) {
                scene.indirectLight = value
            }
        }

    private var defaultSkybox: Skybox? = null

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
    var skybox: Skybox?
        get() = scene.skybox
        set(value) {
            if (scene.skybox != value) {
                scene.skybox = value
            }
        }

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
     * @see [KTXLoader.loadEnvironment]
     * @see [HDRLoader.loadEnvironment]
     */
    open var environment: Environment? = null
        set(value) {
            field = value
            indirectLight = value?.indirectLight
            skybox = value?.skybox
        }

    var childNodes = listOf<Node>()
        set(value) {
            if (field != value) {
                (field - value.toSet()).forEach { removeNode(it) }
                (value - field.toSet()).forEach { addNode(it) }
                field = value
            }
        }

    /**
     * Inverts winding for front face rendering.
     *
     * Inverts the winding order of front faces. By default front faces use a counter-clockwise
     * winding order. When the winding order is inverted, front faces are faces with a clockwise
     * winding order.
     *
     * Changing the winding order will directly affect the culling mode in materials
     * (see [com.google.android.filament.Material.getCullingMode]).
     *
     * Inverting the winding order of front faces is useful when rendering mirrored reflections
     * (water, mirror surfaces, front camera in AR, etc.).
     *
     * `true` to invert front faces, false otherwise.
     */
    var isFrontFaceWindingInverted: Boolean
        get() = view.isFrontFaceWindingInverted
        set(value) {
            view.isFrontFaceWindingInverted = value
        }

    /**
     * Invoked when an frame is processed.
     *
     * Registers a callback to be invoked when a valid Frame is processing.
     *
     * The callback to be invoked once per frame **immediately before the scene is updated.
     *
     * The callback will only be invoked if the Frame is considered as valid.
     */
    var onFrame: ((frameTimeNanos: Long) -> Unit)? = null

    /**
     * Invoked when the `SceneView` is tapped.
     *
     * Only nodes with renderables or their parent nodes can be tapped since Filament picking is
     * used to find a touched node. The ID of the Filament renderable can be used to determine what
     * part of a model is tapped.
     */

    var onTap: ((
        /** The motion event that caused the tap. **/
        motionEvent: MotionEvent,
        /** The node that was tapped or `null`. **/
        node: Node?
    ) -> Unit)? = null

    /**
     * Physics system to handle collision between nodes, hit testing on a model,...
     */
    val collisionSystem = CollisionSystem()

    val gestureDetector by lazy {
        GestureDetector(
            context = context,
            cameraNode = cameraNode,
            listener = this
        )
    }

    protected open val activity: ComponentActivity? = sharedActivity
        get() = field ?: try {
            findFragment<Fragment>().requireActivity()
        } catch (e: Exception) {
            context as? ComponentActivity
        }

    protected open val cameraGestureDetector: CameraGestureDetector? by lazy {
        CameraGestureDetector(this, CameraGestureListener())
    }

    // TODO: Ask Filament to add a startPosition and startRotation in order to handle previous
    //  possible programmatic camera transforms.
    //  Better would be that we don't have to create a new Manipulator and just update  it when
    //  the camera is programmatically updated so it don't come back to  initial position.
    //  Return field for now will use the default node position target or maybe just don't let the
    //  user enable manipulator until the camera position is not anymore at its default
    //  targetPosition
    protected open val cameraManipulator: Manipulator? by lazy {
        Manipulator.Builder()
            .orbitHomePosition(this.cameraNode.worldPosition)
            .targetPosition(DEFAULT_OBJECT_POSITION)
            .viewport(width, height)
            .zoomSpeed(0.05f)
            .build(Manipulator.Mode.ORBIT)
    }

    protected var isDestroyed = false

    private var defaultEglContext: EGLContext? = null
    private val displayHelper = DisplayHelper(context)
    private var swapChain: SwapChain? = null

    private val lifecycleObserver = LifeCycleObserver()
    open var lifecycle: Lifecycle? = sharedLifecycle
        set(value) {
            field?.removeObserver(lifecycleObserver)
            field = value
            value?.addObserver(lifecycleObserver)
        }

    private val frameCallback = FrameCallback()

    private var _viewAttachmentManager: ViewAttachmentManager? = null
    protected val viewAttachmentManager
        get() = _viewAttachmentManager ?: ViewAttachmentManager(context, this).also {
            _viewAttachmentManager = it
        }

    private var lastTouchEvent: MotionEvent? = null

    private var surfaceMirrorer: SurfaceMirrorer? = null

    private var lastFrameTimeNanos: Long? = null

    init {
        view.scene = scene

        _mainLightNode?.let { addNode(it) }
        scene.skybox = sharedSkybox ?: createSkybox(engine).also { defaultSkybox = it }
        scene.indirectLight = sharedIndirectLight ?: createIndirectLight(engine).also {
            defaultIndirectLight = it
        }

        setCameraNode(sharedCameraNode ?: createCameraNode(engine).also {
            defaultCameraNode = it
        })

        sharedLifecycle?.addObserver(lifecycleObserver)
    }

    /**
     * Sets this View's Camera.
     *
     * This method associates the specified Camera with this View. A Camera can be associated with
     * several View instances. To remove an existing association, simply pass null.
     *
     * The View does not take ownership of the Scene pointer. Before destroying a Camera, be sure
     * to remove it from all associated Views.
     */
    fun setCameraNode(cameraNode: CameraNode) {
        if (_cameraNode != cameraNode) {
            _cameraNode?.collisionSystem = null
            _cameraNode = cameraNode
            cameraNode.collisionSystem = collisionSystem
            cameraNode.setView(view)
            view.camera = cameraNode.camera
        }
    }

    /**
     * Add a node to the scene as a direct child.
     *
     * If the node is already in the scene, no change is made.
     *
     * @param node the node to add as a child
     * @throws IllegalArgumentException if the child is the same object as the parent, or if the
     * parent is a descendant of the child
     */
    fun addChildNode(node: Node) {
        childNodes = childNodes + node
    }

    /**
     * Removes a node from the children of this NodeParent.
     *
     * If the node is not in the scene, no change is made.
     *
     * @param child the node to remove from the children
     */
    fun removeChildNode(node: Node) {
        childNodes = childNodes - node
    }

    /**
     * Set the background to transparent.
     */
    fun setTranslucent(translucent: Boolean) {
        holder.setFormat(if (translucent) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE)
        view.blendMode = if (translucent) BlendMode.TRANSLUCENT else BlendMode.OPAQUE
        setZOrderOnTop(translucent)
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clear = translucent
            if (!translucent) {
                (background as? ColorDrawable)?.color?.let { backgroundColor ->
                    clearColor = colorOf(backgroundColor).toFloatArray()
                }

            }
        }
    }

    fun startMirroring(surface: Surface) {
        if (surfaceMirrorer == null) {
            surfaceMirrorer = SurfaceMirrorer()
        }
        surfaceMirrorer?.startMirroring(this, surface)
    }

    fun stopMirroring(surface: Surface) {
        surfaceMirrorer?.stopMirroring(this, surface)
    }

    fun startRecording(mediaRecorder: MediaRecorder) {
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
        }
        mediaRecorder.prepare()
        mediaRecorder.start()
        startMirroring(mediaRecorder.surface)
    }

    fun stopRecording(mediaRecorder: MediaRecorder) {
        stopMirroring(mediaRecorder.surface)
        mediaRecorder.stop()
        mediaRecorder.reset()
        mediaRecorder.surface.release()
    }

    /**
     * Force destroy.
     *
     * You don't have to call this method because everything is already lifecycle aware.
     * Meaning that they are already self destroyed when they receive the `onDestroy()` callback.
     */
    open fun destroy() {
        if (!isDestroyed) {
            lifecycle = null

            runCatching { uiHelper.detach() }

            defaultCameraNode?.destroy()
            defaultMainLight?.destroy()
            defaultIndirectLight?.let { engine.safeDestroyIndirectLight(it) }
            defaultSkybox?.let { engine.safeDestroySkybox(it) }

//        runCatching { ResourceManager.getInstance().destroyAllResources() }

            defaultRenderer?.let { engine.safeDestroyRenderer(it) }
            defaultView?.let { engine.safeDestroyView(it) }
            defaultScene?.let { engine.safeDestroyScene(it) }
            iblPrefilter.destroy()
            defaultMaterialLoader?.let { engine.safeDestroyMaterialLoader(it) }
            defaultModelLoader?.let { engine.safeDestroyModelLoader(it) }

            defaultEngine?.let { it.safeDestroy() }
            defaultEglContext?.let { OpenGL.destroyEglContext(it) }
            isDestroyed = true
        }
    }

    /**
     * Callback that occurs for each display frame. Updates the scene and reposts itself to be
     * called by the choreographer on the next frame.
     *
     * @param frameTimeNanos time in nanoseconds when the frame started being rendered,
     * Typically comes from [Choreographer.FrameCallback]
     */
    protected open fun onFrame(frameTimeNanos: Long) {
        modelLoader.updateLoad()

        childNodes.forEach { it.onFrame(frameTimeNanos) }

        if (uiHelper.isReadyToRender) {
            // Allow the resource loader to finalize textures that have become ready.
//        resourceLoader.asyncUpdateLoad()

//            transformManager.openLocalTransformTransaction()

            // Only update the camera manipulator if a touch has been made
            if (lastTouchEvent != null) {
                cameraManipulator?.let { manipulator ->
                    manipulator.update(frameTimeNanos.intervalSeconds(lastFrameTimeNanos).toFloat())
                    // Extract the camera basis from the helper and push it to the Filament camera.
                    cameraNode.transform = manipulator.transform
                }
            }

            onFrame?.invoke(frameTimeNanos)

//            transformManager.commitLocalTransformTransaction()

            // Render the scene, unless the renderer wants to skip the frame.
            if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                renderer.render(view)
                renderer.endFrame()
            }
        }

        surfaceMirrorer?.onFrame(this)

        lastFrameTimeNanos = frameTimeNanos
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (lifecycle == null) {
            lifecycle = runCatching { findViewTreeLifecycleOwner()?.lifecycle }.getOrNull()
        }
    }

    override fun onDetachedFromWindow() {
        if (!isDestroyed) {
            destroy()
        }
        super.onDetachedFromWindow()
    }

    protected open fun onResized(width: Int, height: Int) {
        view.viewport = Viewport(0, 0, width, height)
        cameraManipulator?.setViewport(width, height)
        cameraNode.updateProjection()
    }

    /**
     * Invoked when the `SceneView` is tapped.
     *
     * Calls the `onTap` listener if it is available.
     *
     * @param node The node that was tapped or `null`.
     * @param motionEvent The motion event that caused the tap.
     */
    open fun onTap(motionEvent: MotionEvent, node: Node?) {
        onTap?.invoke(motionEvent, node)
    }

    override fun onSingleTapConfirmed(e: NodeMotionEvent) {
        onTap(e.motionEvent, e.node)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        // This makes sure that the view's onTouchListener is called.
        if (!super.onTouchEvent(motionEvent)) {
            lastTouchEvent = motionEvent
            gestureDetector.onTouchEvent(motionEvent)
            cameraGestureDetector?.onTouchEvent(motionEvent)
            return true
        }
        return false
    }

    internal fun addNode(node: Node) {
        node.collisionSystem = collisionSystem
        addEntities(node.sceneEntities)
        node.onChildAdded += ::addNode
        node.onChildRemoved += ::removeNode
        node.childNodes.forEach { addNode(it) }
    }

    internal fun removeNode(node: Node) {
        node.collisionSystem = null
        removeEntities(node.sceneEntities)
        node.onChildAdded -= ::addNode
        node.onChildRemoved -= ::removeNode
        node.childNodes.forEach { removeNode(it) }
    }

    internal fun replaceNode(oldNode: Node?, newNode: Node?) {
        oldNode?.let { removeNode(it) }
        newNode?.let { addNode(it) }
    }

    fun addEntity(@FilamentEntity entity: Entity) = scene.addEntity(entity)
    fun removeEntity(@FilamentEntity entity: Entity) = scene.removeEntity(entity)
    fun addEntities(@FilamentEntity entities: List<Entity>) {
        if (entities.isNotEmpty()) {
            scene.addEntities(entities.toIntArray())
        }
    }

    fun removeEntities(@FilamentEntity entities: List<Entity>) {
        if (entities.isNotEmpty()) {
            scene.removeEntities(entities.toIntArray())
        }
    }

    private inner class LifeCycleObserver : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            _viewAttachmentManager?.onResume()

            // Start the drawing when the renderer is resumed.  Remove and re-add the callback
            // to avoid getting called twice.
            Choreographer.getInstance().removeFrameCallback(frameCallback)
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }

        override fun onPause(owner: LifecycleOwner) {
            Choreographer.getInstance().removeFrameCallback(frameCallback)

            _viewAttachmentManager?.onPause()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            destroy()
        }
    }

    private inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(timestamp: Long) {
            // Always post the callback for the next frame.
            Choreographer.getInstance().postFrameCallback(this)

            onFrame(timestamp)
        }
    }

    private inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { runCatching { engine.destroySwapChain(it) } }
            swapChain = engine.createSwapChain(surface)
            displayHelper.attach(renderer, display)
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                runCatching { engine.destroySwapChain(it) }
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            // Wait for all pending frames to be processed before returning. This is to avoid a race
            // between the surface being resized before pending frames are rendered into it.
            engine.createFence().apply {
                wait(Fence.Mode.FLUSH, Fence.WAIT_FOR_EVER)
                engine.destroyFence(this)
            }
            this@SceneView.onResized(width, height)
        }
    }

    inner class CameraGestureListener : CameraGestureDetector.OnCameraGestureListener {

        override fun onScroll(x: Int, y: Int, scrollDelta: Float) {
            cameraManipulator?.scroll(x, y, scrollDelta)
        }

        override fun onGrabBegin(x: Int, y: Int, strafe: Boolean) {
            cameraManipulator?.grabBegin(x, y, strafe)
        }

        override fun onGrabUpdate(x: Int, y: Int) {
            cameraManipulator?.grabUpdate(x, y)
        }

        override fun onGrabEnd() {
            cameraManipulator?.grabEnd()
        }
    }

    class DefaultSkybox {
        class Builder : Skybox.Builder() {
            init {
                color(0.0f, 0.0f, 0.0f, 1.0f)
            }
        }
    }

    class DefaultCameraNode(engine: Engine) : CameraNode(engine) {
        init {
            transform = Transform(position = Position(0.0f, 0.0f, 1.0f))
            // Set the exposure on the camera, this exposure follows the sunny f/16 rule
            // Since we define a light that has the same intensity as the sun, it guarantees a
            // proper exposure
            setExposure(16.0f, 1.0f / 125.0f, 100.0f)
        }
    }

    class DefaultLightNode(engine: Engine) : LightNode(
        engine = engine,
        type = LightManager.Type.DIRECTIONAL,
        apply = {
            color(SceneView.DEFAULT_MAIN_LIGHT_COLOR)
            intensity(SceneView.DEFAULT_MAIN_LIGHT_COLOR_INTENSITY)
            direction(0.0f, -1.0f, 0.0f)
            castShadows(true)
        })

    companion object {
        init {
            Gltfio.init()
            Filament.init()
            Utils.init()
        }

        const val DEFAULT_MAIN_LIGHT_COLOR_TEMPERATURE = 6_500.0f
        const val DEFAULT_MAIN_LIGHT_COLOR_INTENSITY = 100_000.0f

        val DEFAULT_MAIN_LIGHT_COLOR = Colors.cct(DEFAULT_MAIN_LIGHT_COLOR_TEMPERATURE).toColor()
        val DEFAULT_MAIN_LIGHT_INTENSITY = DEFAULT_MAIN_LIGHT_COLOR_INTENSITY

        val DEFAULT_OBJECT_POSITION = Position(0.0f, 0.0f, -4.0f)

        fun createEglContext() = OpenGL.createEglContext()
        fun createEngine(eglContext: EGLContext) = Engine.create(eglContext)

        fun createScene(engine: Engine) = engine.createScene()

        fun createView(engine: Engine) =
            engine.createView().apply {
//                // On mobile, better use lower quality color buffer
//                renderQuality = renderQuality.apply {
//                    hdrColorBuffer = QualityLevel.HIGH
//                }
//                dynamicResolutionOptions = dynamicResolutionOptions.apply {
//                    enabled = false
//                    quality = QualityLevel.MEDIUM
//                }
//                // FXAA is pretty cheap and helps a lot
//                antiAliasing = AntiAliasing.NONE
//                // Ambient occlusion is the cheapest effect that adds a lot of quality
//                ambientOcclusionOptions = ambientOcclusionOptions.apply {
//                    enabled = false
//                }
//                // Bloom is pretty expensive but adds a fair amount of realism
//                bloomOptions = bloomOptions.apply {
//                    enabled = false
//                }
//                // Change the ToneMapper to FILMIC to avoid some over saturated colors, for example
//                // material orange 500.
//                colorGrading = ColorGrading.Builder()
//                    .toneMapping(ColorGrading.ToneMapping.FILMIC)
//                    .build(engine)
                setShadowingEnabled(false)
            }

        fun createRenderer(engine: Engine) = engine.createRenderer()

        fun createModelLoader(engine: Engine, context: Context) = engine.createModelLoader(context)
        fun createMaterialLoader(engine: Engine, context: Context) =
            engine.createMaterialLoader(context)

        fun createCameraNode(engine: Engine): CameraNode = DefaultCameraNode(engine)
        fun createMainLightNode(engine: Engine): LightNode = DefaultLightNode(engine)

        fun createIndirectLight(engine: Engine): IndirectLight? = null
        fun createSkybox(engine: Engine): Skybox = DefaultSkybox.Builder()
            .build(engine)
    }
}