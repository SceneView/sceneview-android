package io.github.sceneview.ar.scene

import com.google.ar.core.Anchor

/**
 * Convenience alias for [Anchor.detach] that follows the SceneView "destroy" naming convention.
 *
 * Detaches this anchor from the ARCore session, releasing its tracking resources.
 */
fun Anchor.destroy() {
    detach()
}