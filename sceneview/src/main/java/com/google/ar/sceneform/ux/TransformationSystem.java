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
import android.view.MotionEvent;

import androidx.annotation.Nullable;

import com.google.ar.sceneform.PickHitResult;

import java.util.ArrayList;

/**
 * Coordinates which {@link BaseTransformableNode} is currently selected. Also, detects various
 * gestures used by the transformation controls of {@link BaseTransformableNode}.
 *
 * <p>{@link #onTouch(PickHitResult, MotionEvent)} must be called for gestures to be detected. By
 * default, this is done automatically by ArFragment.
 */
public class TransformationSystem {
  private final GesturePointersUtility gesturePointersUtility;

  private final DragGestureRecognizer dragGestureRecognizer;
  private final PinchGestureRecognizer pinchGestureRecognizer;
  private final TwistGestureRecognizer twistGestureRecognizer;

  private final ArrayList<BaseGestureRecognizer<?>> recognizers = new ArrayList<>();

  private SelectionVisualizer selectionVisualizer;

  @Nullable private BaseTransformableNode selectedNode;

  @SuppressWarnings("initialization")
  public TransformationSystem(
      DisplayMetrics displayMetrics, SelectionVisualizer selectionVisualizer) {
    this.selectionVisualizer = selectionVisualizer;

    gesturePointersUtility = new GesturePointersUtility(displayMetrics);

    dragGestureRecognizer = new DragGestureRecognizer(gesturePointersUtility);
    addGestureRecognizer(dragGestureRecognizer);

    pinchGestureRecognizer = new PinchGestureRecognizer(gesturePointersUtility);
    addGestureRecognizer(pinchGestureRecognizer);

    twistGestureRecognizer = new TwistGestureRecognizer(gesturePointersUtility);
    addGestureRecognizer(twistGestureRecognizer);
  }

  /**
   * Sets the selection visualizer used to visualize which {@link BaseTransformableNode} is
   * currently selected. If there is already a selected node, then the old selection visual is
   * removed and the new one is applied immediately.
   */
  public void setSelectionVisualizer(SelectionVisualizer selectionVisualizer) {
    if (selectedNode != null) {
      this.selectionVisualizer.removeSelectionVisual(selectedNode);
    }

    this.selectionVisualizer = selectionVisualizer;

    if (selectedNode != null) {
      this.selectionVisualizer.applySelectionVisual(selectedNode);
    }
  }

  /**
   * Gets the selection visualizer used to visualize which {@link BaseTransformableNode} is
   * currently selected.
   */
  public SelectionVisualizer getSelectionVisualizer() {
    return selectionVisualizer;
  }

  /**
   * Gets the utility used by {@link BaseGestureRecognizer} subclasses to retain/release pointer Ids
   * so that each pointer can only be used in one gesture at a time.
   */
  public GesturePointersUtility getGesturePointersUtility() {
    return gesturePointersUtility;
  }

  /**
   * Gets the gesture recognizer for determining when the user performs a drag motion on the touch
   * screen.
   */
  public DragGestureRecognizer getDragRecognizer() {
    return dragGestureRecognizer;
  }

  /**
   * Gets the gesture recognizer for determining when the user performs a two-finger pinch motion on
   * the touch screen.
   */
  public PinchGestureRecognizer getPinchRecognizer() {
    return pinchGestureRecognizer;
  }

  /**
   * Gets the gesture recognizer for determining when the user performs a two-finger twist motion on
   * the touch screen.
   */
  public TwistGestureRecognizer getTwistRecognizer() {
    return twistGestureRecognizer;
  }

  /**
   * Adds a gesture recognizer to this transformation system. Touch events will be dispatched to the
   * recognizer when {@link #onTouch(PickHitResult, MotionEvent)} is called.
   */
  public void addGestureRecognizer(BaseGestureRecognizer<?> gestureRecognizer) {
    recognizers.add(gestureRecognizer);
  }

  /**
   * Gets the currently selected node. Only the currently selected node can be transformed. Nodes
   * are selected automatically when they are tapped, or when the user begins to translate the node
   * with a drag gesture.
   */
  @Nullable
  public BaseTransformableNode getSelectedNode() {
    return selectedNode;
  }

  /**
   * Sets a {@link BaseTransformableNode} as the selected node if there is no currently selected
   * node or if the currently selected node is not actively being transformed. If null, then
   * deselects the currently selected node if the node is not transforming.
   *
   * @see BaseTransformableNode#isTransforming
   * @return true if the node was successfully selected
   */
  public boolean selectNode(@Nullable BaseTransformableNode node) {
    if (!deselectNode()) {
      return false;
    }

    if (node != null) {
      selectedNode = node;
      selectionVisualizer.applySelectionVisual(selectedNode);
    }

    return true;
  }

  /** Dispatches touch events to the gesture recognizers contained by this transformation system. */
  public void onTouch(PickHitResult pickHitResult, MotionEvent motionEvent) {
    for (int i = 0; i < recognizers.size(); i++) {
      recognizers.get(i).onTouch(pickHitResult, motionEvent);
    }
  }

  /**
   * Deselects the currently selected node if the node is not currently transforming.
   *
   * @see BaseTransformableNode#isTransforming
   * @return true if the node was successfully deselected
   */
  private boolean deselectNode() {
    if (selectedNode == null) {
      return true;
    }

    if (selectedNode.isTransforming()) {
      return false;
    }

    selectionVisualizer.removeSelectionVisual(selectedNode);
    selectedNode = null;

    return true;
  }
}
