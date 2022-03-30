package io.github.sceneview.ar.scene

import com.google.ar.core.Anchor

fun Anchor.destroy() {
    detach()
}