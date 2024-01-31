package io.github.sceneview.managers

import com.google.android.filament.TransformManager
import io.github.sceneview.Entity
import io.github.sceneview.EntityInstance

/**
 * Gets a list of children for a transform component
 *
 * @return Array of retrieved children [Entity]
 *
 * @see TransformManager.getChildren
 */
fun TransformManager.getChildren(i: EntityInstance): List<Entity> =
    getChildren(i, null).toList()

/**
 * Gets a flat list of all children within the hierarchy for a transform component
 *
 * @return Array of retrieved children [Entity]
 *
 * @see getChildren
 */
fun TransformManager.getAllChildren(i: EntityInstance): List<Entity> {
    val children = getChildren(i)
    return children + children.flatMap { getChildren(getInstance(it)) }
}