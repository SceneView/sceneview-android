package com.google.ar.sceneform.collision;

import static com.google.ar.sceneform.math.Vector3.add;
import static com.google.ar.sceneform.math.Vector3.subtract;

import com.google.ar.sceneform.math.MathHelper;
import com.google.ar.sceneform.math.Matrix;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.utilities.Preconditions;
import java.util.ArrayList;
import java.util.List;

/** Implementation of common intersection tests used for collision detection. */
class Intersections {
  private static final int NUM_VERTICES_PER_BOX = 8;
  private static final int NUM_TEST_AXES = 15;

  /** Determine if two spheres intersect with each other. */
  static boolean sphereSphereIntersection(Sphere sphere1, Sphere sphere2) {
    Preconditions.checkNotNull(sphere1, "Parameter \"sphere1\" was null.");
    Preconditions.checkNotNull(sphere2, "Parameter \"sphere2\" was null.");

    float combinedRadius = sphere1.getRadius() + sphere2.getRadius();
    float combinedRadiusSquared = combinedRadius * combinedRadius;
    Vector3 difference = Vector3.subtract(sphere2.getCenter(), sphere1.getCenter());
    float differenceLengthSquared = Vector3.dot(difference, difference);

    return differenceLengthSquared - combinedRadiusSquared <= 0.0f
        && differenceLengthSquared != 0.0f;
  }

  /** Determine if two boxes intersect with each other. */
  static boolean boxBoxIntersection(Box box1, Box box2) {
    Preconditions.checkNotNull(box1, "Parameter \"box1\" was null.");
    Preconditions.checkNotNull(box2, "Parameter \"box2\" was null.");

    // Get the vertices of the boxes.
    List<Vector3> box1Vertices = getVerticesFromBox(box1);
    List<Vector3> box2Vertices = getVerticesFromBox(box2);

    // Determine the test axes
    Matrix box1Rotation = box1.getRawRotationMatrix();
    Matrix box2Rotation = box2.getRawRotationMatrix();
    ArrayList<Vector3> testAxes = new ArrayList<>(NUM_TEST_AXES);
    testAxes.add(extractXAxisFromRotationMatrix(box1Rotation));
    testAxes.add(extractYAxisFromRotationMatrix(box1Rotation));
    testAxes.add(extractZAxisFromRotationMatrix(box1Rotation));
    testAxes.add(extractXAxisFromRotationMatrix(box2Rotation));
    testAxes.add(extractYAxisFromRotationMatrix(box2Rotation));
    testAxes.add(extractZAxisFromRotationMatrix(box2Rotation));

    for (int i = 0; i < 3; i++) {
      testAxes.add(Vector3.cross(testAxes.get(i), testAxes.get(0)));
      testAxes.add(Vector3.cross(testAxes.get(i), testAxes.get(1)));
      testAxes.add(Vector3.cross(testAxes.get(i), testAxes.get(2)));
    }

    // Attempt to find a separating axis.
    for (int i = 0; i < testAxes.size(); i++) {
      if (!testSeparatingAxis(box1Vertices, box2Vertices, testAxes.get(i))) {
        return false;
      }
    }

    return true;
  }

  /** Determine if a sphere and a box intersect with each other. */
  static boolean sphereBoxIntersection(Sphere sphere, Box box) {
    Preconditions.checkNotNull(sphere, "Parameter \"sphere\" was null.");
    Preconditions.checkNotNull(box, "Parameter \"box\" was null.");

    Vector3 point = closestPointOnBox(sphere.getCenter(), box);
    Vector3 sphereDiff = Vector3.subtract(point, sphere.getCenter());
    float sphereDiffLengthSquared = Vector3.dot(sphereDiff, sphereDiff);

    if (sphereDiffLengthSquared > sphere.getRadius() * sphere.getRadius()) {
      return false;
    }

    if (MathHelper.almostEqualRelativeAndAbs(sphereDiffLengthSquared, 0.0f)) {
      Vector3 boxDiff = Vector3.subtract(point, box.getCenter());
      float boxDiffLengthSquared = Vector3.dot(boxDiff, boxDiff);
      if (MathHelper.almostEqualRelativeAndAbs(boxDiffLengthSquared, 0.0f)) {
        return false;
      }
    }

    return true;
  }

