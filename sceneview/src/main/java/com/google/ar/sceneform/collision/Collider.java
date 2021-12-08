package com.google.ar.sceneform.collision;

import androidx.annotation.Nullable;
import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.utilities.ChangeId;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * Represents the collision information associated with a transformation that can be attached to the
 * collision system. Not publicly exposed.
 *
 * @hide
 */
public class Collider {
  private TransformProvider transformProvider;
  @Nullable private CollisionSystem attachedCollisionSystem;

  private CollisionShape localShape;
  @Nullable private CollisionShape cachedWorldShape;

  private boolean isWorldShapeDirty;
  private int shapeId = ChangeId.EMPTY_ID;

  /** @hide */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Collider(TransformProvider transformProvider, CollisionShape localCollisionShape) {
    Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.");
    Preconditions.checkNotNull(localCollisionShape, "Parameter \"localCollisionShape\" was null.");

    this.transformProvider = transformProvider;
    setShape(localCollisionShape);
  }

  /** @hide */
  public void setShape(CollisionShape localCollisionShape) {
    Preconditions.checkNotNull(localCollisionShape, "Parameter \"localCollisionShape\" was null.");

    localShape = localCollisionShape;
    cachedWorldShape = null;
  }

  /** @hide */
  public CollisionShape getShape() {
    return localShape;
  }

  public TransformProvider getTransformProvider() {
    return transformProvider;
  }

  /** @hide */
  @Nullable
  public CollisionShape getTransformedShape() {
    updateCachedWorldShape();
    return cachedWorldShape;
  }

  /** @hide */
  public void setAttachedCollisionSystem(@Nullable CollisionSystem collisionSystem) {
    if (attachedCollisionSystem != null) {
      attachedCollisionSystem.removeCollider(this);
    }

    attachedCollisionSystem = collisionSystem;

    if (attachedCollisionSystem != null) {
      attachedCollisionSystem.addCollider(this);
    }
  }

  /** @hide */
  public void markWorldShapeDirty() {
    isWorldShapeDirty = true;
  }

  private boolean doesCachedWorldShapeNeedUpdate() {
    if (localShape == null) {
      return false;
    }

    ChangeId changeId = localShape.getId();
    return changeId.checkChanged(shapeId) || isWorldShapeDirty || cachedWorldShape == null;
  }

  private void updateCachedWorldShape() {
    if (!doesCachedWorldShapeNeedUpdate()) {
      return;
    }

    if (cachedWorldShape == null) {
      cachedWorldShape = localShape.transform(transformProvider);
    } else {
      localShape.transform(transformProvider, cachedWorldShape);
    }

    ChangeId changeId = localShape.getId();
    shapeId = changeId.get();
  }
}
