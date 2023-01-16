package com.google.ar.sceneform.math;

import android.util.Log;
import com.google.ar.sceneform.utilities.Preconditions;

/**
 * 4x4 Matrix representing translation, scale, and rotation. Column major, right handed [0, 4, 8,
 * 12] [1, 5, 9, 13] [2, 6, 10, 14] [3, 7, 11, 15]
 *
 * @hide
 */
// TODO: Evaluate consolidating internal math.
public class Matrix {
  private static final String TAG = Matrix.class.getSimpleName();

  public static final float[] IDENTITY_DATA =
      new float[] {
        1.0f, 0.0f, 0.0f, 0.0f,
        0.0f, 1.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 1.0f, 0.0f,
        0.0f, 0.0f, 0.0f, 1.0f
      };

  public float[] data = new float[16];

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Matrix() {
    set(IDENTITY_DATA);
  }

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public Matrix(float[] data) {
    set(data);
  }

  public void set(float[] data) {
    if (data == null || data.length != 16) {
      Log.w(TAG, "Cannot set Matrix, invalid data.");
      return;
    }

    for (int i = 0; i < data.length; i++) {
      this.data[i] = data[i];
    }
  }

  @SuppressWarnings("initialization") // Suppress @UnderInitialization warning.
  public void set(Matrix m) {
    Preconditions.checkNotNull(m, "Parameter \"m\" was null.");
    set(m.data);
  }

  public void decomposeTranslation(Vector3 destTranslation) {
    destTranslation.x = data[12];
    destTranslation.y = data[13];
    destTranslation.z = data[14];
  }

  public void decomposeScale(Vector3 destScale) {
    Vector3 temp = new Vector3(data[0], data[1], data[2]);
    destScale.x = temp.length();
    temp.set(data[4], data[5], data[6]);
    destScale.y = temp.length();
    temp.set(data[8], data[9], data[10]);
    destScale.z = temp.length();
  }

  public void decomposeRotation(Vector3 decomposedScale, Quaternion destRotation) {
    float m00 = data[0];
    float m01 = data[1];
    float m02 = data[2];
    float m03 = data[3];
    float m10 = data[4];
    float m11 = data[5];
    float m12 = data[6];
    float m13 = data[7];
    float m20 = data[8];
    float m21 = data[9];
    float m22 = data[10];
    float m23 = data[11];
    float m30 = data[12];
    float m31 = data[13];
    float m32 = data[14];
    float m33 = data[15];

    // To extract the quaternion, we first remove the scale from the matrix. This is done in-place,
    // and then after the quaternion is extracted the matrix is set back to it's original values.
    // This allows us to decompose the rotation without allocating an additional matrix to hold the
    // rotation matrix, which is better for performance.
    decomposeRotation(decomposedScale, this);
    extractQuaternion(destRotation);

    data[0] = m00;
    data[1] = m01;
    data[2] = m02;
    data[3] = m03;
    data[4] = m10;
    data[5] = m11;
    data[6] = m12;
    data[7] = m13;
    data[8] = m20;
    data[9] = m21;
    data[10] = m22;
    data[11] = m23;
    data[12] = m30;
    data[13] = m31;
    data[14] = m32;
    data[15] = m33;
  }

  public void decomposeRotation(Vector3 decomposedScale, Matrix destMatrix) {
    // Remove x scale.
    if (decomposedScale.x != 0.0f) {
      for (int i = 0; i < 3; i++) {
        destMatrix.data[i] = data[i] / decomposedScale.x;
      }
    }

    destMatrix.data[3] = 0.0f;

    // Remove y scale.
    if (decomposedScale.y != 0.0f) {
      for (int i = 4; i < 7; i++) {
        destMatrix.data[i] = data[i] / decomposedScale.y;
      }
    }

    destMatrix.data[7] = 0.0f;

    // Remove z scale.
    if (decomposedScale.z != 0.0f) {
      for (int i = 8; i < 11; i++) {
        destMatrix.data[i] = data[i] / decomposedScale.z;
      }
    }

    destMatrix.data[11] = 0.0f;
    destMatrix.data[12] = 0.0f;
    destMatrix.data[13] = 0.0f;
    destMatrix.data[14] = 0.0f;
    destMatrix.data[15] = 1.0f;
  }

