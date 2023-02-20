package io.github.sceneview.model

import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.Filament.assetLoader
import io.github.sceneview.renderable.Renderable

typealias Model = FilamentAsset

fun Model.createInstance() = assetLoader?.createInstance(this)

val Model.renderableNames get() = renderableEntities.map { getName(it) }

fun Model.getRenderableByName(name: String): Renderable? =
    getFirstEntityByName(name).takeIf { it != 0 }

fun Animator.getAnimationIndex(animationName: String) =
    (0 until animationCount).firstOrNull { getAnimationName(it) == animationName }

fun Model.destroy() {
    runCatching { assetLoader?.destroyAsset(this) }
}