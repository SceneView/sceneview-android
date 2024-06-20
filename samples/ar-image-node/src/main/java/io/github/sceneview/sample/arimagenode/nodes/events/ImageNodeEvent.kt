package io.github.sceneview.sample.arimagenode.nodes.events

import io.github.sceneview.ar.node.AnchorNode

sealed interface ImageNodeEvent {
    data class NewImageNode(val anchorNode: AnchorNode) : ImageNodeEvent
}