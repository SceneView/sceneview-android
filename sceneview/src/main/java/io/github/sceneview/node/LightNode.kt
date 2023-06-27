package io.github.sceneview.node

import com.google.android.filament.Engine
import io.github.sceneview.light.Light
import io.github.sceneview.light.destroyLight
import io.github.sceneview.light.direction
import io.github.sceneview.light.position
import io.github.sceneview.math.Direction
import io.github.sceneview.math.lookTowards
import io.github.sceneview.utils.FrameTime

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a light for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class LightNode(engine: Engine) : Node(engine) {

    var light: Light? = null
        set(value) {
//            field?.destroyLight()
            field = value
            sceneEntities = value?.let { intArrayOf(it) } ?: intArrayOf()
        }

    /**
     * TODO : Doc
     */
    constructor(engine: Engine, light: Light) : this(engine) {
        this.light = light
        worldPosition = light.position
        worldQuaternion = lookTowards(eye = light.position, direction = light.direction)
    }

    override fun onFrame(frameTime: FrameTime) {
        super.onFrame(frameTime)

        if (isAttached) {
            light?.let { light ->
                if (light.position != worldPosition) {
                    light.position = worldPosition
                }
                //TODO: Check that
                val worldDirection = worldQuaternion * Direction(y = 1.0f)
                if (light.direction != worldDirection) {
                    light.direction = worldDirection
                }
            }
        }
    }

    /** ### Detach and destroy the node */
    override fun destroy() {
        super.destroy()
        light?.let { engine.destroyLight(it) }
    }
}