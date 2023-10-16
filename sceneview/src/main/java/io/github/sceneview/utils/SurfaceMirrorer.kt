package io.github.sceneview.utils

import android.view.Surface
import com.google.android.filament.Renderer
import com.google.android.filament.SwapChain
import com.google.android.filament.Viewport
import io.github.sceneview.SceneView

/**
 * Displays the Camera stream using Filament.
 */
class SurfaceMirrorer {

    data class SurfaceMirror(
        val surface: Surface,
        var swapChain: SwapChain?,
        val viewport: Viewport
    )

    private val surfaceMirrors = mutableListOf<SurfaceMirror>()

    fun onFrame(sceneView: SceneView) {
        surfaceMirrors.forEach { mirror ->
            if (mirror.swapChain != null) {
                sceneView.renderer.copyFrame(
                    mirror.swapChain!!,
                    // TODO could this be moved to [startMirroring]?
                    getLetterboxViewport(sceneView.view.viewport, mirror.viewport),
                    sceneView.view.viewport,
                    Renderer.MIRROR_FRAME_FLAG_COMMIT
                            or Renderer.MIRROR_FRAME_FLAG_SET_PRESENTATION_TIME
                            or Renderer.MIRROR_FRAME_FLAG_CLEAR
                )
            }
        }
    }

    /**
     * Mirror the rendering to a surface.
     *
     * This can be used to video record the actual SceneView rendering.
     *
     * To capture the contents of this view, designate a [Surface] onto which this SceneView should
     * be mirrored. Use [android.media.MediaRecorder.getSurface],
     * [android.media.MediaCodec.createInputSurface] or
     * [android.media.MediaCodec.createPersistentInputSurface] to obtain the input surface for
     * recording. This will incur a rendering performance cost and should only be set when capturing
     * this view. To stop the additional rendering, call [stopMirroring].
     *
     * @param surface the Surface onto which the rendered scene should be mirrored.
     * @param left the left edge of the rectangle into which the view should be mirrored on surface.
     * @param bottom the bottom edge of the rectangle into which the view should be mirrored on
     * surface.
     * @param width the width of the rectangle into which the SceneView should be mirrored on
     * surface.
     * @param height the height of the rectangle into which the SceneView should be mirrored on
     * surface.
     */
    fun startMirroring(
        sceneView: SceneView,
        surface: Surface,
        left: Int = 0,
        bottom: Int = 0,
        width: Int = sceneView.width,
        height: Int = sceneView.height
    ) {
        surfaceMirrors.add(
            SurfaceMirror(
                surface,
                sceneView.engine.createSwapChain(surface),
                Viewport(left, bottom, width, height)
            )
        )
    }

    /**
     * Stops mirroring to the specified [Surface].
     *
     * When capturing is complete, call this method to stop mirroring the SceneView to the specified
     * [Surface]. If this is not called, the additional performance cost will remain.
     *
     * The application is responsible for calling [Surface.release] on the Surface when done.
     */
    fun stopMirroring(sceneView: SceneView, surface: Surface) {
        surfaceMirrors.removeAll(surfaceMirrors.filter { it.surface == surface }.onEach { mirror ->
            mirror.swapChain?.let { runCatching { sceneView.engine.destroySwapChain(it) } }
            mirror.swapChain = null
        })
    }

    private fun getLetterboxViewport(srcViewport: Viewport, destViewport: Viewport): Viewport {
        val scale =
            if (destViewport.width.toFloat() / destViewport.height.toFloat() > srcViewport.width.toFloat() / srcViewport.height.toFloat()) {
                destViewport.height.toFloat() / srcViewport.height.toFloat()
            } else {
                destViewport.width / srcViewport.width.toFloat()
            }
        val width = (srcViewport.width * scale).toInt()
        val height = (srcViewport.height * scale).toInt()
        return Viewport(
            (destViewport.width - width) / 2,
            (destViewport.height - height) / 2,
            width,
            height
        )
    }
}