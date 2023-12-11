package io.github.sceneview.animation

import androidx.compose.animation.core.AnimationVector3D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.TwoWayConverter
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion

object VectorConverters {

    /**
     * A type converter that converts a [Float3] to a [AnimationVector3D], and vice versa.
     */
    val Float3VectorConverter: TwoWayConverter<Float3, AnimationVector3D>
        get() = Float3ToVector

    /**
     * A type converter that converts a [Float3] to a [AnimationVector3D], and vice versa.
     */
    private val Float3ToVector: TwoWayConverter<Float3, AnimationVector3D> =
        TwoWayConverter(
            convertToVector = {
                AnimationVector3D(it.x, it.y, it.z)
            },
            convertFromVector = {
                Float3(it.v1, it.v2, it.v3)
            }
        )

    /**
     * A type converter that converts a [Quaternion] to a [AnimationVector4D], and vice versa.
     */
    val QuaternionVectorConverter: TwoWayConverter<Quaternion, AnimationVector4D>
        get() = QuaternionToVector

    /**
     * A type converter that converts a [Quaternion] to a [AnimationVector4D], and vice versa.
     */
    private val QuaternionToVector: TwoWayConverter<Quaternion, AnimationVector4D> =
        TwoWayConverter(
            convertToVector = {
                AnimationVector4D(it.x, it.y, it.z, it.w)
            },
            convertFromVector = {
                Quaternion(it.v1, it.v2, it.v3, it.v4)
            }
        )
}