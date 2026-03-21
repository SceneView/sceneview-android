package io.github.sceneview.environment

/**
 * Cross-platform environment configuration.
 *
 * Holds IBL (Image-Based Lighting) and skybox data for a 3D scene.
 * The generic types allow platform-specific implementations:
 * - Android: Filament IndirectLight + Skybox
 * - iOS: SceneKit SCNMaterialProperty + SCNScene.background
 *
 * @param L platform-specific indirect light type
 * @param S platform-specific skybox type
 */
data class Environment<L, S>(
    /** IBL for physically-based lighting. */
    val indirectLight: L? = null,
    /** Background skybox. */
    val skybox: S? = null,
    /** Spherical harmonics coefficients for diffuse lighting. */
    val sphericalHarmonics: List<Float>? = null
)
