package com.google.ar.sceneform.rendering

import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout

/**
 * Manages a [FrameLayout] that is attached directly to a [WindowManager] that other
 * views can be added and removed from.
 *
 * To render a [View], the [View] must be attached to a [WindowManager] so that
 * it can be properly drawn. This class encapsulates a [FrameLayout] that is attached to a
 * [WindowManager] that other views can be added to as children. This allows us to safely and
 * correctly draw the [View] associated with [ViewRenderable]'s while keeping them
 * isolated from the rest of the activities View hierarchy.
 *
 * Additionally, this manages the lifecycle of the window to help ensure that the window is
 * added/removed from the WindowManager at the appropriate times.
 *
 * @hide
 */
// TODO: Create Unit Tests for this class.
class ViewAttachmentManager(context: Context, private val ownerView: View) {
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val windowLayoutParams: WindowManager.LayoutParams = createWindowLayoutParams()

    private val frameLayout: FrameLayout = FrameLayout(context)
    private val viewLayoutParams: ViewGroup.LayoutParams = createViewLayoutParams()

    fun getFrameLayout(): FrameLayout = frameLayout

    fun onResume() {
        // A ownerView can only be added to the WindowManager after the activity has finished resuming.
        // Therefore, we must use post to ensure that the window is only added after resume is finished.
        ownerView.post {
            if (frameLayout.parent == null && ownerView.isAttachedToWindow) {
                windowManager.addView(frameLayout, windowLayoutParams)
            }
        }
    }

    fun onPause() {
        // The ownerView must be removed from the WindowManager before the activity is destroyed, or the
        // window will be leaked. Therefore we add/remove the ownerView in resume/pause.
        if (frameLayout.parent != null) {
            windowManager.removeView(frameLayout)
        }
    }

    /**
     * Add a ownerView as a child of the [FrameLayout] that is attached to the [WindowManager].
     *
     * Used by [RenderViewToExternalTexture] to ensure that the ownerView is drawn with all
     * appropriate lifecycle events being called correctly.
     */
    fun addView(view: View) {
        if (view.parent == frameLayout) {
            return
        }
        frameLayout.addView(view, viewLayoutParams)
    }

    /**
     * Remove a ownerView from the [FrameLayout] that is attached to the [WindowManager].
     *
     * Used by [RenderViewToExternalTexture] to remove ownerView's that no longer need to be drawn.
     */
    fun removeView(view: View) {
        if (view.parent != frameLayout) {
            return
        }
        frameLayout.removeView(view)
    }

    companion object {
        private const val VIEW_RENDERABLE_WINDOW = "ViewRenderableWindow"

        private fun createWindowLayoutParams(): WindowManager.LayoutParams {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            )
            params.title = VIEW_RENDERABLE_WINDOW
            return params
        }

        private fun createViewLayoutParams(): ViewGroup.LayoutParams {
            return ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }
}
