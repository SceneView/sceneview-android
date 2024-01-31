package io.github.sceneview.collision;

/**
 * A Vector with 3 floats.
 */
// TODO: Evaluate consolidating internal math. Additional bugs: b/69935335
public class Vector3 {
    public float x;
    public float y;
    public float z;

    /**
     * Construct a Vector3 and assign zero to all values
     */
    public Vector3() {
        x = 0;
        y = 0;
        z = 0;
    }

    /**
     * Construct a Vector3 and assign each value
     */
    @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Construct a Vector3 and copy the values
     */
    @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
    public Vector3(Vector3 v) {
        Preconditions.checkNotNull(v, "Parameter \"v\" was null.");
        set(v);
    }

    /**
     * Copy the values from another Vector3 to this Vector3
     */
    public void set(Vector3 v) {
        Preconditions.checkNotNull(v, "Parameter \"v\" was null.");
        x = v.x;
        y = v.y;
        z = v.z;
    }

    /**
     * Set each value
     */
    public void set(float vx, float vy, float vz) {
        x = vx;
        y = vy;
        z = vz;
    }

    /**
     * Set each value to zero
     */
    void setZero() {
        set(0, 0, 0);
    }

    /**
     * Set each value to one
     */
    void setOne() {
        set(1, 1, 1);
    }

    /**
     * Forward into the screen is the negative Z direction
     */
    void setForward() {
        set(0, 0, -1);
    }

    /**
     * Back out of the screen is the positive Z direction
     */
    void setBack() {
        set(0, 0, 1);
    }

    /**
     * Up is the positive Y direction
     */
    void setUp() {
        set(0, 1, 0);
    }

    /**
     * Down is the negative Y direction
     */
    void setDown() {
        set(0, -1, 0);
    }

    /**
     * Right is the positive X direction
     */
    void setRight() {
        set(1, 0, 0);
    }

