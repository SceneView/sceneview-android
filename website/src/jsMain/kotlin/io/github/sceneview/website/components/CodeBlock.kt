package io.github.sceneview.website.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    showLabel: Boolean = true
) {
    val colorMode by ColorMode.currentState

    Div(attrs = {
        style {
            property("border-radius", Shape.MEDIUM)
            property("background-color", colorMode.codeBg())
            property("border", "1px solid ${colorMode.outlineVariant()}")
            property("overflow", "hidden")
            width(100.percent)
            property("box-shadow", Elevation.LEVEL1)
        }
    }) {
        if (showLabel && language.isNotEmpty()) {
            Div(attrs = {
                style {
                    padding(8.px, 16.px)
                    property("font-size", TypeScale.LABEL_LARGE_SIZE)
                    property("font-weight", TypeScale.LABEL_LARGE_WEIGHT)
                    color(Color(colorMode.text2()))
                    property("text-transform", "uppercase")
                    property("letter-spacing", TypeScale.LABEL_LARGE_TRACKING)
                    property("border-bottom", "1px solid ${colorMode.outlineVariant()}")
                    property("font-family", Fonts.SYSTEM)
                }
            }) { Text(language) }
        }

        Pre(attrs = {
            style {
                margin(0.px)
                padding(20.px)
                property("overflow", "auto")
                property("-webkit-overflow-scrolling", "touch")
            }
        }) {
            Code(attrs = {
                style {
                    property("font-family", Fonts.MONO)
                    fontSize(0.875.cssRem)
                    property("line-height", "1.7")
                    color(Color(colorMode.text()))
                    property("white-space", "pre")
                    property("tab-size", "4")
                }
            }) { Text(code) }
        }
    }
}
