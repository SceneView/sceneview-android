package io.github.sceneview.collision;

import dev.romainguy.kotlin.math.Float3;
import io.github.sceneview.math.MathKt;

/**
 * Stores the results of ray intersection tests against various types of CollisionShape.
 *
 * @hide
 */
public class RayHit {
    private float distance = Float.MAX_VALUE;
    private final Vector3 point = new Vector3();

    /**
     * @hide
     */
    public void setDistance(float distance) {
        this.distance = distance;
    }

    /**
     * Get the distance along the ray to the impact point on the surface of the collision shape.
     *
     * @return distance along the ray that the hit occurred at
     */
    public float getDistance() {
        return distance;
    }

    /**
     * @hide
     */
    public void setPoint(Vector3 point) {
        Preconditions.checkNotNull(point, "Parameter \"point\" was null.");
        this.point.set(point);
    }

    /**
     * Get the position in world-space where the ray hit the collision shape.
     *
     * @return a new vector that represents the position in world-space that the hit occurred at
     */
    public Vector3 getPoint() {
        return new Vector3(point);
    }

    /**
     * Get the position in world-space where the ray hit the collision shape.
     *
     * @return a new Float3 that represents the position in world-space that the hit occurred at
     */
    public Float3 getWorldPosition() {
        return MathKt.toFloat3(getPoint());
    }

    /**
     * @hide
     */
    public void set(RayHit other) {
        Preconditions.checkNotNull(other, "Parameter \"other\" was null.");

        setDistance(other.distance);
        setPoint(other.point);
    }

    /**
     * @hide
     */
    public void reset() {
        distance = Float.MAX_VALUE;
        point.set(0, 0, 0);
    }
}
