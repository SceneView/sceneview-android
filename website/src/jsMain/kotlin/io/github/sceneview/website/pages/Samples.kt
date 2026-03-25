package io.github.sceneview.website.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import io.github.sceneview.website.components.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private data class SampleEntry(
    val title: String,
    val description: String,
    val directory: String,
    val platform: String,
    val tags: List<String>,
    val modelUrl: String? = null
)

private val samples = listOf(
    // Platform showcase apps
    SampleEntry("Android Demo", "Unified showcase: 3D viewer, AR tap-to-place, 14 interactive demos, Material 3 Expressive.", "samples/android-demo", "Android", listOf("3D", "AR", "Material 3"), modelUrl = "/models/DamagedHelmet.glb"),
    SampleEntry("iOS Demo", "SwiftUI 3-tab app: 3D scenes, AR with ARKit, sample gallery.", "samples/ios-demo", "iOS", listOf("3D", "AR", "SwiftUI")),
    SampleEntry("Web Demo", "Browser 3D viewer with Filament.js (WASM) and WebXR AR/VR support.", "samples/web-demo", "Web", listOf("3D", "WebXR", "Filament.js")),
    SampleEntry("Desktop Demo", "Software 3D renderer: wireframe cube, octahedron, diamond. Compose Desktop.", "samples/desktop-demo", "Desktop", listOf("3D", "Compose Desktop")),
    SampleEntry("Android TV Demo", "D-pad controlled 3D viewer with model cycling and auto-rotation.", "samples/android-tv-demo", "Android TV", listOf("3D", "TV", "D-pad")),
    SampleEntry("Flutter Demo", "PlatformView bridge to SceneView on Android and iOS.", "samples/flutter-demo", "Flutter", listOf("3D", "Flutter", "Cross-Platform")),
    SampleEntry("React Native Demo", "Fabric bridge to SceneView on Android and iOS.", "samples/react-native-demo", "React Native", listOf("3D", "React Native", "Cross-Platform")),
    // Cross-platform recipes
    SampleEntry("Model Viewer Recipe", "Side-by-side recipe: identical 3D model viewers in Kotlin and Swift.", "samples/recipes/model-viewer.md", "Android + iOS", listOf("3D", "Cross-Platform", "Recipe"), modelUrl = "/models/DamagedHelmet.glb"),
    SampleEntry("AR Tap-to-Place Recipe", "Recipe for tap-to-place AR on both Android (ARCore) and iOS (ARKit).", "samples/recipes/ar-tap-to-place.md", "Android + iOS", listOf("AR", "Cross-Platform", "Recipe")),
    SampleEntry("Physics Recipe", "Rigid body physics with gravity, collision, and bounce on both platforms.", "samples/recipes/physics.md", "Android + iOS", listOf("3D", "Physics", "Recipe")),
    SampleEntry("Procedural Geometry Recipe", "Create cubes, spheres, cylinders, and custom shapes from code.", "samples/recipes/procedural-geometry.md", "Android + iOS", listOf("3D", "Procedural", "Recipe"))
)

