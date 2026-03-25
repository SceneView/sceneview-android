package io.github.sceneview.website

import androidx.compose.runtime.Composable
import com.varabyte.kobweb.core.App
import com.varabyte.kobweb.silk.SilkApp
import com.varabyte.kobweb.silk.style.common.SmoothColorStyle
import com.varabyte.kobweb.silk.style.toModifier
import com.varabyte.kobweb.compose.ui.modifiers.*
import org.jetbrains.compose.web.dom.Style

@App
@Composable
fun AppEntry(content: @Composable () -> Unit) {
    SilkApp {
        // Import Inter from Google Fonts + global M3 resets
        Style {
            """
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');
            @import url('https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;600&display=swap');

            *, *::before, *::after {
                box-sizing: border-box;
            }
            html {
                scroll-behavior: smooth;
                -webkit-font-smoothing: antialiased;
                -moz-osx-font-smoothing: grayscale;
            }
            body {
                margin: 0;
                font-family: ${Fonts.SYSTEM};
            }
            ::selection {
                background-color: rgba(26, 115, 232, 0.2);
            }
            """.trimIndent()
        }

        com.varabyte.kobweb.compose.foundation.layout.Box(
            modifier = SmoothColorStyle.toModifier()
                .fillMaxSize()
                .fontFamily(Fonts.SYSTEM)
        ) {
            content()
        }
    }
}
