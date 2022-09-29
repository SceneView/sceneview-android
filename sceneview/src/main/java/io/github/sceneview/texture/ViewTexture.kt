package io.github.sceneview.texture

import com.google.android.filament.Engine
import com.google.android.filament.Texture
import io.github.sceneview.SceneView

class ViewTexture {

    class Builder : Texture.Builder() {

        private lateinit var viewStream: ViewStream

        init {
            sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            format(Texture.InternalFormat.RGB8)
        }

        fun viewStream(viewStream: ViewStream) = apply {
            this.viewStream = viewStream
        }

        override fun build(engine: Engine): Texture = super.build(engine).apply {
            setViewStream(engine, viewStream)
        }
    }
}

fun Texture.setViewStream(engine: Engine, viewStream: ViewStream) {
    setExternalStream(engine, viewStream.stream)
}

fun Texture.setViewStream(sceneView: SceneView, viewStream: ViewStream) =
    setViewStream(sceneView.engine, viewStream)