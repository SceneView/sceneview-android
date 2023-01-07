package io.github.sceneview.managers

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import io.github.sceneview.Entity
import io.github.sceneview.SceneView
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position

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

fun SceneView.destroyLight(entity: Entity) {
    lightManager.destroy(entity)
    lights -= entity
}