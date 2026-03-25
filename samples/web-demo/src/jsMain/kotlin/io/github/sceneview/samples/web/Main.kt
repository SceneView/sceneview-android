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
import org.w3c.dom.HTMLElement

/**
 * SceneView Web Demo — branded showcase application.
 *
 * Features:
 * - Model selector with popular glTF sample models
 * - Orbit camera controls (drag, scroll, pinch)
 * - Auto-rotation toggle
 * - Loading indicator (initial + model switching)
 * - WebXR AR/VR buttons (shown when browser supports it)
 * - Mobile responsive layout
 * - SceneView blue branding (#1a73e8)
 *
 * Uses Filament.js (WASM) — the same rendering engine as SceneView Android.
 */

/** Available demo models — name to URL mapping. */
private val MODELS = linkedMapOf(
    "Damaged Helmet" to "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/DamagedHelmet/glTF-Binary/DamagedHelmet.glb",
    "Flight Helmet" to "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/FlightHelmet/glTF-Binary/FlightHelmet.glb",
    "Avocado" to "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Avocado/glTF-Binary/Avocado.glb",
    "Lantern" to "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Lantern/glTF-Binary/Lantern.glb",
    "Suzanne" to "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/Suzanne/glTF-Binary/Suzanne.glb"
)

private var currentSceneView: SceneView? = null
private var currentModelName: String = MODELS.keys.first()
private var autoRotateEnabled = true

fun main() {
    val canvas = document.getElementById("scene-canvas") as? HTMLCanvasElement
    if (canvas == null) {
        console.error("Canvas element 'scene-canvas' not found")
        return
    }

    // Resize canvas to fill viewport
    canvas.width = canvas.clientWidth
    canvas.height = canvas.clientHeight

    // Build the model selector UI
    buildModelSelector()

    // Initialize SceneView with the first model
    initSceneView(canvas, MODELS.values.first())
}

/**
 * Initialize (or re-initialize) SceneView with a given model URL.
 */
private fun initSceneView(canvas: HTMLCanvasElement, modelUrl: String) {
    showLoadingOverlay("Loading ${currentModelName}...")

    SceneView.create(
        canvas = canvas,
        configure = {
            camera {
                eye(0.0, 1.5, 5.0)
                target(0.0, 0.0, 0.0)
                fov(45.0)
            }
            light {
                directional()
                intensity(120_000.0)
                direction(0.6f, -1.0f, -0.8f)
            }
            model(modelUrl) {
                onLoaded {
                    hideLoadingOverlay()
                    hideLoadingChip()
                    console.log("Model loaded: $currentModelName")
                }
            }
            autoRotate(autoRotateEnabled)
        },
        onReady = { sceneView ->
            currentSceneView = sceneView
            sceneView.startRendering()

            // Handle window resize
            window.addEventListener("resize", {
                canvas.width = canvas.clientWidth
                canvas.height = canvas.clientHeight
            })

            console.log("SceneView Web initialized — Filament.js renderer active")

            // Set up WebXR buttons
            setupXRButtons(canvas)

            // Set up auto-rotate toggle
            setupAutoRotateToggle(sceneView)
        }
    )
}

/**
 * Build the model selector chips in the left panel.
 */
private fun buildModelSelector() {
    val container = document.getElementById("model-selector") as? HTMLElement ?: return

    MODELS.entries.forEachIndexed { index, (name, url) ->
        val chip = document.createElement("button") as HTMLButtonElement
        chip.className = if (index == 0) "model-chip active" else "model-chip"
        chip.textContent = name
        chip.title = "Load $name"
        chip.addEventListener("click", {
            if (name != currentModelName) {
                switchModel(name, url)
            }
        })
        container.appendChild(chip)
    }
}

/**
 * Switch to a different model — shows inline loading chip.
 */
