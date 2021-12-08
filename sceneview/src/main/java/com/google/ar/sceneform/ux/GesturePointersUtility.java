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

import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;

import com.google.ar.sceneform.math.Vector3;

import java.util.HashSet;

/**
 * Retains/Releases pointer Ids so that each pointer can only be used in one gesture at a time.
 * Provides helper functions for converting touch coordinates between pixels and inches.
 */
public class GesturePointersUtility {
  private final DisplayMetrics displayMetrics;
  private final HashSet<Integer> retainedPointerIds;

  public GesturePointersUtility(DisplayMetrics displayMetrics) {
    this.displayMetrics = displayMetrics;
    retainedPointerIds = new HashSet<>();
  }

  public void retainPointerId(int pointerId) {
    if (!isPointerIdRetained(pointerId)) {
      retainedPointerIds.add(pointerId);
    }
  }

  public void releasePointerId(int pointerId) {
    retainedPointerIds.remove(Integer.valueOf(pointerId));
  }

  public boolean isPointerIdRetained(int pointerId) {
    return retainedPointerIds.contains(pointerId);
  }

  public float inchesToPixels(float inches) {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, inches, displayMetrics);
  }

  public float pixelsToInches(float pixels) {
    float inchOfPixels =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_IN, 1.0f, displayMetrics);
    return pixels / inchOfPixels;
  }

  public static Vector3 motionEventToPosition(MotionEvent me, int pointerId) {
    int index = me.findPointerIndex(pointerId);
    return new Vector3(me.getX(index), me.getY(index), 0.0f);
  }
}
