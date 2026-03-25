package io.github.sceneview.website.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import io.github.sceneview.website.components.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private const val KOTLIN_EXAMPLE = """Scene(
    modifier = Modifier.fillMaxSize(),
    engine = rememberEngine(),
    modelLoader = rememberModelLoader(engine),
    environment = rememberEnvironment(engine, "envs/studio.hdr")
) {
    ModelNode(
        modelInstance = rememberModelInstance(
            modelLoader, "models/car.glb"
        ),
        scaleToUnits = 1.0f
    )
}"""

private const val SWIFT_EXAMPLE = """SceneView(environment: .studio) {
    ModelNode(named: "car.usdz")
        .scaleToUnits(1.0)
        .position(x: 0, y: 0, z: -1)
}"""

@Page
@Composable
fun HomePage() {
    PageHead(
        title = "SceneView",
        description = "Open-source 3D and AR SDK for Android (Jetpack Compose + Filament) and iOS (SwiftUI + RealityKit). Build immersive experiences with declarative UI.",
        path = "/"
    )
    // Inject JSON-LD structured data
    StructuredData()

    PageLayout {
        HeroSection()
        CodeExampleSection()
        FeaturesSection()
        InstallSection()
        CommunitySection()
    }
}

@Composable
private fun StructuredData() {
    LaunchedEffect(Unit) {
        val existing = kotlinx.browser.document.getElementById("structured-data-jsonld")
        if (existing == null) {
            val script = kotlinx.browser.document.createElement("script")
            script.setAttribute("type", "application/ld+json")
            script.id = "structured-data-jsonld"
            script.textContent = """{
  "@context": "https://schema.org",
  "@type": "SoftwareApplication",
  "name": "SceneView",
  "description": "Open-source 3D and AR SDK for Android (Jetpack Compose) and iOS (SwiftUI)",
  "url": "https://sceneview.github.io/",
  "applicationCategory": "DeveloperApplication",
  "operatingSystem": "Android, iOS, macOS, visionOS, Web, Windows, Linux, Android TV",
  "softwareVersion": "3.4.0",
  "author": {"@type": "Organization", "name": "SceneView", "url": "https://github.com/SceneView"},
  "offers": {"@type": "Offer", "price": "0", "priceCurrency": "USD"},
  "license": "https://opensource.org/licenses/Apache-2.0"
}"""
            kotlinx.browser.document.head?.appendChild(script)
        }
    }
}

