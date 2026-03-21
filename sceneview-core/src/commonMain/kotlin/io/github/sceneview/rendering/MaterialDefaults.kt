package io.github.sceneview.rendering

/**
 * Standard PBR material default values.
 *
 * These follow the physically-based rendering conventions used by Filament,
 * RealityKit, and most modern renderers. Platform-independent constants
 * that can be used as default parameter values across any renderer backend.
 */

/** Default metallic value (dielectric). */
const val kMaterialDefaultMetallic = 0.0f

/** Default roughness value (slightly smooth). */
const val kMaterialDefaultRoughness = 0.4f

/** Default reflectance at normal incidence (4% — typical for dielectrics). */
const val kMaterialDefaultReflectance = 0.5f

/** Render priority: default layer. */
const val PRIORITY_DEFAULT = 4

/** Render priority: rendered first (background). */
const val PRIORITY_FIRST = 0

/** Render priority: rendered last (foreground). */
const val PRIORITY_LAST = 7
