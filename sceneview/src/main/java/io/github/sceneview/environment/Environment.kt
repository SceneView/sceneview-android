package io.github.sceneview.environment

import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.utils.HDRLoader
import io.github.sceneview.safeDestroyIndirectLight
import io.github.sceneview.safeDestroySkybox
import java.io.Closeable
import com.google.android.filament.utils.KTX1Loader as KTXLoader

/**
 * Default Filament indirect light intensity for default camera exposure
 */
const val defaultIndirectLightIntensity = 30_000.0f

/**
 *
 * ### Indirect light and skybox environment for a scene
 *
 * Environments are usually captured as high-resolution HDR equirectangular images and processed by
 * the cmgen tool to generate the data needed by IndirectLight.
 *
 * You can also process an hdr at runtime but this is more consuming.
 *
 * - Currently IndirectLight is intended to be used for "distant probes", that is, to represent
 * global illumination from a distant (i.e. at infinity) environment, such as the sky or distant
 * mountains.
 * Only a single IndirectLight can be used in a Scene. This limitation will be lifted in the future.
 *
 * - When added to a Scene, the Skybox fills all untouched pixels.
 *
 * Defines the lighting environment and the skybox for the scene
 *
 *
 * @property indirectLight ### IndirectLight is used to simulate environment lighting.
 * Environment lighting has a two components:
 * - irradiance
 * - reflections (specular component)
 *
 * @property sphericalHarmonics ### Array of 9 * 3 floats
 *
 * @property skybox ### The Skybox is drawn last and covers all pixels not touched by geometry.
 * The Skybox to use to fill untouched pixels, or null to unset the Skybox.
 *
 * @see [IndirectLight]
 * @see [KTXLoader.loadEnvironment]
 * @see [HDRLoader.loadEnvironment]
 */
open class Environment(
    val engine: Engine,
    val indirectLight: IndirectLight? = null,
    val skybox: Skybox? = null,
    val sphericalHarmonics: FloatArray? = null
) : Closeable {

    /**
     * ### Destroys the EnvironmentLights and frees all its associated resources.
     *
     * You shouldn't have to call it if you used lifecycle, since each entity is destroyed
     * at the onDestroy()
     */
    open fun destroy() {
        indirectLight?.let { engine.safeDestroyIndirectLight(it) }
        skybox?.let { engine.safeDestroySkybox(it) }
    }

    override fun close() = destroy()
}