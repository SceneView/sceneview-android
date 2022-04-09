package io.github.sceneview.model

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

object GLBLoader {

    val assetLoader get() = Filament.assetLoader
    val resourceLoader get() = Filament.resourceLoader

    val progressiveLoad = false

    /**
     * ### Utility for loading a glTF 3D model
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
    ): ModelRenderable? = withContext(Dispatchers.Main) {
        createModel(context, glbFileLocation).await()
    }

    /**
     * ### Utility for loading a glTF 3D model
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [GLBLoader.loadModel]
     *
     * [Documentation][GLBLoader.loadEnvironment]
     *
     */
    fun loadModelAsync(
        context: Context,
        glbFileLocation: String,
        coroutineScope: LifecycleCoroutineScope,
        result: (ModelRenderable?) -> Unit
    ) = coroutineScope.launchWhenCreated {
        result(
            loadModel(context, glbFileLocation)
        )
    }

    fun createModel(context: Context, glbFileLocation: String) =
        ModelRenderable.builder()
            .setSource(context, Uri.parse(glbFileLocation))
            .setIsFilamentGltf(true)
            .build()
            .exceptionally {
                throw it
            }


    /**
     * ### Load a glTF 3D model from a glb file asynchronously
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [GLBLoader.loadModel]
     *
     * [Documentation][GLBLoader.loadModel]
     */
    fun loadModelAsync2(
        context: Context,
        glbFileLocation: String,
        progressiveLoad: Boolean = GLBLoader.progressiveLoad,
        coroutineScope: LifecycleCoroutineScope,
        result: (FilamentAsset?) -> Unit
    ) = coroutineScope.launchWhenCreated {
        result(loadModel2(context, glbFileLocation, progressiveLoad))
    }

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
    suspend fun loadModel2(
        context: Context,
        glbFileLocation: String,
        progressiveLoad: Boolean = GLBLoader.progressiveLoad
    ): FilamentAsset? {
        return try {
            context.fileBuffer(glbFileLocation)?.let { buffer ->
                withContext(Dispatchers.Main) {
                    createModel2(buffer, progressiveLoad)
                }
            }
        } finally {
            // TODO: See why the finally is called before the onDestroy()
//        asset?.destroy()
        }
    }

    /**
     *
     * ### Create a model from the contents of a GLB file
     *
     * @param progressiveLoad Asynchronous resource load.
     * True means that the model will be displayed even if its textures are not completely loaded.
     * The model textures will appear progressively while loaded.
     */
    fun createModel2(
        glbBuffer: Buffer,
        progressiveLoad: Boolean = GLBLoader.progressiveLoad
    ): FilamentAsset? {
        return assetLoader.createAssetFromBinary(glbBuffer)?.also { asset ->
            if (progressiveLoad) {
                resourceLoader.asyncBeginLoad(asset)
            } else {
                resourceLoader.loadResources(asset)
            }
        }
    }
}