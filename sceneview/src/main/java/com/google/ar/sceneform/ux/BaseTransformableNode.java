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
import io.github.sceneview.node.Node;

import java.util.ArrayList;

/**
 * Base class for nodes that can be transformed using gestures from {@link TransformationSystem}.
 */
public abstract class BaseTransformableNode extends Node {
  private final TransformationSystem transformationSystem;
  private final ArrayList<BaseTransformationController<?>> controllers = new ArrayList<>();

  @SuppressWarnings("initialization")
  public BaseTransformableNode(TransformationSystem transformationSystem) {
    this.transformationSystem = transformationSystem;

    setOnTap((hitTestResult, motionEvent) -> {
      onTap(hitTestResult, motionEvent);
      return null;
    });
  }

  public TransformationSystem getTransformationSystem() {
    return transformationSystem;
  }

  /** Returns true if any of the transformation controllers are actively transforming this node. */
  public boolean isTransforming() {
    for (int i = 0; i < controllers.size(); i++) {
      if (controllers.get(i).isTransforming()) {
        return true;
      }
    }

    return false;
  }

  /** Returns true if this node is currently selected by the TransformationSystem. */
  public boolean isSelected() {
    return transformationSystem.getSelectedNode() == this;
  }

  /**
   * Sets this as the selected node in the TransformationSystem if there is no currently selected
   * node or if the currently selected node is not actively being transformed.
   *
   * @see TransformableNode#isTransforming
   * @return true if the node was successfully selected
   */
  public boolean select() {
    return transformationSystem.selectNode(this);
  }

  public void onTap(PickHitResult pickHitResult, MotionEvent motionEvent) {
    select();
  }

  protected void addTransformationController(
      BaseTransformationController<?> transformationController) {
    controllers.add(transformationController);
  }

  protected void removeTransformationController(
      BaseTransformationController<?> transformationController) {
    controllers.remove(transformationController);
  }
}
