package com.google.ar.sceneform;

import java.util.concurrent.TimeUnit;

/** Provides time information for the current frame. */
public class FrameTime {
  private long lastNanoTime = 0;
  private long deltaNanoseconds = 0;

  private static final float NANOSECONDS_TO_SECONDS = 1.0f / 1_000_000_000.0f;


  /**
   * Get the time when this frame started.
   */
  public long getNanoTime() {
    return lastNanoTime;
  }

  /** Get the time in seconds between this frame and the last frame. */
  public float getDeltaSeconds() {
    return deltaNanoseconds * NANOSECONDS_TO_SECONDS;
  }

  /** Get the time in seconds when this frame started. */
  public float getStartSeconds() {
    return lastNanoTime * NANOSECONDS_TO_SECONDS;
  }

  /**
   * Get the time between this frame and the last frame.
   *
   * @param unit The unit time will be returned in
   * @return The time between frames
   */
  public long getDeltaTime(TimeUnit unit) {
    return unit.convert(deltaNanoseconds, TimeUnit.NANOSECONDS);
  }

  /**
   * Get the time when this frame started.
   *
   * @param unit The unit time will be returned in
   * @return The start time of the frame in nanoseconds
   */
  public long getStartTime(TimeUnit unit) {
    return unit.convert(lastNanoTime, TimeUnit.NANOSECONDS);
  }

  /** FrameTime is only created internally. Update events provide access to it. */
  public FrameTime() {}

  public void update(long frameTimeNanos) {
    deltaNanoseconds = (lastNanoTime == 0) ? 0 : (frameTimeNanos - lastNanoTime);
    lastNanoTime = frameTimeNanos;
  }
}
