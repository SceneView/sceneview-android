package io.github.sceneview.collision

/** Implementation of common intersection tests used for collision detection. */
internal object Intersections {
    private const val NUM_VERTICES_PER_BOX = 8
    private const val NUM_TEST_AXES = 15

    /** Determine if two spheres intersect with each other. */
    fun sphereSphereIntersection(sphere1: Sphere, sphere2: Sphere): Boolean {
        Preconditions.checkNotNull(sphere1, "Parameter \"sphere1\" was null.")
        Preconditions.checkNotNull(sphere2, "Parameter \"sphere2\" was null.")

        val combinedRadius = sphere1.getRadius() + sphere2.getRadius()
        val combinedRadiusSquared = combinedRadius * combinedRadius
        val difference = Vector3.subtract(sphere2.getCenter(), sphere1.getCenter())
        val differenceLengthSquared = Vector3.dot(difference, difference)

        return differenceLengthSquared - combinedRadiusSquared <= 0.0f
                && differenceLengthSquared != 0.0f
    }

    /** Determine if two boxes intersect with each other. */
    fun boxBoxIntersection(box1: Box, box2: Box): Boolean {
        Preconditions.checkNotNull(box1, "Parameter \"box1\" was null.")
        Preconditions.checkNotNull(box2, "Parameter \"box2\" was null.")

        // Get the vertices of the boxes.
        val box1Vertices = getVerticesFromBox(box1)
        val box2Vertices = getVerticesFromBox(box2)

        // Determine the test axes
        val box1Rotation = box1.getRawRotationMatrix()
        val box2Rotation = box2.getRawRotationMatrix()
        val testAxes = ArrayList<Vector3>(NUM_TEST_AXES)
        testAxes.add(extractXAxisFromRotationMatrix(box1Rotation))
        testAxes.add(extractYAxisFromRotationMatrix(box1Rotation))
        testAxes.add(extractZAxisFromRotationMatrix(box1Rotation))
        testAxes.add(extractXAxisFromRotationMatrix(box2Rotation))
        testAxes.add(extractYAxisFromRotationMatrix(box2Rotation))
        testAxes.add(extractZAxisFromRotationMatrix(box2Rotation))

        for (i in 0..2) {
            testAxes.add(Vector3.cross(testAxes[i], testAxes[0]))
            testAxes.add(Vector3.cross(testAxes[i], testAxes[1]))
            testAxes.add(Vector3.cross(testAxes[i], testAxes[2]))
        }

        // Attempt to find a separating axis.
        for (i in 0 until testAxes.size) {
            if (!testSeparatingAxis(box1Vertices, box2Vertices, testAxes[i])) {
                return false
            }
        }

        return true
    }

    /** Determine if a sphere and a box intersect with each other. */
    fun sphereBoxIntersection(sphere: Sphere, box: Box): Boolean {
        Preconditions.checkNotNull(sphere, "Parameter \"sphere\" was null.")
        Preconditions.checkNotNull(box, "Parameter \"box\" was null.")

        val point = closestPointOnBox(sphere.getCenter(), box)
        val sphereDiff = Vector3.subtract(point, sphere.getCenter())
        val sphereDiffLengthSquared = Vector3.dot(sphereDiff, sphereDiff)

        if (sphereDiffLengthSquared > sphere.getRadius() * sphere.getRadius()) {
            return false
        }

        if (MathHelper.almostEqualRelativeAndAbs(sphereDiffLengthSquared, 0.0f)) {
            val boxDiff = Vector3.subtract(point, box.getCenter())
            val boxDiffLengthSquared = Vector3.dot(boxDiff, boxDiff)
            if (MathHelper.almostEqualRelativeAndAbs(boxDiffLengthSquared, 0.0f)) {
                return false
            }
        }

        return true
    }

