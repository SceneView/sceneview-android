package io.github.sceneview.environment

import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox

class KTXEnvironment(
    indirectLight: IndirectLight?,
    skybox: Skybox? = null,
    sphericalHarmonics: FloatArray? = null
) : Environment(
    indirectLight = indirectLight,
    skybox = skybox,
    sphericalHarmonics = sphericalHarmonics
)