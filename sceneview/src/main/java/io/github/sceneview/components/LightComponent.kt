package io.github.sceneview.components

import com.google.android.filament.LightManager
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.managers.*
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.toColor

interface LightComponent : Component {

    val lightManager get() = engine.lightManager
    val lightInstance: EntityInstance get() = lightManager.getInstance(entity)

    val type: LightManager.Type get() = lightManager.getType(lightInstance)

    /**
     * The light's position in world space
     *
     * @see LightManager.getDirection
     * @see LightManager.setDirection
     */
    var lightPosition: Position
        get() = lightManager.getPosition(lightInstance)
        set(value) {
            lightManager.setPosition(lightInstance, value)
        }

    /**
     * The light's rotation in world space
     *
     * @see LightManager.getDirection
     * @see LightManager.setDirection
     */
    var lightQuaternion: Quaternion
        get() = lightManager.getQuaternion(lightInstance)
        set(value) {
            lightManager.setQuaternion(lightInstance, value)
        }

    /**
     * The light's direction in world space
     *
     * @see LightManager.getDirection
     * @see LightManager.setDirection
     */
    var lightDirection: Direction
        get() = lightManager.getDirection(lightInstance)
        set(value) {
            lightManager.setDirection(lightInstance, value)
        }

    /**
     * Dynamically updates the light's intensity
     *
     * The intensity can be negative.
     *
     * @see LightManager.getIntensity
     * @see LightManager.setIntensity
     */
    var intensity: Float
        get() = lightManager.getIntensity(lightInstance)
        set(value) = lightManager.setIntensity(lightInstance, value)

    /**
     * Dynamically updates the light's hue as linear sRGB
     *
     * @see LightManager.getColor
     * @see LightManager.setColor
     */
    var color: Color
        get() = FloatArray(3).apply {
            lightManager.getColor(lightInstance, this)
        }.toColor()
        set(value) = lightManager.setColor(lightInstance, value.r, value.g, value.b)

    /**
     * Whether this Light casts shadows (disabled by default)
     *
     * @see LightManager.isShadowCaster
     * @see LightManager.setShadowCaster
     */
    var isShadowCaster: Boolean
        get() = lightManager.isShadowCaster(lightInstance)
        set(value) = lightManager.setShadowCaster(lightInstance, value)

    /**
     * The falloff distance for point lights and spot lights
     *
     * @see LightManager.getFalloff
     * @see LightManager.setFalloff
     */
    var falloff: Float
        get() = lightManager.getFalloff(lightInstance)
        set(value) = lightManager.setFalloff(lightInstance, value)

    /**
     * Dynamically updates the halo falloff of a Type.SUN light
     *
     * The falloff is a dimensionless number used as an exponent.
     *
     * @see LightManager.getSunHaloFalloff
     * @see LightManager.setSunHaloFalloff
     */
    var sunHaloFalloff: Float
        get() = lightManager.getSunHaloFalloff(lightInstance)
        set(value) = lightManager.setSunHaloFalloff(lightInstance, value)

    /**
     * Dynamically updates the halo radius of a Type.SUN light
     *
     * The radius of the halo is defined as a multiplier of the sun angular radius.
     *
     * @see LightManager.getSunHaloSize
     * @see LightManager.setSunHaloSize
     */
    var sunHaloSize: Float
        get() = lightManager.getSunHaloSize(lightInstance)
        set(value) = lightManager.setSunHaloSize(lightInstance, value)

    /**
     * Dynamically updates the angular radius of a Type.SUN light
     *
     * The Sun as seen from Earth has an angular size of 0.526° to 0.545°
     *
     * @see LightManager.getSunAngularRadius
     * @see LightManager.setSunAngularRadius
     */
    var sunAngularRadius: Float
        get() = lightManager.getSunAngularRadius(lightInstance)
        set(value) = lightManager.setSunAngularRadius(lightInstance, value)

    /**
     * Dynamically updates a spot light's cone as angles
     *
     * @see LightManager.getInnerConeAngle
     * @see LightManager.setSpotLightCone
     */
    var innerConeAngle: Float
        get() = lightManager.getInnerConeAngle(lightInstance)
        set(value) = lightManager.setSpotLightCone(lightInstance, value, outerConeAngle)

    /**
     * Dynamically updates a spot light's cone as angles
     *
     * @see LightManager.getOuterConeAngle
     * @see LightManager.setSpotLightCone
     */
    var outerConeAngle: Float
        get() = lightManager.getOuterConeAngle(lightInstance)
        set(value) = lightManager.setSpotLightCone(lightInstance, innerConeAngle, value)

//    // TODO: We need a clone on the Filament side in order to copy all values
//    fun clone(engine: Engine, entity: Entity = EntityManager.get().create()) =
//        LightManager.Builder(type)
//            .castShadows(isShadowCaster)
//            .position(lightPosition)
//            .direction(lightDirection)
//            .intensity(intensity)
//            .color(color.r, color.g, color.b)
//            .falloff(falloff)
//            .sunHaloFalloff(sunHaloFalloff)
//            .sunHaloSize(sunHaloSize)
//            .sunAngularRadius(sunAngularRadius)
//            .spotLightCone(innerConeAngle, outerConeAngle)
//            .build(engine, entity)
}