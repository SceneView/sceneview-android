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

import androidx.annotation.Nullable;

import com.google.ar.sceneform.rendering.ModelRenderable;
import io.github.sceneview.node.ModelNode;

/**
 * Visualizes that a {@link BaseTransformableNode} is selected by rendering a footprint for the
 * selected node.
 */
public class FootprintSelectionVisualizer implements SelectionVisualizer {
  private ModelNode footprintNode;
  @Nullable private ModelRenderable footprintRenderable;

  public FootprintSelectionVisualizer() {
    footprintNode = new ModelNode();
  }

  public void setFootprintRenderable(ModelRenderable renderable) {
    ModelRenderable copyRenderable = renderable.makeCopy();
    footprintNode.setRenderable(copyRenderable);
    copyRenderable.setCollisionShape(null);
    footprintRenderable = copyRenderable;
  }

  @Nullable
  public ModelRenderable getFootprintRenderable() {
    return footprintRenderable;
  }

  @Override
  public void applySelectionVisual(BaseTransformableNode node) {
    footprintNode.setParent(node);
  }

  @Override
  public void removeSelectionVisual(BaseTransformableNode node) {
    footprintNode.setParent(null);
  }
}
