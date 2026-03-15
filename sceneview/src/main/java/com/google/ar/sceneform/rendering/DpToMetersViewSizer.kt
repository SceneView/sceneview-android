package com.google.ar.sceneform.rendering

import android.view.View
import io.github.sceneview.collision.Preconditions
import io.github.sceneview.collision.Vector3

/**
 * Controls the size of a [ViewRenderable] in a [com.google.ar.sceneform.Scene] by
 * defining how many dp (density-independent pixels) there are per meter. This is recommended when
 * using an android layout that is built using dp.
 *
 * @see ViewRenderable.Builder.setSizer
 * @see ViewRenderable.setSizer
 */
class DpToMetersViewSizer(
    /**
     * Constructor for creating a sizer for controlling the size of a [ViewRenderable] by
     * defining how many dp there are per meter.
     *
     * @param dpPerMeters a number greater than zero representing the ratio of dp to meters
     */
    val dpPerMeters: Int
) : ViewSizer {

    init {
        if (dpPerMeters <= 0) {
            throw IllegalArgumentException("dpPerMeters must be greater than zero.")
        }
    }

    override fun getSize(view: View): Vector3 {
        Preconditions.checkNotNull(view, "Parameter \"view\" was null.")

        val widthDp = ViewRenderableHelpers.convertPxToDp(view.width)
        val heightDp = ViewRenderableHelpers.convertPxToDp(view.height)

        return Vector3(widthDp / dpPerMeters, heightDp / dpPerMeters, DEFAULT_SIZE_Z)
    }

    companion object {
        // Defaults to zero, Z value of the size doesn't currently have any semantic meaning,
        // but we may add that in later if we support ViewRenderables that have depth.
        private const val DEFAULT_SIZE_Z = 0.0f
    }
}
