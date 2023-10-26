package io.github.sceneview.collision;

import androidx.annotation.Nullable;

import io.github.sceneview.math.MathKt;
import io.github.sceneview.node.Node;

/**
 * Represents the collision information associated with a transformation that can be attached to the
 * collision system. Not publicly exposed.
 *
 * @hide
 */
public class Collider {

    public Node node;

    @Nullable
    private CollisionSystem attachedCollisionSystem;

    private CollisionShape localShape;
    @Nullable
    private CollisionShape cachedWorldShape;

    private boolean isWorldShapeDirty;
    private int shapeId = ChangeId.EMPTY_ID;

    /**
     * @hide
     */
    @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
    public Collider(Node node) {
        this.node = node;
    }

    /**
     * @hide
     */
    public void setShape(CollisionShape localCollisionShape) {
        Preconditions.checkNotNull(localCollisionShape, "Parameter \"localCollisionShape\" was null.");

        if (localShape != localCollisionShape) {
            localShape = localCollisionShape;
            cachedWorldShape = null;
        }
    }

    /**
     * @hide
     */
    public CollisionShape getShape() {
        return localShape;
    }

    /**
     * @hide
     */
    @Nullable
    public CollisionShape getTransformedShape() {
        updateCachedWorldShape();
        return cachedWorldShape;
    }

    /**
     * @hide
     */
    public void setAttachedCollisionSystem(@Nullable CollisionSystem collisionSystem) {
        if (attachedCollisionSystem != null) {
            attachedCollisionSystem.removeCollider(this);
        }

        attachedCollisionSystem = collisionSystem;

        if (attachedCollisionSystem != null) {
            attachedCollisionSystem.addCollider(this);
        }
    }

    /**
     * @hide
     */
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

    private TransformProvider transformProvider = new TransformProvider() {
        @Override
        public Matrix getTransformationMatrix() {
            return MathKt.toMatrix(node.getWorldTransform());
        }
    };

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
