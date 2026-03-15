package com.google.ar.sceneform.rendering

import com.google.ar.sceneform.resources.ResourceHolder
import com.google.ar.sceneform.resources.ResourceRegistry

/**
 * Minimal resource manager. Maintains mappings from ids to created resources and a task executor
 * dedicated to loading resources asynchronously.
 *
 * @hide
 */
@Suppress("initialization")
class ResourceManager private constructor() {
    private val resourceHolders = ArrayList<ResourceHolder>()
    private val textureRegistry = ResourceRegistry<Texture>()
    private val materialRegistry = ResourceRegistry<Material>()
    private val modelRenderableRegistry = ResourceRegistry<ModelRenderable>()
    private val viewRenderableRegistry = ResourceRegistry<ViewRenderable>()

    init {
        addResourceHolder(textureRegistry)
        addResourceHolder(materialRegistry)
        addResourceHolder(modelRenderableRegistry)
        addViewRenderableRegistry()
    }

    fun getTextureRegistry(): ResourceRegistry<Texture> = textureRegistry

    fun getMaterialRegistry(): ResourceRegistry<Material> = materialRegistry

    fun getModelRenderableRegistry(): ResourceRegistry<ModelRenderable> = modelRenderableRegistry

    fun getViewRenderableRegistry(): ResourceRegistry<ViewRenderable> = viewRenderableRegistry

    fun reclaimReleasedResources(): Long {
        var resourcesInUse = 0L
        for (registry in resourceHolders) {
            resourcesInUse += registry.reclaimReleasedResources()
        }
        return resourcesInUse
    }

    /** Forcibly deletes all tracked references */
    fun destroyAllResources() {
        for (resourceHolder in resourceHolders) {
            resourceHolder.destroyAllResources()
        }
    }

    fun addResourceHolder(resource: ResourceHolder) {
        resourceHolders.add(resource)
    }

    private fun addViewRenderableRegistry() {
        addResourceHolder(viewRenderableRegistry)
    }

    companion object {
        @Volatile
        private var instance: ResourceManager? = null

        @JvmStatic
        fun getInstance(): ResourceManager = instance ?: synchronized(this) {
            instance ?: ResourceManager().also { instance = it }
        }
    }
}
