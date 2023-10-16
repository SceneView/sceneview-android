package io.github.sceneview.texture

import com.google.android.filament.Stream
import io.github.sceneview.SceneView

fun Stream.Builder.build(sceneView: SceneView) = build(sceneView.engine).also {
    sceneView.streams += it
}

fun SceneView.destroyStream(stream: Stream) {
    engine.destroyStream(stream)
    streams -= stream
}