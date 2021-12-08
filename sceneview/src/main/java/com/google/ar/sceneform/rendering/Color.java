package com.google.ar.sceneform.rendering;

import androidx.annotation.ColorInt;
import com.google.android.filament.Colors;

/**
 * An RGBA color. Each component is a value with a range from 0 to 1. Can be created from an Android
 * ColorInt.
 *
 * @deprecated
 */
public class Color {
  private static final float INT_COLOR_SCALE = 1.0f / 255.0f;

  public float r;
  public float g;
  public float b;
  public float a;

  /** Construct a Color and default it to white (1, 1, 1, 1). */
  @SuppressWarnings("initialization")
  public Color() {
    setWhite();
  }

  /** Construct a Color with the values of another color. */
  @SuppressWarnings("initialization")
  public Color(Color color) {
    set(color);
  }

  /** Construct a color with the RGB values passed in and an alpha of 1. */
  @SuppressWarnings("initialization")
  public Color(float r, float g, float b) {
    set(r, g, b);
  }

  /** Construct a color with the RGBA values passed in. */
  @SuppressWarnings("initialization")
  public Color(float r, float g, float b, float a) {
    set(r, g, b, a);
  }

  /**
   * Construct a color with an integer in the sRGB color space packed as an ARGB value. Used for
   * constructing from an Android ColorInt.
   */
  @SuppressWarnings("initialization")
  public Color(@ColorInt int argb) {
    set(argb);
  }

  /** Set to the values of another color. */
  public void set(Color color) {
    set(color.r, color.g, color.b, color.a);
  }

  /** Set to the RGB values passed in and an alpha of 1. */
  public void set(float r, float g, float b) {
    set(r, g, b, 1.0f);
  }

  /** Set to the RGBA values passed in. */
  public void set(float r, float g, float b, float a) {
    this.r = Math.max(0.0f, Math.min(1.0f, r));
    this.g = Math.max(0.0f, Math.min(1.0f, g));
    this.b = Math.max(0.0f, Math.min(1.0f, b));
    this.a = Math.max(0.0f, Math.min(1.0f, a));
  }

  /**
   * Set to RGBA values from an integer in the sRGB color space packed as an ARGB value. Used for
   * setting from an Android ColorInt.
   */
  public void set(@ColorInt int argb) {
    // sRGB color
    final int red = android.graphics.Color.red(argb);
    final int green = android.graphics.Color.green(argb);
    final int blue = android.graphics.Color.blue(argb);
    final int alpha = android.graphics.Color.alpha(argb);

    // Convert from sRGB to linear and from int to float.
    float[] linearColor =
        Colors.toLinear(
            Colors.RgbType.SRGB,
            (float) red * INT_COLOR_SCALE,
            (float) green * INT_COLOR_SCALE,
            (float) blue * INT_COLOR_SCALE);

    r = linearColor[0];
    g = linearColor[1];
    b = linearColor[2];
    a = (float) alpha * INT_COLOR_SCALE;
  }

  /** Sets the color to white. RGBA is (1, 1, 1, 1). */
  private void setWhite() {
    set(1.0f, 1.0f, 1.0f);
  }

  /** Returns a new color with Sceneform's tonemapping inversed. */
  public Color inverseTonemap() {
    Color color = new Color(r, g, b, a);
    color.r = inverseTonemap(r);
    color.g = inverseTonemap(g);
    color.b = inverseTonemap(b);
    return color;
  }

  private static float inverseTonemap(float val) {
    return (val * -0.155f) / (val - 1.019f);
  }
}
