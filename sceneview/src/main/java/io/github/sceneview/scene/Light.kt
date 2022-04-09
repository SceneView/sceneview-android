package io.github.sceneview.scene

import com.google.android.filament.EntityInstance
import com.google.android.filament.LightManager
import io.github.sceneview.Filament
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.toDirection
import io.github.sceneview.math.toPosition
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.toColor

typealias Light = Int
typealias LightInstance = Int

typealias LightBuilder = LightManager.Builder

val lightManager: LightManager get() = Filament.lightManager

/**
 * @see LightManager.getInstance
 */
val Light.instance @EntityInstance get() : LightInstance = Filament.lightManager.getInstance(this)

/**
 * @see LightManager.Builder.build
 */
fun LightManager.Builder.build(): Light =
    Filament.entityManager.create().apply {
        build(Filament.engine, this)
    }

/**
 * @see LightManager.getType
 */
val Light.type: LightManager.Type
    get() = lightManager.getType(instance)

/**
 * @see LightManager.getPosition
 * @see LightManager.setPosition
 */
var Light.position: Position
    get() = FloatArray(3).apply {
        lightManager.getPosition(instance, this)
    }.toPosition()
    set(value) = lightManager.setPosition(instance, value.x, value.y, value.z)

/**
 * @see LightManager.getDirection
 * @see LightManager.setDirection
 */
var Light.direction: Direction
    get() = FloatArray(3).apply {
        lightManager.getDirection(instance, this)
    }.toDirection()
    set(value) = lightManager.setDirection(instance, value.x, value.y, value.z)

/**
 * @see LightManager.getIntensity
 * @see LightManager.setIntensity
 */
var Light.intensity: Float
    get() = lightManager.getIntensity(instance)
    set(value) = lightManager.setIntensity(instance, value)

/**
 * @see LightManager.getColor
 * @see LightManager.setColor
 */
var Light.color: Color
    get() = FloatArray(3).apply {
        lightManager.getColor(instance, this)
    }.toColor()
    set(value) = lightManager.setColor(instance, value.r, value.g, value.b)

/**
 * @see LightManager.isShadowCaster
 * @see LightManager.setShadowCaster
 */
var Light.isShadowCaster: Boolean
    get() = lightManager.isShadowCaster(instance)
    set(value) = lightManager.setShadowCaster(instance, value)

/**
 * @see LightManager.getFalloff
 * @see LightManager.setFalloff
 */
var Light.falloff: Float
    get() = lightManager.getFalloff(instance)
    set(value) = lightManager.setFalloff(instance, value)

/**
 * @see LightManager.getSunHaloFalloff
 * @see LightManager.setSunHaloFalloff
 */
var Light.sunHaloFalloff: Float
    get() = lightManager.getSunHaloFalloff(instance)
    set(value) = lightManager.setSunHaloFalloff(instance, value)

/**
 * @see LightManager.getSunHaloSize
 * @see LightManager.setSunHaloSize
 */
var Light.sunHaloSize: Float
    get() = lightManager.getSunHaloSize(instance)
    set(value) = lightManager.setSunHaloSize(instance, value)

/**
 * @see LightManager.getSunAngularRadius
 * @see LightManager.setSunAngularRadius
 */
var Light.sunAngularRadius: Float
    get() = lightManager.getSunAngularRadius(instance)
    set(value) = lightManager.setSunAngularRadius(instance, value)

/**
 * @see LightManager.getInnerConeAngle
 * @see LightManager.setSpotLightCone
 */
var Light.innerConeAngle: Float
    get() = lightManager.getInnerConeAngle(instance)
    set(value) = lightManager.setSpotLightCone(instance, value, outerConeAngle)

/**
 * @see LightManager.getOuterConeAngle
 * @see LightManager.setSpotLightCone
 */
var Light.outerConeAngle: Float
    get() = lightManager.getOuterConeAngle(instance)
    set(value) = lightManager.setSpotLightCone(instance, innerConeAngle, value)

// TODO: We need a clone on the Filament side in order to copy all values
fun Light.clone() = LightManager.Builder(type)
    .castShadows(isShadowCaster)
    .position(position.x, position.y, position.z)
    .direction(direction.x, direction.y, direction.z)
    .intensity(intensity)
    .color(color.r, color.g, color.b)
    .falloff(falloff)
    .sunHaloFalloff(sunHaloFalloff)
    .sunHaloSize(sunHaloSize)
    .sunAngularRadius(sunAngularRadius)
    .spotLightCone(innerConeAngle, outerConeAngle)
    .build()

/**
 * Destroys a Light and frees all its associated resources.
 */
fun Light.destroy() {
    lightManager.destroy(this)
}