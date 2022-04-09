package io.github.sceneview.node

import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.*
import io.github.sceneview.SceneView
import io.github.sceneview.math.*
import io.github.sceneview.scene.*

/**
 * ### A LightNode allows you to create a light source in the scene, such as a sun or street lights
 *
 * At least one light must be added to a scene in order to see anything (unless the
 * [com.google.android.filament.Material.Shading.UNLIT] is used).
 *
 * A Light component is created using the [LightManager.Builder] and
 * destroyed by calling [destroy].
 * ```
 * val sun: Light = LightManager.Builder(LightManager.Type.SUN)
 *      .castShadows(true)
 *      .build()
 * val lightNode = LightNode(sun)
 * sceneView.addChild(lightNode)
 * ```
 *
 * #### Light types
 *
 * Lights come in three flavors:
 * - directional lights
 * - point lights
 * - spot lights
 *
 * #### Directional lights
 *
 * Directional lights have a direction, but don't have a position. All light rays are parallel and
 * come from infinitely far away and from everywhere. Typically a directional light is used to
 * simulate the sun.
 * Directional lights and spot lights are able to cast shadows.
 * To create a directional light use [LightManager.Type.DIRECTIONAL] or [LightManager.Type.SUN],
 * both are similar, but the later also draws a sun's disk in the sky and its reflection on glossy
 * objects.
 *
 * **warning:** Currently, only a single directional light is supported. If several directional
 * lights are added to the scene, the dominant one will be used.
 *
 * #### Point lights
 *
 * Unlike directional lights, point lights have a position but emit light in all directions.
 * The intensity of the light diminishes with the inverse square of the distance to the light.
 * [LightManager.Builder.falloff] controls the distance beyond which the light has no more
 * influence.
 * A scene can have multiple point lights.
 *
 * #### Spot lights

 * Spot lights are similar to point lights but the light they emit is limited to a cone defined by
 * [LightManager.Builder.spotLightCone] and the light's direction.
 * A spot light is therefore defined by a position, a direction and inner and outer cones. The spot
 * light's influence is limited to inside the outer cone. The inner cone defines the light's falloff
 * attenuation.
 * A physically correct spot light is a little difficult to use because changing the outer angle
 * of the cone changes the illumination levels, as the same amount of light is spread over a
 * changing volume. The coupling of illumination and the outer cone means that an artist cannot
 * tweak the influence cone of a spot light without also changing the perceived illumination.
 * It therefore makes sense to provide artists with a parameter to disable this coupling. This is
 * the difference between [LightManager.Type.FOCUSED_SPOT] (physically correct) and
 * [LightManager.Type.SPOT] (decoupled).
 */
open class LightNode : Node {

    /**
     * ### The light direction in x, y, z coordinates.
     *
     * **note:** The Light's direction is ignored for [LightManager.Type.POINT] lights.
     *
     * default is Direction(x = 0.0f, y = -1.0f, z = 0.0f)
     *
     * ------- +y ----- -z
     *
     * ---------|----/----
     *
     * ---------|--/------
     *
     * -x - - - 0 - - - +x
     *
     * ------/--|---------
     *
     * ----/----|---------
     *
     * +z ---- -y --------
     */
    var direction: Direction = DEFAULT_DIRECTION

    override var quaternion: Quaternion
        get() = rotation(transform).toQuaternion()
        set(value) {
            direction = quaternion * Direction(z = -1.0f)
        }

    override var transform: Transform
        get() = lookTowards(eye = position, forward = direction, up = Direction(y = 1.0f))
        set(value) {
            position = Position(value.position)
            direction = Direction(value.forward)
        }

    /**
     * ### The light world-space direction
     *
     * The world direction of this light (i.e. relative to the [SceneView]).
     * This is the composition of this component's local direction with its parent's world direction.
     *
     * @see worldTransform
     */
    val worldDirection: Rotation get() = worldTransform.forward

    /**
     * ### The [Light] to display.
     *
     * null to remove the light
     *
     * @see com.google.android.filament.LightManager.Builder
     */
    var light: Light? = null
        set(value) {
            doOnAttachedToScene { sceneView ->
                field?.let { sceneView.scene.removeLight(it) }
                field = value
                if (shouldBeRendered) {
                    field?.let { sceneView.scene.addLight(it) }
                }
            }
        }

    /**
     * ### Construct a [LightNode] with it Position, Rotation and Scale
     *
     * @param position See [LightNode.position]
     * @param direction See [LightNode.direction]
     */
    constructor(
        position: Position = DEFAULT_POSITION,
        direction: Direction = DEFAULT_DIRECTION
    ) : super(position) {
        this.direction = direction
    }

    /**
     * TODO : Doc
     */
    constructor(light: Light) : this() {
        this.light = light
    }

    /**
     * ## Smooth move, rotate and scale at a specified speed
     */
    fun smooth(
        position: Position = this.position,
        direction: Direction = this.direction,
        speed: Float = this.smoothSpeed
    ) {
        smoothSpeed = speed
        smoothTransform = lookTowards(eye = position, forward = direction,up = Direction(y = 1.0f))
    }

    override fun updateTransform(worldTransform: Transform) {
        super.updateTransform(worldTransform)
        if (light?.position != worldTransform.position) {
            light?.position = worldTransform.position
        }
        if (light?.direction != worldTransform.forward) {
            light?.direction = worldTransform.forward
        }
    }

    /** ### Detach and destroy the node */
    override fun destroy() {
        super.destroy()
        light?.let {
            scene?.removeEntity(it)
            it.destroy()
        }
    }

    override fun clone() = copy(LightNode())

    fun copy(toNode: LightNode = LightNode()): LightNode = toNode.apply {
        super.copy(toNode)
        light = this@LightNode.light
    }

    companion object {
        val DEFAULT_POSITION get() = Position(x = 0.0f, y = 0.0f, z = 0.0f)
        val DEFAULT_DIRECTION = Direction(x = 0.0f, y = -1.0f, z = 0.0f)
    }
}