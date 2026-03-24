package io.github.sceneview.website.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

@Composable
fun FeatureCard(
    title: String,
    description: String
) {
    val colorMode by ColorMode.currentState

    Div(attrs = {
        style {
            property("background-color", colorMode.surfaceContainer())
            property("border-radius", Shape.EXTRA_LARGE)
            padding(32.px)
            property("border", "1px solid ${colorMode.outlineVariant()}")
            property("transition", Motion.HOVER_TRANSITION)
            property("flex", "1 1 260px")
            property("box-shadow", Elevation.LEVEL1)
        }
        classes("m3-card")
    }) {
        H3(attrs = {
            style {
                property("font-size", TypeScale.TITLE_LARGE_SIZE)
                property("font-weight", TypeScale.TITLE_LARGE_WEIGHT)
                color(Color(colorMode.text()))
                property("margin", "0 0 12px 0")
                property("font-family", Fonts.SYSTEM)
                property("line-height", TypeScale.TITLE_LARGE_LINE_HEIGHT)
            }
        }) { Text(title) }

        P(attrs = {
            style {
                color(Color(colorMode.text2()))
                property("font-size", TypeScale.BODY_LARGE_SIZE)
                property("line-height", TypeScale.BODY_LARGE_LINE_HEIGHT)
                margin(0.px)
                property("font-family", Fonts.SYSTEM)
            }
        }) { Text(description) }
    }

    Style {
        """
        .m3-card:hover {
            transform: translateY(-2px) scale(1.01);
            box-shadow: ${Elevation.LEVEL3};
        }
        """.trimIndent()
    }
}
