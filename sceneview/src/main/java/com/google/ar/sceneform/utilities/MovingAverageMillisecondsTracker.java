package com.google.ar.sceneform.utilities;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Used to track a {@link MovingAverage} that represents the number of milliseconds that elapse
 * within the execution of a block of code.
 *
 * @hide
 */
public class MovingAverageMillisecondsTracker {
  private static final double NANOSECONDS_TO_MILLISECONDS = 0.000001;

  interface Clock {
    long getNanoseconds();
  }

  private static class DefaultClock implements Clock {
    @Override
    public long getNanoseconds() {
      return System.nanoTime();
    }
  }

  @Nullable private MovingAverage movingAverage;
  private final double weight;
  private final Clock clock;
  private long beginSampleTimestampNano;

  public MovingAverageMillisecondsTracker() {
    this(MovingAverage.DEFAULT_WEIGHT);
  }

  public MovingAverageMillisecondsTracker(double weight) {
    this.weight = weight;
    clock = new DefaultClock();
  }

  @VisibleForTesting
  public MovingAverageMillisecondsTracker(Clock clock) {
    this(clock, MovingAverage.DEFAULT_WEIGHT);
  }

  @VisibleForTesting
  public MovingAverageMillisecondsTracker(Clock clock, double weight) {
    this.weight = weight;
    this.clock = clock;
  }

  /**
   * Call at the point in execution when the tracker should start measuring elapsed milliseconds.
   */
  public void beginSample() {
    beginSampleTimestampNano = clock.getNanoseconds();
  }

  /**
   * Call at the point in execution when the tracker should stop measuring elapsed milliseconds and
   * post a new sample.
   */
  public void endSample() {
    long sampleNano = clock.getNanoseconds() - beginSampleTimestampNano;
    double sample = sampleNano * NANOSECONDS_TO_MILLISECONDS;

    if (movingAverage == null) {
      movingAverage = new MovingAverage(sample, weight);
    } else {
      movingAverage.addSample(sample);
    }
  }

  public double getAverage() {
    if (movingAverage != null) {
      return movingAverage.getAverage();
    }

    return 0.0;
  }
}
