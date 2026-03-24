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
    // Static CSS (/styles.css) is loaded via head.add in build.gradle.kts
    // It handles responsive media queries that survive Kobweb static export.

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

            /* Responsive navbar — must be in global Style to override inline display */
            .nav-hamburger { display: none !important; }
            @media (max-width: 768px) {
                .nav-links { display: none !important; }
                .nav-hamburger { display: block !important; }
            }
            @media (min-width: 769px) {
                .nav-mobile-menu { display: none !important; }
            }
            .nav-icon-btn:hover { background-color: rgba(0,0,0,0.06); }
            .nav-link-pill:hover { background-color: rgba(0,0,0,0.04); }
            @media (prefers-color-scheme: dark) {
                .nav-icon-btn:hover { background-color: rgba(255,255,255,0.08); }
                .nav-link-pill:hover { background-color: rgba(255,255,255,0.06); }
            }
            @media (max-width: 860px) {
                .hero-text { text-align: center; }
                .hero-text > div { justify-content: center; }
                .hero-viewer { max-width: 100% !important; }
            }
            .m3-sample-card:hover {
                transform: translateY(-2px) scale(1.01);
                box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
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
