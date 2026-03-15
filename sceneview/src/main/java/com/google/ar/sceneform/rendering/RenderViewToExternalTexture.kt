package com.google.ar.sceneform.rendering

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Picture
import android.graphics.PorterDuff
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import com.google.android.filament.Engine
import io.github.sceneview.collision.Preconditions

/**
 * Used to render an android view to a native open GL texture that can then be rendered by open GL.
 *
 * To correctly draw a hardware accelerated animated view to a surface texture, the view MUST be
 * attached to a window and drawn to a real DisplayListCanvas, which is a hidden class. To achieve
 * this, the following is done:
 *
 * - Attach RenderViewToSurfaceTexture to the WindowManager.
 * - Override dispatchDraw.
 * - Call super.dispatchDraw with the real DisplayListCanvas
 * - Draw the clear color the DisplayListCanvas so that it isn't visible on screen.
 * - Draw the view to the SurfaceTexture every frame. This must be done every frame, because the
 *   view will not be marked as dirty when child views are animating when hardware accelerated.
 *
 * @hide
 */
class RenderViewToExternalTexture(engine: Engine, context: Context, private val view: View) :
    FrameLayout(context) {

    /** Interface definition for a callback to be invoked when the size of the view changes. */
    fun interface OnViewSizeChangedListener {
        fun onViewSizeChanged(width: Int, height: Int)
    }

    val externalTexture: ExternalTexture = ExternalTexture(engine)
    private val picture = Picture()
    private var hasDrawnToSurfaceTexture = false

    private var viewAttachmentManager: ViewAttachmentManager? = null
    private val onViewSizeChangedListeners = ArrayList<OnViewSizeChangedListener>()

    init {
        Preconditions.checkNotNull(view, "Parameter \"view\" was null.")
        addView(view)
    }

    /**
     * Register a callback to be invoked when the size of the view changes.
     *
     * @param onViewSizeChangedListener the listener to attach
     */
    fun addOnViewSizeChangedListener(onViewSizeChangedListener: OnViewSizeChangedListener) {
        if (!onViewSizeChangedListeners.contains(onViewSizeChangedListener)) {
            onViewSizeChangedListeners.add(onViewSizeChangedListener)
        }
    }

    /**
     * Remove a callback to be invoked when the size of the view changes.
     *
     * @param onViewSizeChangedListener the listener to remove
     */
    fun removeOnViewSizeChangedListener(onViewSizeChangedListener: OnViewSizeChangedListener) {
        onViewSizeChangedListeners.remove(onViewSizeChangedListener)
    }

    fun isViewTextureReady(): Boolean = externalTexture.getFilamentStream().timestamp > 0

    fun hasDrawnToSurfaceTexture(): Boolean = hasDrawnToSurfaceTexture

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        externalTexture.getSurfaceTexture().setDefaultBufferSize(view.width, view.height)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        for (listener in onViewSizeChangedListeners) {
            listener.onViewSizeChanged(width, height)
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Sanity that the surface is valid.
        val targetSurface: Surface = externalTexture.getSurface()
        if (!targetSurface.isValid) {
            return
        }

        if (view.isDirty) {
            val pictureCanvas = picture.beginRecording(view.width, view.height)
            pictureCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            super.dispatchDraw(pictureCanvas)
            picture.endRecording()

            val surfaceCanvas = targetSurface.lockCanvas(null)
            picture.draw(surfaceCanvas)
            targetSurface.unlockCanvasAndPost(surfaceCanvas)

            hasDrawnToSurfaceTexture = true
        }

        invalidate()
    }

    fun attachView(viewAttachmentManager: ViewAttachmentManager) {
        if (this.viewAttachmentManager != null) {
            if (this.viewAttachmentManager !== viewAttachmentManager) {
                throw IllegalStateException(
                    "Cannot use the same ViewRenderable with multiple SceneViews."
                )
            }
            return
        }

        this.viewAttachmentManager = viewAttachmentManager
        viewAttachmentManager.addView(this)
    }

    fun detachView() {
        viewAttachmentManager?.let {
            it.removeView(this)
            viewAttachmentManager = null
        }
    }

    fun destroy() {
        detachView()
        externalTexture.destroy()
    }
}
