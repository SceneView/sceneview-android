package io.github.sceneview.collision

/**
 * Interface for providing information about a 3D transformation.
 *
 * @hide
 */
fun interface TransformProvider {
    fun getTransformationMatrix(): Matrix
}
