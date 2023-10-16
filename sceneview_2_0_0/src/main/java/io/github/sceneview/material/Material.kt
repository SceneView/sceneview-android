package io.github.sceneview.material

import android.content.Context
import android.content.res.AssetManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import io.github.sceneview.SceneView
import io.github.sceneview.utils.loadFileBuffer
import io.github.sceneview.utils.readFileBuffer
import kotlinx.coroutines.*

/**
 * Specifies the material .filamat asset file location
 *
 * The material data is a binary blob produced by libfilamat or by matc.
 *
 * @param assetFileLocation the Filamat Image (.filamat) asset file location
 *
 * @see <a href="https://google.github.io/filament/Materials.html">Filament Materials Guide</a>
 */
fun Material.Builder.filamat(assets: AssetManager, assetFileLocation: String) = apply {
    val buffer = assets.readFileBuffer(assetFileLocation)
    payload(buffer, buffer.remaining())
}

/**
 * Specifies the material .filamat file location
 *
 * The material data is a binary blob produced by libfilamat or by matc.
 *
 * @param fileLocation the Filamat Image (.filamat) file location
 *
 * @see <a href="https://google.github.io/filament/Materials.html">Filament Materials Guide</a>
 */
suspend fun Material.Builder.filamat(context: Context, fileLocation: String) = apply {
    val buffer = context.loadFileBuffer(fileLocation)!!
    withContext(Dispatchers.Main) {
        payload(buffer, buffer.remaining())
    }
}

/**
 * Specifies the material .filamat file location
 *
 * The material data is a binary blob produced by libfilamat or by matc.
 *
 * @param fileLocation the Filamat Image (.filamat) file location
 *
 * @see <a href="https://google.github.io/filament/Materials.html">Filament Materials Guide</a>
 */
fun Material.Builder.filamatAsync(
    context: Context,
    fileLocation: String
): Deferred<Material.Builder> = CoroutineScope(Dispatchers.IO).async {
    filamat(context, fileLocation)
}

fun Material.Builder.build(sceneView: SceneView): Material =
    build(sceneView.engine).also {
        sceneView.materials += it
    }

fun Deferred<Material.Builder>.build(sceneView: SceneView, onResult: (Material) -> Unit): Job =
    CoroutineScope(Dispatchers.IO).launch {
        onResult(await().build(sceneView))
    }.also {
        sceneView.loadingJobs += it
    }

/**
 * Creates a new instance of this material
 *
 * Material instances will be destroyed when the [SceneView] is
 */
fun Material.createInstance(sceneView: SceneView, name: String? = null): MaterialInstance =
    (name?.let { createInstance(it) } ?: createInstance()).also {
        sceneView.materialInstances += it
    }

fun SceneView.destroyMaterial(material: Material) {
    engine.destroyMaterial(material)
    materials -= material
}