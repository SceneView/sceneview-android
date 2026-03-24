package io.github.sceneview.website.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.Div

@Composable
fun PageLayout(content: @Composable () -> Unit) {
    Div(attrs = {
        style {
            display(DisplayStyle.Flex)
            flexDirection(FlexDirection.Column)
            minHeight(100.vh)
            width(100.percent)
        }
    }) {
        NavBar()
        Div(attrs = {
            style {
                property("flex-grow", "1")
                width(100.percent)
            }
        }) {
            content()
        }
        Footer()
    }
}