  private static Vector3 closestPointOnBox(Vector3 point, Box box) {
    Vector3 result = new Vector3(box.getCenter());
    Vector3 diff = Vector3.subtract(point, box.getCenter());
    Matrix boxRotation = box.getRawRotationMatrix();
    Vector3 boxExtents = box.getExtents();

    // x-axis
    {
      Vector3 axis = extractXAxisFromRotationMatrix(boxRotation);
      float distance = Vector3.dot(diff, axis);

      if (distance > boxExtents.x) {
        distance = boxExtents.x;
      } else if (distance < -boxExtents.x) {
        distance = -boxExtents.x;
      }

      result = Vector3.add(result, axis.scaled(distance));
    }

    // y-axis
    {
      Vector3 axis = extractYAxisFromRotationMatrix(boxRotation);
      float distance = Vector3.dot(diff, axis);

      if (distance > boxExtents.y) {
        distance = boxExtents.y;
      } else if (distance < -boxExtents.y) {
        distance = -boxExtents.y;
      }

      result = Vector3.add(result, axis.scaled(distance));
    }

    // z-axis
    {
      Vector3 axis = extractZAxisFromRotationMatrix(boxRotation);
      float distance = Vector3.dot(diff, axis);

      if (distance > boxExtents.z) {
        distance = boxExtents.z;
      } else if (distance < -boxExtents.z) {
        distance = -boxExtents.z;
      }

      result = Vector3.add(result, axis.scaled(distance));
    }

    return result;
  }

  private static boolean testSeparatingAxis(
      List<Vector3> vertices1, List<Vector3> vertices2, Vector3 axis) {
    float min1 = Float.MAX_VALUE;
    float max1 = Float.MIN_VALUE;
    for (int i = 0; i < vertices1.size(); ++i) {
      float projection = Vector3.dot(axis, vertices1.get(i));
      min1 = Math.min(projection, min1);
      max1 = Math.max(projection, max1);
    }

    float min2 = Float.MAX_VALUE;
    float max2 = Float.MIN_VALUE;
    for (int i = 0; i < vertices2.size(); i++) {
      float projection = Vector3.dot(axis, vertices2.get(i));
      min2 = Math.min(projection, min2);
      max2 = Math.max(projection, max2);
    }

    return min2 <= max1 && min1 <= max2;
  }

  /** Converts a box into an array of 8 vertices that represent the corners of the box. */
  private static List<Vector3> getVerticesFromBox(Box box) {
    Preconditions.checkNotNull(box, "Parameter \"box\" was null.");

    // Get the properties of the box.
    Vector3 center = box.getCenter();
    Vector3 extents = box.getExtents();
    Matrix rotation = box.getRawRotationMatrix();

    // Get the rotation axes of the box.
    Vector3 xAxis = extractXAxisFromRotationMatrix(rotation);
    Vector3 yAxis = extractYAxisFromRotationMatrix(rotation);
    Vector3 zAxis = extractZAxisFromRotationMatrix(rotation);

    // Scale the rotation axes by the extents.
    Vector3 xScaled = xAxis.scaled(extents.x);
    Vector3 yScaled = yAxis.scaled(extents.y);
    Vector3 zScaled = zAxis.scaled(extents.z);

    // Calculate the 8 vertices of the box.
    ArrayList<Vector3> vertices = new ArrayList<>(NUM_VERTICES_PER_BOX);
    vertices.add(add(add(add(center, xScaled), yScaled), zScaled));
    vertices.add(add(add(subtract(center, xScaled), yScaled), zScaled));
    vertices.add(add(subtract(add(center, xScaled), yScaled), zScaled));
    vertices.add(subtract(add(add(center, xScaled), yScaled), zScaled));
    vertices.add(subtract(subtract(subtract(center, xScaled), yScaled), zScaled));
    vertices.add(subtract(subtract(add(center, xScaled), yScaled), zScaled));
    vertices.add(subtract(add(subtract(center, xScaled), yScaled), zScaled));
    vertices.add(add(subtract(subtract(center, xScaled), yScaled), zScaled));

    return vertices;
  }

  private static Vector3 extractXAxisFromRotationMatrix(Matrix matrix) {
    return new Vector3(matrix.data[0], matrix.data[4], matrix.data[8]);
  }

  private static Vector3 extractYAxisFromRotationMatrix(Matrix matrix) {
    return new Vector3(matrix.data[1], matrix.data[5], matrix.data[9]);
  }

  private static Vector3 extractZAxisFromRotationMatrix(Matrix matrix) {
    return new Vector3(matrix.data[2], matrix.data[6], matrix.data[10]);
  }
}
