package io.github.sceneview.samples.web

import io.github.sceneview.web.SceneView
import io.github.sceneview.web.xr.ARSceneView
import io.github.sceneview.web.xr.VRSceneView
import io.github.sceneview.web.xr.XRSessionMode
import io.github.sceneview.web.xr.WebXRSession
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement

/**
 * Web Model Viewer — SceneView web sample.
 *
 * Demonstrates loading and displaying a 3D glTF model in the browser
 * using Filament.js (the same rendering engine as SceneView Android).
 *
 * Also supports entering an immersive AR or VR session via WebXR
 * on compatible devices (Chrome Android, Quest Browser, etc.).
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

            // Set up WebXR buttons
            setupXRButtons(canvas)
        }
    )
}

/**
 * Check for WebXR support and show AR/VR buttons if available.
 */
private fun setupXRButtons(canvas: HTMLCanvasElement) {
    val buttonContainer = document.getElementById("xr-buttons") as? HTMLDivElement ?: return

    // Check AR support
    WebXRSession.checkSupport(XRSessionMode.IMMERSIVE_AR) { arSupported ->
        if (arSupported) {
            val arButton = document.getElementById("enter-ar") as? HTMLButtonElement
            if (arButton != null) {
                arButton.style.display = "inline-block"
                arButton.addEventListener("click", {
                    enterAR(canvas)
                })
            }
        }
    }

    // Check VR support
    WebXRSession.checkSupport(XRSessionMode.IMMERSIVE_VR) { vrSupported ->
        if (vrSupported) {
            val vrButton = document.getElementById("enter-vr") as? HTMLButtonElement
            if (vrButton != null) {
                vrButton.style.display = "inline-block"
                vrButton.addEventListener("click", {
                    enterVR(canvas)
                })
            }
        }
    }
}

/**
 * Enter an immersive AR session.
 */
private fun enterAR(canvas: HTMLCanvasElement) {
    ARSceneView.create(
        canvas = canvas,
        onError = { error ->
            console.error("AR Error: $error")
            window.alert("Failed to start AR: $error")
        },
        onReady = { arSession ->
            console.log("AR session started — tap to place content")

            // Show reticle / hit test indicator
            arSession.onHitTest = { pose ->
                // Hit test is running — pose.transform.position has the surface point
                // A real app would show a placement indicator here
            }

            // Handle taps — place the model at the hit location
            arSession.onSelect = { inputSource ->
                arSession.loadModel("models/DamagedHelmet.glb") { asset ->
                    console.log("Model placed in AR")
                }
            }

            arSession.onSessionEnd = {
                console.log("AR session ended")
                // The page will return to inline 3D viewing
            }

            arSession.start()
        }
    )
}

/**
 * Enter an immersive VR session.
 */
private fun enterVR(canvas: HTMLCanvasElement) {
    VRSceneView.create(
        canvas = canvas,
        onError = { error ->
            console.error("VR Error: $error")
            window.alert("Failed to start VR: $error")
        },
        onReady = { vrSession ->
            console.log("VR session started")

            vrSession.loadModel("models/DamagedHelmet.glb")

            vrSession.onInputSelect = { source, pose ->
                console.log("VR select from ${source.handedness} hand")
            }

            vrSession.onSessionEnd = {
                console.log("VR session ended")
            }

            vrSession.start()
        }
    )
}
