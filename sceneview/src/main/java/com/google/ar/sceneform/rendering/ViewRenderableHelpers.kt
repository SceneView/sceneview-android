package com.google.ar.sceneform.rendering

import android.content.res.Resources
import android.util.DisplayMetrics
import android.view.View

/** Helper class for utility functions for a view rendered in world space. */
object ViewRenderableHelpers {
    /** Returns the aspect ratio of a view (width / height). */
    @JvmStatic
    fun getAspectRatio(view: View): Float {
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()

        if (viewWidth == 0.0f || viewHeight == 0.0f) {
            return 0.0f
        }

        return viewWidth / viewHeight
    }

    /**
     * Returns the number of density independent pixels that a given number of pixels is equal to on
     * this device.
     */
    @JvmStatic
    fun convertPxToDp(px: Int): Float {
        val displayMetrics = Resources.getSystem().displayMetrics
        return px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)
    }
}
