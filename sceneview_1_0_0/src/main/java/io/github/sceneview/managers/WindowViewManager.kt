@file:JvmName("ViewRenderableManagerKt")

package io.github.sceneview.managers

import android.content.Context
import android.graphics.PixelFormat
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.math.Size
import io.github.sceneview.texture.ViewStream

/**
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
 * @param width The container width (used in case of MATCH_PARENT child)
 * @param height The container height (used in case of MATCH_PARENT child)
 * @param pxPerUnits The number of pixels within a unit. This value should be updated by a SceneView
 */
class WindowViewManager(context: Context, width: Int, height: Int, pxPerUnits: () -> Size) {

    private var _windowManager: WindowManager? =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val windowManager get() = _windowManager!!
    private var _windowManagerLayout: FrameLayout? = FrameLayout(context)
    private val windowManagerLayout get() = _windowManagerLayout!!
    private var _pxPerUnits: (() -> Size)? = pxPerUnits
    private val pxPerUnits get() = _pxPerUnits!!

    init {
        windowManagerLayout.layoutParams = ViewGroup.LayoutParams(width, height)
    }

    constructor(sceneView: SceneView) : this(
        sceneView.context,
        sceneView.width,
        sceneView.height,
        { sceneView.pxPerUnit }
    )

    /**
     * Add a [View] as a child of the [FrameLayout] that is attached to the [WindowManager].
     *
     * Ensure that the [view] is drawn with all appropriate lifecycle events being called correctly.
     */
    fun addView(view: ViewStream) {
        view.pxPerUnits = pxPerUnits.invoke()
        windowManagerLayout.addView(view)
    }

    /**
     * Remove a [View] from the [FrameLayout] that is attached to the [WindowManager].
     *
     * Remove the [view] that no longer need to be drawn.
     */
    fun removeView(view: View) {
        windowManagerLayout.removeView(view)
    }

    /**
     * An owner View can only be added to the WindowManager after the activity has finished
     * resuming.
     * Therefore, we must use post to ensure that the window is only added after resume is finished.
     */
    fun resume(view: View) {
        view.post {
            if (windowManagerLayout.parent == null && view.isAttachedToWindow) {
                windowManager.addView(windowManagerLayout, WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT
                ).apply { title = "WindowViewManager" })
            }
        }
    }

    /**
     * The [layout] must be removed from the [windowManager] before the activity is destroyed, or
     * the window will be leaked. Therefore we add/remove the ownerView in resume/pause.
     */
    fun pause() {
        if (windowManagerLayout.parent != null) {
            windowManager.removeView(windowManagerLayout)
        }
    }

    /**
     * The container size (used in case of MATCH_PARENT child)
     */
    fun setSize(width: Int, height: Int) {
        windowManagerLayout.layoutParams = windowManagerLayout.layoutParams.apply {
            this.width = width
            this.height = height
        }
    }

    fun destroy() {
        _pxPerUnits = null
        _windowManagerLayout = null
        _windowManager = null
    }
}