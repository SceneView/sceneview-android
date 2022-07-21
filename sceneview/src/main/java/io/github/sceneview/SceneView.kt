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
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.*
import com.google.android.filament.Colors
import com.google.android.filament.Entity
import com.google.android.filament.LightManager
import com.google.android.filament.View
import com.google.android.filament.utils.HDRLoader
import com.google.android.filament.utils.Manipulator
import com.google.ar.sceneform.Camera
import com.google.ar.sceneform.collision.CollisionSystem
import com.google.ar.sceneform.rendering.Renderer
import com.google.ar.sceneform.rendering.ResourceManager
import com.gorisse.thomas.lifecycle.getActivity
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
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.node.NodeParent
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.utils.*
import com.google.android.filament.utils.KTX1Loader as KTXLoader

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
    NodeParent,
    GestureDetector.OnGestureListener by GestureDetector.SimpleOnGestureListener() {

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

    enum class SelectionMode {
        NONE, SINGLE, MULTIPLE;

        /**
         * ### Whether it is possible to deselect nodes
         *
         * An `ArNode` can be deselected if no `ArNode`s are picked on tap.
         */
        var allowDeselection = true
    }

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
        ModelNode(context, lifecycle, "sceneview/models/node_selector.glb").apply {
            isSelectable = false
            collisionShape = null
        }
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
        get() = renderer.filamentView.isFrontFaceWindingInverted
        set(value) {
            renderer.filamentView.isFrontFaceWindingInverted = value
        }

    var lastTouchEvent: MotionEvent? = null

    private val gestureDetector by lazy { GestureDetector(context, ::pickNode, this) }

    private val cameraGestureDetector: CameraGestureDetector? by lazy {
        CameraGestureDetector(this, object : CameraGestureDetector.OnCameraGestureListener {
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
        })
    }

    val cameraTargetPosition: Position? = null
        get() = field
            ?: allChildren.firstOrNull { it is ModelNode }?.worldPosition

    open var cameraManipulatorBuilder: (() -> Manipulator)? = {
        Manipulator.Builder()
            .apply {
                camera.worldPosition.let {
                    orbitHomePosition(it.x, it.y, it.z)
                }
                cameraTargetPosition?.let {
                    targetPosition(it.x, it.y, it.z)
                }
            }
            .viewport(width, height)
            .zoomSpeed(0.05f)
            .build(Manipulator.Mode.ORBIT)
    }
        set(value) {
            field = value
            cameraManipulator = value?.invoke()
        }

    // TODO: Ask Filament to add a startPosition and startRotation in order to handle previous
    //  possible programmatic camera transforms.
    //  Better would be that we don't have to create a new Manipulator and just update  it when
    //  the camera is programmatically updated so it don't come back to  initial position.
    //  Return field for now will use the default node position target or maybe just don't let the
    //  user enable manipulator until the camera position is not anymore at its default
    //  targetPosition
    private var cameraManipulator: Manipulator? = null
        get() = field?.takeIf {
            //TODO find a way to solve that
//            it.transform == camera.transform
            true
        } ?: cameraManipulatorBuilder?.invoke().also { field = it }

    val surfaceCopier by lazy { SurfaceCopier(lifecycle) }

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

    init {
        try {
            Filament.retain()

            val (r, g, b) = Colors.cct(6_500.0f)
            mainLight = LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                .intensity(100_000.0f)
                .direction(0.0f, -1.0f, 0.0f)
//                .direction(0.28f, -0.6f, -0.76f)
                .castShadows(true)
                .build(lifecycle)

            // TODO : See if we really can't load it async
            environment = KTXLoader.loadEnvironmentSync(
                context, lifecycle,
                iblKtxFileLocation = defaultIbl
            )
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

        ResourceManager.getInstance().destroyAllResources()

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
            // Only update the camera manipulator if a touch has been made
            if (lastTouchEvent != null) {
                cameraManipulator?.let { manipulator ->
                    manipulator.update(frameTime.intervalSeconds.toFloat())
                    camera.transform = manipulator.transform
                }
            }

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

    fun startRecording(mediaRecorder: MediaRecorder) {
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
        }
        mediaRecorder.prepare()
        mediaRecorder.start()
        surfaceCopier.startMirroring(mediaRecorder.surface)
    }

    fun stopRecording(mediaRecorder: MediaRecorder) {
        surfaceCopier.stopMirroring(mediaRecorder.surface)
        mediaRecorder.stop()
        mediaRecorder.reset()
        mediaRecorder.surface.release()
    }

    fun addEntity(@Entity entity: Int) {
        renderer.filamentScene.addEntity(entity)
    }

    fun removeEntity(@Entity entity: Int) {
        renderer.filamentScene.removeEntity(entity)
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

        val start = System.currentTimeMillis()

        renderer.filamentView.pick(x, invertedY, pickingHandler) { pickResult ->
            val end = System.currentTimeMillis()

            Log.d("Test", "Picking took ${end - start} ms")

            val pickedRenderable = pickResult.renderable
            val pickedNode = allChildren
                .mapNotNull { it as? ModelNode }
                .firstOrNull { modelNode ->
                    modelNode.modelInstance?.let { modelInstance ->
                        modelInstance.entity == pickedRenderable ||
                                modelInstance.childEntities.contains(
                                    pickedRenderable
                                )
                    } ?: false
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