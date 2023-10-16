package io.github.sceneview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.MediaRecorder
import android.opengl.EGLContext
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Size
import android.view.*
import android.view.Choreographer.FrameCallback
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
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.collision.Ray
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
import io.github.sceneview.math.toFloat3
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.LightNode
import io.github.sceneview.node.Node
import io.github.sceneview.utils.*
import com.google.android.filament.utils.KTX1Loader as KTXLoader

typealias Entity = Int
typealias EntityInstance = Int
typealias FilamentEntity = com.google.android.filament.Entity
typealias FilamentEntityInstance = com.google.android.filament.EntityInstance

private const val kDefaultMainLightColorTemperature = 6_500.0f
private const val kDefaultMainLightColorIntensity = 100_000.0f

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
    /**
     * Provide your own instance if you want to share Filament resources between multiple views.
     */
    val sharedEngine: Engine? = null,
    /**
     * Provide your own instance if you want to share [Node]s' scene between multiple views.
     */
    val sharedScene: Scene? = null,
    val sharedView: View? = null,
    val sharedRenderer: Renderer? = null,
    val sharedModelLoader: ModelLoader? = null,
    val sharedMaterialLoader: MaterialLoader? = null,
    cameraNode: (engine: Engine, viewSize: Size) -> CameraNode = { engine, viewSize ->
        CameraNode(engine = engine, viewSize = viewSize)
    },
    /**
     * Invoked when an frame is processed.
     *
     * Registers a callback to be invoked when a valid Frame is processing.
     *
     * The callback to be invoked once per frame **immediately before the scene is updated.
     *
     * The callback will only be invoked if the Frame is considered as valid.
     */
    var onFrame: ((frameTimeNanos: Long) -> Unit)? = null,
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
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes),
    DefaultLifecycleObserver,
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    val engine = sharedEngine ?: Engine.create(
        OpenGL.createEglContext().also { eglContext = it }
    )
    val scene = sharedScene ?: engine.createScene()
    val view = sharedView ?: engine.createView()
    val renderer = sharedRenderer ?: engine.createRenderer()
    val modelLoader = sharedModelLoader ?: engine.createModelLoader(context)
    val materialLoader = sharedMaterialLoader ?: engine.createMaterialLoader(context)
    val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        renderCallback = SurfaceCallback()
        attachTo(this@SceneView)
    }
    val iblPrefilter = IBLPrefilter(engine)

    /**
     * Represents a virtual camera, which determines the perspective through which the scene is
     * viewed.
     *
     * All other functionality in Node is supported. You can access the position and rotation of the
     * camera, assign a collision shape to it, or add children to it. Disabling the camera turns off
     * rendering.
     */
    open val cameraNode: CameraNode get() = _cameraNode!!

    /**
     * Always add a direct light source since it is required for shadowing.
     *
     * We highly recommend adding an [IndirectLight] as well.
     */
    var mainLight: LightNode? = null
        set(value) {
            field?.let { removeChildNode(it) }
            field = value
            value?.let { addChildNode(value) }
        }

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
    open var indirectLight: IndirectLight? = null
        set(value) {
            field = value
            scene.indirectLight = value
        }

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
            scene.skybox = value
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

    var childNodes = setOf<Node>()
        set(value) {
            if (field != value) {
                field.subtract(value).forEach { removeNode(it) }
                value.subtract(field).forEach { addNode(it) }
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
     * Physics system to handle collision between nodes, hit testing on a model,...
     */
    val collisionSystem = CollisionSystem()

    val gestureDetector by lazy {
        GestureDetector(
            context = context,
            collisionSystem = collisionSystem,
            listener = this
        )
    }

    protected open val lifecycle: Lifecycle?
        get() = runCatching { findViewTreeLifecycleOwner()?.lifecycle }.getOrNull()

    protected open val activity: ComponentActivity?
        get() = try {
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
            .targetPosition(kDefaultObjectPosition)
            .viewport(width, height)
            .zoomSpeed(0.05f)
            .build(Manipulator.Mode.ORBIT)
    }

    protected var isDestroyed = false

    private var eglContext: EGLContext? = null
    private val displayHelper = DisplayHelper(context)
    private var swapChain: SwapChain? = null

    private var _cameraNode: CameraNode? = null

    private val frameCallback = object : FrameCallback {
        override fun doFrame(timestamp: Long) {
            // Always post the callback for the next frame.
            Choreographer.getInstance().postFrameCallback(this)

            onFrame(timestamp)
        }
    }

    private var _viewAttachmentManager: ViewAttachmentManager? = null
    protected val viewAttachmentManager
        get() = _viewAttachmentManager ?: ViewAttachmentManager(context, this).also {
            _viewAttachmentManager = it
        }

    private var lastTouchEvent: MotionEvent? = null
    private val pickingHandler by lazy { Handler(Looper.getMainLooper()) }

    private var surfaceMirrorer: SurfaceMirrorer? = null

    private var lastFrameTimeNanos: Long? = null

    init {
        // On mobile, better use lower quality color buffer
//        view.renderQuality = view.renderQuality.apply {
//            hdrColorBuffer = QualityLevel.HIGH
//        }
//        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
//            enabled = false
//            quality = QualityLevel.MEDIUM
//        }
        view.setShadowingEnabled(false)
//        // FXAA is pretty cheap and helps a lot
//        view.antiAliasing = AntiAliasing.NONE
//        // Ambient occlusion is the cheapest effect that adds a lot of quality
//        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
//            enabled = false
//        }
//        // Bloom is pretty expensive but adds a fair amount of realism
//        view.bloomOptions = view.bloomOptions.apply {
//            enabled = false
//        }
//        // Change the ToneMapper to FILMIC to avoid some over saturated colors, for example material
//        // orange 500.
//        view.colorGrading = ColorGrading.Builder()
//            .toneMapping(ColorGrading.ToneMapping.FILMIC)
//            .build(engine)


        view.scene = scene

        // Taken from Filament ModelViewer
        mainLight = LightNode(engine, LightManager.Type.DIRECTIONAL) {
            color(kDefaultMainLightColor)
            intensity(kDefaultMainLightColorIntensity)
            direction(0.0f, -1.0f, 0.0f)
            castShadows(true)
        }

        skybox = Skybox.Builder()
            .color(0.0f, 0.0f, 0.0f, 1.0f)
            .build(engine)

        setCamera(cameraNode(engine, Size(width, height)))
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
     * Sets this View's Camera.
     *
     * This method associates the specified Camera with this View. A Camera can be associated with
     * several View instances. To remove an existing association, simply pass null.
     *
     * The View does not take ownership of the Scene pointer. Before destroying a Camera, be sure
     * to remove it from all associated Views.
     */
    fun setCamera(cameraNode: CameraNode) {
        _cameraNode?.let { removeNode(it) }
        _cameraNode = cameraNode
        cameraNode.viewSize = Size(width, height)
        addNode(cameraNode)
        view.camera = cameraNode.camera
        collisionSystem.cameraNode = cameraNode
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

    /**
     * Tests to see if a motion event is touching any nodes within the scene, based on a ray hit
     * test whose origin is the screen position of the motion event.
     *
     * @param xPx x view coordinate in pixels
     * @param yPx y view coordinate in pixels
     *
     * @return a list of [HitResult] ordered by distance from the screen
     */
    fun hitTest(xPx: Float, yPx: Float) = collisionSystem.hitTest(xPx, yPx)

    /**
     * Tests to see if a ray is hitting any nodes within the scene and outputs
     *
     * @param ray the ray to use for the test
     *
     * @return a list of [HitResult] ordered by closest to the ray origin that intersects with the
     * ray.
     *
     * @see CameraNode.screenPointToRay
     */
    fun hitTest(ray: Ray) = collisionSystem.hitTest(ray)

    /**
     * Tests to see if the given node's collision shape overlaps the collision shape of any other
     * nodes in the scene using [Node.collisionShape].
     *
     * The node used for testing does not need to be active.
     *
     * @param node The node to use for the test.
     *
     * @return A node that is overlapping the test node. If no node is overlapping the test node,
     * then this is null. If multiple nodes are overlapping the test node, then this could be any of
     * them.
     */
    fun overlapTest(node: Node) = node.collider?.let { collider ->
        collisionSystem.intersects(collider)?.node
    }

    /**
     * Tests to see if a node is overlapping any other nodes within the scene using
     * [Node.collisionShape].
     *
     * The node used for testing does not need to be active.
     *
     * @param node The node to use for the test.
     *
     * @return A list of all nodes that are overlapping the test node. If no node is overlapping the
     * test node, then the list is empty.
     */
    fun overlapTestAll(node: Node) = listOfNotNull {
        node.collider?.let { collider ->
            collisionSystem.intersectsAll(collider) {
                it.node
            }
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
     * @param onPickingCompleted Called when picking completes.
     */
    fun pickNode(
        x: Int,
        y: Int,
        onPickingCompleted: (
            /** The Renderable Node at the picking query location  */
            node: Node?,
            /** The value of the depth buffer at the picking query location  */
            depth: Float,
            /** The fragment coordinate in GL convention at the picking query location  */
            fragCoords: Float3
        ) -> Unit
    ) {
        // Invert the y coordinate since its origin is at the bottom
        val invertedY = height - 1 - y

        view.pick(x, invertedY, pickingHandler) { result ->
            onPickingCompleted(
                childNodes.firstOrNull { it.entity == result.renderable },
                result.depth,
                result.fragCoords.toFloat3()
            )
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
            lifecycle?.removeObserver(this)

            runCatching { uiHelper.detach() }

            cameraNode.destroy()
            mainLight?.destroy()
            indirectLight?.let { engine.safeDestroyIndirectLight(it) }
            skybox?.let { engine.safeDestroySkybox(it) }

//        runCatching { ResourceManager.getInstance().destroyAllResources() }

            renderer.takeIf { it != sharedRenderer }?.let { engine.safeDestroyRenderer(it) }
            view.takeIf { it != sharedView }?.let { engine.safeDestroyView(it) }
            scene.takeIf { it != sharedScene }?.let { engine.safeDestroyScene(it) }
            iblPrefilter.destroy()
            materialLoader.takeIf { it != sharedMaterialLoader }
                ?.let { engine.safeDestroyMaterialLoader(it) }
            modelLoader.takeIf { it != sharedModelLoader }
                ?.let { engine.safeDestroyModelLoader(it) }

            engine.takeIf { it != sharedEngine }?.let { it.safeDestroy() }
            eglContext?.let { OpenGL.destroyEglContext(it) }
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

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        _viewAttachmentManager?.onResume()

        // Start the drawing when the renderer is resumed.  Remove and re-add the callback
        // to avoid getting called twice.
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        Choreographer.getInstance().removeFrameCallback(frameCallback)

        _viewAttachmentManager?.onPause()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        lifecycle?.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        if (!isDestroyed) {
            destroy()
        }
        super.onDetachedFromWindow()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        if (!isDestroyed) {
            destroy()
        }
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

    inner class SurfaceCallback : UiHelper.RendererCallback {
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
            view.viewport = Viewport(0, 0, width, height)
            cameraManipulator?.setViewport(width, height)
            cameraNode.viewSize = Size(width, height)
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

    companion object {
        init {
            Gltfio.init()
            Filament.init()
            Utils.init()
        }

        val kDefaultObjectPosition = Position(0.0f, 0.0f, -4.0f)
        val kDefaultMainLightColor = Colors.cct(kDefaultMainLightColorTemperature).toColor()
        val kDefaultMainLightIntensity = kDefaultMainLightColorIntensity
    }
}