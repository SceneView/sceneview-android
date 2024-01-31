package io.github.sceneview.view

import com.google.android.filament.Camera
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.scene.*

typealias FilamentView = View

/**
 * @see clipSpaceToViewSpace
 * @see viewSpaceToWorld
 */
fun FilamentView.viewPortToClipSpace(viewportPosition: Position): ClipSpacePosition {
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
fun FilamentView.clipSpaceToViewPort(clipSpacePosition: ClipSpacePosition): Position {
    val viewPortSize = Float2(x = viewport.width.toFloat(), y = viewport.height.toFloat())
    return ((clipSpacePosition + 1.0f) * 0.5f * viewPortSize).xyz
}

/**
 * @see Camera.clipSpaceToViewSpace
 */
fun FilamentView.clipSpaceToViewSpace(clipSpacePosition: ClipSpacePosition): Position =
    camera!!.clipSpaceToViewSpace(clipSpacePosition)

/**
 * @see Camera.viewSpaceToWorld
 */
fun FilamentView.viewSpaceToWorld(viewSpacePosition: Position): Position =
    camera!!.viewSpaceToWorld(viewSpacePosition)

/**
 * @see Camera.worldToViewSpace
 */
fun FilamentView.worldToViewSpace(worldPosition: Position): Position =
    camera!!.worldToViewSpace(worldPosition)

/**
 * @see Camera.viewSpaceToClipSpace
 */
fun FilamentView.viewSpaceToClipSpace(viewSpacePosition: Position): ClipSpacePosition =
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
fun FilamentView.viewportToWorld(x: Float, y: Float, z: Float = 1.0f): Position {
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
fun FilamentView.worldToViewport(worldPosition: Position): Position {
    val viewSpacePosition = worldToViewSpace(worldPosition)
    val clipSpacePosition = viewSpaceToClipSpace(viewSpacePosition)
    return clipSpaceToViewPort(clipSpacePosition)
}