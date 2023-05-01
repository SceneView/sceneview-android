package io.github.sceneview.model

import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.sceneform.math.Vector3
import io.github.sceneview.Filament.assetLoader
import io.github.sceneview.renderable.Renderable

typealias Model = FilamentAsset

fun Model.createInstance() = assetLoader?.createInstance(this)

val Model.renderableNames get() = renderableEntities.map { getName(it) }

val Model.collisionShape
    get() = boundingBox.let { boundingBox ->
        val halfExtent = boundingBox.halfExtent
        val center = boundingBox.center
        com.google.ar.sceneform.collision.Box(
            Vector3(halfExtent[0], halfExtent[1], halfExtent[2]).scaled(2.0f),
            Vector3(center[0], center[1], center[2])
        )
    }

fun Model.getRenderableByName(name: String): Renderable? =
    getFirstEntityByName(name).takeIf { it != 0 }

fun Animator.getAnimationIndex(animationName: String) =
    (0 until animationCount).firstOrNull { getAnimationName(it) == animationName }

fun Model.destroy() {
    runCatching { assetLoader?.destroyAsset(this) }
}