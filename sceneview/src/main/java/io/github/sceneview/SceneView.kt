package io.github.sceneview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color.BLACK
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.*
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.*
import com.google.android.filament.Colors
import com.google.android.filament.Entity
import com.google.android.filament.LightManager
import com.google.android.filament.View
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.KTXLoader
import com.google.ar.sceneform.*
import com.google.ar.sceneform.collision.CollisionSystem
import com.google.ar.sceneform.rendering.Renderer
import com.gorisse.thomas.lifecycle.getActivity
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookAt
import io.github.sceneview.environment.Environment
import io.github.sceneview.environment.loadEnvironment
import io.github.sceneview.environment.loadEnvironmentSync
import io.github.sceneview.interaction.SceneGestureDetector
import io.github.sceneview.interaction.SelectedNodeVisualizer
import io.github.sceneview.light.*
import io.github.sceneview.math.toFloat3
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.NodeParent
import io.github.sceneview.utils.*
import kotlin.math.roundToInt

const val defaultIbl = "sceneview/environments/indoor_studio/indoor_studio_ibl.ktx"
const val defaultSkybox = "sceneview/environments/indoor_studio/indoor_studio_skybox.ktx"

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

    private val pickingHandler by lazy { Handler(Looper.getMainLooper()) }

    override val activity
        get() = try {
            findFragment<Fragment>().requireActivity()
        } catch (e: Exception) {
            context.getActivity()!!
        }

    protected var sceneLifecycle: SceneLifecycle? = null

    private val parentLifecycleObserver = LifecycleEventObserver { _, event ->
        lifecycle.currentState = event.targetState
    }

    override var _children = listOf<Node>()

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

    var _renderer: Renderer? = null

    /**
     * ### The renderer used for this view
     */
    open val renderer: Renderer
        get() = _renderer ?: Renderer(this, camera)
            .also { renderer ->
                _renderer = renderer
                renderer.mainLight = mainLight
                renderer.environment = environment
                backgroundColor?.let { renderer.setClearColor(backgroundColor) }
                renderer.filamentView.blendMode =
                    if (isTransparent) View.BlendMode.TRANSLUCENT else View.BlendMode.OPAQUE
            }

    /**
     * ### The transformation system
     *
     * Used by [TransformableNode] for detecting gestures and coordinating which node is selected.
     * Can be overridden to create a custom transformation system.
     */
    //val nodeGestureRecognizer by lazy {
    //    TransformableManager(resources.displayMetrics, FootprintSelectionVisualizer())
    //}

    open var gestureListener: SceneGestureDetector.OnSceneGestureListener =
        DefaultSceneGestureListener(this)

    open val gestureDetector: SceneGestureDetector by lazy {
        SceneGestureDetector(
            this,
            listener = gestureListener
        )
    }

    open val selectedNodeVisualizer: SelectedNodeVisualizer by lazy {
        SelectedNodeVisualizer(context, lifecycle)
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
            _renderer?.setEnvironment(value)
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
            field = value
            _renderer?.setMainLight(value)
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
    var onTouchEvent: ((selectedNode: Node?, motionEvent: MotionEvent) -> Boolean)? = null

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

            mainLight = LightManager.Builder(LightManager.Type.DIRECTIONAL).apply {
                val (r, g, b) = Colors.cct(6_500.0f)
                color(r, g, b)
                intensity(100_000.0f)
                direction(0.28f, -0.6f, -0.76f)
                castShadows(true)
            }.build(lifecycle)
            // TODO : See if we really can't load it async
            environment = KTXLoader.loadEnvironmentSync(context, lifecycle,
                iblKtxFileLocation = defaultIbl)
        } catch (exception: Exception) {
            // TODO: This is actually a none sens to call listener on init. Move the try/catch when
            // Filament is kotlined
            onOpenGLNotSupported(exception)
        }
    }

    override fun getLifecycle() =
        sceneLifecycle ?: SceneLifecycle(this).also {
            sceneLifecycle = it
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
        onDestroy()
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

    // We don't use the lifecycle call back because we want to be sure to destroy the Filament
    // engine only after all lifecycle observers has been notified
    open fun onDestroy() {
        destroy()
    }

    /**
     * ### Force destroy
     *
     * You don't have to call this method because everything is already lifecycle aware.
     * Meaning that they are already self destroyed when they receive the `onDestroy()` callback.
     */
    open fun destroy() {
        // Use runCatching because they should normally already been destroyed by the lifecycle and
        // Filament will throw an Exception when destroying them twice.
        runCatching { camera.destroy() }
        runCatching { environment?.destroy() }
        environment = null
        runCatching { mainLight?.destroy() }
        mainLight = null

//        ResourceManager.getInstance().destroyAllResources()

        Filament.release()
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
        if (renderer.render(frameTime.nanoseconds)) {
            lifecycle.dispatchEvent<SceneLifecycleObserver> {
                onFrame(frameTime)
            }
            //TODO: The link between the Camera and gesture onFrame should not be here
            gestureDetector.takeIf { it.lastTouchEvent != null }?.cameraManipulator?.let { manipulator ->
                manipulator.update(frameTime.intervalSeconds.toFloat())
                val (eye, target, up) = Array(3) { FloatArray(3) }.apply {
                    manipulator.getLookAt(this[0], this[1], this[2])
                }
                camera.transform =
                    lookAt(eye = eye.toFloat3(), target = target.toFloat3(), up = Float3(y = 1.0f))
            }
            onFrame?.invoke(frameTime)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        // This makes sure that the view's onTouchListener is called.
        if (!super.onTouchEvent(motionEvent)) {
            gestureDetector.onTouchEvent(motionEvent)
            return true
        }
        return false
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

    open var backgroundColor: Color? = null
        set(value) {
            if (field != value) {
                field = value
                _renderer?.setClearColor(backgroundColor)
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
                _renderer?.filamentView?.blendMode =
                    if (value) View.BlendMode.TRANSLUCENT else View.BlendMode.OPAQUE
            }
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
    open fun onSceneTouch(motionEvent: MotionEvent) {
        if (onTouchEvent?.invoke(null, motionEvent) != true) {
            gestureDetector.onTouchEvent(motionEvent)
        }
    }

    open fun onPickNode(selectedNode: ModelNode) {
        gestureDetector.onTouchNode(selectedNode)
    }

    open fun onTouch(selectedNode: Node?, motionEvent: MotionEvent): Boolean {
        return onTouch?.invoke(selectedNode, motionEvent) ?: false
    }

    fun addEntity(@Entity entity: Int) {
        renderer.filamentScene.addEntity(entity)
    }

    fun removeEntity(@Entity entity: Int) {
        renderer.filamentScene.removeEntity(entity)
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        onTouch = listener?.let {
            { _, _ ->
                it.onClick(this)
                true
            }
        }
    }

    open class DefaultSceneGestureListener(val sceneView: SceneView) :
        SceneGestureDetector.OnSceneGestureListener {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            sceneView.renderer.filamentView.pick(
                e.x.roundToInt(),
                e.y.roundToInt(),
                sceneView.pickingHandler
            ) { pickResult ->
                val pickedEntity = pickResult.renderable
                val selectedNode = sceneView.allChildren
                    .mapNotNull { it as? ModelNode }
                    .firstOrNull { modelNode ->
                        modelNode.modelInstance?.let { modelInstance ->
                            modelInstance.entity == pickedEntity ||
                                    modelInstance.childEntities.contains(
                                        pickedEntity
                                    )
                        } ?: false
                    }
                selectedNode?.let { sceneView.onPickNode(it) }
            }
            return true
        }
    }
}

/**
 * A SurfaceView that integrates with ARCore and renders a scene.
 */
interface SceneLifecycleOwner : LifecycleOwner {
    val activity: ComponentActivity
}

open class SceneLifecycle(open val sceneView: SceneView) : DefaultLifecycle(sceneView) {
    val activity get() = sceneView.activity
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