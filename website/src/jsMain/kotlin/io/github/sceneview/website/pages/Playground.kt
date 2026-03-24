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
fun PlaygroundPage() {
    PageHead(
        title = "Playground",
        description = "Interactive SceneView 3D playground: explore models, change environments, and test parameters directly in your browser.",
        path = "/playground/"
    )

    PageLayout {
        val colorMode by ColorMode.currentState

        Div(attrs = {
            style {
                maxWidth(840.px)
                property("margin", "0 auto")
                padding(64.px, 24.px)
                property("text-align", "center")
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
                    property("margin-bottom", "64px")
                    property("font-family", Fonts.SYSTEM)
                    property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                }
            }) { Text("Interactive 3D viewer right in your browser.") }

            // M3 elevated container
            Div(attrs = {
                style {
                    width(100.percent)
                    maxWidth(720.px)
                    property("margin", "0 auto")
                    padding(80.px, 32.px)
                    property("border-radius", Shape.EXTRA_LARGE)
                    property("background-color", colorMode.surfaceContainer())
                    property("border", "2px dashed ${colorMode.outlineVariant()}")
                    property("box-shadow", Elevation.LEVEL1)
                }
            }) {
                H2(attrs = {
                    style {
                        property("font-size", TypeScale.TITLE_LARGE_SIZE)
                        property("font-weight", "600")
                        color(Color(colorMode.text()))
                        property("margin-bottom", "12px")
                        property("font-family", Fonts.SYSTEM)
                    }
                }) { Text("3D Viewer Coming Soon") }

                P(attrs = {
                    style {
                        color(Color(colorMode.text2()))
                        property("font-size", TypeScale.BODY_LARGE_SIZE)
                        property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                        maxWidth(480.px)
                        property("margin", "0 auto")
                        property("font-family", Fonts.SYSTEM)
                    }
                }) { Text("An interactive Filament.js viewer will let you explore 3D models, change environments, and test parameters directly in the browser. Stay tuned.") }
            }
        }
    }
}
