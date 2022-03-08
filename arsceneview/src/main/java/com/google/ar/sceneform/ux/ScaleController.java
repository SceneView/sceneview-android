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

import com.google.ar.sceneform.math.MathHelper;
import com.google.ar.sceneform.math.Vector3;

import io.github.sceneview.math.UtilsKt;
import io.github.sceneview.node.Node;
import io.github.sceneview.utils.FrameTime;

/**
 * Manipulates the Scale of a {@link BaseTransformableNode} using a Pinch {@link
 * PinchGestureRecognizer}. Applies a tunable elastic bounce-back when scaling the {@link
 * BaseTransformableNode} beyond the min/max scale.
 */
public class ScaleController extends BaseTransformationController<PinchGesture> {
  public static final float DEFAULT_MIN_SCALE = 0.75f;
  public static final float DEFAULT_MAX_SCALE = 1.75f;
  public static final float DEFAULT_SENSITIVITY = 0.75f;
  public static final float DEFAULT_ELASTICITY = 0.15f;

  private float minScale = DEFAULT_MIN_SCALE;
  private float maxScale = DEFAULT_MAX_SCALE;
  private float sensitivity = DEFAULT_SENSITIVITY;
  private float elasticity = DEFAULT_ELASTICITY;

  private float currentScaleRatio;

  private static final float ELASTIC_RATIO_LIMIT = 0.8f;
  private static final float LERP_SPEED = 8.0f;

  public ScaleController(
      BaseTransformableNode transformableNode, PinchGestureRecognizer gestureRecognizer) {
    super(transformableNode, gestureRecognizer);
  }

  public void setMinScale(float minScale) {
    this.minScale = minScale;
  }

  public float getMinScale() {
    return minScale;
  }

  public void setMaxScale(float maxScale) {
    this.maxScale = maxScale;
  }

  public float getMaxScale() {
    return maxScale;
  }

  public void setSensitivity(float sensitivity) {
    this.sensitivity = sensitivity;
  }

  public float getSensitivity() {
    return sensitivity;
  }

  public void setElasticity(float elasticity) {
    this.elasticity = elasticity;
  }

  public float getElasticity() {
    return elasticity;
  }

  @Override
  public void onActivated(Node node) {
    super.onActivated(node);
    Vector3 scale = UtilsKt.toVector3(getTransformableNode().getScale());
    currentScaleRatio = (scale.x - minScale) / getScaleDelta();
  }

  @Override
  public void onFrameUpdated(FrameTime frameTime, Node node) {
    if (isTransforming() || !isEnabled()) {
      return;
    }

    float t = MathHelper.clamp((float) (frameTime.getIntervalSeconds() * LERP_SPEED), 0, 1);
    currentScaleRatio = MathHelper.lerp(currentScaleRatio, getClampedScaleRatio(), t);
    float finalScaleValue = getFinalScale();
    Vector3 finalScale = new Vector3(finalScaleValue, finalScaleValue, finalScaleValue);
    getTransformableNode().setScale(UtilsKt.toFloat3(finalScale));
  }

  @Override
  public boolean canStartTransformation(PinchGesture gesture) {
    return getTransformableNode().isSelected();
  }

  @Override
  public void onContinueTransformation(PinchGesture gesture) {
    currentScaleRatio += gesture.gapDeltaInches() * sensitivity;

    float finalScaleValue = getFinalScale();
    Vector3 finalScale = new Vector3(finalScaleValue, finalScaleValue, finalScaleValue);
    getTransformableNode().setScale(UtilsKt.toFloat3(finalScale));

    if (currentScaleRatio < -ELASTIC_RATIO_LIMIT
        || currentScaleRatio > (1.0f + ELASTIC_RATIO_LIMIT)) {
      gesture.cancel();
    }
  }

  @Override
  public void onEndTransformation(PinchGesture gesture) {}

  private float getScaleDelta() {
    float scaleDelta = maxScale - minScale;

    if (scaleDelta <= 0.0f) {
      throw new IllegalStateException("maxScale must be greater than minScale.");
    }

    return scaleDelta;
  }

  private float getClampedScaleRatio() {
    return Math.min(1.0f, Math.max(0.0f, currentScaleRatio));
  }

  private float getFinalScale() {
    float elasticScaleRatio = getClampedScaleRatio() + getElasticDelta();
    float elasticScale = minScale + elasticScaleRatio * getScaleDelta();
    return elasticScale;
  }

  private float getElasticDelta() {
    float overRatio;
    if (currentScaleRatio > 1.0f) {
      overRatio = currentScaleRatio - 1.0f;
    } else if (currentScaleRatio < 0.0f) {
      overRatio = currentScaleRatio;
    } else {
      return 0.0f;
    }

    return (1.0f - (1.0f / ((Math.abs(overRatio) * elasticity) + 1.0f))) * Math.signum(overRatio);
  }
}
