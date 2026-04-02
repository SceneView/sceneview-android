package io.github.sceneview.demo.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * SceneView M3 Expressive Shape System
 *
 * Radius tokens from DESIGN.md:
 * - ExtraSmall (8dp): chips, utility elements
 * - Small (12dp): buttons, text fields
 * - Medium (16dp): cards, dialogs
 * - Large (28dp): prominent cards, bottom sheets
 * - ExtraLarge (32dp): hero cards, large surfaces
 */
val SceneViewShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
