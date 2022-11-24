package io.github.sceneview.components

import io.github.sceneview.model.Model

interface ModelChildComponent : Component {
    val model: Model
    val name: String? get() = model.getName(entity)
    val extras: String? get() = model.getExtras(entity)
}