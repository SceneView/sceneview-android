package io.github.sceneview.texture

import androidx.lifecycle.Lifecycle
import com.google.android.filament.Stream
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament

fun <R> Stream.use(block: (Stream) -> R): R = block(this).also { destroy() }
fun Stream.Builder.build(lifecycle: Lifecycle? = null): Stream = build(Filament.engine)
    .also { stream ->
        lifecycle?.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { stream.destroy() }
        })
    }

fun Stream.destroy() = Filament.engine.destroyStream(this)