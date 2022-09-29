package io.github.sceneview.components

interface CameraComponent : Component {

    val camera get() = engine.getCameraComponent(entity)!!
}