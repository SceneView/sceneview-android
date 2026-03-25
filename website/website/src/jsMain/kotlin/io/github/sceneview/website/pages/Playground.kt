package io.github.sceneview.website.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import io.github.sceneview.website.components.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private data class ModelOption(
    val name: String,
    val src: String,
    val cameraOrbit: String = "30deg 75deg 105%",
    val exposure: String = "1"
)

private val models = listOf(
    ModelOption(
        "Damaged Helmet",
        "/models/DamagedHelmet.glb",
        cameraOrbit = "30deg 75deg 105%"
    ),
    ModelOption(
        "Astronaut",
        "https://modelviewer.dev/shared-assets/models/Astronaut.glb",
        cameraOrbit = "0deg 75deg 105%"
    ),
    ModelOption(
        "Horse (Animated)",
        "https://modelviewer.dev/shared-assets/models/Horse.glb",
        cameraOrbit = "30deg 75deg 120%",
        exposure = "0.8"
    )
)

private const val KOTLIN_CODE_TEMPLATE = """Scene(
    modifier = Modifier.fillMaxSize(),
    engine = rememberEngine(),
    modelLoader = rememberModelLoader(engine),
    environment = rememberEnvironment(engine, "envs/studio.hdr")
) {
    ModelNode(
        modelInstance = rememberModelInstance(
            modelLoader, "models/%MODEL%"
        ),
        scaleToUnits = 1.0f
    )
}"""

private const val SWIFT_CODE_TEMPLATE = """SceneView(environment: .studio) {
    ModelNode(named: "%MODEL%")
        .scaleToUnits(1.0)
        .position(x: 0, y: 0, z: -1)
}"""

