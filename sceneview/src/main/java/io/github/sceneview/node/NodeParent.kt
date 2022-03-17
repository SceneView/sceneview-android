package io.github.sceneview.node

import androidx.annotation.UiThread
import java.util.*

/**
 * ### Base class for all classes that can contain a set of nodes as children.
 *
 * The classes [Node] and [Scene] are both NodeParents. To make a [Node] the
 * child of another [Node] or a [Scene], use [Node.setParent].
 */
interface NodeParent {

    /**
     * ### An immutable list of this parent's children
     */
    var _children: List<Node>
    val onChildAdded: ((Node) -> Unit)? get() = null
    val onChildRemoved: ((Node) -> Unit)? get() = null

    var children: List<Node>
        get() = _children
        set(value) {
            val oldChildren = _children
            _children = value
            _children.filter { it !in value }.forEach { oldChild ->
                oldChild.parent = null
                onChildRemoved(oldChild)
            }
            value.filter { it !in oldChildren }.forEach { newChild ->
                newChild.parent = this
                onChildAdded(newChild)
            }
        }


    /**
     * Adds a node as a child of this NodeParent. If the node already has a parent, it is removed from
     * its old parent. If the node is already a direct child of this NodeParent, no change is made.
     *
     * @param child the node to add as a child
     * @throws IllegalArgumentException if the child is the same object as the parent, or if the
     * parent is a descendant of the child
     */
    @UiThread
    fun addChild(child: Node): Node {
        // Return early if the parent hasn't changed.
        if (child !in children) {
            children = children + child
        }
        return child
    }

    /**
     * Removes a node from the children of this NodeParent. If the node is not a direct child of this
     * NodeParent, no change is made.
     *
     * @param child the node to remove from the children
     */
    fun removeChild(child: Node): Node {
        if (child in children) {
            children = children - child
        }
        return child
    }

    fun onChildAdded(child: Node) {
        onChildAdded?.invoke(child)
    }

    fun onChildRemoved(child: Node) {
        onChildRemoved?.invoke(child)
    }

    /**
     * ### Traverse the hierarchy
     *
     * Traversal is depth first. If this NodeParent is a Node, traversal starts with this
     * NodeParent, otherwise traversal starts with its children.
     */
    val allChildren: List<Node>
        get() = children + children.flatMap { it.allChildren }


    /**
     * ### Traverse the hierarchy
     *
     * Traversal is depth first. If this NodeParent is a Node, traversal starts with this
     * NodeParent, otherwise traversal starts with its children.
     */
    val hierarchy: List<Node>
        get() = listOfNotNull(this as? Node) + allChildren

    /**
     * ### Traverse the hierarchy and call a method on each node.
     *
     * Traversal is depth first. If this NodeParent is a Node, traversal starts with this
     * NodeParent, otherwise traversal starts with its children.
     *
     * @param action The method to call on each node.
     */
    fun callOnHierarchy(action: (Node) -> Unit) {
        hierarchy.forEach { it.callOnHierarchy(action) }
    }
}