  public void extractQuaternion(Quaternion destQuaternion) {
    float trace = data[0] + data[5] + data[10];

    if (trace > 0) {
      float s = (float) Math.sqrt(trace + 1.0) * 2.0f;
      destQuaternion.w = 0.25f * s;
      destQuaternion.x = (data[6] - data[9]) / s;
      destQuaternion.y = (data[8] - data[2]) / s;
      destQuaternion.z = (data[1] - data[4]) / s;
    } else if ((data[0] > data[5]) && (data[0] > data[10])) {
      float s = (float) Math.sqrt(1.0f + data[0] - data[5] - data[10]) * 2.0f;
      destQuaternion.w = (data[6] - data[9]) / s;
      destQuaternion.x = 0.25f * s;
      destQuaternion.y = (data[4] + data[1]) / s;
      destQuaternion.z = (data[8] + data[2]) / s;
    } else if (data[5] > data[10]) {
      float s = (float) Math.sqrt(1.0f + data[5] - data[0] - data[10]) * 2.0f;
      destQuaternion.w = (data[8] - data[2]) / s;
      destQuaternion.x = (data[4] + data[1]) / s;
      destQuaternion.y = 0.25f * s;
      destQuaternion.z = (data[9] + data[6]) / s;
    } else {
      float s = (float) Math.sqrt(1.0f + data[10] - data[0] - data[5]) * 2.0f;
      destQuaternion.w = (data[1] - data[4]) / s;
      destQuaternion.x = (data[8] + data[2]) / s;
      destQuaternion.y = (data[9] + data[6]) / s;
      destQuaternion.z = 0.25f * s;
    }
    destQuaternion.normalize();
  }

  public void makeTranslation(Vector3 translation) {
    Preconditions.checkNotNull(translation, "Parameter \"translation\" was null.");

    set(IDENTITY_DATA);

    setTranslation(translation);
  }

  public void setTranslation(Vector3 translation) {
    data[12] = translation.x;
    data[13] = translation.y;
    data[14] = translation.z;
  }

  public void makeRotation(Quaternion rotation) {
    Preconditions.checkNotNull(rotation, "Parameter \"rotation\" was null.");

    set(IDENTITY_DATA);

    rotation.normalize();

    float xx = rotation.x * rotation.x;
    float xy = rotation.x * rotation.y;
    float xz = rotation.x * rotation.z;
    float xw = rotation.x * rotation.w;

    float yy = rotation.y * rotation.y;
    float yz = rotation.y * rotation.z;
    float yw = rotation.y * rotation.w;

    float zz = rotation.z * rotation.z;
    float zw = rotation.z * rotation.w;

    data[0] = 1.0f - 2.0f * (yy + zz);
    data[4] = 2.0f * (xy - zw);
    data[8] = 2.0f * (xz + yw);

    data[1] = 2.0f * (xy + zw);
    data[5] = 1.0f - 2.0f * (xx + zz);
    data[9] = 2.0f * (yz - xw);

    data[2] = 2.0f * (xz - yw);
    data[6] = 2.0f * (yz + xw);
    data[10] = 1.0f - 2.0f * (xx + yy);
  }

  public void makeScale(float scale) {
    Preconditions.checkNotNull(scale, "Parameter \"scale\" was null.");

    set(IDENTITY_DATA);

    data[0] = scale;
    data[5] = scale;
    data[10] = scale;
  }

  public void makeScale(Vector3 scale) {
    Preconditions.checkNotNull(scale, "Parameter \"scale\" was null.");

    set(IDENTITY_DATA);

    data[0] = scale.x;
    data[5] = scale.y;
    data[10] = scale.z;
  }

  public void makeTrs(Vector3 translation, Quaternion rotation, Vector3 scale) {
    float mdsqx = 1 - 2 * rotation.x * rotation.x;
    float sqy = rotation.y * rotation.y;
    float dsqz = 2 * rotation.z * rotation.z;
    float dqxz = 2 * rotation.x * rotation.z;
    float dqyw = 2 * rotation.y * rotation.w;
    float dqxy = 2 * rotation.x * rotation.y;
    float dqzw = 2 * rotation.z * rotation.w;
    float dqxw = 2 * rotation.x * rotation.w;
    float dqyz = 2 * rotation.y * rotation.z;

    data[0] = (1 - 2 * sqy - dsqz) * scale.x;
    data[4] = (dqxy - dqzw) * scale.y;
    data[8] = (dqxz + dqyw) * scale.z;

    data[1] = (dqxy + dqzw) * scale.x;
    data[5] = (mdsqx - dsqz) * scale.y;
    data[9] = (dqyz - dqxw) * scale.z;

    data[2] = (dqxz - dqyw) * scale.x;
    data[6] = (dqyz + dqxw) * scale.y;
    data[10] = (mdsqx - 2 * sqy) * scale.z;

    data[12] = translation.x;
    data[13] = translation.y;
    data[14] = translation.z;
    data[15] = 1.0f;
  }

