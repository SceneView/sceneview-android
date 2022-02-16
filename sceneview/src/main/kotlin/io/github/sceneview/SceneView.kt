package io.github.sceneview

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.net.Uri
import android.util.AttributeSet
import android.view.*
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.*
import com.google.android.filament.Entity
import com.google.android.filament.LightManager
import com.google.android.filament.View
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.KTXLoader
import com.google.ar.sceneform.*
import com.google.ar.sceneform.collision.CollisionSystem
import com.google.ar.sceneform.rendering.EngineInstance
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderer
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformationSystem
import com.gorisse.thomas.lifecycle.lifecycleScope
import io.github.sceneview.collision.pickHitTest
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.loadEnvironment
import io.github.sceneview.light.*
import io.github.sceneview.model.await
import io.github.sceneview.node.Node
import io.github.sceneview.node.NodeParent
import io.github.sceneview.utils.DefaultLifecycle
import java.util.concurrent.TimeUnit

const val defaultMaxFPS = 60
const val defaultNodeSelector = "sceneview/models/node_selector.glb"
const val defaultIbl = "sceneview/environments/default_ibl.ktx"
const val defaultSkybox = "sceneview/environments/default_skybox.ktx"

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

    override var _children = listOf<Node>()

    // TODO : Move to the Render when Kotlined it
    private var currentFrameTick = 0L
    private val currentFrameTime = FrameTime()

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
    override val renderer by lazy { Renderer(this, camera) }

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

    enum class FrameRate(val factor: Int) {
        /**
         * Divide the maximal allowed frame rate by 1
         */
        FULL(1),

        /**
         * Divide the maximal allowed frame rate by 2
         */
        HALF(2),

        /**
         * Divide the maximal allowed frame rate by 3
         */
        THIRD(3);
    }

    var frameRate = FrameRate.FULL
    var maxFramesPerSeconds = defaultMaxFPS

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
            renderer.setEnvironment(value)
            field = value
        }

    /**
     * ### The main directional light of the scene
     *
     * Usually the Sun.
     */
    @Entity // Public until full moving to Kotlin
    var mainLight: Light? = null
        set(value) {
            field = value
            renderer.setMainLight(value)
        }

