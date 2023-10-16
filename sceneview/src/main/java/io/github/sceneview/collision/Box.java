package io.github.sceneview.collision;

import android.util.Log;

/**
 * Mathematical representation of a box. Used to perform intersection and collision tests against
 * oriented boxes.
 */
public class Box extends CollisionShape {
  private static final String TAG = Box.class.getSimpleName();
  private final Vector3 center = Vector3.zero();
  private final Vector3 size = Vector3.one();
  private final Matrix rotationMatrix = new Matrix();

  /** Create a box with a center of (0,0,0) and a size of (1,1,1). */
  public Box() {}

  /**
   * Create a box with a center of (0,0,0) and a specified size.
   *
   * @param size the size of the box.
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Box(Vector3 size) {
    this(size, Vector3.zero());
  }

  /**
   * Create a box with a specified center and size.
   *
   * @param size the size of the box
   * @param center the center of the box
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Box(Vector3 size, Vector3 center) {
    Preconditions.checkNotNull(center, "Parameter \"center\" was null.");
    Preconditions.checkNotNull(size, "Parameter \"size\" was null.");

    setCenter(center);
    setSize(size);
  }

  /**
   * Set the center of this box.
   *
   * @see #getCenter()
   * @param center the new center of the box
   */
  public void setCenter(Vector3 center) {
    Preconditions.checkNotNull(center, "Parameter \"center\" was null.");
    this.center.set(center);
    onChanged();
  }

  /**
   * Get a copy of the box's center.
   *
   * @see #setCenter(Vector3)
   * @return a new vector that represents the box's center
   */
  public Vector3 getCenter() {
    return new Vector3(center);
  }

  /**
   * Set the size of this box.
   *
   * @see #getSize()
   * @param size the new size of the box
   */
  public void setSize(Vector3 size) {
    Preconditions.checkNotNull(size, "Parameter \"size\" was null.");
    this.size.set(size);
    onChanged();
  }

  /**
   * Get a copy of the box's size.
   *
   * @see #setSize(Vector3)
   * @return a new vector that represents the box's size
   */
  public Vector3 getSize() {
    return new Vector3(size);
  }

  /**
   * Calculate the extents (half the size) of the box.
   *
   * @return a new vector that represents the box's extents
   */
  public Vector3 getExtents() {
    return getSize().scaled(0.5f);
  }

  /**
   * Set the rotation of this box.
   *
   * @see #getRotation()
   * @param rotation the new rotation of the box
   */
  public void setRotation(Quaternion rotation) {
    Preconditions.checkNotNull(rotation, "Parameter \"rotation\" was null.");
    rotationMatrix.makeRotation(rotation);
    onChanged();
  }

  /**
   * Get a copy of the box's rotation.
   *
   * @see #setRotation(Quaternion)
   * @return a new quaternion that represents the box's rotation
   */
  public Quaternion getRotation() {
    Quaternion result = new Quaternion();
    rotationMatrix.extractQuaternion(result);
    return result;
  }

  @Override
  public Box makeCopy() {
    return new Box(getSize(), getCenter());
  }

  /**
   * Get the raw rotation matrix representing the box's orientation. Do not modify directly.
   * Instead, use setRotation.
   *
   * @return a reference to the box's raw rotation matrix
   */
  Matrix getRawRotationMatrix() {
    return rotationMatrix;
  }

