package io.github.sceneview.website.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.components.icons.fa.FaBars
import com.varabyte.kobweb.silk.components.icons.fa.FaGithub
import com.varabyte.kobweb.silk.components.icons.fa.FaMoon
import com.varabyte.kobweb.silk.components.icons.fa.FaSun
import com.varabyte.kobweb.silk.components.icons.fa.IconSize
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun NavBar() {
    var colorMode by ColorMode.currentState
    var menuOpen by remember { mutableStateOf(false) }

    Nav(attrs = {
        style {
            position(Position.Sticky)
            top(0.px)
            width(100.percent)
            property("z-index", "1000")
            property("background-color", colorMode.surfaceContainer())
            property("border-bottom", "1px solid ${colorMode.outlineVariant()}")
            property("backdrop-filter", "blur(20px) saturate(1.8)")
            property("-webkit-backdrop-filter", "blur(20px) saturate(1.8)")
            property("transition", "box-shadow ${Motion.DURATION_MEDIUM} ${Motion.EMPHASIZED}")
        }
    }) {
        Div(attrs = {
            style {
                maxWidth(1200.px)
                width(100.percent)
                property("margin", "0 auto")
                padding(12.px, 24.px)
                display(DisplayStyle.Flex)
                alignItems(AlignItems.Center)
                property("gap", "32px")
            }
        }) {
            // Logo
            A(href = "/", attrs = {
                style {
                    fontSize(1.25.cssRem)
                    property("font-weight", "700")
                    color(Color(colorMode.primary()))
                    property("text-decoration", "none")
                    property("font-family", Fonts.SYSTEM)
                    property("letter-spacing", "-0.02em")
                    property("transition", "opacity ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}")
                }
            }) {
                Text("SceneView")
            }

            // Desktop links — M3 pill nav items
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    property("gap", "4px")
                    property("flex-grow", "1")
                }
                classes("nav-links")
            }) {
                NavLink("/quickstart", "Docs")
                NavLink("/samples", "Samples")
                NavLink("/playground", "Playground")
                NavLink("/changelog", "Changelog")
            }

            Div(attrs = { style { property("flex-grow", "1") } }) {}

            // Actions
            Div(attrs = {
                style {
                    display(DisplayStyle.Flex)
                    alignItems(AlignItems.Center)
                    property("gap", "8px")
                }
            }) {
                // Dark mode toggle — M3 icon button
                Span(attrs = {
                    style {
                        cursor("pointer")
                        fontSize(1.1.cssRem)
                        padding(10.px)
                        property("border-radius", Shape.FULL)
                        color(Color(colorMode.text2()))
                        property("transition", Motion.HOVER_TRANSITION)
                    }
                    classes("nav-icon-btn")
                    onClick { colorMode = colorMode.opposite }
                }) {
                    if (colorMode == ColorMode.DARK) FaSun(size = IconSize.LG) else FaMoon(size = IconSize.LG)
                }

                // GitHub — M3 icon button
                A(href = "https://github.com/sceneview/sceneview-android", attrs = {
                    attr("target", "_blank")
                    attr("rel", "noopener noreferrer")
                    style {
                        color(Color(colorMode.text()))
                        padding(10.px)
                        property("border-radius", Shape.FULL)
                        property("transition", Motion.HOVER_TRANSITION)
                        property("display", "inline-flex")
                        alignItems(AlignItems.Center)
                    }
                    classes("nav-icon-btn")
                }) { FaGithub(size = IconSize.LG) }

                // Mobile hamburger
                Span(attrs = {
                    style {
                        cursor("pointer")
                        padding(10.px)
                        display(DisplayStyle.None)
                        color(Color(colorMode.text()))
                        property("border-radius", Shape.FULL)
                    }
                    classes("nav-hamburger")
                    onClick { menuOpen = !menuOpen }
                }) { FaBars(size = IconSize.LG) }
            }
        }

        // Mobile menu
        if (menuOpen) {
            Div(attrs = {
                style {
                    width(100.percent)
                    property("background-color", colorMode.surfaceContainer())
                    padding(12.px, 24.px)
                    display(DisplayStyle.Flex)
                    flexDirection(FlexDirection.Column)
                    property("gap", "4px")
                    property("border-top", "1px solid ${colorMode.outlineVariant()}")
                }
                classes("nav-mobile-menu")
            }) {
                NavLink("/quickstart", "Docs")
                NavLink("/samples", "Samples")
                NavLink("/playground", "Playground")
                NavLink("/changelog", "Changelog")
            }
        }
    }

    Style {
        """
        .nav-icon-btn:hover {
            background-color: rgba(0,0,0,0.06);
        }
        @media (prefers-color-scheme: dark) {
            .nav-icon-btn:hover { background-color: rgba(255,255,255,0.08); }
        }
        @media (max-width: 768px) {
            .nav-links { display: none !important; }
            .nav-hamburger { display: block !important; }
        }
        @media (min-width: 769px) {
            .nav-mobile-menu { display: none !important; }
        }
        """.trimIndent()
    }
}

@Composable
private fun NavLink(href: String, label: String) {
    val colorMode by ColorMode.currentState
    A(href = href, attrs = {
        style {
            color(Color(colorMode.text2()))
            property("text-decoration", "none")
            property("font-size", TypeScale.LABEL_LARGE_SIZE)
            property("font-weight", TypeScale.LABEL_LARGE_WEIGHT)
            property("font-family", Fonts.SYSTEM)
            padding(8.px, 16.px)
            property("border-radius", Shape.FULL)
            property("transition", "color ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}, background-color ${Motion.DURATION_SHORT} ${Motion.EMPHASIZED}")
        }
        classes("nav-link-pill")
    }) { Text(label) }
}
