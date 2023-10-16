package io.github.sceneview.texture

import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Picture
import android.graphics.PorterDuff
import android.media.ImageReader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.view.children
import com.google.android.filament.Engine
import com.google.android.filament.Stream
import io.github.sceneview.SceneView
import io.github.sceneview.managers.WindowViewManager
import io.github.sceneview.math.Size

/**
 * Used to render an Android view to a native open GL texture that can then be rendered by
 * Filament.
 *
 * To correctly draw a hardware accelerated animated view to a surface texture, the view MUST be
 * attached to a window and drawn to a real DisplayListCanvas, which is a hidden class.
 * To achieve this, the following is done:
 *
 *  - Attach [ViewStream] to the [WindowManager].
 *  - Override dispatchDraw.
 *  - Call super.dispatchDraw with the real DisplayListCanvas
 *  - Draw the clear color the DisplayListCanvas so that it isn't visible on screen.
 *  - Draw the view to the SurfaceTexture every frame. This must be done every frame, because
 *  the view will not be marked as dirty when child views are animating when hardware
 *  accelerated.
 */
open class ViewStream @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    lateinit var stream: Stream
    var surface: Surface? = null

    // Updated when the view is added to the view manager
    var pxPerUnits: Size = Size(250.0f)
    var worldSize: Size
        get() = _worldSize ?: (Size(width.toFloat(), height.toFloat()) * pxPerUnits)
        set(value) {
            _worldSize = value
        }
    var onSizeChanged: ((Size) -> Unit)? = null

    val view get() = children.firstOrNull()

    private var imageReader: ImageReader? = null
    private val picture = Picture()
    private val directImageHandler = Handler(Looper.getMainLooper())

    private var _worldSize: Size? = null

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        // Only called when we first get View size
        imageReader?.close()
        imageReader = ImageReader.newInstance(
            width,
            height,
            ImageFormat.RGB_565,
            IMAGE_READER_MAX_IMAGES
        ).also {
            surface?.release()
            surface = it.surface
        }
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        onSizeChanged?.invoke(worldSize)
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

    /**
     * Create a [ViewStream]
     *
     * You must attach the created to a window manager in order to draw it and use it as a Texture.
     */
    class Builder {
        private lateinit var view: View

        fun view(view: View) = apply {
            this.view = view
        }

        fun build(engine: Engine, viewWindowViewManager: WindowViewManager): ViewStream {
            val viewStream = ViewStream(view.context).apply {
                stream = Stream.Builder()
                    .build(engine)
                addView(this@Builder.view)
            }
            viewWindowViewManager.addView(viewStream)
            return viewStream
        }

        fun build(sceneView: SceneView): ViewStream =
            build(sceneView.engine, sceneView.windowViewManager).also {
                sceneView.viewStreams += it
            }
    }

    companion object {
        // This seems a little high, but lower values cause occasional "client tried to acquire
        // more than maxImages buffers" on a Pixel 3
        var IMAGE_READER_MAX_IMAGES = 7
    }
}

fun Engine.destroyViewStream(viewStream: ViewStream) {
    viewStream.onSizeChanged = null
    (viewStream.parent as? ViewGroup)?.removeView(viewStream)
    destroyStream(viewStream.stream)
    viewStream.surface?.release()
}

fun SceneView.destroyViewStream(viewStream: ViewStream) {
    destroyViewStream(viewStream)
    viewStreams -= viewStream
}