package io.github.sceneview

import android.opengl.EGLContext
import com.google.android.filament.*
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderLoader
import com.google.android.filament.utils.Utils
import com.google.ar.sceneform.rendering.GLHelper
import io.github.sceneview.environment.IBLPrefilter

// TODO : Add the lifecycle aware management when filament dependents are all kotlined
object Filament {

    init {
        Gltfio.init()
        com.google.android.filament.Filament.init()
        Utils.init()
    }

    private var eglContext: EGLContext? = null
    private var _engine: Engine? = null

    @JvmStatic
    val engine: Engine
        get() = _engine ?: GLHelper.makeContext().let { eglContext ->
            this.eglContext = eglContext
            Engine.create(eglContext).also { _engine = it }
        }

    @JvmStatic
    val entityManager
        get() = EntityManager.get()

    @JvmStatic
    val transformManager
        get() = engine.transformManager

    @JvmStatic
    val renderableManager
        get() = engine.renderableManager

    @JvmStatic
    val lightManager
        get() = engine.lightManager

    @JvmStatic
    val resourceLoader by lazy { ResourceLoader(engine, true, true, false) }

    @JvmStatic
    val assetLoader by lazy { AssetLoader(engine, materialProvider, entityManager) }

    @JvmStatic
    val materialProvider by lazy { UbershaderLoader(engine) }

    @JvmStatic
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
        // We still got some errors due to this nightmare Renderable
        // Should be solved with RIP Renderable
//        resourceLoader.asyncCancelLoad()
//        resourceLoader.evictResourceData()

//        assetLoader.destroy()
        // Despite all the effort, not destroyed Material still exist.
        // Uncomment this after RIP Renderable push
//        materialProvider.destroyMaterials()
//        materialProvider.destroy()
//        resourceLoader.destroy()

//        _engine?.destroy()
//        _engine = null

//        GLHelper.destroyContext(eglContext)
//        eglContext = null
    }
}

fun Engine.createCamera() = createCamera(entityManager.create())
fun RenderableManager.Builder.build(entity: Int) = build(Filament.engine, entity)