package io.github.sceneview.model

import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.Entity
import io.github.sceneview.math.toVector3Box

/**
 * Type alias mapping Filament's [FilamentAsset] to the SceneView domain name [Model].
 *
 * A [Model] represents a loaded glTF/GLB asset containing meshes, materials,
 * animations, lights, and cameras.
 */
typealias Model = FilamentAsset

/**
 * Returns the debug names of all renderable entities in this model.
 * Useful for finding sub-meshes by name (e.g. for selective material overrides).
 */
val Model.renderableNames get() = renderableEntities.map { getName(it) }

/**
 * Computes an axis-aligned collision shape from the model's bounding box.
 * Used by the [CollisionSystem][io.github.sceneview.collision.CollisionSystem] for hit testing.
 */
val Model.collisionShape get() = boundingBox.toVector3Box()

/**
 * Finds a renderable entity by its glTF node name.
 *
 * @param name the glTF node name to search for.
 * @return the entity ID, or `null` if no entity with that name exists.
 */
fun Model.getRenderableByName(name: String): Entity? =
    getFirstEntityByName(name).takeIf { it != 0 }

/**
 * Finds the index of an animation by its name.
 *
 * @param animationName the name assigned to the animation in the glTF file.
 * @return the zero-based animation index, or `null` if no animation matches.
 */
fun Animator.getAnimationIndex(animationName: String) =
    (0 until animationCount).firstOrNull { getAnimationName(it) == animationName }