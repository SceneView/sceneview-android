@file:JvmName("SceneviewMathKt")

package io.github.sceneview.math

import com.google.android.filament.Box

// Re-export all portable math from sceneview-core
// Type aliases, conversions, comparisons, transforms, etc. are all in sceneview-core commonMain.
// This file only contains Android/Filament/Compose-specific extensions.

fun Box(center: Position, halfExtent: Size) = Box(center.toFloatArray(), halfExtent.toFloatArray())
var Box.centerPosition: Position
    get() = center.toPosition()
    set(value) {
        setCenter(value.x, value.y, value.z)
    }
var Box.halfExtentSize: Size
    get() = halfExtent.toSize()
    set(value) {
        setHalfExtent(value.x, value.y, value.z)
    }

fun Box.toVector3Box(): io.github.sceneview.collision.Box =
    io.github.sceneview.collision.Box(
        (halfExtentSize * 2.0f).toVector3(),
        centerPosition.toVector3()
    )

fun colorOf(color: androidx.compose.ui.graphics.Color) = colorOf(
    r = color.red,
    g = color.green,
    b = color.blue,
    a = color.alpha
)

fun colorOf(color: Int) = colorOf(
    r = android.graphics.Color.red(color) / 255.0f,
    g = android.graphics.Color.green(color) / 255.0f,
    b = android.graphics.Color.blue(color) / 255.0f,
    a = android.graphics.Color.alpha(color) / 255.0f
)

// Color.toLinearSpace() is now in sceneview-core (io.github.sceneview.math.Color.kt)
// using the precise piecewise sRGB transfer function.
