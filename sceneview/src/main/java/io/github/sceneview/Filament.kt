package io.github.sceneview

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

    @JvmStatic
    val engine = EngineInstance.getEngine().filamentEngine

    @JvmStatic
    val entityManager
        get() = EntityManager.get()

    val uberShaderLoader by lazy { UbershaderLoader(engine) }

    @JvmStatic
    val assetLoader by lazy {
        AssetLoader(engine, uberShaderLoader, entityManager)
    }

    val transformManager get() = engine.transformManager

    val resourceLoader by lazy { ResourceLoader(engine, true, false, false) }

    val lightManager get() = engine.lightManager

    val iblPrefilter by lazy { IBLPrefilter(engine) }

    fun destroy() {
        //TODO : Add every Filament destroys
    }
}