  public static void multiply(Matrix lhs, Matrix rhs, Matrix dest) {
    Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
    Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");

    float m00 = 0f;
    float m01 = 0f;
    float m02 = 0f;
    float m03 = 0f;
    float m10 = 0f;
    float m11 = 0f;
    float m12 = 0f;
    float m13 = 0f;
    float m20 = 0f;
    float m21 = 0f;
    float m22 = 0f;
    float m23 = 0f;
    float m30 = 0f;
    float m31 = 0f;
    float m32 = 0f;
    float m33 = 0f;

    for (int i = 0; i < 4; i++) {
      float lhs0 = lhs.data[0 + (i * 4)];
      float lhs1 = lhs.data[1 + (i * 4)];
      float lhs2 = lhs.data[2 + (i * 4)];
      float lhs3 = lhs.data[3 + (i * 4)];
      float rhs0 = rhs.data[(0 * 4) + i];
      float rhs1 = rhs.data[(1 * 4) + i];
      float rhs2 = rhs.data[(2 * 4) + i];
      float rhs3 = rhs.data[(3 * 4) + i];

      m00 += lhs0 * rhs0;
      m01 += lhs1 * rhs0;
      m02 += lhs2 * rhs0;
      m03 += lhs3 * rhs0;

      m10 += lhs0 * rhs1;
      m11 += lhs1 * rhs1;
      m12 += lhs2 * rhs1;
      m13 += lhs3 * rhs1;

      m20 += lhs0 * rhs2;
      m21 += lhs1 * rhs2;
      m22 += lhs2 * rhs2;
      m23 += lhs3 * rhs2;

      m30 += lhs0 * rhs3;
      m31 += lhs1 * rhs3;
      m32 += lhs2 * rhs3;
      m33 += lhs3 * rhs3;
    }

    dest.data[0] = m00;
    dest.data[1] = m01;
    dest.data[2] = m02;
    dest.data[3] = m03;
    dest.data[4] = m10;
    dest.data[5] = m11;
    dest.data[6] = m12;
    dest.data[7] = m13;
    dest.data[8] = m20;
    dest.data[9] = m21;
    dest.data[10] = m22;
    dest.data[11] = m23;
    dest.data[12] = m30;
    dest.data[13] = m31;
    dest.data[14] = m32;
    dest.data[15] = m33;
  }

  public Vector3 transformPoint(Vector3 vector) {
    Preconditions.checkNotNull(vector, "Parameter \"vector\" was null.");

    Vector3 result = new Vector3();
    float vx = vector.x;
    float vy = vector.y;
    float vz = vector.z;
    result.x = data[0] * vx;
    result.x += data[4] * vy;
    result.x += data[8] * vz;
    result.x += data[12]; // *1

    result.y = data[1] * vx;
    result.y += data[5] * vy;
    result.y += data[9] * vz;
    result.y += data[13]; // *1

    result.z = data[2] * vx;
    result.z += data[6] * vy;
    result.z += data[10] * vz;
    result.z += data[14]; // *1
    return result;
  }

  /**
   * Transforms a direction by ignoring any translation.
   *
   * <p>If the matrix is uniformly (positively) scaled, then the resulting direction will be correct
   * but scaled by the same factor. If a unit direction is required then the result should be
   * normalized.
   *
   * <p>If the scale is non-uniform or negative then the result vector will be distorted. In this
   * case the matrix used should be the inverse transpose of the incoming matrix.
   */
  public Vector3 transformDirection(Vector3 vector) {
    Preconditions.checkNotNull(vector, "Parameter \"vector\" was null.");

    Vector3 result = new Vector3();
    float vx = vector.x;
    float vy = vector.y;
    float vz = vector.z;
    result.x = data[0] * vx;
    result.x += data[4] * vy;
    result.x += data[8] * vz;

    result.y = data[1] * vx;
    result.y += data[5] * vy;
    result.y += data[9] * vz;

    result.z = data[2] * vx;
    result.z += data[6] * vy;
    result.z += data[10] * vz;
    return result;
  }

