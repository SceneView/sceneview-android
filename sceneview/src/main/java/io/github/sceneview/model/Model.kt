package io.github.sceneview.model

import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.Filament

typealias Model = FilamentAsset

val Model.allEntities get() = entities + lightEntities

val Model.animationCount: Int get() = animator.animationCount
val Model.animationName: String get() = animationName(0)
val Model.animationDuration: Float get() = animationDuration(0)

fun Model.animationName(animationIndex: Int): String =
    animator.getAnimationName(animationIndex)

fun Model.animationDuration(animationIndex: Int): Float =
    animator.getAnimationDuration(animationIndex)

fun Model.applyAnimation(animationIndex: Int, time: Float) =
    animator.applyAnimation(animationIndex, time)


fun Model.destroy() {
    releaseSourceData()
    Filament.assetLoader.destroyAsset(this)
}