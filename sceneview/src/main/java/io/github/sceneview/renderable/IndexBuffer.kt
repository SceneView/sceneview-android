package io.github.sceneview.renderable

import com.google.android.filament.IndexBuffer
import io.github.sceneview.Filament
import java.nio.Buffer

fun IndexBuffer.Builder.build(): IndexBuffer = build(Filament.engine)

fun IndexBuffer.setBuffer(buffer: Buffer) = setBuffer(Filament.engine, buffer)
fun IndexBuffer.setBuffer(buffer: Buffer, destOffsetInBytes: Int, count: Int) =
    setBuffer(Filament.engine, buffer, destOffsetInBytes, count)

fun IndexBuffer.destroy() {
    runCatching { Filament.engine.destroyIndexBuffer(this) }
}