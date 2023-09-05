package io.github.sceneview.managers

import com.google.android.filament.Engine
import io.github.sceneview.Entity
import io.github.sceneview.nodes.Node

class NodeManager(val engine: Engine) {

    private val entityNodes = mutableMapOf<Entity, Node>()
    val entities get() = entityNodes.keys

    fun hasComponent(e: Entity) = getNode(e) != null

    fun getNode(e: Entity): Node? = entityNodes[e]

    fun addComponent(e: Entity, node: Node) {
        entityNodes[e] = node
    }

    fun removeComponent(e: Entity) {
        entityNodes.remove(e)
    }

    fun getComponentCount() = entityNodes.size

    fun empty(): Boolean = entityNodes.isEmpty()

    fun getEntity(node: Node): Entity? = entityNodes.entries.firstOrNull { it.value == node }?.key

    operator fun get(entity: Entity) = entityNodes[entity]

    fun destroyNode(e: Entity) {
        entityNodes[e]?.let { node ->
            node.destroy()
            removeComponent(e)
        }
    }

    fun destroy() {
        entities.toList().forEach { entity ->
            destroyNode(entity)
        }
        entities.clear()
    }
}