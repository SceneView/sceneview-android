package io.github.sceneview.components

import com.google.android.filament.Engine
import io.github.sceneview.Entity

interface Component {
    val engine: Engine
    val entity: Entity
}