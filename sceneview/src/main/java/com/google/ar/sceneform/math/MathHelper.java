package com.google.ar.sceneform.math;

/** Static functions for common math operations. */
public class MathHelper {

  static final float FLT_EPSILON = 1.19209290E-07f;
  static final float MAX_DELTA = 1.0E-10f;

  /**
   * Returns true if two floats are equal within a tolerance. Useful for comparing floating point
   * numbers while accounting for the limitations in floating point precision.
   */
  // https://randomascii.wordpress.com/2012/02/25/comparing-floating-point-numbers-2012-edition/
  public static boolean almostEqualRelativeAndAbs(float a, float b) {
    // Check if the numbers are really close -- needed
    // when comparing numbers near zero.
    float diff = Math.abs(a - b);
    if (diff <= MAX_DELTA) {
      return true;
    }

    a = Math.abs(a);
    b = Math.abs(b);
    float largest = Math.max(a, b);

    if (diff <= largest * FLT_EPSILON) {
      return true;
    }
    return false;
  }

  /** Clamps a value between a minimum and maximum range. */
  public static float clamp(float value, float min, float max) {
    return Math.min(max, Math.max(min, value));
  }

  /** Clamps a value between a range of 0 and 1. */
  static float clamp01(float value) {
    return clamp(value, 0.0f, 1.0f);
  }

  /**
   * Linearly interpolates between a and b by a ratio.
   *
   * @param a the beginning value
   * @param b the ending value
   * @param t ratio between the two floats
   * @return interpolated value between the two floats
   */
  public static float lerp(float a, float b, float t) {
    return a + t * (b - a);
  }
}
