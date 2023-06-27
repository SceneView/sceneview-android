package io.github.sceneview.model

import android.content.Context
import io.github.sceneview.Filament.assetLoader
import io.github.sceneview.Filament.resourceLoader
import io.github.sceneview.utils.useFileBufferNotNull
import io.github.sceneview.utils.useLocalFileBufferNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

object GLBLoader {

    /**
     * ### Utility for loading a glTF 3D model from a binary .glb file
     *
     * @param glbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     */
    suspend fun loadModel(
        context: Context,
        glbFileLocation: String
    ): Model? = context.useFileBufferNotNull(glbFileLocation) { buffer ->
        withContext(Dispatchers.Main) {
            createModel(buffer)
        }
    }

    /**
     * ### Utility for loading a glTF 3D model from a binary .glb file
     *
     * @param glbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     */
    fun loadModelSync(context: Context, glbFileLocation: String): Model? =
        context.useLocalFileBufferNotNull(glbFileLocation) { buffer ->
            createModel(buffer)
        }

    suspend fun loadModelInstance(
        context: Context,
        glbFileLocation: String
    ) = loadModel(context, glbFileLocation)?.instance

    fun loadModelInstanceSync(
        context: Context,
        glbFileLocation: String
    ) = loadModelSync(context, glbFileLocation)?.instance

    /**
     * Consumes the contents of a glTF 2.0 file and produces a primary asset with one or more
     * instances.
     *
     * The given instance array must be sized to the desired number of instances. If successful,
     * this method will populate the array with secondary instances whose resources are shared with
     * the primary asset.
     */
    suspend fun loadInstancedModel(
        context: Context,
        glbFileLocation: String,
        count: Int
    ): Pair<Model, Array<ModelInstance?>>? =
        context.useFileBufferNotNull(glbFileLocation) { buffer ->
            withContext(Dispatchers.Main) {
                createInstancedModel(buffer, count)
            }
        }

    /**
     * Consumes the contents of a glTF 2.0 file and produces a primary asset with one or more
     * instances.
     *
     * The given instance array must be sized to the desired number of instances. If successful,
     * this method will populate the array with secondary instances whose resources are shared with
     * the primary asset.
     */
    fun loadInstancedModelSync(
        context: Context,
        glbFileLocation: String,
        count: Int
    ): Pair<Model, Array<ModelInstance?>>? =
        context.useLocalFileBufferNotNull(glbFileLocation) { buffer ->
            createInstancedModel(buffer, count)
        }

    fun createModel(buffer: Buffer): Model? {
        return assetLoader?.createAsset(buffer)?.also { asset ->

            resourceLoader.loadResources(asset)

            asset.instance.renderables.forEach {
//                it.setScreenSpaceContactShadows(false)
//                it.setCulling(true)
            }
        }
    }

    private fun createInstancedModel(
        buffer: Buffer,
        count: Int
    ): Pair<Model, Array<ModelInstance?>>? {
        val instances = arrayOfNulls<ModelInstance>(count)
        return assetLoader?.createInstancedAsset(buffer, instances)?.let { asset ->
            resourceLoader.loadResources(asset)
            runCatching { asset.releaseSourceData() }

            instances.flatMap { it?.renderables ?: listOf() }.forEach {
//                it.setScreenSpaceContactShadows(false)
//                it.setCulling(true)
            }
            Pair(asset, instances)
        }
    }
}