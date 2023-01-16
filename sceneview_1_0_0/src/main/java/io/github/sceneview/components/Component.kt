package io.github.sceneview.components

import com.google.android.filament.Engine
import io.github.sceneview.Entity

typealias EntityInstance = Int

interface Component {
    val engine: Engine
    val entity: Entity
}