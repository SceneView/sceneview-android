package com.google.ar.sceneform.rendering;

import android.view.View;

import com.google.ar.sceneform.math.Vector3;

/**
 * Interface for controlling the size of a {@link ViewRenderable} in the {@link
 * com.google.ar.sceneform.Scene}. The final size that the view is displayed at will be the size
 * from this {@link ViewSizer} scaled by the {@link com.google.ar.sceneform.Node#getWorldScale()} of
 * the {@link com.google.ar.sceneform.Node} that the {@link ViewRenderable} is attached to.
 */

public interface ViewSizer {
  /**
   * Calculates the desired size of the view in the {@link com.google.ar.sceneform.Scene}. {@link
   * Vector3#x} represents the width, and {@link Vector3#y} represents the height.
   *
   * @param view the view to calculate the size of
   * @return a new vector that represents the view's size in the {@link
   *     com.google.ar.sceneform.Scene}
   */
  Vector3 getSize(View view);
}
