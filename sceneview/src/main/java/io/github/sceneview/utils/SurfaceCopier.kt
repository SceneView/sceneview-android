package io.github.sceneview.utils

import android.view.Surface
import androidx.lifecycle.LifecycleOwner
import com.google.android.filament.Renderer
import com.google.android.filament.Viewport
import io.github.sceneview.Filament
import io.github.sceneview.SceneLifecycle
import io.github.sceneview.SceneLifecycleObserver
import io.github.sceneview.SceneView

class SurfaceCopier(val sceneView: SceneView, val lifecycle: SceneLifecycle) :
    SceneLifecycleObserver {

    private val copyDestinations = mutableListOf<CopyDestination>()

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        copyDestinations.forEach { copyDestination ->
            sceneView.renderer.copyFrame(
                copyDestination.swapChain,
                copyDestination.dstViewport,
                copyDestination.srcViewport,
                copyDestination.flags
            )
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)

        copyDestinations.forEach { it.destroy() }
        copyDestinations.clear()
    }

    /**
     * ### Starts copying the currently rendered [SceneView] to the indicated [Surface]
     *
     * Using the indicated source and destination rectangle.
     *
     * @param dstSurface the [Surface] into which the frame should be copied
     * @param srcViewport the source rectangle to be copied
     * @param dstViewport the destination rectangle in which to draw the view
     * @param flags one or more <code>CopyFrameFlag</code> behavior configuration flags
     */
    fun startCopying(
        dstSurface: Surface,
        srcViewport: Viewport = sceneView.view.viewport,
        dstViewport: Viewport = srcViewport,
        flags: Int = DEFAULT_COPY_FLAGS
    ) {
        copyDestinations += CopyDestination(dstSurface, srcViewport, dstViewport, flags)
    }

    /**
     * ### Stops copying to the specified [Surface].
     */
    fun stopCopying(surface: Surface) {
        copyDestinations.filter { it.dstSurface == surface }.forEach {
            it.destroy()
            copyDestinations -= it
        }
    }

    companion object {
        const val DEFAULT_COPY_FLAGS = Renderer.MIRROR_FRAME_FLAG_COMMIT or
                Renderer.MIRROR_FRAME_FLAG_SET_PRESENTATION_TIME or
                Renderer.MIRROR_FRAME_FLAG_CLEAR

        fun getLetterboxViewport(srcViewport: Viewport, destViewport: Viewport): Viewport {
            val srcRatio = srcViewport.width.toFloat() / srcViewport.height.toFloat()
            val destRatio = destViewport.width.toFloat() / destViewport.height.toFloat()
            val scale = if (destRatio > srcRatio) {
                destViewport.height.toFloat() / srcViewport.height.toFloat()
            } else {
                destViewport.width.toFloat() / srcViewport.width.toFloat()
            }
            val width = (srcViewport.width * scale)
            val height = (srcViewport.height * scale)
            val left = (destViewport.width - width) / 2.0f
            val bottom = (destViewport.height - height) / 2.0f
            return Viewport(left.toInt(), bottom.toInt(), width.toInt(), height.toInt())
        }
    }

    /**
     * ### A Frame copy destination
     *
     * @param dstSurface the [Surface] into which the frame should be copied
     * @param srcViewport the source rectangle to be copied
     * @param dstViewport the destination rectangle in which to draw the view
     * @param flags one or more <code>CopyFrameFlag</code> behavior configuration flags
     */
    private class CopyDestination(
        val dstSurface: Surface,
        val srcViewport: Viewport,
        val dstViewport: Viewport,
        val flags: Int
    ) {
        val swapChain = Filament.engine.createSwapChain(dstSurface)

        fun destroy() {
            Filament.engine.destroySwapChain(swapChain)
        }
    }
}