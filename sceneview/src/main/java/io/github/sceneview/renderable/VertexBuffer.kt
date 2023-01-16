package io.github.sceneview.renderable

import androidx.lifecycle.Lifecycle
import com.google.android.filament.VertexBuffer
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament
import java.nio.Buffer

fun VertexBuffer.Builder.build(lifecycle: Lifecycle? = null): VertexBuffer = build(Filament.engine)
    .also { vertexBuffer ->
        lifecycle?.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { vertexBuffer.destroy() }
        })
    }

fun VertexBuffer.setBufferAt(bufferIndex: Int, buffer: Buffer) =
    setBufferAt(Filament.engine, bufferIndex, buffer)

fun VertexBuffer.setBufferAt(bufferIndex: Int, buffer: Buffer, destOffsetInBytes: Int, count: Int) =
    setBufferAt(Filament.engine, bufferIndex, buffer, destOffsetInBytes, count)

fun VertexBuffer.destroy() = Filament.engine.destroyVertexBuffer(this)