package io.github.sceneview.sample.araugmentedimage.video

import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.filament.Engine
import com.google.android.filament.RenderableManager
import dev.romainguy.kotlin.math.normalize
import io.github.sceneview.geometries.Plane
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.material.VideoMaterial
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size
import io.github.sceneview.node.PlaneNode

open class ExoPlayerNode(
    private val videoMaterial: VideoMaterial,
    val exoPlayer: ExoPlayer,
    size: Size = Plane.DEFAULT_SIZE,
    center: Position = Plane.DEFAULT_CENTER,
    normal: Direction = Plane.DEFAULT_NORMAL,
    rotateToNode: Boolean = false,
    builderApply: RenderableManager.Builder.() -> Unit = {},
) : PlaneNode(
    engine = videoMaterial.engine,
    size = if (rotateToNode) Size(x = size.x, y = 0.0f, z = size.y) else size,
    center = if (rotateToNode) Position(x = 0.0f, y = center.z, z = 0.0f) else center,
    normal = normal,
    materialInstance = videoMaterial.instance,
    builderApply = builderApply,
) {
    init {
        exoPlayer.setVideoSurface(videoMaterial.surface)
        if (size == Plane.DEFAULT_SIZE) {
            exoPlayer.doOnVideoSized { player, width, height ->
                if (exoPlayer == player) {
                    updateGeometry(
                        size = normalize(
                            when (rotateToNode) {
                                true -> Size(x = width.toFloat(), y = 0.0f, z = height.toFloat())
                                false -> Size(x = width.toFloat(), y = height.toFloat(), z = 0.0f)
                            },
                        ),
                    )
                }
            }
        }
    }

    constructor(
        engine: Engine,
        materialLoader: MaterialLoader,
        exoPlayer: ExoPlayer,
        chromaKeyColor: Int? = null,
        rotateToNode: Boolean = false,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        builderApply: RenderableManager.Builder.() -> Unit = {},
    ) : this(
        videoMaterial = VideoMaterial(
            engine = engine,
            materialLoader = materialLoader,
            chromaKeyColor = chromaKeyColor,
        ),
        exoPlayer = exoPlayer,
        rotateToNode = rotateToNode,
        size = size,
        center = center,
        normal = normal,
        builderApply = builderApply,
    )

    override fun destroy() {
        super.destroy()
        exoPlayer.release()
        videoMaterial.destroy()
    }

    override fun updateVisibility() {
        super.updateVisibility()
        when (isVisible) {
            true -> if (exoPlayer.isPlaying.not()) exoPlayer.play()
            else -> if (exoPlayer.isPlaying) exoPlayer.pause()
        }
    }
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.doOnVideoSized(block: (player: ExoPlayer, videoWidth: Int, videoHeight: Int) -> Unit) {
    val videoWidth = videoFormat?.width ?: 0
    val videoHeight = videoFormat?.height ?: 0
    if (videoWidth > 0 && videoHeight > 0) {
        block(this, videoWidth, videoHeight)
    } else {
        addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val width = videoFormat?.width ?: 0
                        val height = videoFormat?.height ?: 0
                        if (width > 0 && height > 0) {
                            block(this@doOnVideoSized, width, height)
                        }
                    }
                }
            },
        )
    }
}