package io.github.sceneview.website.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import io.github.sceneview.website.components.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

private data class Release(val version: String, val date: String, val highlights: List<String>, val isLatest: Boolean = false)

private val releases = listOf(
    Release("3.4.0", "2026-03-25", listOf(
        "WebXR AR/VR support in the browser (sceneview-web + Filament.js)",
        "Desktop software 3D renderer (Compose Desktop — Windows/macOS/Linux)",
        "Unified android-demo Material 3 app (4 tabs, 14 interactive demos)",
        "Samples reorganized: 7 platform demos with {platform}-demo naming",
        "WASM target prepared in sceneview-core (blocked by kotlin-math)",
        "Branding: blue isometric cube logo, store asset checklist",
        "Android XR added to roadmap (Jetpack XR SceneCore)"
    ), isLatest = true),
    Release("3.3.0", "2026-03-24", listOf(
        "iOS support via SceneViewSwift (SwiftUI + RealityKit)",
        "Cross-platform recipes: model-viewer, AR, procedural geometry, text",
        "Kotlin Multiplatform core module with shared collision, math, animation",
        "New SceneViewDemo Android app (Play Store ready)",
        "MCP server for AI assistant integration"
    )),
    Release("3.2.0", "2025-12-15", listOf(
        "Kotlin Multiplatform migration of sceneview-core",
        "Shared collision system (Vector3, Quaternion, Matrix, Ray, Box, Sphere)",
        "Physics simulation (Euler integration, floor bounce)",
        "Animation system (spring, easing, lerp/slerp)"
    )),
    Release("3.1.0", "2025-09-20", listOf(
        "Jetpack Compose-first API redesign",
        "Declarative node system (ModelNode, GeometryNode, LightNode)",
        "Spring-based camera animations"
    )),
    Release("3.0.0", "2025-06-10", listOf(
        "Complete rewrite with Compose-first architecture",
        "Scene {} and ARScene {} composable entry points",
        "Filament engine integration with auto-lifecycle management"
    )),
    Release("2.2.1", "2024-11-01", listOf(
        "Bug fixes for ARCore session management",
        "Improved model loading performance",
        "Updated Filament to latest stable"
    ))
)

@Page
@Composable
fun ChangelogPage() {
    PageHead(
        title = "Changelog",
        description = "SceneView release history: iOS support, Kotlin Multiplatform, Compose-first API, and more. Track every version update.",
        path = "/changelog/"
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
            }) { Text("Changelog") }

            P(attrs = {
                style {
                    fontSize(1.125.cssRem)
                    color(Color(colorMode.text2()))
                    property("margin-bottom", "48px")
                    property("font-family", Fonts.SYSTEM)
                    property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                }
            }) { Text("Release history and notable changes.") }

            releases.forEach { release -> ReleaseCard(release, colorMode) }
        }
    }
}

@Composable
private fun ReleaseCard(release: Release, colorMode: ColorMode) {
    Div(attrs = {
        style {
            property("margin-bottom", "40px")
            property("padding-bottom", "40px")
            property("border-bottom", "1px solid ${colorMode.outlineVariant()}")
        }
    }) {
        Div(attrs = {
            style {
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                property("gap", "12px")
                property("margin-bottom", "16px")
                property("flex-wrap", "wrap")
            }
        }) {
            H2(attrs = {
                style {
                    fontSize(1.5.cssRem)
                    property("font-weight", "700")
                    color(Color(colorMode.text()))
                    margin(0.px)
                    property("font-family", Fonts.MONO)
                }
            }) { Text(release.version) }

            if (release.isLatest) {
                Span(attrs = {
                    style {
                        padding(4.px, 14.px)
                        property("border-radius", Shape.FULL)
                        fontSize(0.6875.cssRem)
                        property("font-weight", "600")
                        property("background-color", if (colorMode == ColorMode.LIGHT) "rgba(52,199,89,0.1)" else "rgba(52,199,89,0.2)")
                        color(Color("rgb(36,138,61)"))
                        property("font-family", Fonts.SYSTEM)
                    }
                }) { Text("Latest") }
            }
            Span(attrs = {
                style {
                    color(Color(colorMode.text2()))
                    property("font-size", TypeScale.LABEL_LARGE_SIZE)
                    property("font-family", Fonts.SYSTEM)
                }
            }) { Text(release.date) }
        }

        Ul(attrs = { style { margin(0.px); property("padding-left", "20px") } }) {
            release.highlights.forEach { item ->
                Li(attrs = {
                    style {
                        color(Color(colorMode.text2()))
                        property("font-size", TypeScale.BODY_LARGE_SIZE)
                        property("line-height", "1.8")
                        property("font-family", Fonts.SYSTEM)
                    }
                }) { Text(item) }
            }
        }
    }
}
