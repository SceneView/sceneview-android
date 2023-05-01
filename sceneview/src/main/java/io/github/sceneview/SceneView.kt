package io.github.sceneview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color.BLACK
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Choreographer
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.*
import com.google.android.filament.*
import com.google.android.filament.Renderer.ClearOptions
import com.google.android.filament.View.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.Manipulator
import com.google.ar.sceneform.CameraNode
import com.google.ar.sceneform.collision.CollisionSystem
import com.google.ar.sceneform.rendering.ResourceManager
import com.google.ar.sceneform.rendering.ViewAttachmentManager
import com.gorisse.thomas.lifecycle.getActivity
import io.github.sceneview.Filament.engine
import io.github.sceneview.Filament.transformManager
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.loadEnvironment
import io.github.sceneview.environment.loadEnvironmentSync
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.gesture.GestureDetector
import io.github.sceneview.gesture.NodeMotionEvent
import io.github.sceneview.gesture.transform
import io.github.sceneview.light.Light
import io.github.sceneview.light.build
import io.github.sceneview.light.destroy
import io.github.sceneview.light.destroyLight
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.NodeParent
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.scene.build
import io.github.sceneview.scene.destroy
import io.github.sceneview.utils.*
import java.util.concurrent.TimeUnit
import com.google.android.filament.utils.KTX1Loader as KTXLoader

private const val maxFramesPerSecond = 60

const val defaultIblLocation = "sceneview/environments/indoor_studio/indoor_studio_ibl.ktx"

