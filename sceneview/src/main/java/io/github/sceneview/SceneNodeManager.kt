package io.github.sceneview

import com.google.android.filament.Scene
import io.github.sceneview.collision.CollisionSystem
import io.github.sceneview.node.Node

/**
 * Manages the Filament scene graph for a fully-composable [io.github.sceneview.Scene] (or
 * [io.github.sceneview.ar.ARScene]) without requiring a [SceneView] instance.
 *
 * Replicates the `addNode` / `removeNode` logic from [SceneView] so that the composable surface
 * variants can manage nodes directly against the Filament [Scene].
 */
class SceneNodeManager(
    val scene: Scene,
    val collisionSystem: CollisionSystem
) {
    fun addNode(node: Node) {
        node.collisionSystem = collisionSystem
        if (node.sceneEntities.isNotEmpty()) {
            scene.addEntities(node.sceneEntities.toIntArray())
        }
        node.onChildAdded += ::addNode
        node.onChildRemoved += ::removeNode
        node.onAddedToScene(scene)
        node.childNodes.forEach { addNode(it) }
    }

    fun removeNode(node: Node) {
        node.collisionSystem = null
        if (node.sceneEntities.isNotEmpty()) {
            scene.removeEntities(node.sceneEntities.toIntArray())
        }
        node.onChildAdded -= ::addNode
        node.onChildRemoved -= ::removeNode
        node.onRemovedFromScene(scene)
        node.childNodes.forEach { removeNode(it) }
    }

    fun replaceNode(oldNode: Node?, newNode: Node?) {
        oldNode?.let { removeNode(it) }
        newNode?.let { addNode(it) }
    }
}
