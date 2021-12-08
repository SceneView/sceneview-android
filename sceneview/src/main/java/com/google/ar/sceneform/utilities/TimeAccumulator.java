package com.google.ar.sceneform.utilities;

/** Sums time samples together. Used for tracking the time elapsed of a set of code blocks. */
public class TimeAccumulator {
  private long elapsedTimeMs;
  private long startSampleTimeMs;

  public void beginSample() {
    startSampleTimeMs = System.currentTimeMillis();
  }

  public void endSample() {
    long endSampleTimeMs = System.currentTimeMillis();
    long sampleMs = endSampleTimeMs - startSampleTimeMs;
    elapsedTimeMs += sampleMs;
  }

  public long getElapsedTimeMs() {
    return elapsedTimeMs;
  }
}
