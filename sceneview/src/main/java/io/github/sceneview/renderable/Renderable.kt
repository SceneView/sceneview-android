package io.github.sceneview.renderable

import androidx.annotation.IntRange
import androidx.lifecycle.Lifecycle
import com.google.android.filament.EntityInstance
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament
import io.github.sceneview.light.destroy

typealias Renderable = Int
typealias RenderableInstance = Int

const val RENDER_PRIORITY_DEFAULT = 4
const val RENDER_PRIORITY_FIRST = 0
const val RENDER_PRIORITY_LAST = 7

/**
 * @see RenderableManager.getInstance
 */
val Renderable.instance: RenderableInstance
    @EntityInstance get() = Filament.renderableManager.getInstance(this)

fun RenderableManager.Builder.build(lifecycle: Lifecycle): Renderable =
    Filament.entityManager.create().apply {
        build(Filament.engine, this)
    }.also { renderable ->
        lifecycle.observe(onDestroy = {
            // Prevent double destroy in case of manually destroyed
            runCatching { renderable.destroy() }
        })
    }

/**
 * @see RenderableManager.setPriority
 */
fun Renderable.setPriority(@IntRange(from = 0, to = 7) priority: Int) =
    Filament.renderableManager.setPriority(instance, priority)

/**
 * @see RenderableManager.getMaterialInstanceAt
 */
fun Renderable.getMaterial() = getMaterialAt(0)

/**
 * @see RenderableManager.getMaterialInstanceAt
 */
fun Renderable.getMaterialAt(@IntRange(from = 0) primitiveIndex: Int) =
    Filament.renderableManager.getMaterialInstanceAt(instance, primitiveIndex)

/**
 * @see RenderableManager.setMaterialInstanceAt
 */
fun Renderable.setMaterial(material: MaterialInstance) = setMaterialAt(0, material)

/**
 * @see RenderableManager.setMaterialInstanceAt
 */
fun Renderable.setMaterialAt(
    @IntRange(from = 0) primitiveIndex: Int,
    material: MaterialInstance
) = Filament.renderableManager.setMaterialInstanceAt(instance, primitiveIndex, material)

/**
 * @see RenderableManager.setScreenSpaceContactShadows
 */
fun Renderable.setScreenSpaceContactShadows(enabled: Boolean) =
    Filament.renderableManager.setScreenSpaceContactShadows(instance, enabled)
