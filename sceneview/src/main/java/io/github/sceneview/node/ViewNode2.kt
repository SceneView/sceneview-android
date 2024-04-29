package io.github.sceneview.node

import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.Scene
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import io.github.sceneview.collision.HitResult
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Size
import io.github.sceneview.node.ViewNode2.WindowManager
import io.github.sceneview.safeDestroyMaterialInstance
import io.github.sceneview.safeDestroyStream
import io.github.sceneview.safeDestroyTexture

/**
 * A Node that can display an Android [View]
 *
 * This node contains a View for the rendering engine to render.
 *
 * Manages a [FrameLayout] that is attached directly to a [WindowManager] that other views can be
 * added and removed from.
 *
 * To render a [View], the [View] must be attached to a [WindowManager] so that it can be properly
 * drawn. This class encapsulates a [FrameLayout] that is attached to a [WindowManager] that other
 * views can be added to as children. This allows us to safely and correctly draw the [View]
 * associated with a [RenderableManager] [Entity] and a [MaterialInstance] while keeping them
 * isolated from the rest of the activities View hierarchy.
 *
 * Additionally, this manages the lifecycle of the window to help ensure that the window is
 * added/removed from the WindowManager at the appropriate times.
 *
 * @param view The 2D Android [View] that is rendered by this [ViewNode2]
 * @param unlit True to disable all lights influences on the rendered view
 * @param invertFrontFaceWinding Inverts the winding order of front faces.
 * Inverting the winding order of front faces is useful when rendering mirrored reflections
 * (water, mirror surfaces, front camera in AR, etc.).
 * True to invert front faces, false otherwise
 */