/**
 * ### A SurfaceView that manages rendering and interactions with the 3D scene.
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
    cameraNode: CameraNode = CameraNode()
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes),
    SceneLifecycleOwner,
    DefaultLifecycleObserver,
    Choreographer.FrameCallback,
    NodeParent,
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

    sealed class FrameRate(val factor: Long) {
        object Full : FrameRate(1)
        object Half : FrameRate(2)
        object Third : FrameRate(3)
    }

    enum class SelectionMode {
        NONE, SINGLE, MULTIPLE;

        /**
         * ### Whether it is possible to deselect nodes
         *
         * A [Node] can be deselected if no [Node] s are picked on tap.
         */
        var allowDeselection = true
    }

    open var frameRate: FrameRate = FrameRate.Full

    val scene: Scene
    val view: View
    val renderer: Renderer
    open val cameraNode: CameraNode = cameraNode

    /** @see View.setRenderQuality **/
    var renderQuality: RenderQuality
        get() = view.renderQuality
        set(value) {
            view.renderQuality = value
        }

    /** @see View.setDynamicResolutionOptions **/
    var dynamicResolution: DynamicResolutionOptions
        get() = view.dynamicResolutionOptions
        set(value) {
            view.dynamicResolutionOptions = value
        }

    /** @see View.setMultiSampleAntiAliasingOptions **/
    var multiSampleAntiAliasingOptions: MultiSampleAntiAliasingOptions
        get() = view.multiSampleAntiAliasingOptions
        set(value) {
            view.multiSampleAntiAliasingOptions = value
        }

    /** @see View.setAntiAliasing **/
    var antiAliasing: AntiAliasing
        get() = view.antiAliasing
        set(value) {
            view.antiAliasing = value
        }

    /** @see View.setAmbientOcclusionOptions **/
    var ambientOcclusionOptions: AmbientOcclusionOptions
        get() = view.ambientOcclusionOptions
        set(value) {
            view.ambientOcclusionOptions = value
        }

    /** @see View.setBloomOptions **/
    var bloomOptions: BloomOptions
        get() = view.bloomOptions
        set(value) {
            view.bloomOptions = value
        }

    /** @see View.setDithering **/
    var dithering: Dithering
        get() = view.dithering
        set(value) {
            view.dithering = value
        }


    /**
     * ### The main directional light of the scene
     *
     * Usually the Sun.
     */
    @Entity
    var mainLight: Light? = null
        set(value) {
            field?.let { removeLight(it) }
            field = value
            value?.let { addLight(value) }
        }


    /**
     * ### Defines the lighting environment and the skybox of the scene
     *
     * Environments are usually captured as high-resolution HDR equirectangular images and processed by
     * the cmgen tool to generate the data needed by IndirectLight.
     *
     * You can also process an hdr at runtime but this is more consuming.
     *
     * - Currently IndirectLight is intended to be used for "distant probes", that is, to represent
     * global illumination from a distant (i.e. at infinity) environment, such as the sky or distant
     * mountains.
     * Only a single IndirectLight can be used in a Scene. This limitation will be lifted in the future.
     *
     * - When added to a Scene, the Skybox fills all untouched pixels.
     *
     * @see [KTXLoader.loadEnvironment]
     * @see [HDRLoader.loadEnvironment]
     */
    var environment: Environment? = null
        set(value) {
            field = value
            indirectLight = value?.indirectLight
            skybox = value?.skybox
        }

    /**
     * ### IndirectLight is used to simulate environment lighting
     *
     * Environment lighting has a two components:
     * - irradiance
     * - reflections (specular component)
     *
     * @see IndirectLight
     * @see Scene.setIndirectLight
     */
    var indirectLight: IndirectLight? = null
        set(value) {
            field = value
            scene.indirectLight = value
        }

    /**
     * ### The Skybox is drawn last and covers all pixels not touched by geometry
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

    var backgroundColor: Color?
        get() = renderer.clearOptions.clearColor.toColor()
        set(value) {
            renderer.clearOptions = ClearOptions().apply {
                clear = true
                isTranslucent = value == null || value.a != 1.0f
                if (value != null && value.a != 0.0f) {
                    clearColor = value.toFloatArray()
                }
            }
        }

    /**
     * ### Set the background to transparent.
     */
    var isTranslucent: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                setZOrderOnTop(value)
                holder.setFormat(if (value) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE)
                view.blendMode = if (value) BlendMode.TRANSLUCENT else BlendMode.OPAQUE
            }
        }

    /**
     * ### Inverts winding for front face rendering
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

    val collisionSystem = CollisionSystem()

    val cameraManipulatorTarget: Node? = null
        get() = field ?: selectedNode ?: allChildren.lastOrNull { it is ModelNode }

    var selectionMode = SelectionMode.SINGLE

    var selectedNodes: List<Node>
        get() = allChildren.filter { it.isSelected }
        set(value) = allChildren.forEach { it.isSelected = value.contains(it) }

    var selectedNode: Node?
        get() = selectedNodes.firstOrNull()
        set(value) {
            selectedNodes = listOfNotNull(value)
        }

    open val selectionVisualizer: (() -> Node)? = {
        ModelNode("sceneview/models/node_selector.glb").apply {
            isSelectable = false
            collisionShape = null
        }
    }

    /**
     * ### Invoked when an frame is processed
     *
     * Registers a callback to be invoked when a valid Frame is processing.
     *
     * The callback to be invoked once per frame **immediately before the scene
     * is updated**.
     *
     * The callback will only be invoked if the Frame is considered as valid.
     */
    var onFrame: ((frameTime: FrameTime) -> Unit)? = null

    /**
     * ### Invoked when the `SceneView` is tapped
     *
     * Only nodes with renderables or their parent nodes can be tapped since Filament picking is
     * used to find a touched node. The ID of the Filament renderable can be used to determine what
     * part of a model is tapped.
     *
     * - `node` - The node that was tapped or `null`.
     * - `renderable` - The ID of the Filament renderable that was tapped.
     * - `motionEvent` - The motion event that caused the tap.
     */
    var onTap: ((motionEvent: MotionEvent, node: Node?, renderable: Renderable?) -> Unit)? = null

    protected var sceneLifecycle: SceneLifecycle? = null

    protected val activity: ComponentActivity
        get() = try {
            findFragment<Fragment>().requireActivity()
        } catch (e: Exception) {
            context.getActivity()!!
        }

    private val uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
        renderCallback = SurfaceCallback()
        attachTo(this@SceneView)
    }
    private val displayHelper = DisplayHelper(context)
    private var swapChain: SwapChain? = null

    private val parentLifecycleObserver = LifecycleEventObserver { _, event ->
        lifecycle.currentState = event.targetState
    }

    override var children = listOf<Node>()

    // TODO: Move to internal when ViewRenderable is kotlined
    val viewAttachmentManager by lazy { ViewAttachmentManager(context, this) }

    private val pickingHandler by lazy { Handler(Looper.getMainLooper()) }

    private var currentFrameTime: FrameTime = FrameTime(0)

    private var lastTouchEvent: MotionEvent? = null
    val gestureDetector by lazy { GestureDetector(context, ::pickNode, this) }

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
            .apply {
                cameraNode.worldPosition.let { (x, y, z) ->
                    orbitHomePosition(x, y, z)
                }
                cameraManipulatorTarget?.worldPosition?.let { (x, y, z) ->
                    targetPosition(x, y, z)
                }
            }
            .viewport(width, height)
            .zoomSpeed(0.05f)
            .build(Manipulator.Mode.ORBIT)
    }

    private var lastTick: Long = 0
    private val surfaceMirrorer by lazy { SurfaceMirrorer(lifecycle) }

    init {
        Filament.retain()

        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        // on mobile, better use lower quality color buffer
//        view.renderQuality = view.renderQuality.apply {
//            hdrColorBuffer = View.QualityLevel.MEDIUM
//        }
//        // dynamic resolution often helps a lot
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = false
            quality = QualityLevel.MEDIUM
        }
