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

import android.view.MotionEvent;

import com.google.ar.sceneform.PickHitResult;

/** Gesture Recognizer for when the user performs a drag motion on the touch screen. */
public class DragGestureRecognizer extends BaseGestureRecognizer<DragGesture> {
  /** Interface definition for a callbacks to be invoked when a {@link DragGesture} starts. */
  public interface OnGestureStartedListener
      extends BaseGestureRecognizer.OnGestureStartedListener<DragGesture> {}

  public DragGestureRecognizer(GesturePointersUtility gesturePointersUtility) {
    super(gesturePointersUtility);
  }

  @Override
  protected void tryCreateGestures(PickHitResult pickHitResult, MotionEvent motionEvent) {
    int action = motionEvent.getActionMasked();
    int actionId = motionEvent.getPointerId(motionEvent.getActionIndex());
    boolean touchBegan =
        action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN;

    if (touchBegan && !gesturePointersUtility.isPointerIdRetained(actionId)) {
      gestures.add(new DragGesture(gesturePointersUtility, pickHitResult, motionEvent));
    }
  }
}
