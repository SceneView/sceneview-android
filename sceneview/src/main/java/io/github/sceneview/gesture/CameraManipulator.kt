package io.github.sceneview.gesture

import com.google.android.filament.utils.Manipulator
import io.github.sceneview.math.Position

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