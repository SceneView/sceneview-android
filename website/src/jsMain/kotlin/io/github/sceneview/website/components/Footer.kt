package io.github.sceneview.website.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun Footer() {
    val colorMode by ColorMode.currentState

    Footer(attrs = {
        style {
            width(100.percent)
            property("background-color", colorMode.surfaceContainerLow())
            property("border-top", "1px solid ${colorMode.outlineVariant()}")
            property("margin-top", "80px")
        }
    }) {
        Div(attrs = {
            style {
                maxWidth(1200.px)
                width(100.percent)
                property("margin", "0 auto")
                padding(48.px, 24.px)
                display(DisplayStyle.Flex)
                property("flex-wrap", "wrap")
                property("gap", "48px")
            }
        }) {
            Div(attrs = { style { property("flex", "1 1 250px") } }) {
                P(attrs = {
                    style {
                        fontSize(1.25.cssRem)
                        property("font-weight", "700")
                        color(Color(colorMode.primary()))
                        property("font-family", Fonts.SYSTEM)
                        property("margin", "0 0 8px 0")
                    }
                }) { Text("SceneView") }
                P(attrs = {
                    style {
                        color(Color(colorMode.text2()))
                        property("font-size", TypeScale.BODY_LARGE_SIZE)
                        property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                        margin(0.px)
                        property("font-family", Fonts.SYSTEM)
                    }
                }) {
                    Text("3D and AR as declarative UI.")
                    Br()
                    Text("Android, iOS, macOS, visionOS.")
                }
            }

            FooterColumn("Resources", listOf(
                "Documentation" to "/quickstart",
                "Samples" to "/samples",
                "Changelog" to "/changelog",
                "llms.txt" to "/llms.txt"
            ), colorMode)

            FooterColumn("Community", listOf(
                "GitHub" to "https://github.com/sceneview/sceneview",
                "Discord" to "https://discord.gg/UbNDDBTNqb",
                "Stack Overflow" to "https://stackoverflow.com/questions/tagged/sceneview"
            ), colorMode)
        }

        Div(attrs = {
            style {
                maxWidth(1200.px)
                width(100.percent)
                property("margin", "0 auto")
                padding(24.px)
                property("border-top", "1px solid ${colorMode.outlineVariant()}")
            }
        }) {
            P(attrs = {
                style {
                    color(Color(colorMode.text2()))
                    fontSize(0.8125.cssRem)
                    margin(0.px)
                    property("font-family", Fonts.SYSTEM)
                }
            }) { Text("Copyright 2024-2026 SceneView contributors. Apache License 2.0.") }
        }
    }
}

@Composable
private fun FooterColumn(title: String, links: List<Pair<String, String>>, colorMode: ColorMode) {
    Div(attrs = { style { property("flex", "0 1 200px") } }) {
        P(attrs = {
            style {
                property("font-size", TypeScale.LABEL_LARGE_SIZE)
                property("font-weight", "600")
                color(Color(colorMode.text2()))
                property("text-transform", "uppercase")
                property("letter-spacing", TypeScale.LABEL_LARGE_TRACKING)
                property("margin", "0 0 16px 0")
                property("font-family", Fonts.SYSTEM)
            }
        }) { Text(title) }

        links.forEach { (label, href) ->
            A(href = href, attrs = {
                if (href.startsWith("http")) {
                    attr("target", "_blank")
                    attr("rel", "noopener noreferrer")
                }
                style {
                    display(DisplayStyle.Block)
                    color(Color(colorMode.text()))
                    property("text-decoration", "none")
                    property("font-size", TypeScale.BODY_LARGE_SIZE)
                    padding(4.px, 0.px)
                    property("font-family", Fonts.SYSTEM)
                    property("transition", "color ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}")
                }
            }) { Text(label) }
        }
    }
}
