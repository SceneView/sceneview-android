package io.github.sceneview.website

import com.varabyte.kobweb.silk.init.InitSilk
import com.varabyte.kobweb.silk.init.InitSilkContext
import com.varabyte.kobweb.silk.theme.colors.ColorMode

// ── M3 Expressive Color Tokens ──────────────────────────────────────────────

object Colors {
    // Primary
    const val PRIMARY = "#1a73e8"
    const val PRIMARY_DARK = "#8ab4f8"
    const val ON_PRIMARY = "#ffffff"
    const val PRIMARY_CONTAINER_LIGHT = "#d3e3fd"
    const val PRIMARY_CONTAINER_DARK = "#004a77"

    // Secondary
    const val SECONDARY = "#5f6368"
    const val SECONDARY_DARK = "#9aa0a6"

    // Tertiary (purple accent)
    const val TERTIARY = "#8b5cf6"
    const val TERTIARY_DARK = "#a78bfa"

    // Surfaces
    const val SURFACE_LIGHT = "#fefefe"
    const val SURFACE_DARK = "#111827"
    const val SURFACE_CONTAINER_LIGHT = "#f1f3f4"
    const val SURFACE_CONTAINER_DARK = "#1f2937"
    const val SURFACE_CONTAINER_LOW_LIGHT = "#f8f9fa"
    const val SURFACE_CONTAINER_LOW_DARK = "#171f2e"

    // On Surface
    const val ON_SURFACE_LIGHT = "#1f2937"
    const val ON_SURFACE_DARK = "#f9fafb"
    const val ON_SURFACE_VARIANT_LIGHT = "#5f6368"
    const val ON_SURFACE_VARIANT_DARK = "#9ca3af"

    // Background
    const val BG_LIGHT = "#fefefe"
    const val BG_DARK = "#111827"

    // Code
    const val CODE_BG_LIGHT = "#f1f3f4"
    const val CODE_BG_DARK = "#1f2937"

    // Outline
    const val BORDER_LIGHT = "#dadce0"
    const val BORDER_DARK = "#374151"
    const val OUTLINE_VARIANT_LIGHT = "#e8eaed"
    const val OUTLINE_VARIANT_DARK = "#2d3748"

    // Error
    const val ERROR = "#dc2626"
    const val ERROR_DARK = "#f87171"
}

fun ColorMode.primary() = if (this == ColorMode.LIGHT) Colors.PRIMARY else Colors.PRIMARY_DARK
fun ColorMode.onPrimary() = Colors.ON_PRIMARY
fun ColorMode.primaryContainer() = if (this == ColorMode.LIGHT) Colors.PRIMARY_CONTAINER_LIGHT else Colors.PRIMARY_CONTAINER_DARK
fun ColorMode.secondary() = if (this == ColorMode.LIGHT) Colors.SECONDARY else Colors.SECONDARY_DARK
fun ColorMode.tertiary() = if (this == ColorMode.LIGHT) Colors.TERTIARY else Colors.TERTIARY_DARK
fun ColorMode.surface() = if (this == ColorMode.LIGHT) Colors.SURFACE_LIGHT else Colors.SURFACE_DARK
fun ColorMode.surfaceContainer() = if (this == ColorMode.LIGHT) Colors.SURFACE_CONTAINER_LIGHT else Colors.SURFACE_CONTAINER_DARK
fun ColorMode.surfaceContainerLow() = if (this == ColorMode.LIGHT) Colors.SURFACE_CONTAINER_LOW_LIGHT else Colors.SURFACE_CONTAINER_LOW_DARK
fun ColorMode.bg() = if (this == ColorMode.LIGHT) Colors.BG_LIGHT else Colors.BG_DARK
fun ColorMode.text() = if (this == ColorMode.LIGHT) Colors.ON_SURFACE_LIGHT else Colors.ON_SURFACE_DARK
fun ColorMode.text2() = if (this == ColorMode.LIGHT) Colors.ON_SURFACE_VARIANT_LIGHT else Colors.ON_SURFACE_VARIANT_DARK
fun ColorMode.codeBg() = if (this == ColorMode.LIGHT) Colors.CODE_BG_LIGHT else Colors.CODE_BG_DARK
fun ColorMode.border() = if (this == ColorMode.LIGHT) Colors.BORDER_LIGHT else Colors.BORDER_DARK
fun ColorMode.outlineVariant() = if (this == ColorMode.LIGHT) Colors.OUTLINE_VARIANT_LIGHT else Colors.OUTLINE_VARIANT_DARK
fun ColorMode.error() = if (this == ColorMode.LIGHT) Colors.ERROR else Colors.ERROR_DARK

