package io.github.sceneview.model

import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.Entity
import io.github.sceneview.math.toVector3Box

typealias Model = FilamentAsset

val Model.renderableNames get() = renderableEntities.map { getName(it) }

val Model.collisionShape get() = boundingBox.toVector3Box()

fun Model.getRenderableByName(name: String): Entity? =
    getFirstEntityByName(name).takeIf { it != 0 }

fun Animator.getAnimationIndex(animationName: String) =
    (0 until animationCount).firstOrNull { getAnimationName(it) == animationName }