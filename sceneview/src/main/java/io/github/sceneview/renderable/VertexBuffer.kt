package io.github.sceneview.renderable

import com.google.android.filament.VertexBuffer
import io.github.sceneview.Filament
import java.nio.Buffer

fun VertexBuffer.Builder.build(): VertexBuffer = build(Filament.engine)

fun VertexBuffer.setBufferAt(bufferIndex: Int, buffer: Buffer) =
    setBufferAt(Filament.engine, bufferIndex, buffer)

fun VertexBuffer.setBufferAt(bufferIndex: Int, buffer: Buffer, destOffsetInBytes: Int, count: Int) =
    setBufferAt(Filament.engine, bufferIndex, buffer, destOffsetInBytes, count)

fun VertexBuffer.destroy() {
    runCatching { Filament.engine.destroyVertexBuffer(this) }
}