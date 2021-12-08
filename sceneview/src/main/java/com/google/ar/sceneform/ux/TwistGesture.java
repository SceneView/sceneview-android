/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.ux;

import android.util.Log;
import android.view.MotionEvent;

import com.google.ar.sceneform.PickHitResult;
import com.google.ar.sceneform.math.Vector3;

/** Gesture for when the user performs a two-finger twist motion on the touch screen. */
public class TwistGesture extends BaseGesture<TwistGesture> {
  private static final String TAG = TwistGesture.class.getSimpleName();

  /** Interface definition for callbacks to be invoked by a {@link TwistGesture}. */
  public interface OnGestureEventListener
      extends BaseGesture.OnGestureEventListener<TwistGesture> {}

  private static final boolean TWIST_GESTURE_DEBUG = false;

  private final int pointerId1;
  private final int pointerId2;
  private final Vector3 startPosition1;
  private final Vector3 startPosition2;
  private final Vector3 previousPosition1;
  private final Vector3 previousPosition2;
  private float deltaRotationDegrees;

  private static final float SLOP_ROTATION_DEGREES = 15.0f;

  public TwistGesture(
      GesturePointersUtility gesturePointersUtility, MotionEvent motionEvent, int pointerId2) {
    super(gesturePointersUtility);

    pointerId1 = motionEvent.getPointerId(motionEvent.getActionIndex());
    this.pointerId2 = pointerId2;
    startPosition1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1);
    startPosition2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2);
    previousPosition1 = new Vector3(startPosition1);
    previousPosition2 = new Vector3(startPosition2);
    debugLog("Created");
  }

  public float getDeltaRotationDegrees() {
    return deltaRotationDegrees;
  }

  @Override
  protected boolean canStart(PickHitResult pickHitResult, MotionEvent motionEvent) {
    if (gesturePointersUtility.isPointerIdRetained(pointerId1)
        || gesturePointersUtility.isPointerIdRetained(pointerId2)) {
      cancel();
      return false;
    }

    int actionId = motionEvent.getPointerId(motionEvent.getActionIndex());
    int action = motionEvent.getActionMasked();

    if (action == MotionEvent.ACTION_CANCEL) {
      cancel();
      return false;
    }

    boolean touchEnded = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP;

    if (touchEnded && (actionId == pointerId1 || actionId == pointerId2)) {
      cancel();
      return false;
    }

    if (action != MotionEvent.ACTION_MOVE) {
      return false;
    }

    Vector3 newPosition1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1);
    Vector3 newPosition2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2);
    Vector3 deltaPosition1 = Vector3.subtract(newPosition1, previousPosition1);
    Vector3 deltaPosition2 = Vector3.subtract(newPosition2, previousPosition2);
    previousPosition1.set(newPosition1);
    previousPosition2.set(newPosition2);

    // Check that both fingers are moving.
    if (Vector3.equals(deltaPosition1, Vector3.zero())
        || Vector3.equals(deltaPosition2, Vector3.zero())) {
      return false;
    }

    float rotation =
        calculateDeltaRotation(newPosition1, newPosition2, startPosition1, startPosition2);
    if (Math.abs(rotation) < SLOP_ROTATION_DEGREES) {
      return false;
    }

    return true;
  }

  @Override
  protected void onStart(PickHitResult pickHitResult, MotionEvent motionEvent) {
    debugLog("Started");
    gesturePointersUtility.retainPointerId(pointerId1);
    gesturePointersUtility.retainPointerId(pointerId2);
  }

  @Override
  protected boolean updateGesture(PickHitResult pickHitResult, MotionEvent motionEvent) {
    int actionId = motionEvent.getPointerId(motionEvent.getActionIndex());
    int action = motionEvent.getActionMasked();

    if (action == MotionEvent.ACTION_CANCEL) {
      cancel();
      return false;
    }

    boolean touchEnded = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP;

    if (touchEnded && (actionId == pointerId1 || actionId == pointerId2)) {
      complete();
      return false;
    }

    if (action != MotionEvent.ACTION_MOVE) {
      return false;
    }

    Vector3 newPosition1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1);
    Vector3 newPosition2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2);
    deltaRotationDegrees =
        calculateDeltaRotation(newPosition1, newPosition2, previousPosition1, previousPosition2);
    previousPosition1.set(newPosition1);
    previousPosition2.set(newPosition2);
    debugLog("Update: " + deltaRotationDegrees);
    return true;
  }

  @Override
  protected void onCancel() {
    debugLog("Cancelled");
  }

  @Override
  protected void onFinish() {
    debugLog("Finished");
    gesturePointersUtility.releasePointerId(pointerId1);
    gesturePointersUtility.releasePointerId(pointerId2);
  }

  @Override
  protected TwistGesture getSelf() {
    return this;
  }

  private static void debugLog(String log) {
    if (TWIST_GESTURE_DEBUG) {
      Log.d(TAG, "TwistGesture:[" + log + "]");
    }
  }

  private static float calculateDeltaRotation(
      Vector3 currentPosition1,
      Vector3 currentPosition2,
      Vector3 previousPosition1,
      Vector3 previousPosition2) {
    Vector3 currentDirection = Vector3.subtract(currentPosition1, currentPosition2).normalized();
    Vector3 previousDirection = Vector3.subtract(previousPosition1, previousPosition2).normalized();
    float sign =
        Math.signum(
            previousDirection.x * currentDirection.y - previousDirection.y * currentDirection.x);
    return Vector3.angleBetweenVectors(currentDirection, previousDirection) * sign;
  }
}
