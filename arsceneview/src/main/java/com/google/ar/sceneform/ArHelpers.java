package com.google.ar.sceneform;

import com.google.ar.core.Pose;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

/** Helper class for utility functions for interacting with the ARCore API. */
public class ArHelpers {
  /** Returns a Sceneform {@link Vector3} representing the position from an ARCore {@link Pose}. */
  public static Vector3 extractPositionFromPose(Pose pose) {
    return new Vector3(pose.tx(), pose.ty(), pose.tz());
  }

  /**
   * Returns a Sceneform {@link Quaternion} representing the rotation from an ARCore {@link Pose}.
   */
  public static Quaternion extractRotationFromPose(Pose pose) {
    return new Quaternion(pose.qx(), pose.qy(), pose.qz(), pose.qw());
  }
}
