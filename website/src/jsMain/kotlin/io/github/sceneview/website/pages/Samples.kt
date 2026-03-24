package io.github.sceneview.website.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import io.github.sceneview.website.components.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private data class SampleEntry(val title: String, val description: String, val directory: String, val platform: String, val tags: List<String>)

private val samples = listOf(
    SampleEntry("Model Viewer", "Load and display a 3D model with HDR environment lighting and orbit camera controls.", "samples/model-viewer", "Android", listOf("3D", "glTF", "HDR")),
    SampleEntry("AR Model Viewer", "Tap on a detected plane to place a 3D model. Supports pinch-to-scale and rotate gestures.", "samples/ar-model-viewer", "Android", listOf("AR", "Planes", "Gestures")),
    SampleEntry("Camera Manipulator", "Orbit, pan, and zoom camera with configurable sensitivity and damping.", "samples/camera-manipulator", "Android", listOf("3D", "Camera")),
    SampleEntry("glTF Camera", "Use cameras embedded in a glTF file for cinematic scene playback.", "samples/gltf-camera", "Android", listOf("3D", "glTF", "Camera")),
    SampleEntry("AR Augmented Image", "Detect real-world images and overlay 3D content on them.", "samples/ar-augmented-image", "Android", listOf("AR", "Image Tracking")),
    SampleEntry("AR Cloud Anchor", "Create persistent, cross-device AR anchors using Google Cloud Anchors.", "samples/ar-cloud-anchor", "Android", listOf("AR", "Cloud")),
    SampleEntry("AR Point Cloud", "Visualize ARCore feature points as a real-time point cloud.", "samples/ar-point-cloud", "Android", listOf("AR", "Debug")),
    SampleEntry("Cross-Platform Model Viewer", "Side-by-side recipe showing identical 3D model viewers in Kotlin and Swift.", "samples/recipes/model-viewer", "Android + iOS", listOf("3D", "Cross-Platform")),
    SampleEntry("Cross-Platform AR", "Recipe for tap-to-place AR on both Android (ARCore) and iOS (ARKit).", "samples/recipes/ar-tap-to-place", "Android + iOS", listOf("AR", "Cross-Platform")),
    SampleEntry("Procedural Geometry", "Create cubes, spheres, cylinders, and custom shapes from code on both platforms.", "samples/recipes/procedural-geometry", "Android + iOS", listOf("3D", "Procedural", "Cross-Platform")),
    SampleEntry("Text Labels", "Render 3D text labels and billboards in your scene.", "samples/recipes/text-labels", "Android + iOS", listOf("3D", "Text", "Cross-Platform"))
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
    A(href = "https://github.com/sceneview/sceneview-android/tree/main/${sample.directory}", attrs = {
        attr("target", "_blank"); attr("rel", "noopener noreferrer")
        style {
            property("flex", "1 1 340px"); maxWidth(400.px)
            property("background-color", colorMode.surfaceContainer())
            property("border-radius", Shape.EXTRA_LARGE)
            padding(28.px)
            property("text-decoration", "none")
            property("border", "1px solid ${colorMode.outlineVariant()}")
            property("transition", Motion.HOVER_TRANSITION)
            display(DisplayStyle.Block)
            property("box-shadow", Elevation.LEVEL1)
        }
        classes("m3-sample-card")
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