    private fun closestPointOnBox(point: Vector3, box: Box): Vector3 {
        var result = Vector3(box.getCenter())
        val diff = Vector3.subtract(point, box.getCenter())
        val boxRotation = box.getRawRotationMatrix()
        val boxExtents = box.getExtents()

        // x-axis
        run {
            val axis = extractXAxisFromRotationMatrix(boxRotation)
            var distance = Vector3.dot(diff, axis)

            if (distance > boxExtents.x) {
                distance = boxExtents.x
            } else if (distance < -boxExtents.x) {
                distance = -boxExtents.x
            }

            result = Vector3.add(result, axis.scaled(distance))
        }

        // y-axis
        run {
            val axis = extractYAxisFromRotationMatrix(boxRotation)
            var distance = Vector3.dot(diff, axis)

            if (distance > boxExtents.y) {
                distance = boxExtents.y
            } else if (distance < -boxExtents.y) {
                distance = -boxExtents.y
            }

            result = Vector3.add(result, axis.scaled(distance))
        }

        // z-axis
        run {
            val axis = extractZAxisFromRotationMatrix(boxRotation)
            var distance = Vector3.dot(diff, axis)

            if (distance > boxExtents.z) {
                distance = boxExtents.z
            } else if (distance < -boxExtents.z) {
                distance = -boxExtents.z
            }

            result = Vector3.add(result, axis.scaled(distance))
        }

        return result
    }

    private fun testSeparatingAxis(
        vertices1: List<Vector3>, vertices2: List<Vector3>, axis: Vector3
    ): Boolean {
        var min1 = Float.MAX_VALUE
        var max1 = Float.MIN_VALUE
        for (i in vertices1.indices) {
            val projection = Vector3.dot(axis, vertices1[i])
            min1 = Math.min(projection, min1)
            max1 = Math.max(projection, max1)
        }

        var min2 = Float.MAX_VALUE
        var max2 = Float.MIN_VALUE
        for (i in vertices2.indices) {
            val projection = Vector3.dot(axis, vertices2[i])
            min2 = Math.min(projection, min2)
            max2 = Math.max(projection, max2)
        }

        return min2 <= max1 && min1 <= max2
    }

    /** Converts a box into an array of 8 vertices that represent the corners of the box. */
    private fun getVerticesFromBox(box: Box): List<Vector3> {
        Preconditions.checkNotNull(box, "Parameter \"box\" was null.")

        // Get the properties of the box.
        val center = box.getCenter()
        val extents = box.getExtents()
        val rotation = box.getRawRotationMatrix()

        // Get the rotation axes of the box.
        val xAxis = extractXAxisFromRotationMatrix(rotation)
        val yAxis = extractYAxisFromRotationMatrix(rotation)
        val zAxis = extractZAxisFromRotationMatrix(rotation)

        // Scale the rotation axes by the extents.
        val xScaled = xAxis.scaled(extents.x)
        val yScaled = yAxis.scaled(extents.y)
        val zScaled = zAxis.scaled(extents.z)

        // Calculate the 8 vertices of the box.
        val vertices = ArrayList<Vector3>(NUM_VERTICES_PER_BOX)
        vertices.add(Vector3.add(Vector3.add(Vector3.add(center, xScaled), yScaled), zScaled))
        vertices.add(Vector3.add(Vector3.add(Vector3.subtract(center, xScaled), yScaled), zScaled))
        vertices.add(Vector3.add(Vector3.subtract(Vector3.add(center, xScaled), yScaled), zScaled))
        vertices.add(Vector3.subtract(Vector3.add(Vector3.add(center, xScaled), yScaled), zScaled))
        vertices.add(Vector3.subtract(Vector3.subtract(Vector3.subtract(center, xScaled), yScaled), zScaled))
        vertices.add(Vector3.subtract(Vector3.subtract(Vector3.add(center, xScaled), yScaled), zScaled))
        vertices.add(Vector3.subtract(Vector3.add(Vector3.subtract(center, xScaled), yScaled), zScaled))
        vertices.add(Vector3.add(Vector3.subtract(Vector3.subtract(center, xScaled), yScaled), zScaled))

        return vertices
    }

    private fun extractXAxisFromRotationMatrix(matrix: Matrix): Vector3 =
        Vector3(matrix.data[0], matrix.data[4], matrix.data[8])

    private fun extractYAxisFromRotationMatrix(matrix: Matrix): Vector3 =
        Vector3(matrix.data[1], matrix.data[5], matrix.data[9])

    private fun extractZAxisFromRotationMatrix(matrix: Matrix): Vector3 =
        Vector3(matrix.data[2], matrix.data[6], matrix.data[10])
}
