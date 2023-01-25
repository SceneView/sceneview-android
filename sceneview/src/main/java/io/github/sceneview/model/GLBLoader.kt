package io.github.sceneview.model

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.gorisse.thomas.lifecycle.observe
import io.github.sceneview.Filament.assetLoader
import io.github.sceneview.Filament.resourceLoader
import io.github.sceneview.renderable.setScreenSpaceContactShadows
import io.github.sceneview.utils.useFileBufferNotNull
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
     *
     * @param lifecycle Provide your lifecycle in order to destroy it (and its resources) when the
     * lifecycle goes to destroy state. You are responsible of manually destroy the [Model] if you
     * don't provide lifecycle.
     */
    suspend fun loadModel(
        context: Context,
        glbFileLocation: String,
        lifecycle: Lifecycle? = null
    ): Model? = context.useFileBufferNotNull(glbFileLocation) { buffer ->
        withContext(Dispatchers.Main) {
            createModel(buffer, lifecycle)
        }
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
        lifecycle: Lifecycle,
        glbFileLocation: String,
        result: (Model?) -> Unit
    ) = lifecycle.coroutineScope.launchWhenCreated {
        result(loadModel(context, glbFileLocation, lifecycle))
    }

    fun createModel(buffer: Buffer, lifecycle: Lifecycle? = null): Model? {
        return assetLoader.createAsset(buffer)?.also { asset ->
            resourceLoader.loadResources(asset)
            runCatching { asset.releaseSourceData() }

            //TODO: Used by Filament ModelViewer, see if it's usefull
            asset.instance.renderables.forEach {
                it.setScreenSpaceContactShadows(true)
            }

            lifecycle?.observe(onDestroy = {
                // Prevent double destroy in case of manually destroyed
                runCatching { asset.destroy() }
            })
        }
    }

    private fun createInstancedModel(
        buffer: Buffer,
        count: Int,
        lifecycle: Lifecycle? = null
    ): Pair<Model, Array<ModelInstance?>>? {
        val instances = arrayOfNulls<ModelInstance>(count)
        return assetLoader.createInstancedAsset(buffer, instances)?.let { asset ->
            resourceLoader.loadResources(asset)
            runCatching { asset.releaseSourceData() }

            //TODO: Used by Filament ModelViewer, see if it's usefull
            instances.flatMap { it?.renderables ?: listOf() }.forEach {
                it.setScreenSpaceContactShadows(true)
            }

            lifecycle?.observe(onDestroy = {
                // Prevent double destroy in case of manually destroyed
                runCatching { asset.destroy() }
            })
            Pair(asset, instances)
        }
    }
}