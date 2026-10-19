package io.github.sceneview.demo.demos.internal

import io.github.sceneview.camera.CameraAnimator
import io.github.sceneview.camera.CameraPath
import io.github.sceneview.camera.CameraShot
import io.github.sceneview.camera.CinematicCameraManipulator

/** The demo's orbit pose as a library [CameraShot]: the same four numbers, the same convention. */
internal fun OrbitPose.toShot(): CameraShot = CameraShot(
    target = target,
    azimuthDegrees = azimuthDegrees,
    elevationDegrees = elevationDegrees,
    distance = distance,
)

/** A library [CameraShot] as the demo's orbit pose. */
internal fun CameraShot.toOrbitPose(): OrbitPose = OrbitPose(
    target = target,
    azimuthDegrees = azimuthDegrees,
    elevationDegrees = elevationDegrees,
    distance = distance,
)

/**
 * The scan result's camera moves (#4631), over [orbit] through [manipulator]: the reveal once
 * the model is drawn on its framing, the fly to a new framing (3D ↔ Plan, Recenter) and the
 * tour. Every move is the library's — time-based, exact at its end — and the first touch hands
 * the orbit camera the shot and the speed the move had.
 *
 * Driven from `onFrame`, after each drawn frame, by [frameDrawn]; the buttons only [request].
 */
internal class ScanResultCinematics(
    val orbit: ArDebugOrbitCamera,
    val animator: CameraAnimator = CameraAnimator(CameraShot()),
) {
    /** The `cameraManipulator` for the `SceneView`: the animator while it plays, [orbit] otherwise. */
    val manipulator = CinematicCameraManipulator(animator, orbit)

    enum class Request { Recenter, ToggleTour }

    /** `true` once the reveal has started: the model is on its framing, the move is on screen. */
    var revealed = false
        private set

    /** `true` while the tour plays. A touch, a request or a reframe ends it. */
    var touring = false
        private set

    private var shown = false
    private var pending: Request? = null

    /** What the next frame should do, from a button; one request at a time, the last one wins. */
    fun request(request: Request) {
        pending = request
    }

    /**
     * A frame was drawn with the camera on [manipulator]; [home] is the framing of the moment
     * and [ready] whether there is a model to show. Returns `true` on the one frame after which
     * the view may be seen: the first frame of the reveal, not the still frame before it.
     */
    fun frameDrawn(opening: DollhouseOpening, home: OrbitPose, ready: Boolean): Boolean {
        if (touring && !animator.isPlaying) touring = false
        if (!revealed) {
            val framed = ready && opening.drawn(orbit, home)
            opening.aim(orbit, home)
            if (framed) {
                revealed = true
                animator.play(CameraPath.reveal(ArDebugFraming.clamp(home).toShot()))
            }
            return false
        }
        opening.aim(orbit, home)
        when (pending) {
            Request.Recenter -> recenter(home)
            Request.ToggleTour -> if (touring) stopTour() else tour(home)
            null -> Unit
        }
        pending = null
        if (!shown && (animator.elapsed > 0f || !animator.isPlaying)) {
            shown = true
            return true
        }
        return false
    }

    /** Flies to [home] — the framing of the moment — and leaves the orbit camera there, following. */
    private fun recenter(home: OrbitPose) {
        touring = false
        val target = ArDebugFraming.clamp(home)
        orbit.recenter()
        orbit.snapTo(target)
        animator.flyTo(target.toShot(), flyOver = 0f)
    }

    private fun tour(home: OrbitPose) {
        val target = ArDebugFraming.clamp(home)
        orbit.recenter()
        orbit.snapTo(target)
        animator.play(CameraPath.tour(target.toShot()))
        touring = true
    }

    private fun stopTour() {
        manipulator.interrupt()
        touring = false
    }
}