@Page
@Composable
fun PlaygroundPage() {
    PageHead(
        title = "Playground",
        description = "Interactive SceneView 3D playground: explore models, change environments, and test parameters directly in your browser.",
        path = "/playground/"
    )

    PageLayout {
        val colorMode by ColorMode.currentState
        var selectedModelIndex by remember { mutableStateOf(0) }
        var autoRotate by remember { mutableStateOf(true) }
        var showShadow by remember { mutableStateOf(true) }
        var selectedCodeTab by remember { mutableStateOf(0) }

        val selectedModel = models[selectedModelIndex]

        // Header
        Div(attrs = {
            style {
                maxWidth(1200.px)
                property("margin", "0 auto")
                padding(48.px, 24.px, 0.px, 24.px)
            }
        }) {
            H1(attrs = {
                style {
                    property("font-size", TypeScale.HEADLINE_LARGE_SIZE)
                    property("font-weight", TypeScale.HEADLINE_LARGE_WEIGHT)
                    color(Color(colorMode.text()))
                    property("margin-bottom", "8px")
                    property("font-family", Fonts.SYSTEM)
                    property("letter-spacing", TypeScale.HEADLINE_LARGE_TRACKING)
                }
            }) { Text("Playground") }

            P(attrs = {
                style {
                    fontSize(1.125.cssRem)
                    color(Color(colorMode.text2()))
                    property("margin-bottom", "32px")
                    property("font-family", Fonts.SYSTEM)
                    property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                }
            }) { Text("Interactive 3D viewer right in your browser. Pick a model, tweak settings, and see the equivalent code.") }
        }

        // Main viewer area
        Div(attrs = {
            style {
                maxWidth(1200.px)
                property("margin", "0 auto")
                padding(0.px, 24.px, 64.px, 24.px)
            }
        }) {
            // 3D Viewer container
            Div(attrs = {
                style {
                    width(100.percent)
                    property("height", "70vh")
                    property("min-height", "400px")
                    property("max-height", "700px")
                    property("border-radius", Shape.EXTRA_LARGE)
                    property("overflow", "hidden")
                    property("background-color", colorMode.surfaceContainer())
                    property("border", "1px solid ${colorMode.outlineVariant()}")
                    property("box-shadow", Elevation.LEVEL2)
                    property("margin-bottom", "24px")
                }
            }) {
                ModelViewer(
                    src = selectedModel.src,
                    alt = "Interactive 3D model: ${selectedModel.name}",
                    autoRotate = autoRotate,
                    cameraControls = true,
                    shadowIntensity = if (showShadow) "1" else "0",
                    exposure = selectedModel.exposure,
                    cameraOrbit = selectedModel.cameraOrbit,
                    autoplay = true,
                    styleMods = {
                        property("border-radius", Shape.EXTRA_LARGE)
                    }
                )
            }

            // Controls row
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    property("flex-wrap", "wrap")
                    property("gap", "16px")
                    alignItems(AlignItems.Center)
                    property("margin-bottom", "40px")
                }
            }) {
                // Model picker
                Div(attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        alignItems(AlignItems.Center)
                        property("gap", "8px")
                    }
                }) {
                    Span(attrs = {
                        style {
                            property("font-size", TypeScale.LABEL_LARGE_SIZE)
                            property("font-weight", TypeScale.LABEL_LARGE_WEIGHT)
                            color(Color(colorMode.text2()))
                            property("font-family", Fonts.SYSTEM)
                        }
                    }) { Text("Model") }

                    Div(attrs = {
                        style {
                            display(DisplayStyle.Flex)
                            property("border-radius", Shape.FULL)
                            property("border", "1px solid ${colorMode.border()}")
                            property("overflow", "hidden")
                            property("background-color", colorMode.surfaceContainer())
                        }
                    }) {
                        models.forEachIndexed { index, model ->
                            val isSelected = index == selectedModelIndex
                            Div(attrs = {
                                style {
                                    padding(8.px, 18.px)
                                    cursor("pointer")
                                    property("font-size", TypeScale.LABEL_LARGE_SIZE)
                                    property("font-weight", if (isSelected) "600" else TypeScale.LABEL_LARGE_WEIGHT)
                                    property("font-family", Fonts.SYSTEM)
                                    property("user-select", "none")
                                    property("transition", "all ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}")
                                    property("white-space", "nowrap")
                                    if (isSelected) {
                                        property("background-color", colorMode.primary())
                                        color(Color(Colors.ON_PRIMARY))
                                    } else {
                                        property("background-color", "transparent")
                                        color(Color(colorMode.text2()))
                                    }
                                }
                                onClick { selectedModelIndex = index }
                            }) { Text(model.name) }
                        }
                    }
                }

                // Toggle controls
                Div(attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        property("gap", "12px")
                        alignItems(AlignItems.Center)
                    }
                }) {
                    ToggleChip("Auto-rotate", autoRotate, colorMode) { autoRotate = !autoRotate }
                    ToggleChip("Shadow", showShadow, colorMode) { showShadow = !showShadow }
                }
            }

            // Code section — shows equivalent code for the current model
            Div(attrs = {
                style {
                    property("margin-bottom", "24px")
                }
            }) {
                H2(attrs = {
                    style {
                        property("font-size", TypeScale.TITLE_LARGE_SIZE)
                        property("font-weight", "600")
                        color(Color(colorMode.text()))
                        property("margin-bottom", "16px")
                        property("font-family", Fonts.SYSTEM)
                    }
                }) { Text("Equivalent code") }

                // Code tabs
                Div(attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        property("border-radius", Shape.FULL)
                        property("border", "1px solid ${colorMode.border()}")
                        property("overflow", "hidden")
                        property("margin-bottom", "16px")
                        property("background-color", colorMode.surfaceContainer())
                        property("display", "inline-flex")
                    }
                }) {
                    listOf("Kotlin", "Swift").forEachIndexed { index, label ->
                        val isSelected = index == selectedCodeTab
                        Div(attrs = {
                            style {
                                padding(8.px, 20.px)
                                cursor("pointer")
                                property("font-size", TypeScale.LABEL_LARGE_SIZE)
                                property("font-weight", if (isSelected) "600" else TypeScale.LABEL_LARGE_WEIGHT)
                                property("font-family", Fonts.SYSTEM)
                                property("user-select", "none")
                                property("transition", "all ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}")
                                if (isSelected) {
                                    property("background-color", colorMode.primary())
                                    color(Color(Colors.ON_PRIMARY))
                                } else {
                                    property("background-color", "transparent")
                                    color(Color(colorMode.text2()))
                                }
                            }
                            onClick { selectedCodeTab = index }
                        }) { Text(label) }
                    }
                }

                val modelFileName = selectedModel.src.substringAfterLast("/")
                when (selectedCodeTab) {
                    0 -> CodeBlock(
                        KOTLIN_CODE_TEMPLATE.replace("%MODEL%", modelFileName),
                        "Kotlin — Jetpack Compose"
                    )
                    1 -> CodeBlock(
                        SWIFT_CODE_TEMPLATE.replace("%MODEL%", modelFileName.replace(".glb", ".usdz")),
                        "Swift — SwiftUI"
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, isOn: Boolean, colorMode: ColorMode, onClick: () -> Unit) {
    Span(attrs = {
        style {
            padding(8.px, 18.px)
            property("border-radius", Shape.FULL)
            property("font-size", TypeScale.LABEL_LARGE_SIZE)
            property("font-weight", if (isOn) "600" else TypeScale.LABEL_LARGE_WEIGHT)
            cursor("pointer")
            property("font-family", Fonts.SYSTEM)
            property("user-select", "none")
            property("transition", "all ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}")
            if (isOn) {
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
