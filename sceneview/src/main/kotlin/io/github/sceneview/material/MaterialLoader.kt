package io.github.sceneview.material

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.Filament
import io.github.sceneview.utils.fileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

object MaterialLoader {

    var cache = mutableMapOf<String, Material>()

    /**
     * ### Load a Material object from an filamat file
     *
     * The material file is a binary blob produced by libfilamat or by matc.
     *
     * @param filamatFileLocation the filamat file location.
     * - A relative file location *materials/mymaterial.filamat*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymaterial)*
     * - A File path *Uri.fromFile(myMaterialFile).path*
     * - An http or https url *https://mydomain.com/mymaterial.filamat*
     */
    suspend fun loadMaterial(
        context: Context,
        filamatFileLocation: String
    ): MaterialInstance? {
        return try {
            cache[filamatFileLocation]?.createInstance()
                ?: context.fileBuffer(filamatFileLocation)
                    ?.let { buffer ->
                        withContext(Dispatchers.Main) {
                            createMaterial(buffer).also {
                                cache += filamatFileLocation to it.material
                            }
                        }
                    }
        } finally {
            // TODO: See why the finally is called before the onDestroy()
//        material?.destroy()
        }
    }

    /**
     * ### Load a Material object from an filamat file
     *
     * The material file is a binary blob produced by libfilamat or by matc.
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [HDRLoader.loadEnvironment]
     *
     * [See][loadMaterial]
     */
    fun loadMaterialAsync(
        context: Context,
        filamatFileLocation: String,
        coroutineScope: LifecycleCoroutineScope,
        result: (MaterialInstance?) -> Unit
    ) = coroutineScope.launchWhenCreated {
        result(loadMaterial(context, filamatFileLocation))
    }

    /**
     * ### Creates and returns the Material object
     *
     * The material data is a binary blob produced by libfilamat or by matc.
     *
     * @param filamatBuffer The content of the Filamat File
     * @return the newly created object
     */
    fun createMaterial(filamatBuffer: Buffer): MaterialInstance {
        return Material.Builder().payload(filamatBuffer, filamatBuffer.remaining())
            .build(Filament.engine)
            .defaultInstance
    }

    fun clearCache() {
        cache.clear()
    }

    fun destroy() {
        cache.forEach { (_, material) -> material.destroy() }
        cache.clear()
    }
}