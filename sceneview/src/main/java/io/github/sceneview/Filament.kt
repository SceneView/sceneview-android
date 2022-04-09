package io.github.sceneview

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderLoader
import com.google.ar.sceneform.rendering.EngineInstance
import io.github.sceneview.environment.IBLPrefilter
import io.github.sceneview.material.MaterialLoader

// TODO : Add the lifecycle aware management when filament dependents are all kotlined
object Filament {

    init {
        Filament.init()
    }

    private var _engine: Engine? = null

    val engine: Engine get() = _engine ?: Engine.create().also { _engine = it }

    val entityManager get() = EntityManager.get()
    val transformManager get() = engine.transformManager
    val renderableManager get() = engine.renderableManager
    val lightManager get() = engine.lightManager

    val resourceLoader by lazy { ResourceLoader(engine, true, false, false) }
    val assetLoader by lazy { AssetLoader(engine, materialProvider, entityManager) }

    val materialProvider by lazy { UbershaderLoader(engine) }

    val iblPrefilter by lazy { IBLPrefilter(engine) }

    var retainers = 0

    fun retain() {
        retainers++
    }

    fun release() {
        retainers--
        if (retainers == 0) {
            destroy()
        }
    }

    fun destroy() {
        resourceLoader.asyncCancelLoad()
        resourceLoader.evictResourceData()

        assetLoader.destroy()
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        resourceLoader.destroy()

        _engine?.destroy()
        _engine = null
    }
}
