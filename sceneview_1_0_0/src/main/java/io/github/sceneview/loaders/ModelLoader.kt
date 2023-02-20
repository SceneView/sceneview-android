package io.github.sceneview.loaders

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.gltfio.*
import io.github.sceneview.model.Model
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.nodes.ModelNode
import io.github.sceneview.utils.FrameTime
import io.github.sceneview.utils.loadFileBuffer
import io.github.sceneview.utils.readFileBuffer
import kotlinx.coroutines.*
import java.nio.Buffer

/**
 * Consumes a blob of glTF 2.0 content (either JSON or GLB) and produces a [Model] object, which is
 * a bundle of Filament textures, vertex buffers, index buffers, etc.
 * A [Model] is composed of 1 or more [ModelInstance] objects which contain entities and components.
 */
class ModelLoader(engine: Engine, context: Context) {

    val materialProvider: MaterialProvider
    val assetLoader: AssetLoader
    var resourceLoader: ResourceLoader

    private var _context: Context? = context
    private val context get() = _context!!

    private val loadingJobs = mutableListOf<Job>()
    private val models = mutableListOf<Model>()
    private val modelInstances = mutableListOf<ModelInstance>()

    init {
        materialProvider = UbershaderProvider(engine)
        assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
        resourceLoader = ResourceLoader(engine, true)
    }

    /**
     * Gets the status of an asynchronous resource load as a percentage in [0,1]
     */
    val progress get() = resourceLoader.asyncGetLoadProgress()

    internal fun onFrame(frameTime: FrameTime) {
        // Allow the resource loader to finalize textures that have become ready.
        resourceLoader.asyncUpdateLoad()
    }

    /**
     * Creates a [FilamentAsset] from the contents of a GLB or GLTF file
     */
    private fun createModel(buffer: Buffer): Model? =
        assetLoader.createAsset(buffer)?.also { model ->
            models += model
        }

    /**
     * Feeds the binary content of an external resource into the loader's URI cache
     */
    private fun loadResources(model: Model, resourceResolver: ((String) -> Buffer)) {
        for (uri in model.resourceUris) {
            resourceLoader.addResourceData(uri, resourceResolver(uri))
        }
        resourceLoader.asyncBeginLoad(model)
    }

    private suspend fun loadResources(model: Model, resourceResolver: ((String) -> String)) {
        for (uri in model.resourceUris) {
            context.loadFileBuffer(resourceResolver(uri))?.let { buffer ->
                resourceLoader.addResourceData(uri, buffer)
            }
        }
        resourceLoader.asyncBeginLoad(model)
    }

    /**
     * Creates a [Model] from the contents of a GLB or GLTF Buffer
     *
     * Don't forget to call [FilamentAsset.releaseSourceData] to free the glTF hierarchy as it is no
     * longer needed:
     * - Right after creating a [ModelNode] that will retrieve the [FilamentAsset.getAnimator]
     * - Not before any call to [AssetLoader.createInstance]
     *
     * @see AssetLoader.createAsset
     */
    fun createModel(buffer: Buffer, resourceResolver: (String) -> Buffer): Model? =
        createModel(buffer)?.also { model ->
            loadResources(model, resourceResolver)
        }

    /**
     * Creates a [Model] from the contents of a GLB or GLTF asset file
     *
     * @see createModel
     */
    fun createModel(assetFileLocation: String): Model? =
        createModel(context.assets.readFileBuffer(assetFileLocation)) {
            context.assets.readFileBuffer(resourceResolver(assetFileLocation, it))
        }

    /**
     * Loads a [Model] from the contents of a GLB or GLTF file
     *
     * @param fileLocation the .glb or .gltf file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param resourceResolver Only used for GLTF file. Return a GLTF resource absolute location
     * from a relative file location. The given callback is triggered for each requested resource.
     *
     * @see createModel
     */
    suspend fun loadModel(
        fileLocation: String,
        resourceResolver: (String) -> String = { resourceResolver(fileLocation, it) }
    ): Model? = context.loadFileBuffer(fileLocation)?.let { buffer ->
        withContext(Dispatchers.Main) {
            createModel(buffer)?.also { model ->
                loadResources(model, resourceResolver)
            }
        }
    }

    /**
     * Loads a [Model] from the contents of a GLB or GLTF file within a created coroutine scope
     *
     * @see loadModel
     */
    fun loadModelAsync(
        fileLocation: String,
        resourceResolver: (String) -> String = { ModelLoader.resourceResolver(fileLocation, it) },
        onResult: (Model?) -> Unit
    ): Job = CoroutineScope(Dispatchers.IO).launch {
        loadModel(fileLocation, resourceResolver).also(onResult)
    }.also {
        loadingJobs += it
    }

