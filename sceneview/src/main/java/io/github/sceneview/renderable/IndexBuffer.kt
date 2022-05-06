package io.github.sceneview.renderable

import androidx.lifecycle.Lifecycle
import com.google.android.filament.IndexBuffer
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament
import java.nio.Buffer

fun IndexBuffer.Builder.build(lifecycle: Lifecycle? = null): IndexBuffer = build(Filament.engine)
    .also { indexBuffer ->
        lifecycle?.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { indexBuffer.destroy() }
        })
    }

fun IndexBuffer.setBuffer(buffer: Buffer) = setBuffer(Filament.engine, buffer)
fun IndexBuffer.setBuffer(buffer: Buffer, destOffsetInBytes: Int, count: Int) =
    setBuffer(Filament.engine, buffer, destOffsetInBytes, count)

fun IndexBuffer.destroy() = Filament.engine.destroyIndexBuffer(this)