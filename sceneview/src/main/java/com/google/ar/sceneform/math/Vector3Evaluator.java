package com.google.ar.sceneform.math;

import android.animation.TypeEvaluator;

/** TypeEvaluator for Vector3. Used to animate positions and other vectors. */
public class Vector3Evaluator implements TypeEvaluator<Vector3> {
  @Override
  public Vector3 evaluate(float fraction, Vector3 startValue, Vector3 endValue) {
    return Vector3.lerp(startValue, endValue, fraction);
  }
}
