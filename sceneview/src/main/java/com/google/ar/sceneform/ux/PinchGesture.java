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

/** Gesture for when the user performs a two-finger pinch motion on the touch screen. */
public class PinchGesture extends BaseGesture<PinchGesture> {
  private static final String TAG = PinchGesture.class.getSimpleName();

  /** Interface definition for callbacks to be invoked by a {@link PinchGesture}. */
  public interface OnGestureEventListener
      extends BaseGesture.OnGestureEventListener<PinchGesture> {}

  private final int pointerId1;
  private final int pointerId2;
  private final Vector3 startPosition1;
  private final Vector3 startPosition2;
  private final Vector3 previousPosition1;
  private final Vector3 previousPosition2;
  private float gap;
  private float gapDelta;

  private static final float SLOP_INCHES = 0.05f;
  private static final float SLOP_MOTION_DIRECTION_DEGREES = 30.0f;

  private static final boolean PINCH_GESTURE_DEBUG = false;

  public PinchGesture(
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

  public float getGap() {
    return gap;
  }

  public float gapInches() {
    return gesturePointersUtility.pixelsToInches(getGap());
  }

  public float getGapDelta() {
    return gapDelta;
  }

  public float gapDeltaInches() {
    return gesturePointersUtility.pixelsToInches(getGapDelta());
  }

  /** Cancels the gesture in progress. */
  @Override
  public void cancel() {
    super.cancel();
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

    Vector3 firstToSecond = Vector3.subtract(startPosition1, startPosition2);
    Vector3 firstToSecondDirection = firstToSecond.normalized();

    Vector3 newPosition1 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId1);
    Vector3 newPosition2 = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId2);
    Vector3 deltaPosition1 = Vector3.subtract(newPosition1, previousPosition1);
    Vector3 deltaPosition2 = Vector3.subtract(newPosition2, previousPosition2);
    previousPosition1.set(newPosition1);
    previousPosition2.set(newPosition2);

    float dot1 = Vector3.dot(deltaPosition1.normalized(), firstToSecondDirection.negated());
    float dot2 = Vector3.dot(deltaPosition2.normalized(), firstToSecondDirection);
    float dotThreshold = (float) Math.cos(Math.toRadians(SLOP_MOTION_DIRECTION_DEGREES));

    // Check angle of motion for the first touch.
    if (!Vector3.equals(deltaPosition1, Vector3.zero()) && Math.abs(dot1) < dotThreshold) {
      return false;
    }

    // Check angle of motion for the second touch.
    if (!Vector3.equals(deltaPosition2, Vector3.zero()) && Math.abs(dot2) < dotThreshold) {
      return false;
    }

    float startGap = firstToSecond.length();
    gap = Vector3.subtract(newPosition1, newPosition2).length();
    float separation = Math.abs(gap - startGap);
    float slopPixels = gesturePointersUtility.inchesToPixels(SLOP_INCHES);
    if (separation < slopPixels) {
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
    float newGap = Vector3.subtract(newPosition1, newPosition2).length();

    if (newGap == gap) {
      return false;
    }

    gapDelta = newGap - gap;
    gap = newGap;
    debugLog("Update: " + gapDelta);
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
  protected PinchGesture getSelf() {
    return this;
  }

  private static void debugLog(String log) {
    if (PINCH_GESTURE_DEBUG) {
      Log.d(TAG, "PinchGesture:[" + log + "]");
    }
  }
}