  /** @hide protected method */
  @Override
  public boolean rayIntersection(Ray ray, RayHit result) {
    Preconditions.checkNotNull(ray, "Parameter \"ray\" was null.");
    Preconditions.checkNotNull(result, "Parameter \"result\" was null.");

    Vector3 rayDirection = ray.getDirection();
    Vector3 rayOrigin = ray.getOrigin();
    Vector3 max = getExtents();
    Vector3 min = max.negated();

    // tMin is the farthest "near" intersection (amongst the X,Y and Z planes pairs)
    float tMin = Float.MIN_VALUE;

    // tMax is the nearest "far" intersection (amongst the X,Y and Z planes pairs)
    float tMax = Float.MAX_VALUE;

    Vector3 delta = Vector3.subtract(center, rayOrigin);

    // Test intersection with the 2 planes perpendicular to the OBB's x axis.
    float[] axes = rotationMatrix.data;
    Vector3 axis = new Vector3(axes[0], axes[1], axes[2]);
    float e = Vector3.dot(axis, delta);
    float f = Vector3.dot(rayDirection, axis);

    if (!MathHelper.almostEqualRelativeAndAbs(f, 0.0f)) {
      float t1 = (e + min.x) / f;
      float t2 = (e + max.x) / f;

      if (t1 > t2) {
        float temp = t1;
        t1 = t2;
        t2 = temp;
      }

      tMax = Math.min(t2, tMax);
      tMin = Math.max(t1, tMin);

      if (tMax < tMin) {
        return false;
      }
    } else if (-e + min.x > 0.0f || -e + max.x < 0.0f) {
      // Ray is almost parallel to one of the planes.
      return false;
    }

    // Test intersection with the 2 planes perpendicular to the OBB's y axis.
    axis = new Vector3(axes[4], axes[5], axes[6]);
    e = Vector3.dot(axis, delta);
    f = Vector3.dot(rayDirection, axis);

    if (!MathHelper.almostEqualRelativeAndAbs(f, 0.0f)) {
      float t1 = (e + min.y) / f;
      float t2 = (e + max.y) / f;

      if (t1 > t2) {
        float temp = t1;
        t1 = t2;
        t2 = temp;
      }

      tMax = Math.min(t2, tMax);
      tMin = Math.max(t1, tMin);

      if (tMax < tMin) {
        return false;
      }
    } else if (-e + min.y > 0.0f || -e + max.y < 0.0f) {
      // Ray is almost parallel to one of the planes.
      return false;
    }

    // Test intersection with the 2 planes perpendicular to the OBB's z axis.
    axis = new Vector3(axes[8], axes[9], axes[10]);
    e = Vector3.dot(axis, delta);
    f = Vector3.dot(rayDirection, axis);

    if (!MathHelper.almostEqualRelativeAndAbs(f, 0.0f)) {
      float t1 = (e + min.z) / f;
      float t2 = (e + max.z) / f;

      if (t1 > t2) {
        float temp = t1;
        t1 = t2;
        t2 = temp;
      }

      tMax = Math.min(t2, tMax);
      tMin = Math.max(t1, tMin);

      if (tMax < tMin) {
        return false;
      }
    } else if (-e + min.z > 0.0f || -e + max.z < 0.0f) {
      // Ray is almost parallel to one of the planes.
      return false;
    }

    result.setDistance(tMin);
    result.setPoint(ray.getPoint(result.getDistance()));
    return true;
  }

  /** @hide protected method */
  @Override
  public boolean shapeIntersection(CollisionShape shape) {
    Preconditions.checkNotNull(shape, "Parameter \"shape\" was null.");
    return shape.boxIntersection(this);
  }

  /** @hide protected method */
  @Override
  protected boolean sphereIntersection(Sphere sphere) {
    return Intersections.sphereBoxIntersection(sphere, this);
  }

  /** @hide protected method */
  @Override
  protected boolean boxIntersection(Box box) {
    return Intersections.boxBoxIntersection(this, box);
  }

  @Override
  CollisionShape transform(TransformProvider transformProvider) {
    Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.");

    Box result = new Box();
    transform(transformProvider, result);
    return result;
  }

  @Override
  void transform(TransformProvider transformProvider, CollisionShape result) {
    Preconditions.checkNotNull(transformProvider, "Parameter \"transformProvider\" was null.");
    Preconditions.checkNotNull(result, "Parameter \"result\" was null.");

    if (!(result instanceof Box)) {
      Log.w(TAG, "Cannot pass CollisionShape of a type other than Box into Box.transform.");
      return;
    }

    if (result == this) {
      throw new IllegalArgumentException("Box cannot transform itself.");
    }

    Box resultBox = (Box) result;

    Matrix modelMatrix = transformProvider.getTransformationMatrix();

    // Transform the center of the box.
    resultBox.center.set(modelMatrix.transformPoint(center));

    // Transform the size of the box.
    Vector3 worldScale = new Vector3();
    modelMatrix.decomposeScale(worldScale);
    resultBox.size.x = size.x * worldScale.x;
    resultBox.size.y = size.y * worldScale.y;
    resultBox.size.z = size.z * worldScale.z;

    // Transform the rotation of the box.
    modelMatrix.decomposeRotation(worldScale, resultBox.rotationMatrix);
    Matrix.multiply(rotationMatrix, resultBox.rotationMatrix, resultBox.rotationMatrix);
  }
}
