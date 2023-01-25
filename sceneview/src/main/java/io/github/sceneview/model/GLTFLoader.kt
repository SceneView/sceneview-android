package io.github.sceneview.model

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament.assetLoader
import io.github.sceneview.Filament.resourceLoader
import io.github.sceneview.renderable.setScreenSpaceContactShadows
import io.github.sceneview.utils.fileBuffer
import io.github.sceneview.utils.useFileBufferNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.Buffer

object GLTFLoader {

    /**
     * ### Utility for loading a glTF 3D model
     *
     * @param gltfFileLocation the gltf file location:
     * - A relative asset file location *models/mymodel.gltf*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.gltf*
     */
    suspend fun loadModel(
        context: Context,
        gltfFileLocation: String,
        resourceLocationResolver: (String) -> String = { resourceFileName: String ->
            "${gltfFileLocation.substringBeforeLast("/")}/$resourceFileName"
        },
        lifecycle: Lifecycle? = null
    ): Model? = context.useFileBufferNotNull(gltfFileLocation) { buffer ->
        withContext(Dispatchers.Main) {
            val buffers = mutableListOf<Buffer>()
            createModel(buffer, { resourceFileName ->
                context.fileBuffer(resourceLocationResolver(resourceFileName))!!.also {
                    buffers += it
                }
            }, lifecycle).also {
                buffers.forEach { it.clear() }
            }
        }
    }

    /**
     * ### Utility for loading a glTF 3D model
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [GLTFLoader.loadModel]
     *
     * [Documentation][GLTFLoader.loadEnvironment]
     *
     */
    fun loadModelAsync(
        context: Context,
        lifecycle: Lifecycle,
        gltfFileLocation: String,
        resourceLocationResolver: (String) -> String = { resourceFileName: String ->
            "${gltfFileLocation.substringBeforeLast("/")}/$resourceFileName"
        },
        result: (Model?) -> Unit
    ) = lifecycle.coroutineScope.launchWhenCreated {
        result(loadModel(context, gltfFileLocation, resourceLocationResolver, lifecycle))
    }

    /**
     * @param callback The given callback is triggered for each requested resource.
     */
    suspend fun createModel(
        buffer: Buffer,
        resourceBufferResolver: suspend (String) -> Buffer,
        lifecycle: Lifecycle? = null
    ): Model? = withContext(Dispatchers.Main) {
        assetLoader.createAsset(buffer)?.also { asset ->
            for (uri in asset.resourceUris) {
                val resourceBuffer = withContext(Dispatchers.IO) {
                    resourceBufferResolver(uri)
                }
                resourceLoader.addResourceData(uri, resourceBuffer)
            }
            resourceLoader.loadResources(asset)
            runCatching { asset.releaseSourceData() }

            //TODO: Used by Filament ModelViewer, see if it's usefull
            asset.renderableEntities.forEach {
                it.setScreenSpaceContactShadows(true)
            }

            lifecycle?.observe(onDestroy = {
                // Prevent double destroy in case of manually destroyed
                runCatching { asset.destroy() }
            })
        }
    }
}