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

/** Gesture for when the user performs a drag motion on the touch screen. */
public class DragGesture extends BaseGesture<DragGesture> {
  private static final String TAG = DragGesture.class.getSimpleName();

  /** Interface definition for callbacks to be invoked by a {@link DragGesture}. */
  public interface OnGestureEventListener extends BaseGesture.OnGestureEventListener<DragGesture> {}

  private final Vector3 startPosition;
  private final Vector3 position;
  private final Vector3 delta;
  private final int pointerId;

  private static final float SLOP_INCHES = 0.1f;
  private static final boolean DRAG_GESTURE_DEBUG = false;

  public DragGesture(
      GesturePointersUtility gesturePointersUtility,
      PickHitResult pickHitResult,
      MotionEvent motionEvent) {
    super(gesturePointersUtility);

    pointerId = motionEvent.getPointerId(motionEvent.getActionIndex());
    startPosition = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId);
    position = new Vector3(startPosition);
    delta = Vector3.zero();
    targetNode = pickHitResult.getNode();
    debugLog("Created: " + pointerId);
  }

  public Vector3 getPosition() {
    return new Vector3(position);
  }

  public Vector3 getDelta() {
    return new Vector3(delta);
  }

  @Override
  protected boolean canStart(PickHitResult pickHitResult, MotionEvent motionEvent) {
    int actionId = motionEvent.getPointerId(motionEvent.getActionIndex());
    int action = motionEvent.getActionMasked();

    if (gesturePointersUtility.isPointerIdRetained(pointerId)) {
      cancel();
      return false;
    }

    if (actionId == pointerId
        && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)) {
      cancel();
      return false;
    } else if (action == MotionEvent.ACTION_CANCEL) {
      cancel();
      return false;
    }

    if (action != MotionEvent.ACTION_MOVE) {
      return false;
    }

    if (motionEvent.getPointerCount() > 1) {
      for (int i = 0; i < motionEvent.getPointerCount(); i++) {
        int id = motionEvent.getPointerId(i);
        if (id != pointerId && !gesturePointersUtility.isPointerIdRetained(id)) {
          return false;
        }
      }
    }

    Vector3 newPosition = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId);
    float diff = Vector3.subtract(newPosition, startPosition).length();
    float slopPixels = gesturePointersUtility.inchesToPixels(SLOP_INCHES);
    if (diff >= slopPixels) {
      return true;
    }

    return false;
  }

  @Override
  protected void onStart(PickHitResult pickHitResult, MotionEvent motionEvent) {
    debugLog("Started: " + pointerId);

    position.set(GesturePointersUtility.motionEventToPosition(motionEvent, pointerId));
    gesturePointersUtility.retainPointerId(pointerId);
  }

  @Override
  protected boolean updateGesture(PickHitResult pickHitResult, MotionEvent motionEvent) {
    int actionId = motionEvent.getPointerId(motionEvent.getActionIndex());
    int action = motionEvent.getActionMasked();

    if (action == MotionEvent.ACTION_MOVE) {
      Vector3 newPosition = GesturePointersUtility.motionEventToPosition(motionEvent, pointerId);
      if (!Vector3.equals(newPosition, position)) {
        delta.set(Vector3.subtract(newPosition, position));
        position.set(newPosition);
        debugLog("Updated: " + pointerId + " : " + position);
        return true;
      }
    } else if (actionId == pointerId
        && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP)) {
      complete();
    } else if (action == MotionEvent.ACTION_CANCEL) {
      cancel();
    }

    return false;
  }

  @Override
  protected void onCancel() {
    debugLog("Cancelled: " + pointerId);
  }

  @Override
  protected void onFinish() {
    debugLog("Finished: " + pointerId);
    gesturePointersUtility.releasePointerId(pointerId);
  }

  @Override
  protected DragGesture getSelf() {
    return this;
  }

  private static void debugLog(String log) {
    if (DRAG_GESTURE_DEBUG) {
      Log.d(TAG, "DragGesture:[" + log + "]");
    }
  }
}
