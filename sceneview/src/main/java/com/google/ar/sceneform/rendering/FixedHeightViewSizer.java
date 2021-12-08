package com.google.ar.sceneform.rendering;

import android.view.View;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * Controls the size of a {@link ViewRenderable} in a {@link com.google.ar.sceneform.Scene} by
 * defining how tall it should be in meters. The width will change to match the aspect ratio of the
 * view.
 *
 * @see ViewRenderable.Builder#setSizer(ViewSizer)
 * @see ViewRenderable#setSizer(ViewSizer)
 */

public class FixedHeightViewSizer implements ViewSizer {
  private final float heightMeters;

  // Defaults to zero, Z value of the size doesn't currently have any semantic meaning,
  // but we may add that in later if we support ViewRenderables that have depth.
  private static final float DEFAULT_SIZE_Z = 0.0f;

  /**
   * Constructor for creating a sizer for controlling the size of a {@link ViewRenderable} by
   * defining a fixed height.
   *
   * @param heightMeters a number greater than zero representing the height in meters.
   */
  public FixedHeightViewSizer(float heightMeters) {
    if (heightMeters <= 0) {
      throw new IllegalArgumentException("heightMeters must be greater than zero.");
    }

    this.heightMeters = heightMeters;
  }

  /** Returns the height in meters used for controlling the size of a {@link ViewRenderable}. */
  public float getHeight() {
    return heightMeters;
  }

  @Override
  public Vector3 getSize(View view) {
    Preconditions.checkNotNull(view, "Parameter \"view\" was null.");

    float aspectRatio = ViewRenderableHelpers.getAspectRatio(view);

    if (aspectRatio == 0.0f) {
      return Vector3.zero();
    }

    return new Vector3(heightMeters * aspectRatio, heightMeters, DEFAULT_SIZE_Z);
  }
}
