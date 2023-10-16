package io.github.sceneview.collision;

/**
 * A Sceneform quaternion class for floats.
 *
 * <p>Quaternion operations are Hamiltonian using the right-hand-rule convention.
 */
// TODO: Evaluate combining with java/com/google/ar/core/Quaternion.java
public class Quaternion {
  private static final float SLERP_THRESHOLD = 0.9995f;
  public float x;
  public float y;
  public float z;
  public float w;

  /** Construct Quaternion and set to Identity */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Quaternion() {
    x = 0;
    y = 0;
    z = 0;
    w = 1;
  }

  /**
   * Construct Quaternion and set each value. The Quaternion will be normalized during construction
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Quaternion(float x, float y, float z, float w) {
    set(x, y, z, w);
  }

  /** Construct Quaternion using values from another Quaternion */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Quaternion(Quaternion q) {
    Preconditions.checkNotNull(q, "Parameter \"q\" was null.");
    set(q);
  }

  /**
   * Construct Quaternion using an axis/angle to define the rotation
   *
   * @param axis Sets rotation direction
   * @param angle Angle size in degrees
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Quaternion(Vector3 axis, float angle) {
    Preconditions.checkNotNull(axis, "Parameter \"axis\" was null.");
    set(Quaternion.axisAngle(axis, angle));
  }

  /**
   * Construct Quaternion based on eulerAngles.
   *
   * @see #eulerAngles(Vector3 eulerAngles)
   * @param eulerAngles - the angle in degrees for each axis.
   */
  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Quaternion(Vector3 eulerAngles) {
    Preconditions.checkNotNull(eulerAngles, "Parameter \"eulerAngles\" was null.");
    set(Quaternion.eulerAngles(eulerAngles));
  }

  /** Copy values from another Quaternion into this one */
  public void set(Quaternion q) {
    Preconditions.checkNotNull(q, "Parameter \"q\" was null.");
    x = q.x;
    y = q.y;
    z = q.z;
    w = q.w;
    normalize();
  }

  /** Update this Quaternion using an axis/angle to define the rotation */
  public void set(Vector3 axis, float angle) {
    Preconditions.checkNotNull(axis, "Parameter \"axis\" was null.");
    set(Quaternion.axisAngle(axis, angle));
  }

  /** Set each value and normalize the Quaternion */
  public void set(float qx, float qy, float qz, float qw) {
    x = qx;
    y = qy;
    z = qz;
    w = qw;
    normalize();
  }

  /** Set the Quaternion to identity */
  public void setIdentity() {
    x = 0;
    y = 0;
    z = 0;
    w = 1;
  }

  /**
   * Rescales the quaternion to the unit length.
   *
   * <p>If the Quaternion can not be scaled, it is set to identity and false is returned.
   *
   * @return true if the Quaternion was non-zero
   */
  public boolean normalize() {
    float normSquared = Quaternion.dot(this, this);
    if (MathHelper.almostEqualRelativeAndAbs(normSquared, 0.0f)) {
      setIdentity();
      return false;
    } else if (normSquared != 1) {
      float norm = (float) (1.0 / Math.sqrt(normSquared));
      x *= norm;
      y *= norm;
      z *= norm;
      w *= norm;
    } else {
      // do nothing if normSquared is already the unit length
    }
    return true;
  }

  /**
   * Get a Quaternion with a matching rotation but scaled to unit length.
   *
   * @return the quaternion scaled to the unit length, or zero if that can not be done.
   */
  public Quaternion normalized() {
    Quaternion result = new Quaternion(this);
    result.normalize();
    return result;
  }

  /**
   * Get a Quaternion with the opposite rotation
   *
   * @return the opposite rotation
   */
  public Quaternion inverted() {
    return new Quaternion(-this.x, -this.y, -this.z, this.w);
  }

  /**
   * Flips the sign of the Quaternion, but represents the same rotation.
   *
   * @return the negated Quaternion
   */
  public Quaternion negated() {
    return new Quaternion(-this.x, -this.y, -this.z, -this.w);
  }

  @Override
  public String toString() {
    return "[x=" + x + ", y=" + y + ", z=" + z + ", w=" + w + "]";
  }

