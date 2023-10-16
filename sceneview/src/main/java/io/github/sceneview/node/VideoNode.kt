package io.github.sceneview.node

import android.media.MediaPlayer
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Plane
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.material.VideoMaterial
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class VideoNode(
    videoMaterial: VideoMaterial,
    val player: MediaPlayer,
    size: Size = Plane.DEFAULT_SIZE,
    center: Position = Plane.DEFAULT_CENTER,
    normal: Direction = Plane.DEFAULT_NORMAL,
    scaleToVideoRatio: Boolean = true,
    /**
     * Keep the video aspect ratio.
     * - `true` to fit within max width or height
     * - `false` the video will be stretched to the node scale
     */
    keepAspectRatio: Boolean = true,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : PlaneNode(
    engine = videoMaterial.engine,
    size = size,
    center = center,
    normal = normal,
    materialInstance = videoMaterial.instance,
    renderableApply = renderableApply
) {
    init {
        if (scaleToVideoRatio) {
            player.doOnVideoSized { player, width, height ->
                if (player == this.player) {
                    if (keepAspectRatio) {
                        scale = scale.apply {
                            x = if (width >= height) 1.0f else width.toFloat() / height.toFloat()
                            y = if (width >= height) height.toFloat() / width.toFloat() else 1.0f
                        }
                    }
                }
            }
        }
    }

    constructor(
        materialLoader: MaterialLoader,
        player: MediaPlayer,
        chromaKeyColor: Int? = null,
        scaleToVideoRatio: Boolean = true,
        /**
         * Keep the video aspect ratio.
         * - `true` to fit within max width or height
         * - `false` the video will be stretched to the node scale
         */
        keepAspectRatio: Boolean = true,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        renderableApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        videoMaterial = materialLoader.createVideoMaterial(player, chromaKeyColor),
        player = player,
        scaleToVideoRatio = scaleToVideoRatio,
        keepAspectRatio = keepAspectRatio,
        size = size,
        center = center,
        normal = normal,
        renderableApply = renderableApply
    )

    override fun destroy() {
        super.destroy()

        player.stop()
    }
}


fun MediaPlayer.doOnVideoSized(block: (player: MediaPlayer, videoWidth: Int, videoHeight: Int) -> Unit) {
    if (videoWidth > 0 && videoHeight > 0) {
        block(this, videoWidth, videoHeight)
    } else {
        setOnVideoSizeChangedListener { _, width: Int, height: Int ->
            if (width > 0 && height > 0) {
                block(this, width, height)
            }
        }
    }
}