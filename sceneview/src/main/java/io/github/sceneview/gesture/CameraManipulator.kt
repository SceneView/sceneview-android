package io.github.sceneview.gesture

import com.google.android.filament.utils.Manipulator
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.lookAt
import io.github.sceneview.math.Position
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toFloat3

/**
 * Sets world-space position of interest, which defaults to (0,0,0).
 *
 * @return this `Builder` object for chaining calls
 */
fun Manipulator.Builder.targetPosition(position: Position) =
    targetPosition(position.x, position.y, position.z)

/**
 * Sets initial eye position in world space for ORBIT mode.
 * This defaults to (0,0,1).
 *
 * @return this <code>Builder</code> object for chaining calls
 */
fun Manipulator.Builder.orbitHomePosition(position: Position) =
    orbitHomePosition(position.x, position.y, position.z)

val Manipulator.transform: Transform
    get() = Array(3) { FloatArray(3) }.apply {
        getLookAt(this[0], this[1], this[2])
    }.let { (eye, target, _) ->
        return lookAt(eye = eye.toFloat3(), target = target.toFloat3(), up = Float3(y = 1.0f))
    }