    private fun createInstancedModel(
        buffer: Buffer,
        count: Int
    ): Pair<Model, Array<ModelInstance?>>? =
        arrayOfNulls<ModelInstance>(count).let { instances ->
            assetLoader.createInstancedAsset(buffer, instances)?.let { model ->
                models += model
                modelInstances += instances.filterNotNull()
                Pair(model, instances)
            }
        }

    /**
     * Creates a primary [Model] with one or more [ModelInstance]s from the contents of a GLB or
     * GLTF file.
     *
     * Don't forget to call [FilamentAsset.releaseSourceData] to free the glTF hierarchy as it is no
     * longer needed:
     * - Right after creating a [ModelNode] that will retrieve the [FilamentAsset.getAnimator]
     * - Not before any call to [AssetLoader.createInstance]
     *
     * @param count Must be sized to the desired number of instances. If successful, this method
     * will return the array with secondary instances whose resources are shared with the primary
     * asset.
     *
     * @see loadInstancedModel
     */
    fun createInstancedModel(
        buffer: Buffer,
        count: Int,
        resourceResolver: (String) -> Buffer
    ): Pair<Model, Array<ModelInstance?>>? =
        createInstancedModel(buffer, count)?.also { (model, _) ->
            loadResources(model, resourceResolver)
        }

    /**
     * Loads a primary [Model] with one or more [ModelInstance]s from the contents of a GLB or GLTF
     * file.
     *
     * @param fileLocation the .glb or .gltf file location:
     * - A relative asset file location *models/mymodel.glb*
     * - An android resource from the res folder *context.getResourceUri(R.raw.mymodel)*
     * - A File path *Uri.fromFile(myModelFile).path*
     * - An http or https url *https://mydomain.com/mymodel.glb*
     * @param count Must be sized to the desired number of instances. If successful,
     * this method will return the array with secondary instances whose resources are shared with
     * the primary asset.
     * @param resourceResolver Only used for GLTF file. Return a GLTF resource absolute location
     * from a relative file location. The given callback is triggered for each requested resource.
     *
     * @see createInstancedModel
     */
    suspend fun loadInstancedModel(
        fileLocation: String,
        count: Int,
        resourceResolver: (String) -> String = { resourceResolver(fileLocation, it) }
    ): Pair<Model, Array<ModelInstance?>>? =
        context.loadFileBuffer(fileLocation)?.let { buffer ->
            withContext(Dispatchers.Main) {
                createInstancedModel(buffer, count)?.also { (model, _) ->
                    loadResources(model, resourceResolver)
                }
            }
        }

    /**
     * Loads a primary [Model] with one or more [ModelInstance]s from the contents of a GLB or GLTF
     * file with a created coroutine scope.
     *
     * @see loadInstancedModel
     */
    fun loadInstancedModelAsync(
        fileLocation: String,
        count: Int,
        resourceResolver: (String) -> String = { resourceResolver(fileLocation, it) },
        onResult: (Pair<Model, Array<ModelInstance?>>?) -> Unit
    ) = CoroutineScope(Dispatchers.IO).launch {
        loadInstancedModel(fileLocation, count, resourceResolver).also(onResult)
    }.also {
        loadingJobs += it
    }

    /**
     * Adds a new instance to the asset.
     *
     * Use this with caution. It is more efficient to pre-allocate a max number of instances, and
     * gradually add them to the scene as needed. Instances can also be "recycled" by removing and
     * re-adding them to the scene.
     *
     * NOTE: destroyInstance() does not exist because gltfio favors flat arrays for storage of
     * entity lists and instance lists, which would be slow to shift. We also wish to discourage
     * create/destroy churn, as noted above.
     *
     * This cannot be called after FilamentAsset#releaseSourceData().
     * Animation is not supported in new instances.
     *
     * @see createInstancedModel
     */
    fun createInstance(model: Model): ModelInstance? = assetLoader.createInstance(model)?.also {
        modelInstances += it
    }

    fun destroyModel(model: Model) {
//        model.releaseSourceData()
//        assetLoader.destroyAsset(model)
        models -= model
    }

    fun destroyModelInstance(instance: ModelInstance) {
        // There is no destroy for FilamentInstance yet on Filament
        modelInstances -= instance
    }

    fun destroy() {
        loadingJobs.forEach { it.cancel() }
        resourceLoader.asyncCancelLoad()
        resourceLoader.evictResourceData()

        models.toList().forEach { runCatching { destroyModel(it) } }
        models.clear()
        modelInstances.toList().forEach { runCatching { destroyModelInstance(it) } }
        modelInstances.clear()

        assetLoader.destroy()
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        resourceLoader.destroy()
        _context = null
    }

    companion object {
        var resourceResolver = { baseFileName: String, resourceFileName: String ->
            "${baseFileName.substringBeforeLast("/")}/$resourceFileName"
        }
    }
}