package io.github.sceneview.managers

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.Entity
import io.github.sceneview.EntityInstance
import io.github.sceneview.SceneView
import io.github.sceneview.math.*


/**
 * The initial position of the light in world space
 *
 * @see LightManager.setPosition
 */
fun LightManager.Builder.position(position: Position) = apply {
    position(position.x, position.y, position.z)
}

/**
 * The initial direction of a light in world space
 *
 * @see LightManager.setDirection
 */
fun LightManager.Builder.direction(direction: Direction) = apply {
    direction(direction.x, direction.y, direction.z)
}

fun LightManager.Builder.build(engine: Engine, entity: Entity = EntityManager.get().create()) =
    build(engine, entity)

fun LightManager.Builder.build(
    sceneView: SceneView,
    entity: Entity = EntityManager.get().create()
) = build(sceneView.engine, entity).also {
    sceneView.lights += entity
}

/**
 * The light's position in world space
 *
 * @see LightManager.getPosition
 */
fun LightManager.getPosition(instance: EntityInstance): Position = FloatArray(3).apply {
    getPosition(instance, this)
}.toPosition()

/**
 * The light's position in world space
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

fun SceneView.destroyLight(entity: Entity) {
    lightManager.destroy(entity)
    lights -= entity
}