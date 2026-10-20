package io.github.sceneview.demo.demos.internal

/**
 * What a capture is doing, said in one word at the top of the screen (#4540 live walls): a scan
 * that only shows points gave no sign of whether it was working.
 *
 * - [Working] — it captures, and the room grows.
 * - [Paused] — it captures nothing: the user paused it, or ARCore lost track of the room.
 * - [Stuck] — it runs but adds nothing: no usable depth, the phone moves too fast or stands
 *   still, or points at nothing it can use.
 */
enum class ScanActivity {
    Working,
    Paused,
    Stuck;

    companion object {
        /**
         * [depthStarved]: a depth scan that gets no usable depth adds nothing whatever the phone
         * does, so it is [Stuck] — and the coaching line says why ([ScanCopy.COACH_NO_DEPTH]).
         */
        fun of(
            phase: ScanPhase,
            trackingLost: Boolean,
            coach: ScanCoach.Line?,
            lowTexture: Boolean = false,
            depthStarved: Boolean = false,
        ): ScanActivity =
            when {
                !phase.integrates || trackingLost -> Paused
                depthStarved || coach != null || lowTexture -> Stuck
                else -> Working
            }
    }
}
