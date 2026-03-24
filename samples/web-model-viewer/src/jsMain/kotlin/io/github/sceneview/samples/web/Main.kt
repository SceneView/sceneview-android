package io.github.sceneview.samples.web

import io.github.sceneview.web.SceneView
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * Web Model Viewer — SceneView web sample.
 *
 * Demonstrates loading and displaying a 3D glTF model in the browser
 * using Filament.js (the same rendering engine as SceneView Android).
 */
fun main() {
    val canvas = document.getElementById("scene-canvas") as? HTMLCanvasElement
    if (canvas == null) {
        console.error("Canvas element 'scene-canvas' not found")
        return
    }

    // Resize canvas to fill viewport
    canvas.width = canvas.clientWidth
    canvas.height = canvas.clientHeight

    // Initialize SceneView with Filament.js
    SceneView.create(
        canvas = canvas,
        configure = {
            camera {
                eye(0.0, 1.5, 5.0)
                target(0.0, 0.0, 0.0)
                fov(45.0)
            }
            model("models/DamagedHelmet.glb")
        },
        onReady = { sceneView ->
            sceneView.startRendering()

            // Handle window resize
            window.addEventListener("resize", {
                canvas.width = canvas.clientWidth
                canvas.height = canvas.clientHeight
            })

            console.log("SceneView Web initialized — Filament.js renderer active")
        }
    )
}
