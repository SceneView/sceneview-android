package io.github.sceneview.nodes

import android.view.MotionEvent
import com.google.android.filament.Box
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.components.RenderableComponent
import io.github.sceneview.components.TransformComponent
import io.github.sceneview.managers.NodeManager
import io.github.sceneview.math.*
import io.github.sceneview.utils.FrameTime

/**
 * A Node represents a transformation within the scene graph's hierarchy.
 *
 * It can contain a renderable for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 *
 * ------- +y ----- -z
 *
 * ---------|----/----
 *
 * ---------|--/------
 *
 * -x - - - 0 - - - +x
 *
 * ------/--|---------
 *
 * ----/----|---------
 *
 * +z ---- -y --------
 */
open class Node(
    engine: Engine,
    nodeManager: NodeManager,
    final override val entity: Entity = EntityManager.get().create(),
    /**
     * The node can be selected when a touch event happened
     *
     * If a not selectable child [RenderableNode] is touched, we check the parent hierarchy to find the
     * closest selectable parent. In this case, the first selectable parent will be the one to have
     * its [isSelected] value to `true`.
     */
    var isSelectable: Boolean = false,
    isEditable: Boolean = false,
    val sceneEntities: List<Entity> = listOf(entity)
) : TransformComponent {

    private var _engine: Engine? = engine
    final override val engine: Engine get() = _engine!!

    private var _nodeManager: NodeManager? = nodeManager
    val nodeManager: NodeManager get() = _nodeManager!!

    /**
     * Parent [Node] from this transform
     *
     * It is an error to re-parent a node to a descendant and will cause undefined behaviour.
     *
     * @see TransformComponent.parentEntity
     */
    var parentNode: Node?
        get() = parentEntity?.let { nodeManager.getNode(it) }
        set(value) {
            parentEntity = value?.entity
        }

    /**
     * Flat list of all parent nodes within the hierarchy
     *
     * @see parentNode
     */
    val allParentNodes: List<Node>
        get() = parentNode?.let { listOf(it) + it.allParentNodes } ?: listOf()

    /**
     * List of child entities that has a node component within the same [NodeManager]
     *
     * @see TransformComponent.childEntities
     */
    val childNodes: List<Node> get() = childEntities.mapNotNull { nodeManager.getNode(it) }

    /**
     * Flat list of all child nodes within the hierarchy
     *
     * @see childNodes
     */
    val allChildNodes: List<Node> get() = childNodes + childNodes.flatMap { it.allChildNodes }

    open var isVisible = true
        set(value) {
            field = value
            updateVisibility(value)
        }

    open var size: Size
        get() = scale * getBoundingBox().size
        set(value) {
            scale = value / getBoundingBox().size
        }

    var centerPosition: Position
        get() = position + getBoundingBox().centerPosition / scale
        set(value) {
            position = value - getBoundingBox().centerPosition * scale
        }

    /**
     * Node selection visualizer
     */
    var selectionNode: Node? = null
        set(value) {
            if (isSelected) {
                field?.let { removeChildNode(it) }
            }
            field = value
            updateSelectionVisualizer()
        }

    open var isSelected = false
        internal set(value) {
            field = value
            updateSelectionVisualizer()
        }

    var isPositionEditable = isEditable
    var isRotationEditable = isEditable
    var isScaleEditable = isEditable

    /**
     * Invoked when the node is tapped
     *
     * Only nodes with renderables or their parent nodes can be tapped since Filament picking is
     * used to find a touched node. The ID of the Filament renderable can be used to determine what
     * part of a model is tapped.
     */
    var onTapListener: ((node: Node) -> Unit)? = null

    var onFrameListener: ((node: Node, frameTime: FrameTime) -> Unit)? = null

    internal val onChildAdded = mutableListOf<((Node) -> Unit)>()
    internal val onChildRemoved = mutableListOf<((Node) -> Unit)>()

    init {
        if (!engine.transformManager.hasComponent(entity)) {
            engine.transformManager.create(entity)
        }
        addComponent(entity)
    }

    constructor(
        sceneView: SceneView,
        entity: Entity = EntityManager.get().create(),
        isSelectable: Boolean = false,
        isEditable: Boolean = false,
        sceneEntities: List<Entity> = listOf(entity)
    ) : this(
        sceneView.engine,
        sceneView.nodeManager,
        entity,
        isSelectable,
        isEditable,
        sceneEntities
    )

    private fun addComponent(entity: Entity) {
        nodeManager.addComponent(entity, this)
    }

    open fun onFrame(frameTime: FrameTime) {
        onFrameListener?.invoke(this, frameTime)
    }

    /**
     * Returns true if the node completely handles the touch event by itself
     */
    open fun onTouchEvent(motionEvent: MotionEvent, pickingResult: SceneView.PickingResult) = false

    fun addChildNode(child: Node) {
        child.parentEntity = entity
        onChildAdded.forEach { it(child) }
    }

    fun addChildNodes(nodes: List<Node>) = nodes.forEach { addChildNode(it) }

    fun removeChildNode(child: Node) {
        child.parentEntity = null
        onChildRemoved.forEach { it(child) }
    }

    /**
     * Sets up a root transform on the current node content to make it fit into a unit cube
     *
     * @param units the number of units of the cube to scale into.
     */
    fun scaleToUnitsCube(units: Float = 1.0f) {
        scale = Scale(units / dev.romainguy.kotlin.math.max(size))
    }

    open fun getBoundingBox(): Box {
        val minPosition = min(childNodes.map { it.position - it.size / 2.0f })
        val maxPosition = max(childNodes.map { it.position + it.size / 2.0f })
        val halfExtent = (maxPosition - minPosition) / 2.0f
        val center = minPosition + halfExtent
        return Box(center, halfExtent)
    }

    fun setEditable(editable: Boolean) {
        isPositionEditable = editable
        isRotationEditable = editable
        isScaleEditable = editable
    }

    // TODO: Should we bring it back?
//    /**
//     * Sets up a root transform on the current [Node] content to make it centered
//     *
//     * Coordinate inside the content cube from where it is centered.
//     * Default : `Position(x = 0.0f, y = 0.0f, z = 0.0f)` = Center the model horizontally and
//     * vertically.
//     *
//     * E.g.:
//     * - `Position(x = 0.0f, y = -1.0f, z = 0.0f)` = center horizontal | bottom aligned
//     * - `Position(x = -1.0f, y = 1.0f, z = 0.0f)` = left | top aligned
//     * - `Position(x = 1.0f, y = -1.0f, z = 0.0f)` = right | bottom aligned
//     */
//    fun centerOrigin(position: Position = Position(0.0f)) {
//        val centerPosition = -position * (size / 2.0f)
//        childNodes.forEach {
//            it.position += centerPosition
//        }
////        position = value - getBoundingBox().centerPosition * scale
//    }


    protected open fun updateVisibility(visible: Boolean) {
        (this as? RenderableComponent)?.setLayerVisible(isVisible)
        childNodes.forEach { childNode ->
            childNode.updateVisibility(visible && childNode.isVisible)
        }
    }

    private fun updateSelectionVisualizer() {
        selectionNode?.let { selectionNode ->
            if (isSelected) {
                addChildNode(selectionNode)
            } else {
                removeChildNode(selectionNode)
            }
        }
    }

    internal fun onTap() {
        onTapListener?.invoke(this)
    }

    open fun destroy() {
        childNodes.toList().forEach {
            removeChildNode(it)
            it.destroy()
        }
        nodeManager.removeComponent(entity)
        engine.destroyEntity(entity)
        EntityManager.get().destroy(entity)

        _engine = null
        _nodeManager = null
    }
}