  /**
   * Rotates a Vector3 by a Quaternion
   *
   * @return The rotated vector
   */
  public static Vector3 rotateVector(Quaternion q, Vector3 src) {
    Preconditions.checkNotNull(q, "Parameter \"q\" was null.");
    Preconditions.checkNotNull(src, "Parameter \"src\" was null.");
    Vector3 result = new Vector3();
    float w2 = q.w * q.w;
    float x2 = q.x * q.x;
    float y2 = q.y * q.y;
    float z2 = q.z * q.z;
    float zw = q.z * q.w;
    float xy = q.x * q.y;
    float xz = q.x * q.z;
    float yw = q.y * q.w;
    float yz = q.y * q.z;
    float xw = q.x * q.w;
    float m00 = w2 + x2 - z2 - y2;
    float m01 = xy + zw + zw + xy;
    float m02 = xz - yw + xz - yw;
    float m10 = -zw + xy - zw + xy;
    float m11 = y2 - z2 + w2 - x2;
    float m12 = yz + yz + xw + xw;
    float m20 = yw + xz + xz + yw;
    float m21 = yz + yz - xw - xw;
    float m22 = z2 - y2 - x2 + w2;
    float sx = src.x;
    float sy = src.y;
    float sz = src.z;
    result.x = m00 * sx + m10 * sy + m20 * sz;
    result.y = m01 * sx + m11 * sy + m21 * sz;
    result.z = m02 * sx + m12 * sy + m22 * sz;
    return result;
  }

  public static Vector3 inverseRotateVector(Quaternion q, Vector3 src) {
    Preconditions.checkNotNull(q, "Parameter \"q\" was null.");
    Preconditions.checkNotNull(src, "Parameter \"src\" was null.");
    Vector3 result = new Vector3();
    float w2 = q.w * q.w;
    float x2 = -q.x * -q.x;
    float y2 = -q.y * -q.y;
    float z2 = -q.z * -q.z;
    float zw = -q.z * q.w;
    float xy = -q.x * -q.y;
    float xz = -q.x * -q.z;
    float yw = -q.y * q.w;
    float yz = -q.y * -q.z;
    float xw = -q.x * q.w;
    float m00 = w2 + x2 - z2 - y2;
    float m01 = xy + zw + zw + xy;
    float m02 = xz - yw + xz - yw;
    float m10 = -zw + xy - zw + xy;
    float m11 = y2 - z2 + w2 - x2;
    float m12 = yz + yz + xw + xw;
    float m20 = yw + xz + xz + yw;
    float m21 = yz + yz - xw - xw;
    float m22 = z2 - y2 - x2 + w2;

    float sx = src.x;
    float sy = src.y;
    float sz = src.z;
    result.x = m00 * sx + m10 * sy + m20 * sz;
    result.y = m01 * sx + m11 * sy + m21 * sz;
    result.z = m02 * sx + m12 * sy + m22 * sz;
    return result;
  }

  /**
   * Create a Quaternion by combining two Quaternions multiply(lhs, rhs) is equivalent to performing
   * the rhs rotation then lhs rotation Ordering is important for this operation.
   *
   * @return The combined rotation
   */
  public static Quaternion multiply(Quaternion lhs, Quaternion rhs) {
    Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
    Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
    float lx = lhs.x;
    float ly = lhs.y;
    float lz = lhs.z;
    float lw = lhs.w;
    float rx = rhs.x;
    float ry = rhs.y;
    float rz = rhs.z;
    float rw = rhs.w;

    Quaternion result =
        new Quaternion(
            lw * rx + lx * rw + ly * rz - lz * ry,
            lw * ry - lx * rz + ly * rw + lz * rx,
            lw * rz + lx * ry - ly * rx + lz * rw,
            lw * rw - lx * rx - ly * ry - lz * rz);
    return result;
  }

  /**
   * Uniformly scales a Quaternion without normalizing
   *
   * @return a Quaternion multiplied by a scalar amount.
   */
  Quaternion scaled(float a) {
    Quaternion result = new Quaternion();
    result.x = this.x * a;
    result.y = this.y * a;
    result.z = this.z * a;
    result.w = this.w * a;

    return result;
  }

  /**
   * Adds two Quaternion's without normalizing
   *
   * @return The combined Quaternion
   */
  public static Quaternion add(Quaternion lhs, Quaternion rhs) {
    Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
    Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
    Quaternion result = new Quaternion();
    result.x = lhs.x + rhs.x;
    result.y = lhs.y + rhs.y;
    result.z = lhs.z + rhs.z;
    result.w = lhs.w + rhs.w;
    return result;
  }

