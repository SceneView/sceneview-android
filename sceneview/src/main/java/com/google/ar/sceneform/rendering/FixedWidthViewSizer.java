package com.google.ar.sceneform.rendering;

import android.view.View;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * Controls the size of a {@link ViewRenderable} in a {@link com.google.ar.sceneform.Scene} by
 * defining how wide it should be in meters. The height will change to match the aspect ratio of the
 * view.
 *
 * @see ViewRenderable.Builder#setSizer(ViewSizer)
 * @see ViewRenderable#setSizer(ViewSizer)
 */

public class FixedWidthViewSizer implements ViewSizer {
  private final float widthMeters;

  // Defaults to zero, Z value of the size doesn't currently have any semantic meaning,
  // but we may add that in later if we support ViewRenderables that have depth.
  private static final float DEFAULT_SIZE_Z = 0.0f;

  /**
   * Constructor for creating a sizer for controlling the size of a {@link ViewRenderable} by
   * defining a fixed width.
   *
   * @param widthMeters a number greater than zero representing the width in meters.
   */
  public FixedWidthViewSizer(float widthMeters) {
    if (widthMeters <= 0) {
      throw new IllegalArgumentException("widthMeters must be greater than zero.");
    }

    this.widthMeters = widthMeters;
  }

  /** Returns the width in meters used for controlling the size of a {@link ViewRenderable}. */
  public float getWidth() {
    return widthMeters;
  }

  @Override
  public Vector3 getSize(View view) {
    Preconditions.checkNotNull(view, "Parameter \"view\" was null.");

    float aspectRatio = ViewRenderableHelpers.getAspectRatio(view);

    if (aspectRatio == 0.0f) {
      return Vector3.zero();
    }

    return new Vector3(widthMeters, widthMeters / aspectRatio, DEFAULT_SIZE_Z);
  }
}