@Composable
private fun HeroSection() {
    val colorMode by ColorMode.currentState

    Section(attrs = {
        style {
            width(100.percent)
            property("padding-top", "100px")
            property("padding-bottom", "100px")
            // M3 tonal background gradient
            property("background",
                if (colorMode == ColorMode.LIGHT)
                    "linear-gradient(180deg, ${Colors.PRIMARY_CONTAINER_LIGHT}33 0%, ${Colors.SURFACE_LIGHT} 100%)"
                else
                    "linear-gradient(180deg, ${Colors.PRIMARY_CONTAINER_DARK}33 0%, ${Colors.SURFACE_DARK} 100%)"
            )
        }
    }) {
        // Split layout: text left, 3D viewer right
        Div(attrs = {
            style {
                maxWidth(1200.px)
                property("margin", "0 auto")
                padding(0.px, 24.px)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                property("gap", "48px")
                property("flex-wrap", "wrap")
            }
        }) {
            // Left side — text content
            Div(attrs = {
                style {
                    property("flex", "1 1 400px")
                    property("min-width", "320px")
                }
                classes("hero-text")
            }) {
                // Version badge — M3 tonal chip
                Span(attrs = {
                    style {
                        display(DisplayStyle.InlineBlock)
                        padding(8.px, 20.px)
                        property("border-radius", Shape.FULL)
                        property("font-size", TypeScale.LABEL_LARGE_SIZE)
                        property("font-weight", TypeScale.LABEL_LARGE_WEIGHT)
                        color(Color(colorMode.primary()))
                        property("font-family", Fonts.SYSTEM)
                        property("background-color",
                            if (colorMode == ColorMode.LIGHT) "rgba(26,115,232,0.1)" else "rgba(138,180,248,0.15)"
                        )
                        property("margin-bottom", "32px")
                        property("border", "1px solid ${if (colorMode == ColorMode.LIGHT) "rgba(26,115,232,0.2)" else "rgba(138,180,248,0.2)"}")
                    }
                }) { Text("v3.4.0 — 9 platforms") }

                // M3 Display Large
                H1(attrs = {
                    style {
                        property("font-size", TypeScale.DISPLAY_LARGE_SIZE)
                        property("font-weight", TypeScale.DISPLAY_LARGE_WEIGHT)
                        color(Color(colorMode.text()))
                        property("margin", "0 0 20px 0")
                        property("font-family", Fonts.SYSTEM)
                        property("letter-spacing", TypeScale.DISPLAY_LARGE_TRACKING)
                        property("line-height", TypeScale.DISPLAY_LARGE_LINE_HEIGHT)
                    }
                }) { Text("SceneView") }

                // M3 Headline Large subtitle
                P(attrs = {
                    style {
                        property("font-size", TypeScale.HEADLINE_LARGE_SIZE)
                        color(Color(colorMode.text2()))
                        property("margin", "0 0 12px 0")
                        property("font-family", Fonts.SYSTEM)
                        property("font-weight", "400")
                        property("line-height", TypeScale.HEADLINE_LARGE_LINE_HEIGHT)
                    }
                }) { Text("3D and AR as declarative UI") }

                P(attrs = {
                    style {
                        fontSize(1.125.cssRem)
                        color(Color(colorMode.text2()))
                        property("margin", "0 0 40px 0")
                        property("font-family", Fonts.SYSTEM)
                        property("letter-spacing", "0.02em")
                    }
                }) { Text("Android · iOS · Web · Desktop · TV · Flutter · React Native") }

                Div(attrs = {
                    style {
                        display(DisplayStyle.Flex)
                        property("gap", "16px")
                        property("flex-wrap", "wrap")
                    }
                }) {
                    PrimaryButton("Get Started", "/quickstart")
                    SecondaryButton("View on GitHub", "https://github.com/SceneView/sceneview")
                }
            }

            // Right side — live 3D model viewer
            Div(attrs = {
                style {
                    property("flex", "1 1 420px")
                    property("min-width", "300px")
                    property("max-width", "560px")
                }
                classes("hero-viewer")
            }) {
                HeroModelViewer(
                    src = "/models/DamagedHelmet.glb",
                    alt = "Interactive 3D Damaged Helmet model — drag to orbit, scroll to zoom",
                    cameraOrbit = "30deg 75deg 105%"
                )

                // Caption below the viewer
                P(attrs = {
                    style {
                        property("text-align", "center")
                        property("font-size", TypeScale.LABEL_LARGE_SIZE)
                        color(Color(colorMode.text2()))
                        property("margin-top", "12px")
                        property("font-family", Fonts.SYSTEM)
                        property("opacity", "0.7")
                    }
                }) { Text("Drag to orbit  ·  Scroll to zoom") }
            }
        }
    }

    // Responsive: stack vertically on mobile
    Style {
        """
        @media (max-width: 860px) {
            .hero-text { text-align: center; }
            .hero-text > div { justify-content: center; }
            .hero-viewer { max-width: 100% !important; }
        }
        """.trimIndent()
    }
}