//        // MSAA is needed with dynamic resolution MEDIUM
//        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
//            enabled = true
//        }
//        // FXAA is pretty cheap and helps a lot
//        view.antiAliasing = View.AntiAliasing.FXAA
//        // ambient occlusion is the cheapest effect that adds a lot of quality
//        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
//            enabled = true
//        }
//        // bloom is pretty expensive but adds a fair amount of realism
//        view.bloomOptions = view.bloomOptions.apply {
//            enabled = true
//        }
//        view.antiAliasing = AntiAliasing.NONE
//        view.setScreenSpaceRefractionEnabled(false)
//        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
//            this.enabled = false
//        }
//        view.setPostProcessingEnabled(false)

        view.scene = scene
        view.camera = cameraNode.camera
        // Change the ToneMapper to FILMIC to avoid some over saturated colors, for example material
        // orange 500.
        view.colorGrading = ColorGrading.Builder()
            .toneMapping(ColorGrading.ToneMapping.FILMIC)
            .build()

        val (r, g, b) = Colors.cct(6_500.0f)
        mainLight = LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            .intensity(100_000.0f)
            .direction(0.0f, -1.0f, 0.0f)
            .castShadows(true)
            .build()
        environment = KTXLoader.loadEnvironmentSync(
            context, lifecycle,
            iblKtxFileLocation = defaultIblLocation
        )

        cameraNode.parent = this
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        lifecycle.addObserver(this)

        findViewTreeLifecycleOwner()?.lifecycle?.let { viewTreeLifecycle ->
            viewTreeLifecycle.addObserver(parentLifecycleObserver)
            if (lifecycle.currentState != viewTreeLifecycle.currentState) {
                lifecycle.currentState = viewTreeLifecycle.currentState
            }
        }
    }

    override fun onDetachedFromWindow() {
        findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(parentLifecycleObserver)
        if (lifecycle.currentState != Lifecycle.State.DESTROYED) {
            lifecycle.currentState = Lifecycle.State.DESTROYED
        }
        destroy()
        super.onDetachedFromWindow()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        viewAttachmentManager.onResume()

        // Start the drawing when the renderer is resumed.  Remove and re-add the callback
        // to avoid getting called twice.
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        Choreographer.getInstance().removeFrameCallback(this)

        viewAttachmentManager.onPause()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        lifecycle.dispatchEvent<SceneLifecycleObserver> {
            onSurfaceChanged(width, height)
        }
    }

    /**
     * Callback that occurs for each display frame. Updates the scene and reposts itself to be called
     * by the choreographer on the next frame.
     */
    override fun doFrame(frameTimeNanos: Long) {
        // Always post the callback for the next frame.
        Choreographer.getInstance().postFrameCallback(this)

        // limit to max fps
        val nanoTime = System.nanoTime()
        val tick = nanoTime / (TimeUnit.SECONDS.toNanos(1) / maxFramesPerSecond)

        if (lastTick / frameRate.factor != tick / frameRate.factor) {
            currentFrameTime = FrameTime(frameTimeNanos, currentFrameTime.nanoseconds)
            doFrame(currentFrameTime)
        }
    }

    open fun doFrame(frameTime: FrameTime) {
        if (uiHelper.isReadyToRender) {
            // Allow the resource loader to finalize textures that have become ready.
//        resourceLoader.asyncUpdateLoad()

            transformManager.openLocalTransformTransaction()

            // Only update the camera manipulator if a touch has been made
            if (lastTouchEvent != null) {
                cameraManipulator?.let { manipulator ->
                    manipulator.update(frameTime.intervalSeconds.toFloat())
                    // Extract the camera basis from the helper and push it to the Filament camera.
                    cameraNode.transform = manipulator.transform
                }
            }

            lifecycle.dispatchEvent<SceneLifecycleObserver> {
                onFrame(frameTime)
            }
            onFrame?.invoke(frameTime)

            transformManager.commitLocalTransformTransaction()

            // Render the scene, unless the renderer wants to skip the frame.
            if (renderer.beginFrame(swapChain!!, frameTime.nanoseconds)) {
                renderer.render(view)
                renderer.endFrame()
            }
        }
    }

    /** @see Scene.addEntity */
    fun addEntity(@Entity entity: Int) = scene.addEntity(entity)

    /** @see Scene.removeEntity */
    fun removeEntity(@Entity entity: Int) = scene.removeEntity(entity)

    /** @see Scene.addEntities */
    fun addEntities(@Entity entities: IntArray) = scene.addEntities(entities)

    /** @see Scene.removeEntities */
    fun removeEntities(@Entity entities: IntArray) = scene.removeEntities(entities)

    /** @see Scene.addEntity */
    fun addLight(@Entity light: Light) = scene.addEntity(light)

    /** @see Scene.removeEntity */
    fun removeLight(@Entity light: Light) = scene.removeEntity(light)

    @Deprecated("Deprecated in Java")
    override fun setBackgroundDrawable(background: Drawable?) {
        super.setBackgroundDrawable(background)

        if (holder != null) {
            updateBackground()
        }
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

    override fun onSingleTapConfirmed(e: NodeMotionEvent) {
        onTap(e.motionEvent, e.node, e.renderable)
    }

    /**
     * ### Invoked when the `SceneView` is tapped
     *
     * Calls the `onTap` listener if it is available.
     *
     * @param node The node that was tapped or `null`.
     * @param renderable The ID of the Filament renderable that was tapped.
     * @param motionEvent The motion event that caused the tap.
     */
    open fun onTap(motionEvent: MotionEvent, node: Node?, renderable: Renderable?) {
        if (node != null) {
            when (selectionMode) {
                SelectionMode.SINGLE ->
                    if (node.isSelectable && !node.isSelected) {
                        selectedNode = node
                    } else if (selectionMode.allowDeselection) {
                        selectedNode = null
                    }
                SelectionMode.MULTIPLE -> selectedNodes =
                    if (node.isSelectable && !node.isSelected) {
                        selectedNodes + node
                    } else {
                        selectedNodes - node
                    }
                else -> if (selectionMode.allowDeselection) {
                    selectedNode = null
                }
            }
        } else if (selectionMode.allowDeselection) {
            selectedNode = null
        }

        onTap?.invoke(motionEvent, node, renderable)
    }

    /**
     * ### Picks a node at given coordinates
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
        onPickingCompleted: (node: ModelNode?, renderable: Renderable) -> Unit
    ) {
        // Invert the y coordinate since its origin is at the bottom
        val invertedY = height - 1 - y

        view.pick(x, invertedY, pickingHandler) { pickResult ->
            val pickedRenderable = pickResult.renderable
            val pickedNode = allChildren
                .mapNotNull { it as? ModelNode }
                .firstOrNull { modelNode ->
                    pickedRenderable in modelNode.renderables
                }
            onPickingCompleted.invoke(pickedNode, pickedRenderable)
        }
    }

    fun pickNode(
        e: MotionEvent,
        onPickingCompleted: (e: NodeMotionEvent) -> Unit
    ) = pickNode(e.x.toInt(), e.y.toInt()) { node, renderable ->
        onPickingCompleted(NodeMotionEvent(e, node, renderable))
    }

    fun startRecording(mediaRecorder: MediaRecorder) {
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
        }
        mediaRecorder.prepare()
        mediaRecorder.start()
        surfaceMirrorer.startMirroring(mediaRecorder.surface)
    }

    fun startMirroring(surface: Surface) {
        surfaceMirrorer.startMirroring(surface)
    }

    fun stopMirroring(surface: Surface) {
        surfaceMirrorer.stopMirroring(surface)
    }

    fun stopRecording(mediaRecorder: MediaRecorder) {
        surfaceMirrorer.stopMirroring(mediaRecorder.surface)
        mediaRecorder.stop()
        mediaRecorder.reset()
        mediaRecorder.surface.release()
    }

    /**
     * ### Force destroy
     *
     * You don't have to call this method because everything is already lifecycle aware.
     * Meaning that they are already self destroyed when they receive the `onDestroy()` callback.
     */
    open fun destroy() {
        runCatching { uiHelper.detach() }

        // Use runCatching because they should normally already been destroyed by the lifecycle and
        // Filament will throw an Exception when destroying them twice.
        runCatching { cameraNode.destroy() }
        runCatching { mainLight?.destroyLight() }
        runCatching { indirectLight?.destroy() }
        runCatching { skybox?.destroy() }

        runCatching { ResourceManager.getInstance().destroyAllResources() }

        runCatching { engine.destroyRenderer(renderer) }
        runCatching { engine.destroyView(view) }
        runCatching { engine.destroyScene(scene) }

        Filament.release()
    }

    override fun getLifecycle() =
        sceneLifecycle ?: SceneLifecycle(this).also {
            sceneLifecycle = it
        }

    private fun updateBackground() {
        if ((background is ColorDrawable && background.alpha == 255) || skybox != null) {
            backgroundColor = colorOf(color = (background as? ColorDrawable)?.color ?: BLACK)
            isTranslucent = false
        } else {
            backgroundColor = colorOf(a = 0.0f)
            isTranslucent = true
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
            view.viewport = Viewport(0, 0, width, height)
            cameraManipulator?.setViewport(width, height)
            cameraNode.refreshProjectionMatrix()
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
}

/**
 * A SurfaceView that integrates with ARCore and renders a scene.
 */
interface SceneLifecycleOwner : LifecycleOwner {
}

open class SceneLifecycle(open val sceneView: SceneView) : DefaultLifecycle(sceneView) {
}

interface SceneLifecycleObserver : DefaultLifecycleObserver {
    /**
     * Records a change in surface dimensions.
     *
     * @param width the updated width of the surface.
     * @param height the updated height of the surface.
     */
    fun onSurfaceChanged(width: Int, height: Int) {
    }

    fun onFrame(frameTime: FrameTime) {
    }
}
