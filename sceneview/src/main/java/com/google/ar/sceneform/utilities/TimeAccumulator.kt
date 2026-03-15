package com.google.ar.sceneform.utilities

/** Sums time samples together. Used for tracking the time elapsed of a set of code blocks. */
class TimeAccumulator {
    private var elapsedTimeMs: Long = 0
    private var startSampleTimeMs: Long = 0

    fun beginSample() {
        startSampleTimeMs = System.currentTimeMillis()
    }

    fun endSample() {
        val endSampleTimeMs = System.currentTimeMillis()
        val sampleMs = endSampleTimeMs - startSampleTimeMs
        elapsedTimeMs += sampleMs
    }

    fun getElapsedTimeMs(): Long {
        return elapsedTimeMs
    }
}