@Composable
private fun PrimaryButton(label: String, href: String) {
    A(href = href, attrs = {
        if (href.startsWith("http")) { attr("target", "_blank"); attr("rel", "noopener noreferrer") }
        style {
            display(DisplayStyle.InlineBlock)
            padding(14.px, 32.px)
            property("border-radius", Shape.LARGE)
            property("background", "linear-gradient(135deg, #1a73e8, #4285f4)")
            color(Color("white"))
            property("text-decoration", "none")
            property("font-size", TypeScale.BODY_LARGE_SIZE)
            property("font-weight", "600")
            property("font-family", Fonts.SYSTEM)
            property("transition", Motion.HOVER_TRANSITION)
            property("box-shadow", "0 2px 8px rgba(26,115,232,0.3)")
        }
        classes("m3-btn-primary")
    }) { Text(label) }

    Style {
        """
        .m3-btn-primary:hover {
            transform: scale(1.03);
            box-shadow: 0 4px 16px rgba(26,115,232,0.4);
            filter: brightness(1.05);
        }
        """.trimIndent()
    }
}

@Composable
private fun SecondaryButton(label: String, href: String) {
    val colorMode by ColorMode.currentState
    A(href = href, attrs = {
        if (href.startsWith("http")) { attr("target", "_blank"); attr("rel", "noopener noreferrer") }
        style {
            display(DisplayStyle.InlineBlock)
            padding(14.px, 32.px)
            property("border-radius", Shape.LARGE)
            property("background-color", "transparent")
            color(Color(colorMode.text()))
            property("text-decoration", "none")
            property("font-size", TypeScale.BODY_LARGE_SIZE)
            property("font-weight", "600")
            property("font-family", Fonts.SYSTEM)
            property("border", "1px solid ${colorMode.border()}")
            property("transition", Motion.HOVER_TRANSITION)
        }
        classes("m3-btn-secondary")
    }) { Text(label) }

    Style {
        """
        .m3-btn-secondary:hover {
            transform: scale(1.02);
            background-color: rgba(0,0,0,0.04);
        }
        """.trimIndent()
    }
}

@Composable
private fun CodeExampleSection() {
    val colorMode by ColorMode.currentState

    Section(attrs = { style { width(100.percent); padding(80.px, 0.px) } }) {
        Div(attrs = { style { maxWidth(1200.px); property("margin", "0 auto"); padding(0.px, 24.px) } }) {
            SectionTitle("Declarative 3D in a few lines")
            P(attrs = {
                style {
                    property("text-align", "center")
                    color(Color(colorMode.text2()))
                    fontSize(1.125.cssRem)
                    property("margin-bottom", "48px")
                    property("font-family", Fonts.SYSTEM)
                    property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                }
            }) { Text("Write 3D scenes as composable functions. Same mental model, different platforms.") }

            PlatformTabs(mapOf(
                Platform.ANDROID to { CodeBlock(KOTLIN_EXAMPLE, "Kotlin — Jetpack Compose") },
                Platform.IOS to { CodeBlock(SWIFT_EXAMPLE, "Swift — SwiftUI") }
            ))
        }
    }
}

@Composable
private fun FeaturesSection() {
    Section(attrs = { style { width(100.percent); padding(80.px, 0.px) } }) {
        Div(attrs = { style { maxWidth(1200.px); property("margin", "0 auto"); padding(0.px, 24.px) } }) {
            SectionTitle("Built for modern apps")
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    property("flex-wrap", "wrap")
                    property("gap", "24px")
                }
            }) {
                FeatureCard(
                    "Model Viewer",
                    "Load glTF/GLB and USDZ models with PBR materials, HDR environment lighting, and orbit camera controls."
                )
                FeatureCard(
                    "Augmented Reality",
                    "Place 3D objects on real surfaces with ARCore (Android) and ARKit (iOS). Plane detection, tap-to-place, image tracking."
                )
                FeatureCard(
                    "Procedural Geometry",
                    "Create cubes, spheres, cylinders, and custom shapes as composable nodes with full material support."
                )
                FeatureCard(
                    "Cross-Platform",
                    "Android with Filament, Apple with RealityKit. Shared logic through Kotlin Multiplatform. One SDK, every platform."
                )
            }
        }
    }
}