  public static boolean invert(Matrix matrix, Matrix dest) {
    Preconditions.checkNotNull(matrix, "Parameter \"matrix\" was null.");
    Preconditions.checkNotNull(dest, "Parameter \"dest\" was null.");

    float m0 = matrix.data[0];
    float m1 = matrix.data[1];
    float m2 = matrix.data[2];
    float m3 = matrix.data[3];
    float m4 = matrix.data[4];
    float m5 = matrix.data[5];
    float m6 = matrix.data[6];
    float m7 = matrix.data[7];
    float m8 = matrix.data[8];
    float m9 = matrix.data[9];
    float m10 = matrix.data[10];
    float m11 = matrix.data[11];
    float m12 = matrix.data[12];
    float m13 = matrix.data[13];
    float m14 = matrix.data[14];
    float m15 = matrix.data[15];

    dest.data[0] =
        m5 * m10 * m15
            - m5 * m11 * m14
            - m9 * m6 * m15
            + m9 * m7 * m14
            + m13 * m6 * m11
            - m13 * m7 * m10;

    dest.data[4] =
        -m4 * m10 * m15
            + m4 * m11 * m14
            + m8 * m6 * m15
            - m8 * m7 * m14
            - m12 * m6 * m11
            + m12 * m7 * m10;

    dest.data[8] =
        m4 * m9 * m15
            - m4 * m11 * m13
            - m8 * m5 * m15
            + m8 * m7 * m13
            + m12 * m5 * m11
            - m12 * m7 * m9;

    dest.data[12] =
        -m4 * m9 * m14
            + m4 * m10 * m13
            + m8 * m5 * m14
            - m8 * m6 * m13
            - m12 * m5 * m10
            + m12 * m6 * m9;

    dest.data[1] =
        -m1 * m10 * m15
            + m1 * m11 * m14
            + m9 * m2 * m15
            - m9 * m3 * m14
            - m13 * m2 * m11
            + m13 * m3 * m10;

    dest.data[5] =
        m0 * m10 * m15
            - m0 * m11 * m14
            - m8 * m2 * m15
            + m8 * m3 * m14
            + m12 * m2 * m11
            - m12 * m3 * m10;

    dest.data[9] =
        -m0 * m9 * m15
            + m0 * m11 * m13
            + m8 * m1 * m15
            - m8 * m3 * m13
            - m12 * m1 * m11
            + m12 * m3 * m9;

    dest.data[13] =
        m0 * m9 * m14
            - m0 * m10 * m13
            - m8 * m1 * m14
            + m8 * m2 * m13
            + m12 * m1 * m10
            - m12 * m2 * m9;

    dest.data[2] =
        m1 * m6 * m15
            - m1 * m7 * m14
            - m5 * m2 * m15
            + m5 * m3 * m14
            + m13 * m2 * m7
            - m13 * m3 * m6;

    dest.data[6] =
        -m0 * m6 * m15
            + m0 * m7 * m14
            + m4 * m2 * m15
            - m4 * m3 * m14
            - m12 * m2 * m7
            + m12 * m3 * m6;

    dest.data[10] =
        m0 * m5 * m15
            - m0 * m7 * m13
            - m4 * m1 * m15
            + m4 * m3 * m13
            + m12 * m1 * m7
            - m12 * m3 * m5;

    dest.data[14] =
        -m0 * m5 * m14
            + m0 * m6 * m13
            + m4 * m1 * m14
            - m4 * m2 * m13
            - m12 * m1 * m6
            + m12 * m2 * m5;

    dest.data[3] =
        -m1 * m6 * m11
            + m1 * m7 * m10
            + m5 * m2 * m11
            - m5 * m3 * m10
            - m9 * m2 * m7
            + m9 * m3 * m6;

    dest.data[7] =
        m0 * m6 * m11 - m0 * m7 * m10 - m4 * m2 * m11 + m4 * m3 * m10 + m8 * m2 * m7 - m8 * m3 * m6;

    dest.data[11] =
        -m0 * m5 * m11 + m0 * m7 * m9 + m4 * m1 * m11 - m4 * m3 * m9 - m8 * m1 * m7 + m8 * m3 * m5;

    dest.data[15] =
        m0 * m5 * m10 - m0 * m6 * m9 - m4 * m1 * m10 + m4 * m2 * m9 + m8 * m1 * m6 - m8 * m2 * m5;

    float det = m0 * dest.data[0] + m1 * dest.data[4] + m2 * dest.data[8] + m3 * dest.data[12];

    if (det == 0) {
      return false;
    }

    det = 1.0f / det;

    for (int i = 0; i < 16; i++) {
      dest.data[i] *= det;
    }

    return true;
  }

  /** Compares Matrix values */
  public static boolean equals(Matrix lhs, Matrix rhs) {
    Preconditions.checkNotNull(lhs, "Parameter \"lhs\" was null.");
    Preconditions.checkNotNull(rhs, "Parameter \"rhs\" was null.");

    boolean result = true;
    for (int i = 0; i < 16; i++) {
      result &= MathHelper.almostEqualRelativeAndAbs(lhs.data[i], rhs.data[i]);
    }
    return result;
  }
}
