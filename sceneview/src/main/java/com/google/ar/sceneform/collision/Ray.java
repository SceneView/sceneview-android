package com.google.ar.sceneform.collision;

import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.Preconditions;

/** Mathematical representation of a ray. Used to perform intersection and collision tests. */
public class Ray {
  private Vector3 origin = new Vector3();
  private Vector3 direction = Vector3.forward();

  /** Create a ray with an origin of (0,0,0) and a direction of Vector3.forward(). */
  public Ray() {}

  /**
   * Create a ray with a specified origin and direction. The direction will automatically be
   * normalized.
   *
   * @param origin the ray's origin
   * @param direction the ray's direction
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Ray(Vector3 origin, Vector3 direction) {
    Preconditions.checkNotNull(origin, "Parameter \"origin\" was null.");
    Preconditions.checkNotNull(direction, "Parameter \"direction\" was null.");

    setOrigin(origin);
    setDirection(direction);
  }

  /**
   * Set the origin of the ray in world coordinates.
   *
   * @param origin the new origin of the ray.
   */
  public void setOrigin(Vector3 origin) {
    Preconditions.checkNotNull(origin, "Parameter \"origin\" was null.");
    this.origin.set(origin);
  }

  /**
   * Get the origin of the ray.
   *
   * @return a new vector that represents the ray's origin
   */
  public Vector3 getOrigin() {
    return new Vector3(origin);
  }

  /**
   * Set the direction of the ray. The direction will automatically be normalized.
   *
   * @param direction the new direction of the ray
   */
  public void setDirection(Vector3 direction) {
    Preconditions.checkNotNull(direction, "Parameter \"direction\" was null.");

    this.direction.set(direction.normalized());
  }

  /**
   * Get the direction of the ray.
   *
   * @return a new vector that represents the ray's direction
   */
  public Vector3 getDirection() {
    return new Vector3(direction);
  }

  /**
   * Get a point at a distance along the ray.
   *
   * @param distance distance along the ray of the point
   * @return a new vector that represents a point at a distance along the ray.
   */
  public Vector3 getPoint(float distance) {
    return Vector3.add(origin, direction.scaled(distance));
  }

  @Override
  public String toString() {
    return "[Origin:" + origin + ", Direction:" + direction + "]";
  }
}
