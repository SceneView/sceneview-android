package io.github.sceneview.managers

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.Entity
import io.github.sceneview.EntityInstance
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.lookTowards
import io.github.sceneview.math.toDirection
import io.github.sceneview.math.toPosition

/**
 * The initial position of the light in world space.
 *
 * @see LightManager.setPosition
 */
fun LightManager.Builder.position(position: Position) = apply {
    position(position.x, position.y, position.z)
}

/**
 * The initial direction of a light in world space.
 *
 * @see LightManager.setDirection
 */
fun LightManager.Builder.direction(direction: Direction) = apply {
    direction(direction.x, direction.y, direction.z)
}

/**
 * Sets the initial color of a light.
 *
 * The light color is specified in the linear sRGB color-space. The default is white.
 *
 * @param linearColor rgb components of the color (default is (1, 1, 1))
 * @return This Builder, for chaining calls.
 */
fun LightManager.Builder.color(linearColor: Color) = apply {
    color(linearColor.r, linearColor.g, linearColor.b)
}

fun LightManager.Builder.build(engine: Engine, entity: Entity = EntityManager.get().create()) =
    build(engine, entity)

/**
 * The light's position in world space.
 *
 * @see LightManager.getPosition
 */
fun LightManager.getPosition(instance: EntityInstance): Position = FloatArray(3).apply {
    getPosition(instance, this)
}.toPosition()

/**
 * The light's position in world space.
 *
 * @see LightManager.setPosition
 */
fun LightManager.setPosition(instance: EntityInstance, position: Position) {
    setPosition(instance, position.x, position.y, position.z)
}

/**
 * The light's direction in world space
 *
 * @see LightManager.getDirection
 */
fun LightManager.getDirection(instance: EntityInstance): Direction = FloatArray(3).apply {
    getDirection(instance, this)
}.toDirection()

/**
 * @see LightManager.setDirection
 */
fun LightManager.setDirection(instance: EntityInstance, direction: Direction) {
    setDirection(instance, direction.x, direction.y, direction.z)
}

/**
 * @see LightManager.getPosition
 */
fun LightManager.getQuaternion(instance: EntityInstance): Quaternion =
    lookTowards(eye = getPosition(instance), direction = getDirection(instance))

/**
 * @see LightManager.setPosition
 */
fun LightManager.setQuaternion(instance: EntityInstance, quaternion: Quaternion) {
    setDirection(instance, quaternion * Direction(y = 1.0f))
}