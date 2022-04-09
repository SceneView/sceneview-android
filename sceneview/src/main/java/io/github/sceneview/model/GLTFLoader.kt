package io.github.sceneview.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.filament.gltfio.FilamentAsset
import com.google.ar.sceneform.rendering.ModelRenderable
import io.github.sceneview.Filament
import io.github.sceneview.utils.fileBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.nio.Buffer
import java.nio.ByteBuffer

object GLTFLoader {

    val assetLoader get() = Filament.assetLoader
    val resourceLoader get() = Filament.resourceLoader

    val progressiveLoad = false

    /**
     * ### Load a glTF 3D model from a glb file
     *
     * @param glbFileLocation the glb file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param progressiveLoad Asynchronous resource load.
     * True means that the model will be displayed even if its textures are not completely loaded.
     * The model textures will appear progressively while loaded.
     */
    suspend fun loadModel(
        context: Context,
        gltfFileLocation: String,
        fileResolver: (fileName: String) -> String = defaultFileResolver(gltfFileLocation),
        progressiveLoad: Boolean = GLTFLoader.progressiveLoad
    ): FilamentAsset? {
        return try {
            val contentResolver: suspend (String) -> ByteBuffer? = { fileName ->
                context.fileBuffer(fileResolver(fileName))
            }
            context.fileBuffer(gltfFileLocation)?.let { buffer ->
                withContext(Dispatchers.Main) {
                    createModel(buffer, contentResolver, progressiveLoad)
                }
            }
        } finally {
            // TODO: See why the finally is called before the onDestroy()
//        asset?.destroy()
        }
    }

    /**
     * ### Load a glTF 3D model from a glb file asynchronously
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [GLTFLoader.loadModel]
     *
     * [Documentation][GLTFLoader.loadModel]
     */
    fun loadModelAsync(
        context: Context,
        gltfFileLocation: String,
        fileResolver: (fileName: String) -> String = defaultFileResolver(gltfFileLocation),
        progressiveLoad: Boolean = GLTFLoader.progressiveLoad,
        coroutineScope: LifecycleCoroutineScope,
        result: (FilamentAsset?) -> Unit
    ) = coroutineScope.launchWhenCreated {
        result(loadModel(context, gltfFileLocation, fileResolver, progressiveLoad))
    }

    /**
     *
     * ### Create a model from the contents of a GLTF file and folder
     *
     * @param progressiveLoad Asynchronous resource load.
     * True means that the model will be displayed even if its textures are not completely loaded.
     * The model textures will appear progressively while loaded.
     */
    suspend fun createModel(
        glbBuffer: Buffer,
        contentResolver: suspend (String) -> ByteBuffer?,
        progressiveLoad: Boolean = GLTFLoader.progressiveLoad
    ): FilamentAsset? {
        return assetLoader.createAssetFromJson(glbBuffer)?.also { asset ->
            for (uri in asset.resourceUris) {
                contentResolver(uri)?.let { resourceBuffer ->
                    resourceLoader.addResourceData(uri, resourceBuffer)
                }
            }
            if (progressiveLoad) {
                resourceLoader.asyncBeginLoad(asset)
            } else {
                resourceLoader.loadResources(asset)
            }
        }
    }

    fun defaultFileResolver(gltfFileLocation: String): (fileName: String) -> String = { fileName ->
        gltfFileLocation.substringBeforeLast("/", "") + "/${fileName}"
    }
}