@RequiresApi(Build.VERSION_CODES.P)
class ViewNode2(
    engine: Engine,
    val windowManager: WindowManager,
    materialLoader: MaterialLoader,
    view: View,
    unlit: Boolean = false,
    invertFrontFaceWinding: Boolean = false,
    // This seems a little high, but lower values cause occasional "client tried to acquire
    // more than maxImages buffers" on a Pixel 3
    val imageReaderMaxImages: Int = 7
) : PlaneNode(engine = engine) {

    var surface: Surface? = null

    // Updated when the view is added to the view manager
    var pxPerUnits = 250.0f
        set(value) {
            field = value
            updateGeometrySize()
        }

    var viewSize = Size(0.0f)
        set(value) {
            field = value
            updateGeometrySize()
        }

    val layout: Layout = Layout(view.context).apply {
        addView(view)
    }

    private var imageReader: ImageReader? = null
    private val picture = Picture()
    private val directImageHandler = Handler(Looper.getMainLooper())

    val stream: Stream = Stream.Builder()
        .build(engine)

    val texture: Texture = Texture.Builder()
        .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
        .format(Texture.InternalFormat.RGB8)
        .build(engine)
        .apply {
            setExternalStream(engine, stream)
        }

    init {
        materialInstance = materialLoader.createViewInstance(
            viewTexture = texture,
            unlit = unlit,
            invertFrontFaceWinding = invertFrontFaceWinding
        )
    }

    constructor(
        engine: Engine,
        windowManager: WindowManager,
        materialLoader: MaterialLoader,
        @LayoutRes viewLayoutRes: Int,
        unlit: Boolean = false,
        invertFrontFaceWinding: Boolean = false
    ) : this(
        engine = engine,
        windowManager = windowManager,
        materialLoader = materialLoader,
        view = LayoutInflater.from(materialLoader.context).inflate(viewLayoutRes, null, false),
        unlit = unlit,
        invertFrontFaceWinding = invertFrontFaceWinding
    )

    /**
     * Set the Jetpack Compose UI content for this view.
     * Initial composition will occur when the view becomes attached to a window or when
     * createComposition is called, whichever comes first.
     *
     * @param content the themed composable.
     * E.g.
     * ```
     * MaterialTheme {
     *     // In Compose world
     *     Text("Hello Compose!")
     * }
     */
    constructor(
        engine: Engine,
        windowManager: WindowManager,
        materialLoader: MaterialLoader,
        unlit: Boolean = false,
        invertFrontFaceWinding: Boolean = false,
        content: @Composable () -> Unit
    ) : this(
        engine = engine,
        windowManager = windowManager,
        materialLoader = materialLoader,
        view = ComposeView(materialLoader.context).apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent(content)
        },
        unlit = unlit,
        invertFrontFaceWinding = invertFrontFaceWinding
    )

    fun updateGeometrySize() {
        updateGeometry(size = viewSize / pxPerUnits)
    }

    override fun onAddedToScene(scene: Scene) {
        super.onAddedToScene(scene)

        windowManager.addView(layout)
    }

    override fun onRemovedFromScene(scene: Scene) {
        super.onRemovedFromScene(scene)

        windowManager.removeView(layout)
    }

    override fun onTouchEvent(e: MotionEvent, hitResult: HitResult): Boolean {
        return super.onTouchEvent(e, hitResult).also {
            //TODO
//        val scene: SceneView = node.getSceneViewInternal() ?: return false
//        val pointerCount = motionEvent.pointerCount
//        val pointerProperties = arrayOfNulls<MotionEvent.PointerProperties>(pointerCount)
//        val pointerCoords = arrayOfNulls<MotionEvent.PointerCoords>(pointerCount)
//        val nodeTransformMatrix: Matrix = node.getTransformationMatrix()
//        val nodePosition = Vector3()
//        nodeTransformMatrix.decomposeTranslation(nodePosition)
//        val nodeScale = Vector3()
//        nodeTransformMatrix.decomposeScale(nodeScale)
//        val nodeRotation = Quaternion()
//        nodeTransformMatrix.decomposeRotation(nodeScale, nodeRotation)
//        val nodeForward = Quaternion.rotateVector(nodeRotation, Vector3.forward())
//        val nodeBack = Quaternion.rotateVector(nodeRotation, Vector3.back())
//
//        /*
//     * Cast a ray against a plane that extends to infinity located where the view is in 3D space
//     * instead of casting against the node's collision shape. This is important for the UX of touch
//     * events after the initial ACTION_DOWN event. i.e. If a user is dragging a slider and their
//     * finger moves beyond the view the position of their finger relative to the slider should still
//     * be respected.
//     */
//        val plane = Plane(nodePosition, nodeForward)
//        val rayHit = RayHit()
//
//        // Also cast a ray against a back-facing plane because we render the view as double-sided.
//        val backPlane = Plane(nodePosition, nodeBack)
//
//        // Convert the pointer coordinates for each pointer into the view's local coordinate space.
//        for (i in 0 until pointerCount) {
//            val props = MotionEvent.PointerProperties()
//            val coords = MotionEvent.PointerCoords()
//            motionEvent.getPointerProperties(i, props)
//            motionEvent.getPointerCoords(i, coords)
//            val camera: CameraNode? = scene.cameraNode
//            val ray = camera!!.screenPointToRay(coords.x, coords.y)
//            if (plane.rayIntersection(ray, rayHit)) {
//                val viewPosition = convertWorldPositionToLocalView(node, rayHit.point)
//                coords.x = viewPosition.x
//                coords.y = viewPosition.y
//            } else if (backPlane.rayIntersection(ray, rayHit)) {
//                val viewPosition = convertWorldPositionToLocalView(node, rayHit.point)
//
//                // Flip the x coordinate for the back-facing plane.
//                coords.x = getView().width - viewPosition.x
//                coords.y = viewPosition.y
//            } else {
//                coords.clear()
//                props.clear()
//            }
//            pointerProperties[i] = props
//            pointerCoords[i] = coords
//        }
//
//        // We must copy the touch event with the new coordinates and dispatch it to the view.
//        val me = MotionEvent.obtain(
//            motionEvent.downTime,
//            motionEvent.eventTime,
//            motionEvent.action,
//            pointerCount,
//            pointerProperties,
//            pointerCoords,
//            motionEvent.metaState,
//            motionEvent.buttonState,
//            motionEvent.xPrecision,
//            motionEvent.yPrecision,
//            motionEvent.deviceId,
//            motionEvent.edgeFlags,
//            motionEvent.source,
//            motionEvent.flags
//        )
//        return getView().dispatchTouchEvent(me)
        }
    }

