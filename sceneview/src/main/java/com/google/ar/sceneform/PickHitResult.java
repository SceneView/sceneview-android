package com.google.ar.sceneform;

import androidx.annotation.Nullable;
import com.google.ar.sceneform.collision.RayHit;
import io.github.sceneview.node.Node;

/**
 * Stores the results of calls to Scene.hitTest and Scene.hitTestAll. Contains a node that was hit
 * by the hit test, and associated information.
 */
public class PickHitResult extends RayHit {
  @Nullable private Node node;

  /** @hide */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public void setNode(@Nullable Node node) {
    this.node = node;
  }

  /**
   * The node that was hit by the hit test. Null when there is no hit.
   *
   * @return the hit node
   */
  @Nullable
  public Node getNode() {
    return node;
  }

  /** @hide */
  public void set(PickHitResult other) {
    super.set(other);
    setNode(other.node);
  }

  /** @hide */
  @Override
  public void reset() {
    super.reset();
    node = null;
  }
}
