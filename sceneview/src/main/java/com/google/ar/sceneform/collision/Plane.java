package com.google.ar.sceneform.collision;


import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * Mathematical representation of a plane with an infinite size. Used for intersection tests.
 *
 * @hide
 */
public class Plane {
  private final Vector3 center = new Vector3();
  private final Vector3 normal = new Vector3();

  private static final double NEAR_ZERO_THRESHOLD = 1e-6;

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Plane(Vector3 center, Vector3 normal) {
    setCenter(center);
    setNormal(normal);
  }

  public void setCenter(Vector3 center) {
    Preconditions.checkNotNull(center, "Parameter \"center\" was null.");

    this.center.set(center);
  }

  public Vector3 getCenter() {
    return new Vector3(center);
  }

  public void setNormal(Vector3 normal) {
    Preconditions.checkNotNull(normal, "Parameter \"normal\" was null.");
    this.normal.set(normal.normalized());
  }

  public Vector3 getNormal() {
    return new Vector3(normal);
  }

  public boolean rayIntersection(Ray ray, RayHit result) {
    Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.");
    Preconditions.checkNotNull(result, "Parameter \"result\" was null.");

    Vector3 rayDirection = ray.getDirection();
    Vector3 rayOrigin = ray.getOrigin();

    float denominator = Vector3.dot(normal, rayDirection);
    if (Math.abs(denominator) > NEAR_ZERO_THRESHOLD) {
      Vector3 delta = Vector3.subtract(center, rayOrigin);
      float distance = Vector3.dot(delta, normal) / denominator;
      if (distance >= 0) {
        result.setDistance(distance);
        result.setPoint(ray.getPoint(result.getDistance()));
        return true;
      }
    }

    return false;
  }
}
