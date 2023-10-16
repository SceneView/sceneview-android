package io.github.sceneview.collision;

import android.util.Log;

/**
 * Mathematical representation of a sphere. Used to perform intersection and collision tests against
 * spheres.
 */
public class Sphere extends CollisionShape {
  private static final String TAG = Sphere.class.getSimpleName();

  private final Vector3 center = new Vector3();
  private float radius = 1.0f;

  /** Create a sphere with a center of (0,0,0) and a radius of 1. */
  public Sphere() {}

  /**
   * Create a sphere with a center of (0,0,0) and a specified radius.
   *
   * @param radius the radius of the sphere
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Sphere(float radius) {
    this(radius, Vector3.zero());
  }

  /**
   * Create a sphere with a specified center and radius.
   *
   * @param radius the radius of the sphere
   * @param center the center of the sphere
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Sphere(float radius, Vector3 center) {
    Preconditions.checkNotNull(center, "Parameter \"center\" was null.");

    setCenter(center);
    setRadius(radius);
  }

  /**
   * Set the center of this sphere.
   *
   * @see #getCenter()
   * @param center the new center of the sphere
   */
  public void setCenter(Vector3 center) {
    Preconditions.checkNotNull(center, "Parameter \"center\" was null.");
    this.center.set(center);
    onChanged();
  }

  /**
   * Get a copy of the sphere's center.
   *
   * @see #setCenter(Vector3)
   * @return a new vector that represents the sphere's center
   */
  public Vector3 getCenter() {
    return new Vector3(center);
  }

  /**
   * Set the radius of the sphere.
   *
   * @see #getRadius()
   * @param radius the new radius of the sphere
   */
  public void setRadius(float radius) {
    this.radius = radius;
    onChanged();
  }

  /**
   * Get the radius of the sphere.
   *
   * @see #setRadius(float)
   * @return the radius of the sphere
   */
  public float getRadius() {
    return radius;
  }

  @Override
  public Sphere makeCopy() {
    return new Sphere(getRadius(), getCenter());
  }

  /** @hide */
  @Override
  public boolean rayIntersection(Ray ray, RayHit result) {
    Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.");
    Preconditions.checkNotNull(result, "Parameter \"result\" was null.");

    Vector3 rayDirection = ray.getDirection();
    Vector3 rayOrigin = ray.getOrigin();

    Vector3 difference = Vector3.subtract(rayOrigin, center);
    float b = 2.0f * Vector3.dot(difference, rayDirection);
    float c = Vector3.dot(difference, difference) - radius * radius;
    float discriminant = b * b - 4.0f * c;

    if (discriminant < 0.0f) {
      return false;
    }

    float discriminantSqrt = (float) Math.sqrt(discriminant);
    float tMinus = (-b - discriminantSqrt) / 2.0f;
    float tPlus = (-b + discriminantSqrt) / 2.0f;

    if (tMinus < 0.0f && tPlus < 0.0f) {
      return false;
    }

    if (tMinus < 0 && tPlus > 0) {
      result.setDistance(tPlus);
    } else {
      result.setDistance(tMinus);
    }

    result.setPoint(ray.getPoint(result.getDistance()));
    return true;
  }

  /** @hide */
  @Override
  public boolean shapeIntersection(CollisionShape shape) {
    Preconditions.checkNotNull(shape, "Parameter \"shape\" was null.");
    return shape.sphereIntersection(this);
  }

  /** @hide */
  @Override
  protected boolean sphereIntersection(Sphere sphere) {
    return Intersections.sphereSphereIntersection(this, sphere);
  }

  /** @hide */
  @Override
  protected boolean boxIntersection(Box box) {
    return Intersections.sphereBoxIntersection(this, box);
  }

  @Override
  CollisionShape transform(TransformProvider transformProvider) {
    Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.");

    Sphere result = new Sphere();
    transform(transformProvider, result);
    return result;
  }

  @Override
  void transform(TransformProvider transformProvider, CollisionShape result) {
    Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.");
    Preconditions.checkNotNull(result, "Parameter \"result\" was null.");

    if (!(result instanceof Sphere)) {
      Log.w(TAG, "Cannot pass CollisionShape of a type other than Sphere into Sphere.transform.");
      return;
    }

    Sphere resultSphere = (Sphere) result;

    Matrix modelMatrix = transformProvider.getTransformationMatrix();

    // Transform the center of the sphere.
    resultSphere.setCenter(modelMatrix.transformPoint(center));

    // Transform the radius of the sphere.
    Vector3 worldScale = new Vector3();
    modelMatrix.decomposeScale(worldScale);
    // Find the max component scale, ignoring sign.
    float maxScale =
        Math.max(
            Math.abs(Math.min(Math.min(worldScale.x, worldScale.y), worldScale.z)),
            Math.max(Math.max(worldScale.x, worldScale.y), worldScale.z));
    resultSphere.radius = radius * maxScale;
  }
}
