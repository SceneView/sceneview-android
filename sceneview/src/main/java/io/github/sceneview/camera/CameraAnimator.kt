package io.github.sceneview.camera

/**
 * Plays camera moves frame by frame: a [CameraTransition] ([flyTo]) or a [CameraPath] ([play]),
 * one at a time, each starting from wherever the camera is now.
 *
 * Pure Kotlin and time-based: call [advance] once per frame with the frame's delta and read
 * [shot]. The same move lands on the same shot at 30, 60 or 120 Hz, and a stalled frame
 * (a GLB upload, a shader link) advances by at most [MAX_STEP_SECONDS], so a move runs a little
 * long rather than jumping. [velocity] is how fast the camera is moving right now; a touch that
 * [interrupt]s a move gets it, to hand a gesture the speed the camera already had.
 *
 * ```kotlin
 * val animator = CameraAnimator(initial = home)
 * animator.play(CameraPath.reveal(home))                 // the entrance
 * animator.flyTo(home.copy(elevationDegrees = 84f))      // to the plan view, 0.7 s
 * // every frame:
 * cameraNode.transform = animator.advance(deltaSeconds).transform()
 * ```
 *
 * In a `SceneView`, [CinematicCameraManipulator] wires this to the gesture manipulator so a
 * touch takes over without a cut.
 */
class CameraAnimator(initial: CameraShot) {

    /** The shot the camera is on now. */
    var shot: CameraShot = initial
        private set

    /** How fast [shot] moved over the last [advance]; [CameraShotVelocity.Zero] at rest. */
    var velocity: CameraShotVelocity = CameraShotVelocity.Zero
        private set

    private var path: CameraPath? = null
    private var transition: CameraTransition? = null
    private var elapsedSeconds = 0f
    private var onFinished: (() -> Unit)? = null

    /** `true` while a move plays. */
    val isPlaying: Boolean get() = path != null || transition != null

    /** Seconds into the current move, `0` at rest. */
    val elapsed: Float get() = elapsedSeconds

    /** Starts [path] from its own first keyframe, dropping any move in progress. */
    fun play(path: CameraPath, onFinished: (() -> Unit)? = null) {
        this.transition = null
        this.path = path
        this.onFinished = onFinished
        elapsedSeconds = 0f
        velocity = CameraShotVelocity.Zero
        path.start?.let { shot = it }
    }

    /**
     * Moves from the current [shot] to [to] over [durationSeconds], [CameraEasing.Expressive] by
     * default, flying over the gap between two subjects ([CameraTransition.flyOver]). A move in
     * progress is dropped: the new one starts from the shot on screen, so it never jumps.
     */
    fun flyTo(
        to: CameraShot,
        durationSeconds: Float = FLY_TO_SECONDS,
        easing: (Float) -> Float = CameraEasing.Expressive,
        flyOver: Float = CameraTransition.DEFAULT_FLY_OVER,
        onFinished: (() -> Unit)? = null,
    ) {
        this.path = null
        this.transition = CameraTransition(shot, to, durationSeconds, easing, flyOver)
        this.onFinished = onFinished
        elapsedSeconds = 0f
        velocity = CameraShotVelocity.Zero
    }

    /** Puts the camera on [shot] at once, stopping any move; nothing is called back. */
    fun snapTo(shot: CameraShot) {
        clear()
        this.shot = shot
        velocity = CameraShotVelocity.Zero
    }

    /**
     * Stops the move where it is, leaving the camera on the last [shot], and returns the
     * velocity it had: what the gesture taking over should start with. Rest returns
     * [CameraShotVelocity.Zero]. The move's `onFinished` is not called — it did not finish.
     */
    fun interrupt(): CameraShotVelocity {
        val had = velocity
        clear()
        velocity = CameraShotVelocity.Zero
        return had
    }

    /**
     * Advances the move by [deltaSeconds] (capped at [MAX_STEP_SECONDS], never negative) and
     * returns the new [shot]. A move that reaches its end lands exactly on it, calls its
     * `onFinished` once, and the animator is at rest. At rest this returns [shot] unchanged.
     */
    fun advance(deltaSeconds: Float): CameraShot {
        val step = deltaSeconds.takeIf { it.isFinite() }?.coerceIn(0f, MAX_STEP_SECONDS) ?: 0f
        val playingPath = path
        val playingTransition = transition
        if (playingPath == null && playingTransition == null) {
            velocity = CameraShotVelocity.Zero
            return shot
        }
        val previous = shot
        elapsedSeconds += step
        val finished: Boolean
        shot = if (playingPath != null) {
            finished = playingPath.isFinishedAt(elapsedSeconds)
            playingPath.shotAt(elapsedSeconds)
        } else {
            finished = checkNotNull(playingTransition).isFinishedAt(elapsedSeconds)
            playingTransition.shotAt(elapsedSeconds)
        }
        velocity = CameraShotVelocity.between(previous, shot, step)
        if (finished) {
            val callback = onFinished
            clear()
            callback?.invoke()
        }
        return shot
    }

    private fun clear() {
        path = null
        transition = null
        onFinished = null
        elapsedSeconds = 0f
    }

    companion object {
        /** The default [flyTo] duration: `motion-entrance` of DESIGN.md, 700 ms. */
        const val FLY_TO_SECONDS = 0.7f

        /** The most one frame advances a move by: a stall runs the move long, never jumps it. */
        const val MAX_STEP_SECONDS = 0.05f
    }
}
