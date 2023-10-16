package io.github.sceneview.texture

import com.google.android.filament.Engine
import com.google.android.filament.Stream
import com.google.android.filament.Texture

class VideoTexture {
    class Builder : Texture.Builder() {
        private lateinit var stream: Stream

        init {
            sampler(Texture.Sampler.SAMPLER_EXTERNAL)
            format(Texture.InternalFormat.RGBA8)
        }

        fun stream(stream: Stream) = apply {
            this.stream = stream
        }

        override fun build(engine: Engine): Texture = super.build(engine).apply {
            setExternalStream(engine, stream)
        }
    }
}