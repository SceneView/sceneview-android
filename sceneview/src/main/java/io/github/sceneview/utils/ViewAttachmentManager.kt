package io.github.sceneview.utils

import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.LifecycleOwner
import io.github.sceneview.SceneLifecycle
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneView


private const val VIEW_RENDERABLE_WINDOW = "ViewRenderableWindow"

/**
 * ### Manages a [FrameLayout] that is attached directly to a [WindowManager] that other
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
 */
class ViewAttachmentManager(val sceneView: SceneView, val lifecycle: SceneLifecycle) :
    SceneLifecycleObserver {
    val context get() = sceneView.context

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val windowLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT
    ).apply { title = VIEW_RENDERABLE_WINDOW }
    private val containerLayout: FrameLayout = FrameLayout(context)
    private val viewLayoutParams: ViewGroup.LayoutParams
        get() = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

    init {
        lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)

        // A SceneView can only be added to the WindowManager after the activity has finished
        // resuming. Therefore, we must use post to ensure that the window is only added after
        // resume is finished.
        sceneView.post {
            if (containerLayout.parent == null && sceneView.isAttachedToWindow) {
                windowManager.addView(containerLayout, windowLayoutParams)
            }
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)

        // The ownerView must be removed from the WindowManager before the activity is destroyed,
        // or the window will be leaked. Therefore we add/remove the ownerView in resume/pause.
        if (containerLayout.parent != null) {
            windowManager.removeView(containerLayout)
        }
    }

    /**
     * ### Add a ownerView as a child of the [FrameLayout] that is attached to the [ ].
     *
     * Used by [RenderViewToExternalTexture] to ensure that the ownerView is drawn with all
     * appropriate lifecycle events being called correctly.
     */
    fun addView(view: View) {
        if (view.parent != containerLayout) {
            containerLayout.addView(view, viewLayoutParams)
        }
    }

    /**
     * ### Remove a ownerView from the [FrameLayout] that is attached to the [WindowManager].
     *
     * Used by [RenderViewToExternalTexture] to remove ownerView's that no longer need to be drawn.
     */
    fun removeView(view: View) {
        if (view.parent == containerLayout) {
            containerLayout.removeView(view)
        }
    }
}