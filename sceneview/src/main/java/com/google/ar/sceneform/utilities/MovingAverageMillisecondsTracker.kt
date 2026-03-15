package com.google.ar.sceneform.utilities

import androidx.annotation.VisibleForTesting

/**
 * Used to track a [MovingAverage] that represents the number of milliseconds that elapse
 * within the execution of a block of code.
 *
 * @hide
 */
class MovingAverageMillisecondsTracker {
    companion object {
        private const val NANOSECONDS_TO_MILLISECONDS = 0.000001
    }

    internal interface Clock {
        fun getNanoseconds(): Long
    }

    private class DefaultClock : Clock {
        override fun getNanoseconds(): Long {
            return System.nanoTime()
        }
    }

    private var movingAverage: MovingAverage? = null
    private val weight: Double
    private val clock: Clock
    private var beginSampleTimestampNano: Long = 0

    constructor() : this(MovingAverage.DEFAULT_WEIGHT)

    constructor(weight: Double) {
        this.weight = weight
        clock = DefaultClock()
    }

    @VisibleForTesting
    internal constructor(clock: Clock) : this(clock, MovingAverage.DEFAULT_WEIGHT)

    @VisibleForTesting
    internal constructor(clock: Clock, weight: Double) {
        this.weight = weight
        this.clock = clock
    }

    /**
     * Call at the point in execution when the tracker should start measuring elapsed milliseconds.
     */
    fun beginSample() {
        beginSampleTimestampNano = clock.getNanoseconds()
    }

    /**
     * Call at the point in execution when the tracker should stop measuring elapsed milliseconds and
     * post a new sample.
     */
    fun endSample() {
        val sampleNano = clock.getNanoseconds() - beginSampleTimestampNano
        val sample = sampleNano * NANOSECONDS_TO_MILLISECONDS

        if (movingAverage == null) {
            movingAverage = MovingAverage(sample, weight)
        } else {
            movingAverage!!.addSample(sample)
        }
    }

    fun getAverage(): Double {
        return movingAverage?.getAverage() ?: 0.0
    }
}
