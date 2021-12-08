package com.google.ar.sceneform.collision;

import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.utilities.ChangeId;

/** Base class for all types of shapes that collision checks can be performed against. */
public abstract class CollisionShape {
  private final ChangeId changeId = new ChangeId();

  public abstract CollisionShape makeCopy();

  /**
   * Must be called by subclasses when the shape changes to inform listeners of the change.
   *
   * @hide
   */
  protected void onChanged() {
    changeId.update();
  }

  /** @hide */
  protected abstract boolean rayIntersection(Ray ray, RayHit result);

  /** @hide */
  protected abstract boolean shapeIntersection(CollisionShape shape);

  /** @hide */
  protected abstract boolean sphereIntersection(Sphere sphere);

  /** @hide */
  protected abstract boolean boxIntersection(Box box);

  @SuppressWarnings("initialization")
  CollisionShape() {
    changeId.update();
  }

  ChangeId getId() {
    return changeId;
  }

  abstract CollisionShape transform(TransformProvider transformProvider);

  abstract void transform(TransformProvider transformProvider, CollisionShape result);
}