@Composable
private fun InstallSection() {
    val colorMode by ColorMode.currentState

    Section(attrs = { style { width(100.percent); padding(80.px, 0.px) } }) {
        Div(attrs = { style { maxWidth(1200.px); property("margin", "0 auto"); padding(0.px, 24.px) } }) {
            SectionTitle("Get started in seconds")

            var selectedTab by remember { mutableStateOf(0) }
            val tabs = listOf("Android 3D", "Android AR", "iOS (Swift)", "Web", "Desktop", "Flutter")

            // M3 segmented buttons
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    property("border-radius", Shape.FULL)
                    property("border", "1px solid ${colorMode.border()}")
                    property("overflow", "hidden")
                    property("margin-bottom", "20px")
                    property("background-color", colorMode.surfaceContainer())
                }
            }) {
                tabs.forEachIndexed { index, label ->
                    val isSelected = index == selectedTab
                    Div(attrs = {
                        style {
                            padding(10.px, 24.px)
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
                        onClick { selectedTab = index }
                    }) { Text(label) }
                }
            }

            when (selectedTab) {
                0 -> CodeBlock("// build.gradle.kts\ndependencies {\n    implementation(\"io.github.sceneview:sceneview:3.4.0\")\n}", "Gradle (Kotlin DSL)")
                1 -> CodeBlock("// build.gradle.kts\ndependencies {\n    implementation(\"io.github.sceneview:arsceneview:3.4.0\")\n}", "Gradle (Kotlin DSL)")
                2 -> CodeBlock("// Package.swift\ndependencies: [\n    .package(\n        url: \"https://github.com/SceneView/SceneViewSwift.git\",\n        from: \"3.3.0\"\n    )\n]", "Swift Package Manager")
                3 -> CodeBlock("// build.gradle.kts (Kotlin/JS)\ndependencies {\n    implementation(\"io.github.sceneview:sceneview-web:3.4.0\")\n}", "Gradle (Kotlin DSL)")
                4 -> CodeBlock("// Run with Compose Desktop\n./gradlew :samples:desktop-demo:run", "Terminal")
                5 -> CodeBlock("# pubspec.yaml\ndependencies:\n  sceneview_flutter: ^3.4.0", "Flutter (Dart)")
            }
        }
    }
}

@Composable
private fun CommunitySection() {
    val colorMode by ColorMode.currentState

    Section(attrs = {
        style {
            width(100.percent)
            padding(80.px, 0.px)
            property("text-align", "center")
            // M3 tonal background
            property("background-color", colorMode.surfaceContainerLow())
            property("border-radius", "${Shape.EXTRA_LARGE} ${Shape.EXTRA_LARGE} 0 0")
        }
    }) {
        Div(attrs = { style { maxWidth(600.px); property("margin", "0 auto"); padding(0.px, 24.px) } }) {
            SectionTitle("Join the community")
            P(attrs = {
                style {
                    color(Color(colorMode.text2()))
                    fontSize(1.125.cssRem)
                    property("margin-bottom", "32px")
                    property("font-family", Fonts.SYSTEM)
                    property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                }
            }) { Text("Get help, share projects, and contribute to the SDK.") }

            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    justifyContent(JustifyContent.Center)
                    property("gap", "16px")
                    property("flex-wrap", "wrap")
                }
            }) {
                SecondaryButton("Discord", "https://discord.gg/UbNDDBTNqb")
                SecondaryButton("GitHub Discussions", "https://github.com/SceneView/sceneview/discussions")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    val colorMode by ColorMode.currentState
    H2(attrs = {
        style {
            property("text-align", "center")
            property("font-size", TypeScale.HEADLINE_LARGE_SIZE)
            property("font-weight", TypeScale.HEADLINE_LARGE_WEIGHT)
            color(Color(colorMode.text()))
            property("margin-bottom", "16px")
            property("font-family", Fonts.SYSTEM)
            property("letter-spacing", TypeScale.HEADLINE_LARGE_TRACKING)
            property("line-height", TypeScale.HEADLINE_LARGE_LINE_HEIGHT)
        }
    }) { Text(text) }
}
