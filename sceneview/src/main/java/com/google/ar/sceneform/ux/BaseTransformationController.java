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

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import io.github.sceneview.node.Node;
import io.github.sceneview.utils.FrameTime;

/**
 * Manipulates the transform properties (i.e. scale/rotation/translation) of a {@link
 * BaseTransformableNode} by responding to Gestures via a {@link BaseGestureRecognizer}.
 *
 * <p>Example's include, changing the TransformableNode's Scale based on a Pinch Gesture.
 */
public abstract class BaseTransformationController<T extends BaseGesture<T>>
    implements BaseGestureRecognizer.OnGestureStartedListener<T>,
        BaseGesture.OnGestureEventListener<T> {
  private final BaseTransformableNode transformableNode;
  private final BaseGestureRecognizer<T> gestureRecognizer;

  @Nullable private T activeGesture;
  private boolean enabled;
  private boolean activeAndEnabled;

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public BaseTransformationController(
      BaseTransformableNode transformableNode, BaseGestureRecognizer<T> gestureRecognizer) {
    this.transformableNode = transformableNode;
    this.transformableNode.getOnRenderingChanged().add((node, isRendering) -> {
      if(isRendering) {
        onActivated(node);
      } else {
        onDeactivated(node);
      }
      return null;
    });
    this.transformableNode.getOnFrameUpdate().add((frameTime, node) -> {
      onFrameUpdated(frameTime, node);
      return null;
    });
    this.gestureRecognizer = gestureRecognizer;
    setEnabled(true);
  }

  public boolean isEnabled() {
    return enabled;
  }

  @Nullable
  public T getActiveGesture() {
    return activeGesture;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    updateActiveAndEnabled();
  }

  public boolean isTransforming() {
    return activeGesture != null;
  }

  public BaseTransformableNode getTransformableNode() {
    return transformableNode;
  }

  // ---------------------------------------------------------------------------------------
  // Implementation of interface Node.LifecycleListener
  // ---------------------------------------------------------------------------------------

  @CallSuper
  public void onActivated(Node node) {
    updateActiveAndEnabled();
  }

  public void onFrameUpdated(FrameTime frameTime, Node node) {}

  @CallSuper
  public void onDeactivated(Node node) {
    updateActiveAndEnabled();
  }

  // ---------------------------------------------------------------------------------------
  // Implementation of interface BaseGestureRecognizer.OnGestureStartedListener
  // ---------------------------------------------------------------------------------------

  @Override
  public void onGestureStarted(T gesture) {
    if (isTransforming()) {
      return;
    }

    if (canStartTransformation(gesture)) {
      setActiveGesture(gesture);
    }
  }

  // ---------------------------------------------------------------------------------------
  // Implementation of interface BaseGesture.OnGestureEventListener
  // ---------------------------------------------------------------------------------------

  @SuppressWarnings("UngroupedOverloads") // This is not an overload, it is a different interface.
  @Override
  public void onUpdated(T gesture) {
    onContinueTransformation(gesture);
  }

  @Override
  public void onFinished(T gesture) {
    onEndTransformation(gesture);
    setActiveGesture(null);
  }

  protected abstract boolean canStartTransformation(T gesture);

  protected abstract void onContinueTransformation(T gesture);

  protected abstract void onEndTransformation(T gesture);

  private void setActiveGesture(@Nullable T gesture) {
    if (activeGesture != null) {
      activeGesture.setGestureEventListener(null);
    }

    activeGesture = gesture;

    if (activeGesture != null) {
      activeGesture.setGestureEventListener(this);
    }
  }

  private void updateActiveAndEnabled() {
    boolean newActiveAndEnabled = getTransformableNode().isRendered() && enabled;
    if (newActiveAndEnabled == activeAndEnabled) {
      return;
    }

    activeAndEnabled = newActiveAndEnabled;

    if (activeAndEnabled) {
      connectToRecognizer();
    } else {
      disconnectFromRecognizer();
      if (activeGesture != null) {
        activeGesture.cancel();
      }
    }
  }

  private void connectToRecognizer() {
    gestureRecognizer.addOnGestureStartedListener(this);
  }

  private void disconnectFromRecognizer() {
    gestureRecognizer.removeOnGestureStartedListener(this);
  }
}