//    fun convertWorldPositionToLocalView(node: Node, worldPos: Vector3?): Vector3 {
//        Preconditions.checkNotNull(node, "Parameter \"node\" was null.")
//        Preconditions.checkNotNull(worldPos, "Parameter \"worldPos\" was null.")
//
//        // Find where the view renderable is being touched in local space.
//        // this will be in meters relative to the bottom-middle of the view.
//        val localPos: Vector3 = node.getTransformationMatrixInverted().transformPoint(worldPos)
//
//        // Calculate the pixels to meters ratio.
//        val view = getView()
//        val width = view.width
//        val height = view.height
//        val pixelsToMetersRatio = getPixelsToMetersRatio()
//
//        // We must convert the position to pixels
//        var xPixels = (localPos.x * pixelsToMetersRatio).toInt()
//        var yPixels = (localPos.y * pixelsToMetersRatio).toInt()
//
//        // We must convert the coordinates from the renderable's alignment origin to top-left origin.
//        val halfWidth = width / 2
//        val halfHeight = height / 2
//        val verticalAlignment = getVerticalAlignment()
//        yPixels = when (verticalAlignment) {
//            VerticalAlignment.BOTTOM -> height - yPixels
//            VerticalAlignment.CENTER -> height - (yPixels + halfHeight)
//            VerticalAlignment.TOP -> height - (yPixels + height)
//        }
//        val horizontalAlignment = getHorizontalAlignment()
//        when (horizontalAlignment) {
//            HorizontalAlignment.LEFT -> {}
//            HorizontalAlignment.CENTER -> xPixels = xPixels + halfWidth
//            HorizontalAlignment.RIGHT -> xPixels = xPixels + width
//        }
//        return Vector3(xPixels.toFloat(), yPixels.toFloat(), 0.0f)
//    }

    override fun destroy() {
        super.destroy()

        engine.safeDestroyMaterialInstance(materialInstance)
        engine.safeDestroyTexture(texture)
        engine.safeDestroyStream(stream)

        windowManager.removeView(layout)
    }

    /**
     * Used to render an Android view to a native open GL texture that can then be rendered by
     * Filament.
     *
     * To correctly draw a hardware accelerated animated view to a surface texture, the view MUST be
     * attached to a window and drawn to a real DisplayListCanvas, which is a hidden class.
     * To achieve this, the following is done:
     *
     *  - Attach [Layout] to the [WindowManager].
     *  - Override dispatchDraw.
     *  - Call super.dispatchDraw with the real DisplayListCanvas
     *  - Draw the clear color the DisplayListCanvas so that it isn't visible on screen.
     *  - Draw the view to the SurfaceTexture every frame. This must be done every frame, because
     *  the view will not be marked as dirty when child views are animating when hardware
     *  accelerated.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    inner class Layout @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        defStyleRes: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)

            // Only called when we first get View size
            imageReader?.close()
            imageReader = ImageReader.newInstance(
                width,
                height,
                ImageFormat.RGB_565,
                imageReaderMaxImages
            )
            surface?.release()
            surface = imageReader?.surface
        }

        override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
            super.onSizeChanged(width, height, oldWidth, oldHeight)

            viewSize = Size(width.toFloat(), height.toFloat())
        }

        override fun dispatchDraw(canvas: Canvas) {
            if (!isAttachedToWindow) return
            // Check for Stream validity
            val stream = stream.takeIf { it.timestamp > 0 } ?: return

            // Sanity that the surface is valid.
            val viewSurface = surface?.takeIf { it.isValid } ?: return
            if (isDirty) {
                val pictureCanvas = picture.beginRecording(width, height)
                pictureCanvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                super.dispatchDraw(pictureCanvas)
                picture.endRecording()
                val surfaceCanvas = viewSurface.lockCanvas(null)
                picture.draw(surfaceCanvas)
                viewSurface.unlockCanvasAndPost(surfaceCanvas)

                val image = imageReader!!.acquireLatestImage()
                stream.setAcquiredImage(
                    image.hardwareBuffer!!,
                    directImageHandler
                ) {
                    image.close()
                }
            }
            // Ask for redraw to update on each frames until stream is null
            invalidate()
        }
    }

    class WindowManager(context: Context) {

        private val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager

        val layout by lazy { FrameLayout(context) }

        fun addView(view: View) = layout.addView(view)
        fun addView(view: View, params: FrameLayout.LayoutParams) = layout.addView(view, params)

        fun removeView(view: View) = layout.removeView(view)

        /**
         * An owner View can only be added to the WindowManager after the activity has finished
         * resuming.
         * Therefore, we must use post to ensure that the window is only added after resume is finished.
         */
        fun resume(ownerView: View) {
            // A ownerView can only be added to the WindowManager after the activity has finished resuming.
            // Therefore, we must use post to ensure that the window is only added after resume is finished.
            ownerView.post {
                if (ownerView.isAttachedToWindow) {
                    tryAttachingView()
                }
            }
        }

        /**
         * The [layout] must be removed from the [windowManager] before the activity is destroyed, or
         * the window will be leaked. Therefore we add/remove the ownerView in resume/pause.
         */
        fun pause() {
            tryDetachingView()
        }

        fun destroy() {
            tryDetachingView()
        }

        private fun tryAttachingView() = runCatching {
            if (layout.parent == null) {
                windowManager.addView(layout, LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.TYPE_APPLICATION_PANEL,
                    LayoutParams.FLAG_NOT_FOCUSABLE
                            or LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            or LayoutParams.FLAG_NOT_TOUCHABLE
                            or LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    title = "ViewNodeWindowManager"
                })
            }
        }


        private fun tryDetachingView() = runCatching {
            if (layout.parent != null) {
                windowManager.removeView(layout)
            }
        }
    }
}
