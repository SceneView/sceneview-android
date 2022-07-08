package com.google.ar.sceneform.collision;

import androidx.annotation.Nullable;

import com.google.ar.sceneform.common.TransformProvider;
import com.google.ar.sceneform.utilities.Preconditions;
import io.github.sceneview.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages all of the colliders within a scene.
 */
public class CollisionSystem {
    private static final String TAG = CollisionSystem.class.getSimpleName();

    // TODO: Store things in some spatial partition or another.
    private final ArrayList<Collider> colliders = new ArrayList<>();

    public void addCollider(Collider collider) {
        Preconditions.checkNotNull(collider, "Parameter \"collider\" was null.");
        colliders.add(collider);
    }

    public void removeCollider(Collider collider) {
        Preconditions.checkNotNull(collider, "Parameter \"collider\" was null.");
        colliders.remove(collider);
    }

    @Nullable
    public Collider raycast(Ray ray, RayHit resultHit, boolean selectableOnly) {
        Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.");
        Preconditions.checkNotNull(resultHit, "Parameter \"resultHit\" was null.");

        resultHit.reset();
        Collider result = null;
        RayHit tempResult = new RayHit();
        for (Collider collider : colliders) {
            CollisionShape collisionShape = collider.getTransformedShape();
            if (collisionShape == null) {
                continue;
            }
            if (collisionShape.rayIntersection(ray, tempResult)) {
                TransformProvider transformProvider = collider.getTransformProvider();
                if(!selectableOnly
                        || !(transformProvider instanceof Node)
                        || ((Node) transformProvider).isSelectable()) {
                    if (tempResult.getDistance() < resultHit.getDistance()) {
                        resultHit.set(tempResult);
                        result = collider;
                    }
                }
            }
        }

        return result;
    }

    @SuppressWarnings("AndroidApiChecker")
    public <T extends RayHit> int raycastAll(
            Ray ray,
            ArrayList<T> resultBuffer,
            @Nullable BiConsumer<T, Collider> processResult,
            Supplier<T> allocateResult) {
        Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.");
        Preconditions.checkNotNull(resultBuffer, "Parameter \"resultBuffer\" was null.");
        Preconditions.checkNotNull(allocateResult, "Parameter \"allocateResult\" was null.");

        RayHit tempResult = new RayHit();
        int hitCount = 0;

        // Check the ray against all the colliders.
        for (Collider collider : colliders) {
            CollisionShape collisionShape = collider.getTransformedShape();
            if (collisionShape == null) {
                continue;
            }

            if (collisionShape.rayIntersection(ray, tempResult)) {
                hitCount++;
                T result = null;
                if (resultBuffer.size() >= hitCount) {
                    result = resultBuffer.get(hitCount - 1);
                } else {
                    result = allocateResult.get();
                    resultBuffer.add(result);
                }

                result.reset();
                result.set(tempResult);

                if (processResult != null) {
                    processResult.accept(result, collider);
                }
            }
        }

        // Reset extra hits in the buffer.
        for (int i = hitCount; i < resultBuffer.size(); i++) {
            resultBuffer.get(i).reset();
        }

        // Sort the hits by distance.
        Collections.sort(resultBuffer, (a, b) -> Float.compare(a.getDistance(), b.getDistance()));

        return hitCount;
    }

    @Nullable
    public Collider intersects(Collider collider) {
        Preconditions.checkNotNull(collider, "Parameter \"collider\" was null.");

        CollisionShape collisionShape = collider.getTransformedShape();
        if (collisionShape == null) {
            return null;
        }

        for (Collider otherCollider : colliders) {
            if (otherCollider == collider) {
                continue;
            }

            CollisionShape otherCollisionShape = otherCollider.getTransformedShape();
            if (otherCollisionShape == null) {
                continue;
            }

            if (collisionShape.shapeIntersection(otherCollisionShape)) {
                return otherCollider;
            }
        }

        return null;
    }

    @SuppressWarnings("AndroidApiChecker")
    public void intersectsAll(Collider collider, Consumer<Collider> processResult) {
        Preconditions.checkNotNull(collider, "Parameter \"collider\" was null.");
        Preconditions.checkNotNull(processResult, "Parameter \"processResult\" was null.");

        CollisionShape collisionShape = collider.getTransformedShape();
        if (collisionShape == null) {
            return;
        }

        for (Collider otherCollider : colliders) {
            if (otherCollider == collider) {
                continue;
            }

            CollisionShape otherCollisionShape = otherCollider.getTransformedShape();
            if (otherCollisionShape == null) {
                continue;
            }

            if (collisionShape.shapeIntersection(otherCollisionShape)) {
                processResult.accept(otherCollider);
            }
        }
    }
}
