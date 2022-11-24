package io.github.sceneview.loaders

import android.content.Context
import com.google.android.filament.Engine
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import io.github.sceneview.material.setInvertFrontFaceWinding
import io.github.sceneview.material.setViewTexture
import io.github.sceneview.utils.loadFileBuffer
import io.github.sceneview.utils.readFileBuffer
import kotlinx.coroutines.*
import java.nio.Buffer

/**
 * A Filament Material defines the visual appearance of an object
 *
 * Materials function as a templates from which [MaterialInstance]s can be spawned.
 */
class MaterialLoader(engine: Engine, context: Context) {

    private var _engine: Engine? = engine
    private val engine get() = _engine!!
    private var _context: Context? = context
    private val context get() = _context!!

    private val loadingJobs = mutableListOf<Job>()
    private val materials = mutableListOf<Material>()
    private val materialInstances = mutableListOf<MaterialInstance>()

    val viewLitMaterial: Material by lazy {
        createMaterial("materials/view_lit.filamat")
    }
    val viewUnlitMaterial: Material by lazy {
        createMaterial("materials/view_unlit.filamat")
    }

    /**
     * Creates and returns a [Material] object
     *
     * A Filament Material defines the visual appearance of an object. Materials function as a
     * templates from which [MaterialInstance]s can be spawned.
     *
     * Documentation: [Filament Materials Guide](https://google.github.io/filament/Materials.html)
     *
     * @param payload Specifies the material data. The material data is a binary blob produced by
     * libfilamat or by matc.
     *
     * @see MaterialLoader.loadMaterial
     */
    fun createMaterial(payload: Buffer): Material =
        Material.Builder()
            .payload(payload, payload.remaining())
            .build(engine).also { material ->
                materials += material
            }

    /**
     * Creates and returns a [Material] object from Filamat asset file
     *
     * @see createMaterial
     */
    fun createMaterial(assetFileLocation: String): Material =
        createMaterial(context.assets.readFileBuffer(assetFileLocation))

    /**
     * Loads a [Material] from the contents of a Filamat file
     *
     * The material data is a binary blob produced by libfilamat or by matc.
     *
     * @param fileLocation the .filamat file location:
     * - A relative asset file location *materials/mymaterial.filamat*
     * - An Android resource from the res folder *context.getResourceUri(R.raw.mymaterial)*
     * - A File path *Uri.fromFile(myMaterialFile).path*
     * - An http or https url *https://mydomain.com/mymaterial.filamat*
     */
    suspend fun loadMaterial(fileLocation: String): Material? =
        context.loadFileBuffer(fileLocation)?.let { buffer ->
            withContext(Dispatchers.Main) {
                createMaterial(buffer)
            }
        }

    /**
     * Loads a [Material] from the contents of a Filamat file within a created coroutine scope.
     *
     * @see loadMaterial
     */
    fun loadMaterialAsync(fileLocation: String, onResult: (Material?) -> Unit) =
        CoroutineScope(Dispatchers.IO).launch {
            loadMaterial(fileLocation).also(onResult)
        }.also {
            loadingJobs += it
        }

    fun createViewMaterial(
        viewTexture: Texture,
        unlit: Boolean = false,
        invertFrontFaceWinding: Boolean = false
    ): MaterialInstance {
        val material = if (unlit) viewLitMaterial else viewUnlitMaterial
        val materialInstance = material.createInstance().apply {
            setViewTexture(viewTexture)
            setInvertFrontFaceWinding(invertFrontFaceWinding)
        }
        materialInstances += materialInstance
        return materialInstance
    }

    fun destroyMaterial(material: Material) {
        engine.destroyMaterial(material)
        materials -= material
    }

    fun destroyMaterialInstance(materialInstance: MaterialInstance) {
        engine.destroyMaterialInstance(materialInstance)
        materialInstances -= materialInstance
    }

    fun destroy() {
        loadingJobs.forEach { it.cancel() }
        loadingJobs.clear()
        materialInstances.toList().forEach { runCatching { destroyMaterialInstance(it) } }
        materialInstances.clear()
        materials.toList().forEach { runCatching { destroyMaterial(it) } }
        materials.clear()
        _engine = null
        _context = null
    }
}