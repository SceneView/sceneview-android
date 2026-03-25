package io.github.sceneview.website.components

import androidx.compose.runtime.*
import com.varabyte.kobweb.silk.theme.colors.ColorMode
import io.github.sceneview.website.*
import kotlinx.browser.document
import org.jetbrains.compose.web.css.*
import org.jetbrains.compose.web.dom.*
import org.w3c.dom.HTMLElement

/**
 * Injects the model-viewer web component script once into the document head.
 * Must be called before any ModelViewer composable is used.
 */
@Composable
fun ModelViewerScript() {
    LaunchedEffect(Unit) {
        if (document.querySelector("script[data-model-viewer]") == null) {
            val script = document.createElement("script")
            script.setAttribute("type", "module")
            script.setAttribute("src", "https://ajax.googleapis.com/ajax/libs/model-viewer/3.5.0/model-viewer.min.js")
            script.setAttribute("data-model-viewer", "true")
            document.head?.appendChild(script)
        }
    }
}

/**
 * A composable wrapper around Google's <model-viewer> web component.
 * Renders an interactive 3D model viewer with orbit controls.
 */
@Composable
fun ModelViewer(
    src: String,
    alt: String = "3D Model",
    autoRotate: Boolean = true,
    cameraControls: Boolean = true,
    shadowIntensity: String = "1",
    exposure: String = "1",
    cameraOrbit: String? = null,
    minCameraOrbit: String? = null,
    maxCameraOrbit: String? = null,
    environmentImage: String? = null,
    poster: String? = null,
    loading: String = "eager",
    reveal: String? = null,
    animationName: String? = null,
    autoplay: Boolean = false,
    styleMods: (StyleScope.() -> Unit)? = null
) {
    // Ensure model-viewer script is loaded
    ModelViewerScript()

    TagElement<HTMLElement>(
        elementBuilder = ElementBuilder.createBuilder("model-viewer"),
        applyAttrs = {
            attr("src", src)
            attr("alt", alt)
            if (autoRotate) attr("auto-rotate", "")
            if (cameraControls) attr("camera-controls", "")
            attr("shadow-intensity", shadowIntensity)
            attr("exposure", exposure)
            attr("tone-mapping", "neutral")
            attr("loading", loading)
            cameraOrbit?.let { attr("camera-orbit", it) }
            minCameraOrbit?.let { attr("min-camera-orbit", it) }
            maxCameraOrbit?.let { attr("max-camera-orbit", it) }
            environmentImage?.let { attr("environment-image", it) }
            poster?.let { attr("poster", it) }
            reveal?.let { attr("reveal", it) }
            animationName?.let { attr("animation-name", it) }
            if (autoplay) attr("autoplay", "")
            attr("interaction-prompt", "none")
            style {
                property("width", "100%")
                property("height", "100%")
                property("display", "block")
                property("--poster-color", "transparent")
                styleMods?.invoke(this)
            }
        },
        content = {}
    )
}

/**
 * A hero-sized 3D model viewer with a rounded container and loading state.
 * Designed for prominent placement like the homepage hero section.
 */
@Composable
fun HeroModelViewer(
    src: String,
    alt: String = "Interactive 3D model",
    height: CSSNumeric = 420.px,
    cameraOrbit: String? = "30deg 75deg 105%",
    autoRotate: Boolean = true
) {
    val colorMode by ColorMode.currentState

    Div(attrs = {
        style {
            width(100.percent)
            property("height", height.toString())
            property("border-radius", Shape.EXTRA_LARGE)
            property("overflow", "hidden")
            property("background-color", colorMode.surfaceContainer())
            property("border", "1px solid ${colorMode.outlineVariant()}")
            property("box-shadow", Elevation.LEVEL2)
            property("position", "relative")
        }
    }) {
        ModelViewer(
            src = src,
            alt = alt,
            autoRotate = autoRotate,
            cameraControls = true,
            shadowIntensity = "1",
            exposure = "1",
            cameraOrbit = cameraOrbit,
            styleMods = {
                property("border-radius", Shape.EXTRA_LARGE)
            }
        )
    }
}

/**
 * A compact model viewer for use in cards and grid layouts.
 */
@Composable
fun CardModelViewer(
    src: String,
    alt: String = "3D model preview",
    height: CSSNumeric = 200.px
) {
    val colorMode by ColorMode.currentState

    Div(attrs = {
        style {
            width(100.percent)
            property("height", height.toString())
            property("border-radius", "${Shape.LARGE} ${Shape.LARGE} 0 0")
            property("overflow", "hidden")
            property("background-color", colorMode.surfaceContainer())
        }
    }) {
        ModelViewer(
            src = src,
            alt = alt,
            autoRotate = true,
            cameraControls = true,
            shadowIntensity = "0.5",
            exposure = "1",
            cameraOrbit = "30deg 75deg 105%",
            loading = "lazy",
            styleMods = {
                property("border-radius", "${Shape.LARGE} ${Shape.LARGE} 0 0")
            }
        )
    }
}