// ── M3 Expressive Typography ────────────────────────────────────────────────

object Fonts {
    const val SYSTEM = "'Inter', 'Google Sans Text', system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"
    const val MONO = "'JetBrains Mono', 'Fira Code', 'Cascadia Code', 'SF Mono', Consolas, 'Liberation Mono', Menlo, monospace"
}

/** M3 Expressive type scale — larger display, bolder weights */
object TypeScale {
    // Display Large — hero headlines
    const val DISPLAY_LARGE_SIZE = "3.5625rem"  // 57px
    const val DISPLAY_LARGE_WEIGHT = "700"
    const val DISPLAY_LARGE_TRACKING = "-0.25px"
    const val DISPLAY_LARGE_LINE_HEIGHT = "1.12"

    // Headline Large — section titles
    const val HEADLINE_LARGE_SIZE = "2rem"  // 32px
    const val HEADLINE_LARGE_WEIGHT = "600"
    const val HEADLINE_LARGE_TRACKING = "-0.02em"
    const val HEADLINE_LARGE_LINE_HEIGHT = "1.25"

    // Title Large
    const val TITLE_LARGE_SIZE = "1.375rem"  // 22px
    const val TITLE_LARGE_WEIGHT = "500"
    const val TITLE_LARGE_LINE_HEIGHT = "1.4"

    // Body Large
    const val BODY_LARGE_SIZE = "1rem"  // 16px
    const val BODY_LARGE_LINE_HEIGHT = "1.75"

    // Label Large — chips, small labels
    const val LABEL_LARGE_SIZE = "0.875rem"  // 14px
    const val LABEL_LARGE_WEIGHT = "500"
    const val LABEL_LARGE_TRACKING = "0.04em"
}

// ── M3 Expressive Shape ─────────────────────────────────────────────────────

object Shape {
    const val EXTRA_LARGE = "28px"   // cards, dialogs
    const val LARGE = "16px"         // buttons, containers
    const val MEDIUM = "12px"        // chips, small cards
    const val SMALL = "8px"          // code blocks
    const val FULL = "9999px"        // FABs, pills
}

// ── M3 Expressive Motion ────────────────────────────────────────────────────

object Motion {
    /** M3 emphasized easing — spring-like feel */
    const val EMPHASIZED = "cubic-bezier(0.2, 0, 0, 1)"
    const val EMPHASIZED_DECELERATE = "cubic-bezier(0.05, 0.7, 0.1, 1)"
    const val EMPHASIZED_ACCELERATE = "cubic-bezier(0.3, 0, 0.8, 0.15)"

    const val DURATION_SHORT = "200ms"
    const val DURATION_MEDIUM = "300ms"
    const val DURATION_LONG = "500ms"

    /** Hover transition preset */
    const val HOVER_TRANSITION = "transform ${DURATION_SHORT} ${EMPHASIZED}, box-shadow ${DURATION_SHORT} ${EMPHASIZED}, background-color ${DURATION_SHORT} ${EMPHASIZED}"
}

// ── M3 Elevation (shadow tokens) ────────────────────────────────────────────

object Elevation {
    const val LEVEL0 = "none"
    const val LEVEL1 = "0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06)"
    const val LEVEL2 = "0 4px 6px rgba(0,0,0,0.07), 0 2px 4px rgba(0,0,0,0.06)"
    const val LEVEL3 = "0 10px 15px rgba(0,0,0,0.07), 0 4px 6px rgba(0,0,0,0.05)"
    const val LEVEL4 = "0 20px 25px rgba(0,0,0,0.08), 0 10px 10px rgba(0,0,0,0.04)"
}

@InitSilk
fun initTheme(@Suppress("UNUSED_PARAMETER") ctx: InitSilkContext) {
    // Theme colors are applied via CSS property() calls in components.
    // Silk palette customization left as default for compatibility.
}
