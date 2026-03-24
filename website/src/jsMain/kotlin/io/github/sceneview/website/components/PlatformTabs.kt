package io.github.sceneview.website.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*

enum class Platform(val label: String) {
    ANDROID("Kotlin"),
    IOS("Swift")
}

@Composable
fun PlatformTabs(tabs: Map<Platform, @Composable () -> Unit>) {
    var selected by remember { mutableStateOf(Platform.ANDROID) }
    val colorMode by ColorMode.currentState

    Div(attrs = { style { width(100.percent) } }) {
        // M3 segmented button container
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
            tabs.keys.forEach { platform ->
                val isSelected = platform == selected
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
                            color(Color(colorMode.onPrimary()))
                        } else {
                            property("background-color", "transparent")
                            color(Color(colorMode.text2()))
                        }
                    }
                    onClick { selected = platform }
                }) { Text(platform.label) }
            }
        }

        tabs[selected]?.invoke()
    }
}
