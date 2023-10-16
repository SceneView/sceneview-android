package io.github.sceneview.collision;

import android.animation.TypeEvaluator;

/** TypeEvaluator for Quaternions. Used to animate rotations. */
public class QuaternionEvaluator implements TypeEvaluator<Quaternion> {
  @Override
  public Quaternion evaluate(float fraction, Quaternion startValue, Quaternion endValue) {
    return Quaternion.slerp(startValue, endValue, fraction);
  }
}