//    var backgroundColor: Color? = null
//        set(value) {
//            field = value
//            renderer.setClearColor(backgroundColor)
//        }

    init {
        try {
            // TODO : Remove it here when moved Filament to lifecycle aware
            EngineInstance.getEngine()

            lifecycleScope.launchWhenCreated {
                mainLight = LightManager.Builder(LightManager.Type.SUN)
                    .intensity(defaultMainLightIntensity)
                    .castShadows(true).build()
                environment = KTXLoader.loadEnvironment(context, defaultIbl)
                nodeSelectorModel = ModelRenderable.builder()
                    .setSource(context, Uri.parse(defaultNodeSelector))
                    .setIsFilamentGltf(true)
                    .await().apply {
                        collisionShape = null
                        BuildConfig.VERSION_NAME
                    }
            }
        } catch (exception: Exception) {
            // TODO: This is actually a none sens to call listener on init. Move the try/catch when
            // Filament is kotlined
            onOpenGLNotSupported(exception)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        lifecycle.addObserver(this)

//        lifecycle.currentState = Lifecycle.State.CREATED

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

        renderer.onResume()

        // Start the drawing when the renderer is resumed.  Remove and re-add the callback
        // to avoid getting called twice.
        Choreographer.getInstance().removeFrameCallback(this)
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        Choreographer.getInstance().removeFrameCallback(this)

        renderer.onPause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        renderer.destroyAllResources()
        camera.destroy();
        environment?.destroy()
        environment = null
        mainLight?.destroy()
        mainLight = null
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        //TODO move to lifecycle when Rendere is kotlined
        val width = right - left
        val height = bottom - top
        renderer.setDesiredSize(width, height)
        lifecycle.dispatchEvent<SceneLifecycleObserver> {
            onSurfaceChanged(width, height)
        }
    }

    fun onOpenGLNotSupported(exception: Exception) {
        onOpenGLNotSupported?.invoke(exception)
    }

    /**
     * Callback that occurs for each display frame. Updates the scene and reposts itself to be called
     * by the choreographer on the next frame.
     */
    override fun doFrame(frameTimeNanos: Long) {
        // Always post the callback for the next frame.
        Choreographer.getInstance().postFrameCallback(this)

        // TODO : Move the frame rate part to the Renderer when Kotlined it
        // limit to max fps
        val tick = System.nanoTime() / (TimeUnit.SECONDS.toNanos(1) / maxFramesPerSeconds)
        if (currentFrameTick / frameRate.factor != tick / frameRate.factor) {
            currentFrameTick = tick
            currentFrameTime.update(frameTimeNanos)
            doFrame(currentFrameTime)
        }
    }

    open fun doFrame(frameTime: FrameTime) {
        lifecycle.dispatchEvent<SceneLifecycleObserver> { onFrame(frameTime) }
        renderer.render(frameTime.nanoTime, false)
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

    /**
     * ### Set the background to transparent.
     */
    var isTransparent: Boolean = false
        set(value) {
            field = value
            setZOrderOnTop(value)
            holder.setFormat(if (value) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE)
            renderer.filamentView.blendMode =
                if (value) View.BlendMode.TRANSLUCENT else View.BlendMode.OPAQUE
        }

    // TODO: See if we still need it
//    /**
//     * Set the background to a given [Drawable], or remove the background.
//     * If the background is a [ColorDrawable], then the background color of the [SceneView] is set
//     * to [ColorDrawable.getColor] (the alpha of the color is ignored).
//     * Otherwise, default to the behavior of [SurfaceView.setBackground].
//     */
//    override fun setBackground(background: Drawable?) {
//        if (background is ColorDrawable) {
//            backgroundColor = Color(background.color)
//            renderer.setClearColor(backgroundColor)
//        } else {
//            super.setBackground(background)
//            backgroundColor = null
//            renderer.setDefaultClearColor()
//        }
//    }

    /**
     * To capture the contents of this view, designate a [Surface] onto which this SceneView
     * should be mirrored. Use [android.media.MediaRecorder.getSurface], [ ][android.media.MediaCodec.createInputSurface] or [ ][android.media.MediaCodec.createPersistentInputSurface] to obtain the input surface for
     * recording. This will incur a rendering performance cost and should only be set when capturing
     * this view. To stop the additional rendering, call stopMirroringToSurface.
     *
     * @param surface the Surface onto which the rendered scene should be mirrored.
     * @param left    the left edge of the rectangle into which the view should be mirrored on surface.
     * @param bottom  the bottom edge of the rectangle into which the view should be mirrored on
     * surface.
     * @param width   the width of the rectangle into which the SceneView should be mirrored on surface.
     * @param height  the height of the rectangle into which the SceneView should be mirrored on
     * surface.
     */
    fun startMirroringToSurface(surface: Surface, left: Int, bottom: Int, width: Int, height: Int) =
        renderer.startMirroring(surface, left, bottom, width, height)

    /**
     * When capturing is complete, call this method to stop mirroring the SceneView to the specified
     * [Surface]. If this is not called, the additional performance cost will remain.
     *
     *
     * The application is responsible for calling [Surface.release] on the Surface when
     * done.
     */
    fun stopMirroringToSurface(surface: Surface) = renderer.stopMirroring(surface)

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

    var onOpenGLNotSupported: ((exception: Exception) -> Unit)? = null

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
}

/**
 * A SurfaceView that integrates with ARCore and renders a scene.
 */
interface SceneLifecycleOwner : LifecycleOwner {
    val activity: ComponentActivity
    val renderer: Renderer
}

open class SceneLifecycle(context: Context, open val owner: SceneLifecycleOwner) :
    DefaultLifecycle(context, owner) {
    val activity get() = owner.activity
    val renderer get() = owner.renderer
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