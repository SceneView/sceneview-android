package io.github.sceneview.components

import com.google.android.filament.LightManager

/**
 * Configuration for shadow quality on a light source.
 *
 * Wraps Filament's [LightManager.ShadowOptions] for a more Kotlin-idiomatic API that is safe
 * to use from Compose `apply` blocks.
 *
 * ```kotlin
 * LightNode(
 *     type = LightManager.Type.DIRECTIONAL,
 *     apply = {
 *         intensity(100_000f)
 *         castShadows(true)
 *     }
 * ).apply {
 *     shadowOptions = ShadowOptions(
 *         mapSize = 2048,
 *         shadowCascades = 4,
 *         constantBias = 0.001f,
 *         normalBias = 1.0f
 *     )
 * }
 * ```
 *
 * @param mapSize           Shadow map resolution in pixels (must be a power of two). Default 1024.
 * @param shadowCascades    Number of shadow cascades for directional lights (1-4). Default 1.
 * @param constantBias      Constant depth bias to reduce shadow acne. Default 0.001.
 * @param normalBias        Normal-direction bias to reduce shadow acne on sloped surfaces. Default 1.0.
 * @param shadowFar         Maximum shadow distance in world units. 0 = automatic. Default 0.
 * @param shadowNearHint    Near plane hint for shadow map frustum. Default 1.0.
 * @param shadowFarHint     Far plane hint for shadow map frustum. Default 100.0.
 * @param stable            Whether to stabilise shadow edges when the camera moves. Default false.
 * @param lispsm            Whether to use LiSPSM (Light Space Perspective Shadow Maps). Default true.
 * @param screenSpaceContactShadows Whether to enable screen-space contact shadows. Default false.
 * @param stepCount         Number of steps for screen-space contact shadows. Default 8.
 * @param maxShadowDistance Maximum distance for screen-space contact shadows in meters. Default 0.3.
 */
data class ShadowOptions(
    val mapSize: Int = 1024,
    val shadowCascades: Int = 1,
    val constantBias: Float = 0.001f,
    val normalBias: Float = 1.0f,
    val shadowFar: Float = 0f,
    val shadowNearHint: Float = 1.0f,
    val shadowFarHint: Float = 100.0f,
    val stable: Boolean = false,
    val lispsm: Boolean = true,
    val screenSpaceContactShadows: Boolean = false,
    val stepCount: Int = 8,
    val maxShadowDistance: Float = 0.3f
)

/**
 * Applies [ShadowOptions] to this [LightComponent]'s shadow configuration.
 *
 * This updates the Filament [LightManager.ShadowOptions] on the underlying light entity.
 * Must be called on the main thread (same as all Filament JNI operations).
 *
 * ```kotlin
 * lightNode.applyShadowOptions(ShadowOptions(mapSize = 2048, shadowCascades = 4))
 * ```
 */
fun LightComponent.applyShadowOptions(options: ShadowOptions) {
    val instance = lightInstance
    val lm = lightManager

    val filamentOptions = lm.getShadowOptions(instance)
    filamentOptions.mapSize = options.mapSize
    filamentOptions.shadowCascades = options.shadowCascades
    filamentOptions.constantBias = options.constantBias
    filamentOptions.normalBias = options.normalBias
    filamentOptions.shadowFar = options.shadowFar
    filamentOptions.shadowNearHint = options.shadowNearHint
    filamentOptions.shadowFarHint = options.shadowFarHint
    filamentOptions.stable = options.stable
    filamentOptions.lispsm = options.lispsm
    filamentOptions.screenSpaceContactShadows = options.screenSpaceContactShadows
    filamentOptions.stepCount = options.stepCount
    filamentOptions.maxShadowDistance = options.maxShadowDistance
    lm.setShadowOptions(instance, filamentOptions)
}

/**
 * The current [ShadowOptions] for this light.
 *
 * Reads from Filament and returns a Kotlin-idiomatic snapshot. Setting a new value applies it
 * to the underlying Filament light entity.
 */
var LightComponent.shadowOptions: ShadowOptions
    get() {
        val fo = lightManager.getShadowOptions(lightInstance)
        return ShadowOptions(
            mapSize = fo.mapSize,
            shadowCascades = fo.shadowCascades,
            constantBias = fo.constantBias,
            normalBias = fo.normalBias,
            shadowFar = fo.shadowFar,
            shadowNearHint = fo.shadowNearHint,
            shadowFarHint = fo.shadowFarHint,
            stable = fo.stable,
            lispsm = fo.lispsm,
            screenSpaceContactShadows = fo.screenSpaceContactShadows,
            stepCount = fo.stepCount,
            maxShadowDistance = fo.maxShadowDistance
        )
    }
    set(value) = applyShadowOptions(value)
