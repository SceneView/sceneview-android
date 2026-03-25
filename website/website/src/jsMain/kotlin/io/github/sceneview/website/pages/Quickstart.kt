package io.github.sceneview.website.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import io.github.sceneview.website.components.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Page
@Composable
fun QuickstartPage() {
    PageHead(
        title = "Get Started",
        description = "Add 3D and AR to your Android or iOS app in under 5 minutes with SceneView. Step-by-step setup for Jetpack Compose and SwiftUI.",
        path = "/quickstart/"
    )

    PageLayout {
        val colorMode by ColorMode.currentState

        Div(attrs = { style { maxWidth(840.px); property("margin", "0 auto"); padding(64.px, 24.px) } }) {
            H1(attrs = {
                style {
                    property("font-size", TypeScale.HEADLINE_LARGE_SIZE)
                    property("font-weight", TypeScale.HEADLINE_LARGE_WEIGHT)
                    color(Color(colorMode.text()))
                    property("margin-bottom", "8px")
                    property("font-family", Fonts.SYSTEM)
                    property("letter-spacing", TypeScale.HEADLINE_LARGE_TRACKING)
                }
            }) { Text("Get Started") }

            P(attrs = {
                style {
                    fontSize(1.125.cssRem)
                    color(Color(colorMode.text2()))
                    property("margin-bottom", "48px")
                    property("font-family", Fonts.SYSTEM)
                    property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                }
            }) { Text("Add 3D and AR to your app in under 5 minutes.") }

            DocSection("Android — 3D Scene", colorMode) {
                Step("1", "Add the dependency", colorMode) {
                    CodeBlock("// build.gradle.kts\ndependencies {\n    implementation(\"io.github.sceneview:sceneview:3.4.0\")\n}", "Gradle")
                }
                Step("2", "Add a 3D model to your assets", colorMode) {
                    P(attrs = {
                        style {
                            color(Color(colorMode.text2()))
                            property("font-size", TypeScale.BODY_LARGE_SIZE)
                            property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                            margin(0.px)
                            property("font-family", Fonts.SYSTEM)
                        }
                    }) { Text("Place your .glb or .gltf file in src/main/assets/models/.") }
                }
                Step("3", "Create a Scene composable", colorMode) {
                    CodeBlock("@Composable\nfun ModelViewer() {\n    val engine = rememberEngine()\n    val modelLoader = rememberModelLoader(engine)\n\n    Scene(\n        modifier = Modifier.fillMaxSize(),\n        engine = engine,\n        modelLoader = modelLoader,\n        environment = rememberEnvironment(engine, \"envs/studio.hdr\")\n    ) {\n        ModelNode(\n            modelInstance = rememberModelInstance(\n                modelLoader, \"models/car.glb\"\n            ),\n            scaleToUnits = 1.0f\n        )\n    }\n}", "Kotlin")
                }
            }

            Div(attrs = { style { height(48.px) } }) {}

            DocSection("Android — Augmented Reality", colorMode) {
                Step("1", "Add the AR dependency", colorMode) {
                    CodeBlock("// build.gradle.kts\ndependencies {\n    implementation(\"io.github.sceneview:arsceneview:3.4.0\")\n}", "Gradle")
                }
                Step("2", "Add AR permissions to your manifest", colorMode) {
                    CodeBlock("<uses-permission android:name=\"android.permission.CAMERA\" />\n<uses-feature android:name=\"android.hardware.camera.ar\" android:required=\"true\" />\n\n<application ...>\n    <meta-data android:name=\"com.google.ar.core\" android:value=\"required\" />\n</application>", "XML")
                }
                Step("3", "Create an ARScene composable", colorMode) {
                    CodeBlock("@Composable\nfun ARModelViewer() {\n    val engine = rememberEngine()\n    val modelLoader = rememberModelLoader(engine)\n\n    ARScene(\n        modifier = Modifier.fillMaxSize(),\n        engine = engine,\n        modelLoader = modelLoader,\n        planeRenderer = true,\n        onTapAR = { hitResult ->\n            hitResult?.let {\n                ARModelNode(\n                    engine = engine,\n                    modelInstance = modelLoader.createModelInstance(\n                        \"models/robot.glb\"\n                    ),\n                    anchor = it.createAnchor()\n                )\n            }\n        }\n    )\n}", "Kotlin")
                }
            }

            Div(attrs = { style { height(48.px) } }) {}

            DocSection("iOS — SwiftUI", colorMode) {
                Step("1", "Add the Swift package", colorMode) {
                    CodeBlock("// In Xcode: File > Add Package Dependencies...\n// URL: https://github.com/SceneView/SceneViewSwift.git\n// Version: from 3.3.0", "Xcode")
                }
                Step("2", "Create a SceneView", colorMode) {
                    CodeBlock("import SwiftUI\nimport SceneViewSwift\n\nstruct ModelViewer: View {\n    var body: some View {\n        SceneView(environment: .studio) {\n            ModelNode(named: \"car.usdz\")\n                .scaleToUnits(1.0)\n                .position(x: 0, y: 0, z: -1)\n        }\n    }\n}", "Swift")
                }
            }

            Div(attrs = { style { height(48.px) } }) {}

            DocSection("Web — Kotlin/JS + Filament.js", colorMode) {
                Step("1", "Add the dependency", colorMode) {
                    CodeBlock("// build.gradle.kts (Kotlin/JS)\ndependencies {\n    implementation(\"io.github.sceneview:sceneview-web:3.4.0\")\n}", "Gradle")
                }
                Step("2", "Create a scene in Kotlin/JS", colorMode) {
                    CodeBlock("import io.github.sceneview.web.*\n\nfun main() {\n    SceneView(\"canvas\") {\n        camera {\n            position(0.0, 1.5, 5.0)\n            lookAt(0.0, 0.0, 0.0)\n        }\n        light {\n            type(LightType.SUN)\n            intensity(100_000.0)\n        }\n        model(\"models/helmet.glb\")\n    }\n}", "Kotlin")
                }
            }

            Div(attrs = { style { height(48.px) } }) {}

            DocSection("Flutter", colorMode) {
                Step("1", "Add the dependency", colorMode) {
                    CodeBlock("# pubspec.yaml\ndependencies:\n  sceneview_flutter: ^3.4.0", "YAML")
                }
                Step("2", "Use the widget", colorMode) {
                    CodeBlock("import 'package:sceneview_flutter/sceneview_flutter.dart';\n\nclass MyApp extends StatelessWidget {\n  @override\n  Widget build(BuildContext context) {\n    return SceneView(\n      model: 'assets/models/helmet.glb',\n      environment: 'assets/envs/studio.hdr',\n    );\n  }\n}", "Dart")
                }
            }

            Div(attrs = { style { height(48.px) } }) {}

            DocSection("Desktop — Compose Desktop", colorMode) {
                Step("1", "Run the demo", colorMode) {
                    CodeBlock("# Clone the repo and run\ngit clone https://github.com/SceneView/sceneview.git\ncd sceneview\n./gradlew :samples:desktop-demo:run", "Terminal")
                }
            }
        }
    }
}

