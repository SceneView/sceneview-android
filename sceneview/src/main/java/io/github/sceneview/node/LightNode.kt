package io.github.sceneview.node

import io.github.sceneview.light.Light
import io.github.sceneview.light.destroyLight
import io.github.sceneview.light.direction
import io.github.sceneview.light.position
import io.github.sceneview.math.*
import io.github.sceneview.utils.FrameTime

/**
 * ### A Node represents a transformation within the scene graph's hierarchy.
 *
 * This node contains a light for the rendering engine to render.
 *
 * Each node can have an arbitrary number of child nodes and one parent. The parent may be
 * another node, or the scene.
 */
open class LightNode : Node {

    var light: Light? = null
        set(value) {
//            field?.destroyLight()
            field = value
            sceneEntities = value?.let { intArrayOf(it) } ?: intArrayOf()
        }

    /**
     * ### Construct a [LightNode] with it Position, Rotation and Scale
     *
     * @param position See [Node.position]
     * @param rotation See [Node.rotation]
     * @param scale See [Node.scale]
     */
    constructor(
        position: Position = DEFAULT_POSITION,
        rotation: Rotation = DEFAULT_ROTATION,
        scale: Scale = DEFAULT_SCALE
    ) : super(position, rotation, scale)

    /**
     * TODO : Doc
     */
    constructor(light: Light) : this() {
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
        light?.destroyLight()
    }
}