package io.github.sceneview.ar.components

import com.google.ar.core.Camera
import com.google.ar.core.Frame
import com.google.ar.core.Pose
import io.github.sceneview.components.Component
import io.github.sceneview.math.Transform
import io.github.sceneview.math.toTransform
import io.github.sceneview.utils.FrameTime

typealias ARFrame = Frame

interface ARComponent : Component {

    val pose: Pose?

    fun onARFrame(frameTime: FrameTime, frame: ARFrame)
}