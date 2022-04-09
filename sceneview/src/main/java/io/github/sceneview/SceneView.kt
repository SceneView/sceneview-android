package io.github.sceneview

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color.BLACK
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.*
import com.google.android.filament.*
import com.google.android.filament.Renderer.ClearOptions
import com.google.android.filament.View
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.KTXLoader
import com.google.ar.sceneform.*
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.collision.CollisionSystem
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderer
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import com.gorisse.thomas.lifecycle.lifecycleScope
import io.github.sceneview.collision.pickHitTest
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.createEnvironment
import io.github.sceneview.environment.loadEnvironment
import io.github.sceneview.light.*
import io.github.sceneview.model.GLBLoader
import io.github.sceneview.node.Node
import io.github.sceneview.node.NodeParent
import io.github.sceneview.scene.*
import io.github.sceneview.utils.*

const val defaultIbl = "sceneview/environments/indoor_studio/indoor_studio_ibl.ktx"
const val defaultSkybox = "sceneview/environments/indoor_studio/indoor_studio_skybox.ktx"
const val defaultNodeSelector = "sceneview/models/node_selector.glb"

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
    defStyleRes: Int = 0
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes),
    SceneLifecycleOwner,
    DefaultLifecycleObserver,
    Choreographer.FrameCallback,
    NodeParent {

    val engine: Engine get() = Filament.engine
    val renderer: com.google.android.filament.Renderer = engine.createRenderer()
    val scene: Scene = engine.createScene()
    val view: View = engine.createView().apply {
        scene = this@SceneView.scene
        camera = this@SceneView.camera
        engine.createCamera()

        // On mobile, better use lower quality color buffer
        renderQuality = renderQuality.apply {
            hdrColorBuffer = View.QualityLevel.MEDIUM
        }
        // Dynamic resolution often helps a lot
        dynamicResolutionOptions = dynamicResolutionOptions.apply {
            enabled = true
            quality = View.QualityLevel.MEDIUM
        }
        // MSAA is needed with dynamic resolution MEDIUM
        multiSampleAntiAliasingOptions = multiSampleAntiAliasingOptions.apply {
            enabled = true
        }
        // FXAA is pretty cheap and helps a lot
        antiAliasing = View.AntiAliasing.FXAA
        // Ambient occlusion is the cheapest effect that adds a lot of quality
        ambientOcclusionOptions = ambientOcclusionOptions.apply {
            enabled = true
        }
        // bloom is pretty expensive but adds a fair amount of realism
        bloomOptions = bloomOptions.apply {
            enabled = true
        }
        // Change the ToneMapper to FILMIC to avoid some over saturated colors, for example material
        // orange 500.
        colorGrading = ColorGrading.Builder()
            .toneMapping(ColorGrading.ToneMapping.FILMIC)
            .build(engine)
    }
    private var swapChain: SwapChain? = null
    private val uiHelper by lazy {
        UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
            renderCallback = SurfaceCallback()
            // NOTE: To choose a specific rendering resolution, add the following line:
            // setDesiredSize(1280, 720)
            attachTo(this@SceneView)
        }
    }

    override val activity
        get() = try {
            findFragment<Fragment>().requireActivity()
        } catch (e: Exception) {
            context.getActivity()!!
        }

    open val sceneLifecycle: SceneLifecycle by lazy { SceneLifecycle(context, this) }
    override fun getLifecycle() = sceneLifecycle

    private val parentLifecycleObserver = LifecycleEventObserver { _, event ->
        lifecycle.currentState = event.targetState
    }

    // TODO : Move to the Render when Kotlined it
    var currentFrameTime: FrameTime = FrameTime(0)

    /**
     * ### The camera that is used to render the scene
     *
     * The camera is a type of node.
     */
    //TODO : Move it to Lifecycle and NodeParent when Kotlined
    open val camera: Camera by lazy { Camera(this) }
    val collisionSystem = CollisionSystem()

    /**
     * ### The renderer used for this view
     */
    override val rendererOld by lazy { Renderer(this, camera) }

    val viewAttachmentManager by lazy { ViewAttachmentManager(this, lifecycle) }
    val surfaceCopier by lazy { SurfaceCopier(this, lifecycle) }

    override var _children = listOf<Node>()

    // TODO: Remove this nightmare class quick and replace it with the new Filament Pick system
    private val nodesTouchEventDispatcher by lazy { TouchEventSystem() }
    private val surfaceGestureDetector by lazy { SurfaceGestureDetector() }

    /**
     * ### The transformation system
     *
     * Used by [TransformableNode] for detecting gestures and coordinating which node is selected.
     * Can be overridden to create a custom transformation system.
     */
    val nodeGestureRecognizer by lazy {
        TransformationSystem(resources.displayMetrics, FootprintSelectionVisualizer())
    }

    var nodeSelectorModel: ModelRenderable?
        get() = (nodeGestureRecognizer.selectionVisualizer as? FootprintSelectionVisualizer)?.footprintRenderable
        set(value) {
            (nodeGestureRecognizer.selectionVisualizer as? FootprintSelectionVisualizer)?.footprintRenderable =
                value
        }

    /**
     * ### Defines the lighting environment and the skybox of the scene
     *
     * Environments are usually captured as high-resolution HDR equirectangular images and processed
     * by the cmgen tool to generate the data needed by IndirectLight.
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
            scene.setEnvironment(environment)
            updateBackground()
        }

    /**
     * ### The main directional light of the scene
     *
     * Usually the Sun.
     */
    @Entity
    var mainLight: Light? = null
        set(value) {
            field?.let { scene.removeLight(it) }
            field = value
            value?.let { scene.addLight(it) }
        }

    var onOpenGLNotSupported: ((exception: Exception) -> Unit)? = null

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
     * ### Register a callback to be invoked when the scene is touched.
     *
     * You should not use this callback in you have anything on your scene.
     * Node selection, gestures recognizer and surface gesture recognizer won't be updated if you
     * return true.
     *
     * **Have a look at [onTouch] or other gestures listeners**
     *
     * Called even if the touch is not over a node, in which case [PickHitResult.getNode]
     * will be null.
     *
     * - `pickHitResult` - represents the node that was touched if any
     * - `motionEvent` - the motion event
     * - `return` true if the listener has consumed the event
     */
    var onTouchEvent: ((pickHitResult: PickHitResult, motionEvent: MotionEvent) -> Boolean)? = null

    /**
     * ### Register a callback to be invoked on the surface singleTap
     *
     * Called even if the touch is not over a selectable node, in which case `node` will be null.
     *
     * - `selectedNode` - The node that was hit by the hit test. Null when there is no hit
     * - `motionEvent` - The original [MotionEvent]
     */
    var onTouch: ((selectedNode: Node?, motionEvent: MotionEvent) -> Boolean)? = null

    init {
        try {
            Filament.retain()

            mainLight = defaultMainLight
            //Load the environment synchronously
            environment = KTXLoader.createEnvironment(context.fileBufferLocal(defaultIbl))

            lifecycleScope.launchWhenCreated {
                nodeSelectorModel = GLBLoader.loadModel(context, defaultNodeSelector)?.apply {
                    collisionShape = null
                    BuildConfig.VERSION_NAME
                }
            }
            updateBackground()
        } catch (exception: Exception) {
            // TODO: This is actually a none sens to call listener on init. Move the try/catch when
            // Filament is kotlined
            onOpenGLNotSupported(exception)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        lifecycle.addObserver(this)

        findViewTreeLifecycleOwner()?.let { parentLifecycleOwner ->
            parentLifecycleOwner.lifecycle.addObserver(parentLifecycleObserver)
            lifecycle.currentState = parentLifecycleOwner.lifecycle.currentState
        }
    }

    fun Context.getActivity(): ComponentActivity? = this as? ComponentActivity
        ?: (this as? ContextWrapper)?.baseContext?.getActivity()

    override fun onDetachedFromWindow() {
        findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(parentLifecycleObserver)
        lifecycle.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        rendererOld.onResume()

        // Start the drawing when the renderer is resumed.  Remove and re-add the callback
        // to avoid getting called twice.
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        Choreographer.getInstance().removeFrameCallback(this)

        rendererOld.onPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        uiHelper.detach()

        rendererOld.destroyAllResources()
        camera.destroy()

        camera.destroy()
        environment?.destroy()
        environment = null
        mainLight?.destroy()
        mainLight = null

        Filament.release()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        lifecycle.dispatchEvent<SceneLifecycleObserver> {
            onSurfaceChanged(right - left, bottom - top)
        }
    }

    fun onOpenGLNotSupported(exception: Exception) {
        onOpenGLNotSupported?.invoke(exception)
    }

    var lastNanoseconds = 0L

    /**
     * Callback that occurs for each display frame. Updates the scene and reposts itself to be called
     * by the choreographer on the next frame.
     */
    override fun doFrame(frameTimeNanos: Long) {
        // Always post the callback for the next frame.
        Choreographer.getInstance().postFrameCallback(this)

        currentFrameTime = FrameTime(frameTimeNanos, currentFrameTime.nanoseconds)
        doFrame(currentFrameTime)
    }

    open fun doFrame(frameTime: FrameTime) {
        if (rendererOld.render(frameTime.nanoseconds)) {
            lifecycle.dispatchEvent<SceneLifecycleObserver> {
                onFrame(frameTime)
            }
            onFrame?.invoke(frameTime)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        // This makes sure that the view's onTouchListener is called.
        if (!super.onTouchEvent(motionEvent)) {
            onTouchEvent(pickHitTest(motionEvent, focusableOnly = true), motionEvent)
            return true
        }
        return true
    }

    override fun setBackgroundDrawable(background: Drawable?) {
        super.setBackgroundDrawable(background)

        if (holder != null) {
            updateBackground()
        }
    }

    private fun updateBackground() {
        if ((background is ColorDrawable && background.alpha == 255) || environment?.skybox != null) {
            backgroundColor = colorOf(color = (background as? ColorDrawable)?.color ?: BLACK)
            isTransparent = false
        } else {
            backgroundColor = colorOf(a = 0.0f)
            isTransparent = true
        }
    }

    var backgroundColor: Color? = null
        set(value) {
            if (field != value) {
                field = value
                renderer.clearOptions = ClearOptions().apply {
                    clear = true
                    if (value != null && value.a > 0.0f) {
                        clearColor = value.toFloatArray()
                    }
                }
            }
        }

    /**
     * ### Set the background to transparent.
     */
    var isTransparent: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                setZOrderOnTop(value)
                holder.setFormat(if (value) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE)
                view.blendMode = if (value) View.BlendMode.TRANSLUCENT else View.BlendMode.OPAQUE
            }
        }

    /**
     * ### Starts copying the currently rendered [SceneView] to the indicated [Surface]
     *
     * To capture the contents of this view, designate a [Surface] onto which this [SceneView]
     * should be mirrored.
     * This will incur a rendering performance cost and should only be set when capturing this view.
     * To stop the additional rendering, call [stopCopying].
     *
     * @param dstSurface the [Surface] onto which the rendered scene should be copied.
     * Use [android.media.MediaRecorder.getSurface],
     * [android.media.MediaCodec.createInputSurface] or
     * [android.media.MediaCodec.createPersistentInputSurface] to obtain the input surface for
     * recording.
     * @param srcViewport the source rectangle to be copied.
     * @param dstViewport the destination rectangle in which to draw the view.
     *
     * Use [SurfaceCopier.getLetterboxViewport] to ensure the destination scaling.
     * @param flags one or more <code>CopyFrameFlag</code> behavior configuration flags
     */
    fun startCopyingToSurface(
        dstSurface: Surface,
        srcViewport: Viewport = view.viewport,
        dstViewport: Viewport = srcViewport,
        flags: Int = SurfaceCopier.DEFAULT_COPY_FLAGS
    ) = surfaceCopier.startCopying(dstSurface, srcViewport, dstViewport, flags)

    /**
     * ### Stops copying to the specified [Surface].
     *
     * When capturing is complete, call this method to stop mirroring the SceneView to the specified
     * [Surface]. If this is not called, the additional performance cost will remain.
     *
     * The application is responsible for calling [Surface.release] on the Surface when done.
     */
    fun stopCopyingToSurface(surface: Surface) = surfaceCopier.stopCopying(surface)

    /**
     * ### Invoked when the scene is touched.
     *
     * Called even if the touch is not over a node, in which case [PickHitResult.getNode]
     * will be null.
     *
     * @param pickHitResult represents the node that was touched
     * @param motionEvent   the motion event
     */
    open fun onTouchEvent(pickHitResult: PickHitResult, motionEvent: MotionEvent) {
        if (onTouchEvent?.invoke(pickHitResult, motionEvent) != true) {
            nodesTouchEventDispatcher.onTouchEvent(pickHitResult, motionEvent)
            nodeGestureRecognizer.onTouch(pickHitResult, motionEvent)
            surfaceGestureDetector.onTouchEvent(pickHitResult, motionEvent)
        }
    }

    open fun onTouch(selectedNode: Node?, motionEvent: MotionEvent): Boolean {
        return onTouch?.invoke(selectedNode, motionEvent) ?: false
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        onTouch = listener?.let {
            { _, _ ->
                it.onClick(this)
                true
            }
        }
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        val displayHelper by lazy { DisplayHelper(context) }

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
            view.viewport = Viewport(0, 0, width, height)
            //TODO:
//            cameraManipulator.setViewport(width, height)
//            updateCameraProjection()
//            private fun updateCameraProjection() {
//                val width = view.viewport.width
//                val height = view.viewport.height
//                val aspect = width.toDouble() / height.toDouble()
//                camera.setLensProjection(cameraFocalLength.toDouble(), aspect, kNearPlane, kFarPlane)
//            }
        }
    }


    inner class SurfaceGestureDetector : GestureDetector(context, OnGestureListener()) {
        lateinit var pickHitResult: PickHitResult

        fun onTouchEvent(pickHitResult: PickHitResult, motionEvent: MotionEvent): Boolean {
            this.pickHitResult = pickHitResult
            return onTouchEvent(motionEvent)
        }
    }

    inner class OnGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
            val hitTestResult = surfaceGestureDetector.pickHitResult
            onTouch(hitTestResult.node, motionEvent)
            return true
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }

    companion object {
        val defaultMainLight: Light by lazy {
            LightManager.Builder(LightManager.Type.DIRECTIONAL).apply {
                val (r, g, b) = Colors.cct(6_500.0f)
                color(r, g, b)
                intensity(100_000.0f)
                direction(0.28f, -0.6f, -0.76f)
                castShadows(true)
            }.build()
        }
    }
}

/**
 * A SurfaceView that integrates with ARCore and renders a scene.
 */
interface SceneLifecycleOwner : LifecycleOwner {
    val activity: ComponentActivity
    val rendererOld: Renderer
}

open class SceneLifecycle(context: Context, open val owner: SceneLifecycleOwner) :
    DefaultLifecycle(context, owner) {
    val activity get() = owner.activity
    val renderer get() = owner.rendererOld
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