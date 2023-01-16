package com.google.ar.sceneform.rendering;

import android.view.View;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * Controls the size of a {@link ViewRenderable} in a {@link com.google.ar.sceneform.Scene} by
 * defining how many dp (density-independent pixels) there are per meter. This is recommended when
 * using an android layout that is built using dp.
 *
 * @see ViewRenderable.Builder#setSizer(ViewSizer)
 * @see ViewRenderable#setSizer(ViewSizer)
 */

public class DpToMetersViewSizer implements ViewSizer {
  private final int dpPerMeters;

  // Defaults to zero, Z value of the size doesn't currently have any semantic meaning,
  // but we may add that in later if we support ViewRenderables that have depth.
  private static final float DEFAULT_SIZE_Z = 0.0f;

  /**
   * Constructor for creating a sizer for controlling the size of a {@link ViewRenderable} by
   * defining how many dp there are per meter.
   *
   * @param dpPerMeters a number greater than zero representing the ratio of dp to meters
   */
  public DpToMetersViewSizer(int dpPerMeters) {
    if (dpPerMeters <= 0) {
      throw new IllegalArgumentException("dpPerMeters must be greater than zero.");
    }

    this.dpPerMeters = dpPerMeters;
  }

  /**
   * Returns the number of dp (density-independent pixels) there are per meter that is used for
   * controlling the size of a {@link ViewRenderable}.
   */
  public int getDpPerMeters() {
    return dpPerMeters;
  }

  @Override
  public Vector3 getSize(View view) {
    Preconditions.checkNotNull(view, "Parameter \"view\" was null.");

    float widthDp = ViewRenderableHelpers.convertPxToDp(view.getWidth());
    float heightDp = ViewRenderableHelpers.convertPxToDp(view.getHeight());

    return new Vector3(widthDp / dpPerMeters, heightDp / dpPerMeters, DEFAULT_SIZE_Z);
  }
}
