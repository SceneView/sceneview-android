package io.github.sceneview

import android.opengl.EGLContext
import com.google.android.filament.*
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.Gltfio
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import com.google.android.filament.utils.Utils
import io.github.sceneview.environment.IBLPrefilter
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toColumnsFloatArray
import io.github.sceneview.utils.OpenGL
import java.lang.ref.WeakReference

// TODO : Add the lifecycle aware management when filament dependents are all kotlined
object Filament {

    init {
        Gltfio.init()
        com.google.android.filament.Filament.init()
        Utils.init()
    }

    private var eglContext: WeakReference<EGLContext>? = null

    private var _engine: WeakReference<Engine>? = null

    @JvmStatic
    val engine: Engine
        get() = _engine?.get() ?: (eglContext?.get() ?: OpenGL.createEglContext())
            .let { eglContext ->
                this.eglContext = WeakReference(eglContext)
                Engine.create(eglContext).also { engine ->
                    _engine = WeakReference(engine)
                }
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

    private var _resourceLoader: ResourceLoader? = null

    @JvmStatic
    val resourceLoader: ResourceLoader
        get() = _resourceLoader ?: ResourceLoader(engine).also { _resourceLoader = it }

    private var _materialProvider: UbershaderProvider? = null

    @JvmStatic
    val materialProvider
        get() = _materialProvider ?: UbershaderProvider(engine).also { _materialProvider = it }

    private var _assetLoader: AssetLoader? = null

    @JvmStatic
    val assetLoader: AssetLoader?
        get() = _assetLoader ?: _engine?.get()?.let {
            AssetLoader(
                it,
                materialProvider,
                entityManager
            ).also { _assetLoader = it }
        }

    private var _iblPrefilter: IBLPrefilter? = null

    @JvmStatic
    val iblPrefilter: IBLPrefilter
        get() = _iblPrefilter ?: IBLPrefilter(engine).also { _iblPrefilter = it }

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
        // TODO: We still got some errors on this destroy due to this nightmare Renderable
        //  Should be solved with RIP Renderable
//        _assetLoader?.destroy()
        _assetLoader = null

        _resourceLoader?.apply {
            runCatching { asyncCancelLoad() }
            runCatching { evictResourceData() }
            runCatching { destroy() }
        }
        _resourceLoader = null

        // TODO: Materials should be destroyed by their own
        // TODO: Hot fix because of not destroyed instances
//        runCatching { _materialProvider?.destroyMaterials() }
//        runCatching { _materialProvider?.destroy() }
        _materialProvider = null

        runCatching { _iblPrefilter?.destroy() }
        _iblPrefilter = null

        runCatching { _engine?.get()?.destroy() }
        _engine?.clear()
        _engine = null

        eglContext?.get()?.let {
            runCatching { OpenGL.destroyEglContext(it) }
        }
        eglContext?.clear()
        eglContext = null
    }
}

fun Engine.createCamera() = createCamera(entityManager.create())

fun RenderableManager.Builder.build(entity: Int) = build(Filament.engine, entity)

fun TransformManager.setTransform(@EntityInstance i: Int, worldTransform: Transform) =
    setTransform(i, worldTransform.toColumnsFloatArray())