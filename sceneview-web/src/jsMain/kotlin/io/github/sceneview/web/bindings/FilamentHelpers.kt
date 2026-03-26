package io.github.sceneview.web.bindings

// -----------------------------------------------------------------------
// Helper functions for creating Filament.js array types.
//
// Filament.js uses number[] (JavaScript arrays) for float3, float4, mat4, etc.
// These helpers create the arrays in a Kotlin/JS-friendly way.
//
// NOTE: These MUST NOT be in the @JsModule("filament") file (Filament.kt)
// because non-external declarations are not allowed in @JsModule files.
// -----------------------------------------------------------------------

/**
 * Create a float3 array [x, y, z] for Filament.js API calls.
 *
 * Used by Camera.lookAt, LightManager.Builder.direction/position/color, etc.
 */
fun float3(x: Double, y: Double, z: Double): dynamic {
    val arr = js("[]")
    arr.push(x, y, z)
    return arr
}

/**
 * Create a float4 array [x, y, z, w] for Filament.js API calls.
 *
 * Used by Renderer.setClearOptions, Skybox.setColor, etc.
 */
fun float4(x: Double, y: Double, z: Double, w: Double): dynamic {
    val arr = js("[]")
    arr.push(x, y, z, w)
    return arr
}

/**
 * Create a viewport array [x, y, width, height] for View.setViewport().
 */
fun viewport(x: Int, y: Int, width: Int, height: Int): dynamic {
    val arr = js("[]")
    arr.push(x, y, width, height)
    return arr
}
