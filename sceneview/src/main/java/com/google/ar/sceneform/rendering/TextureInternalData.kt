package com.google.ar.sceneform.rendering

import com.google.android.filament.Engine
import com.google.android.filament.proguard.UsedByNative
import com.google.ar.sceneform.utilities.AndroidPreconditions

/**
 * Represents shared data used by [Texture]s for rendering. The data will be released when all
 * [Texture]s using this data are finalized.
 *
 * @hide Only for use for private features such as occlusion.
 */
@UsedByNative("material_java_wrappers.h")
class TextureInternalData @UsedByNative("material_java_wrappers.h") constructor(
    filamentTexture: com.google.android.filament.Texture,
    private val sampler: Texture.Sampler
) {
    private var filamentTexture: com.google.android.filament.Texture? = filamentTexture

    fun getFilamentTexture(): com.google.android.filament.Texture {
        return filamentTexture
            ?: throw IllegalStateException("Filament Texture is null.")
    }

    fun getSampler(): Texture.Sampler = sampler

    fun destroy(engine: Engine) {
        AndroidPreconditions.checkUiThread()

        filamentTexture?.let { engine.destroyTexture(it) }
        filamentTexture = null
    }
}