  /** The dot product of two Quaternions. */
  static float dot(Quaternion lhs, Quaternion rhs) {
    Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
    Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
    return lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z + lhs.w * rhs.w;
  }

  /**
   * Returns the linear interpolation between two given rotations by a ratio. The ratio is clamped
   * between a range of 0 and 1.
   */
  static Quaternion lerp(Quaternion a, Quaternion b, float ratio) {
    Preconditions.checkNotNull(a, "Parameter \"a\" was null.");
    Preconditions.checkNotNull(b, "Parameter \"b\" was null.");
    return new Quaternion(
        MathHelper.lerp(a.x, b.x, ratio),
        MathHelper.lerp(a.y, b.y, ratio),
        MathHelper.lerp(a.z, b.z, ratio),
        MathHelper.lerp(a.w, b.w, ratio));
  }

  /*
   * Returns the spherical linear interpolation between two given orientations.
   *
   * If t is 0 this returns a.
   * As t approaches 1 {@link #slerp} may approach either b or -b (whichever is closest
   * to a)
   * If t is above 1 or below 0 the result will be extrapolated.
   * @param a the beginning value
   * @param b the ending value
   * @param t the ratio between the two floats
   * @return interpolated value between the two floats
   */
  public static Quaternion slerp(final Quaternion start, final Quaternion end, float t) {
    Preconditions.checkNotNull(start, "Parameter \"start\" was null.");
    Preconditions.checkNotNull(end, "Parameter \"end\" was null.");
    Quaternion orientation0 = start.normalized();
    Quaternion orientation1 = end.normalized();

    // cosTheta0 provides the angle between the rotations at t=0
    double cosTheta0 = Quaternion.dot(orientation0, orientation1);

    // Flip end rotation to get shortest path if needed
    if (cosTheta0 < 0.0f) {
      orientation1 = orientation1.negated();
      cosTheta0 = -cosTheta0;
    }

    // Small rotations should just use lerp
    if (cosTheta0 > SLERP_THRESHOLD) {
      return lerp(orientation0, orientation1, t);
    }

    // Cosine function range is -1,1. Clamp larger rotations.
    cosTheta0 = Math.max(-1, Math.min(1, cosTheta0));

    double theta0 = Math.acos(cosTheta0); // Angle between orientations at t=0
    double thetaT = theta0 * t; // theta0 scaled to current t

    // s0 = sin(theta0 - thetaT) / sin(theta0)
    double s0 = (Math.cos(thetaT) - cosTheta0 * Math.sin(thetaT) / Math.sin(theta0));
    double s1 = (Math.sin(thetaT) / Math.sin(theta0));
    // result = s0*start + s1*end
    Quaternion result =
        Quaternion.add(orientation0.scaled((float) s0), orientation1.scaled((float) s1));
    return result.normalized();
  }

  /**
   * Get a new Quaternion using an axis/angle to define the rotation
   *
   * @param axis Sets rotation direction
   * @param degrees Angle size in degrees
   */
  public static Quaternion axisAngle(Vector3 axis, float degrees) {
    Preconditions.checkNotNull(axis, "Parameter \"axis\" was null.");
    Quaternion dest = new Quaternion();
    double angle = Math.toRadians(degrees);
    double factor = Math.sin(angle / 2.0);

    dest.x = (float) (axis.x * factor);
    dest.y = (float) (axis.y * factor);
    dest.z = (float) (axis.z * factor);
    dest.w = (float) Math.cos(angle / 2.0);
    dest.normalize();
    return dest;
  }

  /**
   * Get a new Quaternion using eulerAngles to define the rotation.
   *
   * <p>The rotations are applied in Z, Y, X order. This is consistent with other graphics engines.
   * One thing to note is the coordinate systems are different between Sceneform and Unity, so the
   * same angles used here will have cause a different orientation than Unity. Carefully check your
   * parameter values to get the same effect as in other engines.
   *
   * @param eulerAngles - the angles in degrees.
   */
  public static Quaternion eulerAngles(Vector3 eulerAngles) {
    Preconditions.checkNotNull(eulerAngles, "Parameter \"eulerAngles\" was null.");
    Quaternion qX = new Quaternion(Vector3.right(), eulerAngles.x);
    Quaternion qY = new Quaternion(Vector3.up(), eulerAngles.y);
    Quaternion qZ = new Quaternion(Vector3.back(), eulerAngles.z);
    return Quaternion.multiply(Quaternion.multiply(qY, qX), qZ);
  }

