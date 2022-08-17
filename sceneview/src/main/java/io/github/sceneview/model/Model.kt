package io.github.sceneview.model

import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.Filament

typealias Model = FilamentAsset

val Model.allEntities get() = entities + lightEntities

fun Animator.getAnimationIndex(animationName: String) =
    (0 until animationCount).firstOrNull { getAnimationName(it) == animationName }

fun Model.destroy() {
    releaseSourceData()
    Filament.assetLoader.destroyAsset(this)
}