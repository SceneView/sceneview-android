/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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
 */
package com.google.ar.sceneform.ux;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;


/** This drives the AR hand motion animation. */

public class HandMotionAnimation extends Animation {
  private final View handImageView;
  private final View containerView;
  private static final float TWO_PI = (float) Math.PI * 2.0f;
  private static final float HALF_PI = (float) Math.PI / 2.0f;

  public HandMotionAnimation(View containerView, View handImageView) {
    this.handImageView = handImageView;
    this.containerView = containerView;

  }

  @Override
  protected void applyTransformation(float interpolatedTime, Transformation transformation) {
    float startAngle = HALF_PI;
    float progressAngle = TWO_PI * interpolatedTime;
    float currentAngle = startAngle + progressAngle;

    float handWidth = handImageView.getWidth();
    float radius = handImageView.getResources().getDisplayMetrics().density * 25.0f;

    float xPos = radius * 2.0f * (float) Math.cos(currentAngle);
    float yPos = radius * (float) Math.sin(currentAngle);

    xPos += containerView.getWidth() / 2.0f;
    yPos += containerView.getHeight() / 2.0f;

    xPos -= handWidth / 2.0f;
    yPos -= handImageView.getHeight() / 2.0f;

    // Position the hand.
    handImageView.setX(xPos);
    handImageView.setY(yPos);

    handImageView.invalidate();
  }
}