    /**
     * Left is the negative X direction
     */
    void setLeft() {
        set(-1, 0, 0);
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    @Override
    public String toString() {
        return "[x=" + x + ", y=" + y + ", z=" + z + "]";
    }

    /**
     * Scales the Vector3 to the unit length
     */
    public Vector3 normalized() {
        Vector3 result = new Vector3(this);
        float normSquared = Vector3.dot(this, this);

        if (MathHelper.almostEqualRelativeAndAbs(normSquared, 0.0f)) {
            result.setZero();
        } else if (normSquared != 1) {
            float norm = (float) (1.0 / Math.sqrt(normSquared));
            result.set(this.scaled(norm));
        }
        return result;
    }

    /**
     * Uniformly scales a Vector3
     *
     * @return a Vector3 multiplied by a scalar amount
     */
    public Vector3 scaled(float a) {
        return new Vector3(x * a, y * a, z * a);
    }

    /**
     * Negates a Vector3
     *
     * @return A Vector3 with opposite direction
     */
    public Vector3 negated() {
        return new Vector3(-x, -y, -z);
    }

    /**
     * Adds two Vector3's
     *
     * @return The combined Vector3
     */
    public static Vector3 add(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        return new Vector3(lhs.x + rhs.x, lhs.y + rhs.y, lhs.z + rhs.z);
    }

    /**
     * Subtract two Vector3
     *
     * @return The combined Vector3
     */
    public static Vector3 subtract(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        return new Vector3(lhs.x - rhs.x, lhs.y - rhs.y, lhs.z - rhs.z);
    }

    public static Vector3 multiply(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        return new Vector3(lhs.x * rhs.x, lhs.y * rhs.y, lhs.z * rhs.z);
    }

    /**
     * Get dot product of two Vector3's
     *
     * @return The scalar product of the Vector3's
     */
    public static float dot(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        return lhs.x * rhs.x + lhs.y * rhs.y + lhs.z * rhs.z;
    }

    /**
     * Get cross product of two Vector3's
     *
     * @return A Vector3 perpendicular to Vector3's
     */
    public static Vector3 cross(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        float lhsX = lhs.x;
        float lhsY = lhs.y;
        float lhsZ = lhs.z;
        float rhsX = rhs.x;
        float rhsY = rhs.y;
        float rhsZ = rhs.z;
        return new Vector3(
                lhsY * rhsZ - lhsZ * rhsY, lhsZ * rhsX - lhsX * rhsZ, lhsX * rhsY - lhsY * rhsX);
    }

    /**
     * Get a Vector3 with each value set to the element wise minimum of two Vector3's values
     */
    public static Vector3 min(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        return new Vector3(Math.min(lhs.x, rhs.x), Math.min(lhs.y, rhs.y), Math.min(lhs.z, rhs.z));
    }

    /**
     * Get a Vector3 with each value set to the element wise maximum of two Vector3's values
     */
    public static Vector3 max(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        return new Vector3(Math.max(lhs.x, rhs.x), Math.max(lhs.y, rhs.y), Math.max(lhs.z, rhs.z));
    }

    /**
     * Get the maximum value in a single Vector3
     */
    static float componentMax(Vector3 a) {
        Preconditions.checkNotNull(a, "Parameter \"a\" was null.");
        return Math.max(Math.max(a.x, a.y), a.z);
    }

    /**
     * Get the minimum value in a single Vector3
     */
    static float componentMin(Vector3 a) {
        Preconditions.checkNotNull(a, "Parameter \"a\" was null.");
        return Math.min(Math.min(a.x, a.y), a.z);
    }

    /**
     * Linearly interpolates between a and b.
     *
     * @param a the beginning value
     * @param b the ending value
     * @param t ratio between the two floats.
     * @return interpolated value between the two floats
     */
    public static Vector3 lerp(Vector3 a, Vector3 b, float t) {
        Preconditions.checkNotNull(a, "Parameter \"a\" was null.");
        Preconditions.checkNotNull(b, "Parameter \"b\" was null.");
        return new Vector3(
                MathHelper.lerp(a.x, b.x, t), MathHelper.lerp(a.y, b.y, t), MathHelper.lerp(a.z, b.z, t));
    }

    /**
     * Returns the shortest angle in degrees between two vectors. The result is never greater than 180
     * degrees.
     */
    public static float angleBetweenVectors(Vector3 a, Vector3 b) {
        float lengthA = a.length();
        float lengthB = b.length();
        float combinedLength = lengthA * lengthB;

        if (MathHelper.almostEqualRelativeAndAbs(combinedLength, 0.0f)) {
            return 0.0f;
        }

        float dot = Vector3.dot(a, b);
        float cos = dot / combinedLength;

        // Clamp due to floating point precision that could cause dot to be > combinedLength.
        // Which would cause acos to return NaN.
        cos = MathHelper.clamp(cos, -1.0f, 1.0f);
        float angleRadians = (float) Math.acos(cos);
        return (float) Math.toDegrees(angleRadians);
    }

    /**
     * Compares two Vector3's are equal if each component is equal within a tolerance.
     */
    public static boolean equals(Vector3 lhs, Vector3 rhs) {
        Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
        Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");
        boolean result = true;
        result &= MathHelper.almostEqualRelativeAndAbs(lhs.x, rhs.x);
        result &= MathHelper.almostEqualRelativeAndAbs(lhs.y, rhs.y);
        result &= MathHelper.almostEqualRelativeAndAbs(lhs.z, rhs.z);
        return result;
    }

    /**
     * Returns true if the other object is a Vector3 and each component is equal within a tolerance.
     */
    @Override
    @SuppressWarnings("override.param.invalid")
    public boolean equals(Object other) {
        if (!(other instanceof Vector3)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        return Vector3.equals(this, (Vector3) other);
    }

    /**
     * @hide
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(x);
        result = prime * result + Float.floatToIntBits(y);
        result = prime * result + Float.floatToIntBits(z);
        return result;
    }

    /**
     * Gets a Vector3 with all values set to zero
     */
    public static Vector3 zero() {
        return new Vector3();
    }

    /**
     * Gets a Vector3 with all values set to one
     */
    public static Vector3 one() {
        Vector3 result = new Vector3();
        result.setOne();
        return result;
    }

    /**
     * Gets a Vector3 set to (0, 0, -1)
     */
    public static Vector3 forward() {
        Vector3 result = new Vector3();
        result.setForward();
        return result;
    }

    /**
     * Gets a Vector3 set to (0, 0, 1)
     */
    public static Vector3 back() {
        Vector3 result = new Vector3();
        result.setBack();
        return result;
    }

    /**
     * Gets a Vector3 set to (0, 1, 0)
     */
    public static Vector3 up() {
        Vector3 result = new Vector3();
        result.setUp();
        return result;
    }

    /**
     * Gets a Vector3 set to (0, -1, 0)
     */
    public static Vector3 down() {
        Vector3 result = new Vector3();
        result.setDown();
        return result;
    }

    /**
     * Gets a Vector3 set to (1, 0, 0)
     */
    public static Vector3 right() {
        Vector3 result = new Vector3();
        result.setRight();
        return result;
    }

    /**
     * Gets a Vector3 set to (-1, 0, 0)
     */
    public static Vector3 left() {
        Vector3 result = new Vector3();
        result.setLeft();
        return result;
    }
}
