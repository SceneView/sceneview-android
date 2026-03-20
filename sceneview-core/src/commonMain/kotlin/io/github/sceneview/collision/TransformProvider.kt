package io.github.sceneview.collision

/**
 * Interface for providing information about a 3D transformation.
 */
fun interface TransformProvider {
    fun getTransformationMatrix(): Matrix
}