@Page
@Composable
fun SamplesPage() {
    PageHead(
        title = "Samples",
        description = "Explore working SceneView examples: 3D model viewers, AR tap-to-place, procedural geometry, cross-platform recipes for Android and iOS.",
        path = "/samples/"
    )

    PageLayout {
        val colorMode by ColorMode.currentState

        Div(attrs = { style { maxWidth(1200.px); property("margin", "0 auto"); padding(64.px, 24.px) } }) {
            H1(attrs = {
                style {
                    property("font-size", TypeScale.HEADLINE_LARGE_SIZE)
                    property("font-weight", TypeScale.HEADLINE_LARGE_WEIGHT)
                    color(Color(colorMode.text()))
                    property("margin-bottom", "8px")
                    property("font-family", Fonts.SYSTEM)
                    property("letter-spacing", TypeScale.HEADLINE_LARGE_TRACKING)
                }
            }) { Text("Samples") }

            P(attrs = {
                style {
                    fontSize(1.125.cssRem)
                    color(Color(colorMode.text2()))
                    property("margin-bottom", "48px")
                    property("font-family", Fonts.SYSTEM)
                    property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                }
            }) {
                Text("Explore working examples for every feature. Each sample is a self-contained project you can build and run.")
            }

            var filter by remember { mutableStateOf<String?>(null) }
            val allTags = samples.flatMap { it.tags }.distinct().sorted()

            // M3 filter chips
            Div(attrs = { style { display(DisplayStyle.Flex); property("flex-wrap", "wrap"); property("gap", "8px"); property("margin-bottom", "32px") } }) {
                FilterChip("All", filter == null, colorMode) { filter = null }
                allTags.forEach { tag -> FilterChip(tag, filter == tag, colorMode) { filter = tag } }
            }

            val filtered = if (filter == null) samples else samples.filter { filter in it.tags }

            Div(attrs = { style { display(DisplayStyle.Flex); property("flex-wrap", "wrap"); property("gap", "24px") } }) {
                filtered.forEach { sample -> SampleCard(sample, colorMode) }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean, colorMode: ColorMode, onClick: () -> Unit) {
    Span(attrs = {
        style {
            padding(8.px, 18.px)
            property("border-radius", Shape.FULL)
            property("font-size", TypeScale.LABEL_LARGE_SIZE)
            property("font-weight", if (isSelected) "600" else TypeScale.LABEL_LARGE_WEIGHT)
            cursor("pointer")
            property("font-family", Fonts.SYSTEM)
            property("user-select", "none")
            property("transition", "all ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}")
            if (isSelected) {
                property("background-color", colorMode.primary())
                color(Color(Colors.ON_PRIMARY))
            } else {
                property("background-color", colorMode.surfaceContainer())
                color(Color(colorMode.text2()))
                property("border", "1px solid ${colorMode.outlineVariant()}")
            }
        }
        onClick { onClick() }
    }) { Text(label) }
}

@Composable
private fun SampleCard(sample: SampleEntry, colorMode: ColorMode) {
    A(href = "https://github.com/sceneview/sceneview/tree/main/${sample.directory}", attrs = {
        attr("target", "_blank"); attr("rel", "noopener noreferrer")
        style {
            property("flex", "1 1 340px"); maxWidth(400.px)
            property("background-color", colorMode.surfaceContainer())
            property("border-radius", Shape.EXTRA_LARGE)
            property("text-decoration", "none")
            property("border", "1px solid ${colorMode.outlineVariant()}")
            property("transition", Motion.HOVER_TRANSITION)
            display(DisplayStyle.Block)
            property("box-shadow", Elevation.LEVEL1)
            property("overflow", "hidden")
        }
        classes("m3-sample-card")
    }) {
        // 3D preview if a model URL is available
        if (sample.modelUrl != null) {
            CardModelViewer(
                src = sample.modelUrl,
                alt = "3D preview for ${sample.title} sample"
            )
        }

        // Card content with padding
        Div(attrs = {
            style { padding(28.px) }
        }) {

        // Platform badge
        Span(attrs = {
            style {
                display(DisplayStyle.InlineBlock)
                padding(4.px, 12.px)
                property("border-radius", Shape.FULL)
                fontSize(0.6875.cssRem)
                property("font-weight", "600")
                property("margin-bottom", "12px")
                property("font-family", Fonts.SYSTEM)
                if (sample.platform.contains("iOS")) {
                    property("background-color", if (colorMode == ColorMode.LIGHT) "rgba(52,199,89,0.1)" else "rgba(52,199,89,0.2)")
                    color(Color("rgb(36,138,61)"))
                } else {
                    property("background-color", if (colorMode == ColorMode.LIGHT) "rgba(26,115,232,0.1)" else "rgba(138,180,248,0.15)")
                    color(Color(colorMode.primary()))
                }
            }
        }) { Text(sample.platform) }

        H3(attrs = {
            style {
                property("font-size", TypeScale.TITLE_LARGE_SIZE)
                property("font-weight", "600")
                color(Color(colorMode.text()))
                property("margin", "0 0 8px 0")
                property("font-family", Fonts.SYSTEM)
            }
        }) { Text(sample.title) }

        P(attrs = {
            style {
                color(Color(colorMode.text2()))
                property("font-size", TypeScale.BODY_LARGE_SIZE)
                property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                property("margin", "0 0 16px 0")
                property("font-family", Fonts.SYSTEM)
            }
        }) { Text(sample.description) }

        Div(attrs = { style { display(DisplayStyle.Flex); property("flex-wrap", "wrap"); property("gap", "6px") } }) {
            sample.tags.forEach { tag ->
                Span(attrs = {
                    style {
                        padding(2.px, 10.px)
                        property("border-radius", Shape.SMALL)
                        fontSize(0.6875.cssRem)
                        property("background-color", colorMode.codeBg())
                        color(Color(colorMode.text2()))
                        property("font-family", Fonts.MONO)
                    }
                }) { Text(tag) }
            }
        }

        } // Close card content Div wrapper
    }

    Style {
        """
        .m3-sample-card:hover {
            transform: translateY(-2px) scale(1.01);
            box-shadow: ${Elevation.LEVEL3};
        }
        """.trimIndent()
    }
}
