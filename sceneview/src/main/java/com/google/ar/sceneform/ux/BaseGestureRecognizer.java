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

import java.util.ArrayList;

/**
 * Base class for all Gesture Recognizers (i.e. DragGestureRecognizer).
 *
 * <p>A Gesture recognizer processes touch input to determine if a gesture should start and fires an
 * event when the gesture is started.
 *
 * <p>To determine when an gesture is finished/updated, listen to the events on the gesture object.
 */
public abstract class BaseGestureRecognizer<T extends BaseGesture<T>> {
  /** Interface definition for a callbacks to be invoked when a {@link BaseGesture} starts. */
  public interface OnGestureStartedListener<T extends BaseGesture<T>> {
    void onGestureStarted(T gesture);
  }

  protected final GesturePointersUtility gesturePointersUtility;
  protected final ArrayList<T> gestures = new ArrayList<>();

  private final ArrayList<OnGestureStartedListener<T>> gestureStartedListeners;

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public BaseGestureRecognizer(GesturePointersUtility gesturePointersUtility) {
    this.gesturePointersUtility = gesturePointersUtility;
    gestureStartedListeners = new ArrayList<>();
  }

  public void addOnGestureStartedListener(OnGestureStartedListener<T> listener) {
    if (!gestureStartedListeners.contains(listener)) {
      gestureStartedListeners.add(listener);
    }
  }

  public void removeOnGestureStartedListener(OnGestureStartedListener<T> listener) {
    gestureStartedListeners.remove(listener);
  }

  public void onTouch(PickHitResult pickHitResult, MotionEvent motionEvent) {
    // Instantiate gestures based on touch input.
    // Just because a gesture was created, doesn't mean that it is started.
    // For example, a DragGesture is created when the user touch's down,
    // but doesn't actually start until the touch has moved beyond a threshold.
    tryCreateGestures(pickHitResult, motionEvent);

    // Propagate event to gestures and determine if they should start.
    for (int i = 0; i < gestures.size(); i++) {
      T gesture = gestures.get(i);
      gesture.onTouch(pickHitResult, motionEvent);

      if (gesture.justStarted()) {
        dispatchGestureStarted(gesture);
      }
    }

    removeFinishedGestures();
  }

  protected abstract void tryCreateGestures(PickHitResult pickHitResult, MotionEvent motionEvent);

  private void dispatchGestureStarted(T gesture) {
    for (int i = 0; i < gestureStartedListeners.size(); i++) {
      OnGestureStartedListener<T> listener = gestureStartedListeners.get(i);
      listener.onGestureStarted(gesture);
    }
  }

  private void removeFinishedGestures() {
    for (int i = gestures.size() - 1; i >= 0; i--) {
      T gesture = gestures.get(i);
      if (gesture.hasFinished()) {
        gestures.remove(i);
      }
    }
  }
}
