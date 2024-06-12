package io.github.sceneview.sample.arviewnode.nodes.events

import io.github.sceneview.ar.node.AnchorNode

sealed interface ViewNodeEvent {
    data class NewViewNode(val anchorNode: AnchorNode) : ViewNodeEvent
}