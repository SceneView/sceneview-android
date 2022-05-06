package io.github.sceneview.material

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.utils.useFileBufferNotNull
import io.github.sceneview.utils.useLocalFileBufferNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

object MaterialLoader {

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
        lifecycle: Lifecycle,
        filamatFileLocation: String
    ): MaterialInstance? = context.useFileBufferNotNull(filamatFileLocation) { buffer ->
        withContext(Dispatchers.Main) {
            createMaterial(lifecycle, buffer)
        }
    }

    /**
     * ### Load a Material object from an filamat file
     *
     * The material file is a binary blob produced by libfilamat or by matc.
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [loadMaterial]
     *
     * [See][loadMaterial]
     */
    fun loadMaterialAsync(
        context: Context,
        lifecycle: Lifecycle,
        filamatFileLocation: String,
        result: (MaterialInstance?) -> Unit
    ) = lifecycle.coroutineScope.launchWhenCreated {
        result(loadMaterial(context, lifecycle, filamatFileLocation))
    }

    /**
     * ### Load a Material object from an filamat file locally
     *
     * @see MaterialLoader.loadMaterial
     */
    fun loadMaterialSync(
        context: Context,
        lifecycle: Lifecycle,
        filamatFileLocation: String
    ): MaterialInstance? = context.useLocalFileBufferNotNull(filamatFileLocation) { buffer ->
        createMaterial(lifecycle, buffer)
    }

    /**
     * ### Creates and returns the Material object
     *
     * The material data is a binary blob produced by libfilamat or by matc.
     *
     * @param filamatBuffer The content of the Filamat File
     * @return the newly created object
     */
    fun createMaterial(lifecycle: Lifecycle, filamatBuffer: Buffer): MaterialInstance {
        return Material.Builder()
            .payload(filamatBuffer, filamatBuffer.remaining())
            .build(lifecycle)
            .defaultInstance
    }
}