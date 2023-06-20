package io.github.sceneview.model

import com.google.android.filament.gltfio.Animator
import com.google.android.filament.gltfio.FilamentAsset
import io.github.sceneview.Filament.assetLoader
import io.github.sceneview.math.toVector3Box
import io.github.sceneview.renderable.Renderable
import io.github.sceneview.renderable.setCulling

typealias Model = FilamentAsset

fun Model.createInstance() = assetLoader?.createInstance(this)?.apply {
    renderables.forEach {
//        it.setScreenSpaceContactShadows(false)
        it.setCulling(true)
    }
}

val Model.renderableNames get() = renderableEntities.map { getName(it) }

val Model.collisionShape get() = boundingBox.let { it.toVector3Box() }

fun Model.getRenderableByName(name: String): Renderable? =
    getFirstEntityByName(name).takeIf { it != 0 }

fun Animator.getAnimationIndex(animationName: String) =
    (0 until animationCount).firstOrNull { getAnimationName(it) == animationName }

fun Model.destroy() {
    runCatching { assetLoader?.destroyAsset(this) }
}