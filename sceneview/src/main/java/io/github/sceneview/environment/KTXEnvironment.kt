package io.github.sceneview.environment

import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox

class KTXEnvironment(
    engine: Engine,
    indirectLight: IndirectLight?,
    skybox: Skybox? = null,
    sphericalHarmonics: FloatArray? = null
) : Environment(
    engine = engine,
    indirectLight = indirectLight,
    skybox = skybox,
    sphericalHarmonics = sphericalHarmonics
)