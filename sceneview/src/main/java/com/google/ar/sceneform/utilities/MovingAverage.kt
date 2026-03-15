package com.google.ar.sceneform.utilities

/**
 * Calculates an exponentially weighted moving average for a series of data.
 *
 * @hide
 */
class MovingAverage {
    companion object {
        @JvmField
        val DEFAULT_WEIGHT: Double = 0.9
    }

    private var average: Double
    private val weight: Double

    /**
     * Construct an object to track the exponentially weighted moving average for a series of data.
     * The weight is set to a default of 0.9, which is good for data with lots of samples when the
     * average should be resistant to spikes (i.e. frame rate).
     *
     * The weight is a ratio between 0 and 1 that represents how much of the previous average is
     * kept compared to the new sample. With a weight of 0.9, 90% of the previous average is kept and
     * 10% of the new sample is added to the average.
     *
     * @param initialSample the first sample in the average
     */
    constructor(initialSample: Double) : this(initialSample, DEFAULT_WEIGHT)

    /**
     * Construct an object to track the exponentially weighted moving average for a series of data.
     *
     * The weight is a ratio between 0 and 1 that represents how much of the previous average is
     * kept compared to the new sample. With a weight of 0.9, 90% of the previous average is kept and
     * 10% of the new sample is added to the average.
     *
     * @param initialSample the first sample in the average
     * @param weight the weight to used when adding samples
     */
    constructor(initialSample: Double, weight: Double) {
        average = initialSample
        this.weight = weight
    }

    /** Add a sample and calculate a new average. */
    fun addSample(sample: Double) {
        average = weight * average + (1.0 - weight) * sample
    }

    /** Returns the current average for all samples. */
    fun getAverage(): Double {
        return average
    }
}