@Composable
private fun DocSection(title: String, colorMode: ColorMode, content: @Composable () -> Unit) {
    Div(attrs = { style { property("margin-bottom", "32px") } }) {
        H2(attrs = {
            style {
                fontSize(1.75.cssRem)
                property("font-weight", "700")
                color(Color(colorMode.text()))
                property("margin-bottom", "24px")
                property("padding-bottom", "12px")
                property("border-bottom", "2px solid ${colorMode.primary()}")
                property("font-family", Fonts.SYSTEM)
            }
        }) { Text(title) }
        content()
    }
}

@Composable
private fun Step(number: String, title: String, colorMode: ColorMode, content: @Composable () -> Unit) {
    Div(attrs = { style { property("margin-bottom", "32px") } }) {
        Div(attrs = {
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                property("gap", "12px")
                property("margin-bottom", "16px")
            }
        }) {
            // M3 tonal step indicator
            Span(attrs = {
                style {
                    property("display", "inline-flex")
                    alignItems(AlignItems.Center)
                    justifyContent(JustifyContent.Center)
                    width(32.px)
                    height(32.px)
                    property("border-radius", Shape.FULL)
                    property("background-color", colorMode.primary())
                    color(Color(Colors.ON_PRIMARY))
                    property("font-size", TypeScale.LABEL_LARGE_SIZE)
                    property("font-weight", "700")
                    property("font-family", Fonts.SYSTEM)
                    property("flex-shrink", "0")
                }
            }) { Text(number) }
            H3(attrs = {
                style {
                    property("font-size", TypeScale.TITLE_LARGE_SIZE)
                    property("font-weight", "600")
                    color(Color(colorMode.text()))
                    margin(0.px)
                    property("font-family", Fonts.SYSTEM)
                }
            }) { Text(title) }
        }
        content()
    }
}