private fun switchModel(name: String, url: String) {
    currentModelName = name

    // Update chip active states
    val chips = document.querySelectorAll(".model-chip")
    for (i in 0 until chips.length) {
        val chip = chips.item(i) as? HTMLElement ?: continue
        chip.className = if (chip.textContent == name) "model-chip active" else "model-chip"
    }

    // Show inline loading chip
    showLoadingChip("Loading $name...")

    val canvas = document.getElementById("scene-canvas") as? HTMLCanvasElement ?: return

    // Destroy current scene and re-create with the new model
    currentSceneView?.destroy()
    currentSceneView = null

    SceneView.create(
        canvas = canvas,
        configure = {
            camera {
                eye(0.0, 1.5, 5.0)
                target(0.0, 0.0, 0.0)
                fov(45.0)
            }
            light {
                directional()
                intensity(120_000.0)
                direction(0.6f, -1.0f, -0.8f)
            }
            model(url) {
                onLoaded {
                    hideLoadingChip()
                    console.log("Model switched to: $name")
                }
            }
            autoRotate(autoRotateEnabled)
        },
        onReady = { sceneView ->
            currentSceneView = sceneView
            sceneView.startRendering()

            // Handle window resize
            window.addEventListener("resize", {
                canvas.width = canvas.clientWidth
                canvas.height = canvas.clientHeight
            })

            setupXRButtons(canvas)
            setupAutoRotateToggle(sceneView)
        }
    )
}

/**
 * Set up the auto-rotate toggle button.
 */
private fun setupAutoRotateToggle(sceneView: SceneView) {
    val btn = document.getElementById("auto-rotate-toggle") as? HTMLButtonElement ?: return
    btn.className = if (autoRotateEnabled) "auto-rotate-btn active" else "auto-rotate-btn"

    // Remove previous listener by cloning
    val newBtn = btn.cloneNode(true) as HTMLButtonElement
    btn.parentNode?.replaceChild(newBtn, btn)

    newBtn.addEventListener("click", {
        autoRotateEnabled = !autoRotateEnabled
        sceneView.cameraController?.autoRotate = autoRotateEnabled
        newBtn.className = if (autoRotateEnabled) "auto-rotate-btn active" else "auto-rotate-btn"
    })
}

// --- Loading UI helpers ---

private fun showLoadingOverlay(text: String) {
    val overlay = document.getElementById("loading-overlay") as? HTMLElement ?: return
    overlay.className = "loading-overlay"
    val modelLabel = document.getElementById("loading-model-name") as? HTMLElement
    modelLabel?.textContent = text
}

private fun hideLoadingOverlay() {
    val overlay = document.getElementById("loading-overlay") as? HTMLElement ?: return
    overlay.className = "loading-overlay hidden"
}

private fun showLoadingChip(text: String) {
    val chip = document.getElementById("loading-chip") as? HTMLElement ?: return
    val chipText = document.getElementById("loading-chip-text") as? HTMLElement
    chipText?.textContent = text
    chip.className = "loading-chip visible"
}

private fun hideLoadingChip() {
    val chip = document.getElementById("loading-chip") as? HTMLElement ?: return
    chip.className = "loading-chip"
}

// --- WebXR ---

/**
 * Check for WebXR support and show AR/VR buttons if available.
 */
private fun setupXRButtons(canvas: HTMLCanvasElement) {
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
 * Enter an immersive AR session via WebXR.
 */
private fun enterAR(canvas: HTMLCanvasElement) {
    val modelUrl = MODELS[currentModelName] ?: MODELS.values.first()

    ARSceneView.create(
        canvas = canvas,
        onError = { error ->
            console.error("AR Error: $error")
            window.alert("Failed to start AR: $error")
        },
        onReady = { arSession ->
            console.log("AR session started — tap to place content")

            arSession.onHitTest = { pose ->
                // Hit test running — pose.transform.position has the surface point
            }

            arSession.onSelect = { inputSource ->
                arSession.loadModel(modelUrl) { asset ->
                    console.log("Model placed in AR")
                }
            }

            arSession.onSessionEnd = {
                console.log("AR session ended")
            }

            arSession.start()
        }
    )
}

/**
 * Enter an immersive VR session via WebXR.
 */
private fun enterVR(canvas: HTMLCanvasElement) {
    val modelUrl = MODELS[currentModelName] ?: MODELS.values.first()

    VRSceneView.create(
        canvas = canvas,
        onError = { error ->
            console.error("VR Error: $error")
            window.alert("Failed to start VR: $error")
        },
        onReady = { vrSession ->
            console.log("VR session started")

            vrSession.loadModel(modelUrl)

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
