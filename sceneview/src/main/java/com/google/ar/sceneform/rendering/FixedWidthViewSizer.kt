package com.google.ar.sceneform.rendering

import android.view.View
import io.github.sceneview.collision.Preconditions
import io.github.sceneview.collision.Vector3

/**
 * Controls the size of a [ViewRenderable] in a [com.google.ar.sceneform.Scene] by
 * defining how wide it should be in meters. The height will change to match the aspect ratio of the
 * view.
 *
 * @see ViewRenderable.Builder.setSizer
 * @see ViewRenderable.setSizer
 */
class FixedWidthViewSizer(
    /**
     * Constructor for creating a sizer for controlling the size of a [ViewRenderable] by
     * defining a fixed width.
     *
     * @param widthMeters a number greater than zero representing the width in meters.
     */
    private val widthMeters: Float
) : ViewSizer {

    init {
        if (widthMeters <= 0) {
            throw IllegalArgumentException("widthMeters must be greater than zero.")
        }
    }

    /** Returns the width in meters used for controlling the size of a [ViewRenderable]. */
    fun getWidth(): Float = widthMeters

    override fun getSize(view: View): Vector3 {
        Preconditions.checkNotNull(view, "Parameter \"view\" was null.")

        val aspectRatio = ViewRenderableHelpers.getAspectRatio(view)

        if (aspectRatio == 0.0f) {
            return Vector3.zero()
        }

        return Vector3(widthMeters, widthMeters / aspectRatio, DEFAULT_SIZE_Z)
    }

    companion object {
        // Defaults to zero, Z value of the size doesn't currently have any semantic meaning,
        // but we may add that in later if we support ViewRenderables that have depth.
        private const val DEFAULT_SIZE_Z = 0.0f
    }
}
