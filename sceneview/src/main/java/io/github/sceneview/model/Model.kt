package io.github.sceneview.model

import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.Filament
import io.github.sceneview.renderable.Renderable

typealias Model = FilamentAsset

val Model.allEntities get() = entities + lightEntities

val Model.renderableNames get() = renderableEntities.map { getName(it) }

fun Model.getRenderableByName(name: String) : Renderable? =
    getFirstEntityByName(name).takeIf { it != 0 }

fun Animator.getAnimationIndex(animationName: String) =
    (0 until animationCount).firstOrNull { getAnimationName(it) == animationName }

fun Model.destroy() {
    releaseSourceData()
    Filament.assetLoader.destroyAsset(this)
}