  public Vector3 getEulerAngles() {
    double xRadians = Math.atan2(2.0f * (y * z + w * x), w * w - x * x - y * y + z * z);
    double yRadians = Math.asin(-2.0f * (x * z - w * y));
    double zRadians = Math.atan2(2.0f * (x * y + w * z), w * w + x * x - y * y - z * z);
    return new Vector3(
            (float)Math.toDegrees(xRadians),
            (float)Math.toDegrees(yRadians),
            (float)Math.toDegrees(zRadians));
  }

  /** Get a new Quaternion representing the rotation from one vector to another. */
  public static Quaternion rotationBetweenVectors(Vector3 start, Vector3 end) {
    Preconditions.checkNotNull(start, "Parameter \"start\" was null.");
    Preconditions.checkNotNull(end, "Parameter \"end\" was null.");

    start = start.normalized();
    end = end.normalized();

    float cosTheta = Vector3.dot(start, end);
    Vector3 rotationAxis;

    if (cosTheta < -1.0f + 0.001f) {
      // special case when vectors in opposite directions:
      // there is no "ideal" rotation axis
      // So guess one; any will do as long as it's perpendicular to start
      rotationAxis = Vector3.cross(Vector3.back(), start);
      if (rotationAxis.lengthSquared() < 0.01f) { // bad luck, they were parallel, try again!
        rotationAxis = Vector3.cross(Vector3.right(), start);
      }

      rotationAxis = rotationAxis.normalized();
      return axisAngle(rotationAxis, 180.0f);
    }

    rotationAxis = Vector3.cross(start, end);

    float squareLength = (float) Math.sqrt((1.0 + cosTheta) * 2.0);
    float inverseSquareLength = 1.0f / squareLength;

    return new Quaternion(
        rotationAxis.x * inverseSquareLength,
        rotationAxis.y * inverseSquareLength,
        rotationAxis.z * inverseSquareLength,
        squareLength * 0.5f);
  }

  /**
   * Get a new Quaternion representing a rotation towards a specified forward direction. If
   * upInWorld is orthogonal to forwardInWorld, then the Y axis is aligned with desiredUpInWorld.
   */
  public static Quaternion lookRotation(Vector3 forwardInWorld, Vector3 desiredUpInWorld) {
    Preconditions.checkNotNull(forwardInWorld, "Parameter \"forwardInWorld\" was null.");
    Preconditions.checkNotNull(desiredUpInWorld, "Parameter \"desiredUpInWorld\" was null.");

    // Find the rotation between the world forward and the forward to look at.
    Quaternion rotateForwardToDesiredForward =
        rotationBetweenVectors(Vector3.forward(), forwardInWorld);

    // Recompute upwards so that it's perpendicular to the direction
    Vector3 rightInWorld = Vector3.cross(forwardInWorld, desiredUpInWorld);
    desiredUpInWorld = Vector3.cross(rightInWorld, forwardInWorld);

    // Find the rotation between the "up" of the rotated object, and the desired up
    Vector3 newUp = Quaternion.rotateVector(rotateForwardToDesiredForward, Vector3.up());
    Quaternion rotateNewUpToUpwards = rotationBetweenVectors(newUp, desiredUpInWorld);

    return Quaternion.multiply(rotateNewUpToUpwards, rotateForwardToDesiredForward);
  }

  /**
   * Compare two Quaternions
   *
   * <p>Tests for equality by calculating the dot product of lhs and rhs. lhs and -lhs will not be
   * equal according to this function.
   */
  public static boolean equals(Quaternion lhs, Quaternion rhs) {
    Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
    Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
    float dot = Quaternion.dot(lhs, rhs);
    return MathHelper.almostEqualRelativeAndAbs(dot, 1.0f);
  }

  /**
   * Returns true if the other object is a Quaternion and the dot product is 1.0 +/- a tolerance.
   */
  @Override
  @SuppressWarnings("override.param.invalid")
  public boolean equals(Object other) {
    if (!(other instanceof Quaternion)) {
      return false;
    }
    if (this == other) {
      return true;
    }
    return Quaternion.equals(this, (Quaternion) other);
  }

  /** @hide */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Float.floatToIntBits(w);
    result = prime * result + Float.floatToIntBits(x);
    result = prime * result + Float.floatToIntBits(y);
    result = prime * result + Float.floatToIntBits(z);
    return result;
  }

  /** Get a Quaternion set to identity */
  public static Quaternion identity() {
    return new Quaternion();
  }
}
