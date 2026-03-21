package io.github.sceneview.gesture

import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform

/**
 * Cross-platform camera manipulator interface.
 *
 * Abstracts orbit/pan/zoom camera control for any platform.
 * Android implements via Filament's Manipulator, iOS via custom or SceneKit.
 */
interface CameraManipulator {

    /**
     * Sets the viewport dimensions (needed for coordinate normalization).
     */
    fun setViewport(width: Int, height: Int)

    /**
     * Returns the current camera transform (view matrix).
     */
    fun getTransform(): Transform

    /**
     * Begins a grab (orbit or pan) gesture at the given pixel coordinates.
     *
     * @param x horizontal pixel coordinate
     * @param y vertical pixel coordinate
     * @param strafe true for pan, false for orbit
     */
    fun grabBegin(x: Int, y: Int, strafe: Boolean)

    /**
     * Updates a grab gesture to new pixel coordinates.
     */
    fun grabUpdate(x: Int, y: Int)

    /**
     * Ends the current grab gesture.
     */
    fun grabEnd()

    /**
     * Begins a scroll/pinch gesture.
     */
    fun scrollBegin(x: Int, y: Int, separation: Float)

    /**
     * Updates a scroll/pinch gesture with new separation.
     */
    fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float)

    /**
     * Ends the current scroll gesture.
     */
    fun scrollEnd()

    /**
     * Called each frame to apply inertia/animation.
     *
     * @param deltaTime time since last frame in seconds
     */
    fun update(deltaTime: Float)
}

/**
 * Camera gesture state.
 */
enum class CameraGesture {
    NONE,
    ORBIT,
    PAN,
    ZOOM
}

/**
 * Builder for configuring a CameraManipulator.
 */
interface CameraManipulatorBuilder {
    fun targetPosition(position: Position): CameraManipulatorBuilder
    fun orbitHomePosition(position: Position): CameraManipulatorBuilder
    fun orbitSpeed(x: Float, y: Float): CameraManipulatorBuilder
    fun zoomSpeed(speed: Float): CameraManipulatorBuilder
    fun build(): CameraManipulator
}
