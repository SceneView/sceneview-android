package io.github.sceneview.material

import android.graphics.SurfaceTexture
import android.view.Surface
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Stream
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.safeDestroyStream
import io.github.sceneview.safeDestroyTexture
import io.github.sceneview.texture.VideoTexture

class VideoMaterial(
    val engine: Engine,
    materialLoader: MaterialLoader,
    chromaKeyColor: Int? = null
) {
    /**
     * Images drawn to the Surface will be made available to the Filament Stream.
     */
    val surfaceTexture = SurfaceTexture(0).apply {
        detachFromGLContext()
    }

    /**
     * The Android surface.
     */
    val surface = Surface(surfaceTexture)

    /**
     * The Filament Stream.
     */
    val stream = Stream.Builder()
        .stream(surfaceTexture)
        .build(engine)

    /**
     * The Filament Texture diffusing the stream.
     */
    val texture = VideoTexture.Builder()
        .stream(stream)
        .build(engine)

    val instance: MaterialInstance = materialLoader.createVideoInstance(texture, chromaKeyColor)

    init {
        instance.setExternalTexture(texture)
    }

    fun destroy() {
        engine.safeDestroyTexture(texture)
        engine.safeDestroyStream(stream)
        surface.release()
        surfaceTexture.release()
    }
}