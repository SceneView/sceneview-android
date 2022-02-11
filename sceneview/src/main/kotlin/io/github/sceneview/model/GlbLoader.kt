package io.github.sceneview.model

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.sceneform.rendering.ModelRenderable

object GlbLoader {

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
    ): ModelRenderable? = ModelRenderable.builder()
        .setSource(context, Uri.parse(glbFileLocation))
        .setIsFilamentGltf(true)
        .await()

    /**
     * ### Utility for loading a glTF 3D model
     *
     * For Java compatibility usage.
     *
     * Kotlin developers should use [GlbLoader.loadModel]
     *
     * [Documentation][GlbLoader.loadEnvironment]
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
}