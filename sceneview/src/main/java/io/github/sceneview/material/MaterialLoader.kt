package io.github.sceneview.material

import android.content.Context
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.utils.useFileBufferNotNull
import io.github.sceneview.utils.useLocalFileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
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
        filamatFileLocation: String
    ): Material? = context.useFileBufferNotNull(filamatFileLocation) { buffer ->
        withContext(Dispatchers.Main) {
            createMaterial(buffer)
        }
    }

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
    suspend fun loadMaterialInstance(
        context: Context,
        filamatFileLocation: String
    ): MaterialInstance? = loadMaterial(context, filamatFileLocation)?.defaultInstance

    /**
     * ### Load a Material object outside of a coroutine scope from a local filamat file.
     *
     * @see MaterialLoader.loadMaterialInstance
     */
    fun createMaterial(
        context: Context,
        filamatFileLocation: String,
    ): Material = context.useLocalFileBuffer(filamatFileLocation) { buffer ->
        if (buffer == null) throw IOException("Unable to load the material. Check whether the material exists")

        createMaterial(buffer)
    }

    /**
     * ### Creates and returns the Material object
     *
     * The material data is a binary blob produced by libfilamat or by matc.
     *
     * @param filamatBuffer The content of the Filamat File
     * @return the newly created object
     */
    fun createMaterial(
        filamatBuffer: Buffer
    ): Material = Material.Builder()
        .payload(filamatBuffer, filamatBuffer.remaining())
        .build()
}