package io.github.sceneview.utils

import android.os.Looper
import com.google.android.filament.Camera
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float2
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Entity
import io.github.sceneview.math.ClipSpacePosition
import io.github.sceneview.math.Position
import io.github.sceneview.math.toFloat3
import io.github.sceneview.node.Node

/**
 * @see clipSpaceToViewSpace
 * @see viewSpaceToWorld
 */
fun View.viewPortToClipSpace(viewportPosition: Position): ClipSpacePosition {
    val viewPortSize = Float2(x = viewport.width.toFloat(), y = viewport.height.toFloat())
    // Normalize between -1 and 1.
    return ClipSpacePosition(
        viewportPosition / viewPortSize * 2.0f - 1.0f,
        w = 1.0f
    )
}

/**
 * @see viewSpaceToClipSpace
 */
fun View.clipSpaceToViewPort(clipSpacePosition: ClipSpacePosition): Position {
    val viewPortSize = Float2(x = viewport.width.toFloat(), y = viewport.height.toFloat())
    return ((clipSpacePosition + 1.0f) * 0.5f * viewPortSize).xyz
}

/**
 * @see Camera.clipSpaceToViewSpace
 */
fun View.clipSpaceToViewSpace(clipSpacePosition: ClipSpacePosition): Position =
    camera!!.clipSpaceToViewSpace(clipSpacePosition)

/**
 * @see Camera.viewSpaceToWorld
 */
fun View.viewSpaceToWorld(viewSpacePosition: Position): Position =
    camera!!.viewSpaceToWorld(viewSpacePosition)

/**
 * @see Camera.worldToViewSpace
 */
fun View.worldToViewSpace(worldPosition: Position): Position =
    camera!!.worldToViewSpace(worldPosition)

/**
 * @see Camera.viewSpaceToClipSpace
 */
fun View.viewSpaceToClipSpace(viewSpacePosition: Position): ClipSpacePosition =
    camera!!.viewSpaceToClipSpace(viewSpacePosition)

/**
 * Get a world space position from a screen space position
 *
 * @param x (0..Viewport width) = (left..right)
 * The X value is negative when the point is left of the viewport, between 0 and the width of
 * the [View.getViewport] width when the point is within the viewport, and greater than the width when
 * the point is to the right of the viewport.
 *
 * @param y (0..Viewport height) = (bottom..top)
 * The Y value is negative when the point is below the viewport, between 0 and the height of
 * the [View.getViewport] height when the point is within the viewport, and greater than the height when
 * the point is above the viewport.
 *
 * @param z (0..1) (far..near)
 * The Z value is used for the depth between 1 and 0 (1=near, 0=infinity).
 *
 * @return a new Position that represents the point in screen-space.
 *
 * @see worldToViewport
 */
fun View.viewportToWorld(x: Float, y: Float, z: Float = 1.0f): Position {
    val clipSpacePosition = viewPortToClipSpace(Position(x, y, z))
    val viewSpacePosition = clipSpaceToViewSpace(clipSpacePosition)
    return viewSpaceToWorld(viewSpacePosition)
}

/**
 * Get a screen space position from a world space position
 *
 * @param worldPosition The world position to convert
 *
 * @return Viewport space position in Filament screen coordinates:
 * BottomLeft = (0, 0), TopRight = (viewport Width, viewport Height).
 *
 * The device coordinate space is unaffected by the orientation of the device
 *
 * @see viewportToWorld
 */
fun View.worldToViewport(worldPosition: Position): Position {
    val viewSpacePosition = worldToViewSpace(worldPosition)
    val clipSpacePosition = viewSpaceToClipSpace(viewSpacePosition)
    return clipSpaceToViewPort(clipSpacePosition)
}

/**
 * Picks an [Entity] at given coordinates
 *
 * Filament picking works with a small delay, therefore, a callback is used.
 * If no node is picked, the callback is invoked with a `null` value instead of a entity.
 *
 * @param x The x coordinate within the `SceneView`.
 * @param y The y coordinate within the `SceneView`.
 * @param onCompleted Called when picking completes.
 */
fun View.pick(
    x: Int,
    y: Int,
    onCompleted: (
        /** The Renderable Entity at the picking query location  */
        renderable: Entity,
        /** The value of the depth buffer at the picking query location  */
        depth: Float,
        /** The fragment coordinate in GL convention at the picking query location  */
        fragCoords: Float3
    ) -> Unit
) {
    // Invert the y coordinate since its origin is at the bottom
    val invertedY = viewport.height - 1 - y

    pick(x, invertedY, Looper.getMainLooper()) { result ->
        onCompleted(
            result.renderable,
            result.depth,
            result.fragCoords.toFloat3()
        )
    }
}

/**
 * Picks a node at given coordinates
 *
 * Filament picking works with a small delay, therefore, a callback is used.
 * If no node is picked, the callback is invoked with a `null` value instead of a node.
 *
 * @param x The x coordinate within the `SceneView`.
 * @param y The y coordinate within the `SceneView`.
 * @param nodes List of nodes to pick from.
 * @param onCompleted Called when picking completes.
 */
fun View.pickNode(
    x: Int,
    y: Int,
    nodes: List<Node>,
    onCompleted: (
        /** The Renderable Node at the picking query location  */
        node: Node?,
        /** The value of the depth buffer at the picking query location  */
        depth: Float,
        /** The fragment coordinate in GL convention at the picking query location  */
        fragCoords: Float3
    ) -> Unit
) {
    // Invert the y coordinate since its origin is at the bottom
    val invertedY = viewport.height - 1 - y

    pick(x, invertedY, Looper.getMainLooper()) { result ->
        onCompleted(
            nodes.firstOrNull { it.entity == result.renderable },
            result.depth,
            result.fragCoords.toFloat3()
        )
    }
}