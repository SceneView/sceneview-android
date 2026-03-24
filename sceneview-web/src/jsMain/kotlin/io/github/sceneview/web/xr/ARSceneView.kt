package io.github.sceneview.web.xr

import io.github.sceneview.web.SceneView
import io.github.sceneview.web.bindings.*
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * AR SceneView for Web — WebXR-based augmented reality in the browser.
 *
 * Uses the WebXR Device API for camera passthrough AR on supported devices
 * (Chrome Android, Safari iOS 18+, Quest Browser, etc.) combined with
 * Filament.js for 3D rendering.
 *
 * Usage:
 * ```kotlin
 * ARSceneView.checkSupport { supported ->
 *     if (supported) {
 *         ARSceneView.create(canvas) { arSession ->
 *             arSession.onHitTest = { pose ->
 *                 // Place model at hit position
 *                 sceneView.loadModel("models/chair.glb")
 *             }
 *             arSession.start()
 *         }
 *     }
 * }
 * ```
 */
class ARSceneView private constructor(
    val sceneView: SceneView,
    val session: XRSession,
    val referenceSpace: XRReferenceSpace,
) {
    /** Callback when user taps and a hit test result is found. */
    var onHitTest: ((XRPose) -> Unit)? = null

    /** Callback when session ends. */
    var onSessionEnd: (() -> Unit)? = null

    private var hitTestSource: XRHitTestSource? = null
    private var isRunning = false

    companion object {

        /**
         * Check if AR is supported in the current browser.
         */
        fun checkSupport(callback: (Boolean) -> Unit) {
            val xr = getXRSystem()
            if (xr == null) {
                callback(false)
                return
            }
            val promise = xr.isSessionSupported("immersive-ar")
            promise.then { supported: Boolean ->
                callback(supported)
            }.catch {
                callback(false)
            }
        }

        /**
         * Create an AR session with Filament rendering.
         *
         * @param canvas The HTML canvas for rendering
         * @param onReady Callback with the configured ARSceneView
         */
        fun create(
            canvas: HTMLCanvasElement,
            onReady: (ARSceneView) -> Unit
        ) {
            val xr = getXRSystem() ?: run {
                console.error("WebXR not supported in this browser")
                return
            }

            // Request immersive AR session with hit-test
            val options = js("{}")
            options.requiredFeatures = js("['hit-test']")
            options.optionalFeatures = js("['dom-overlay', 'light-estimation']")

            xr.requestSession("immersive-ar", options).then { session: XRSession ->
                // Create SceneView with Filament
                SceneView.create(
                    canvas = canvas,
                    configure = {
                        // AR uses camera from WebXR, not user-configured camera
                        cameraControls(false)
                    },
                    onReady = { sceneView ->
                        // Set up XR rendering
                        val gl = canvas.asDynamic().getContext("webgl2", js("{xrCompatible: true}"))
                        val xrLayer = XRWebGLLayer(session, gl)
                        session.updateRenderState(js("{baseLayer: xrLayer}"))

                        session.requestReferenceSpace("local-floor").then { refSpace: XRReferenceSpace ->
                            val arView = ARSceneView(sceneView, session, refSpace)

                            // Set up hit test source
                            session.requestReferenceSpace("viewer").then { viewerSpace: XRReferenceSpace ->
                                val hitTestOptions = js("{}")
                                hitTestOptions.space = viewerSpace
                                session.asDynamic().requestHitTestSource(hitTestOptions).then { source: XRHitTestSource ->
                                    arView.hitTestSource = source
                                }
                            }

                            session.onend = {
                                arView.isRunning = false
                                arView.onSessionEnd?.invoke()
                            }

                            onReady(arView)
                        }
                    }
                )
            }.catch { error: dynamic ->
                console.error("Failed to start AR session:", error)
            }
        }

        private fun getXRSystem(): XRSystem? {
            return js("navigator.xr") as? XRSystem
        }
    }

    /** Start the AR render loop. */
    fun start() {
        isRunning = true
        session.requestAnimationFrame(::renderFrame)
    }

    /** Stop the AR session. */
    fun stop() {
        isRunning = false
        session.end()
    }

    private fun renderFrame(timestamp: Double, frame: XRFrame) {
        if (!isRunning) return

        val pose = frame.getViewerPose(referenceSpace)
        if (pose != null) {
            // Update Filament camera from WebXR pose
            val view = pose.views.firstOrNull()
            if (view != null) {
                sceneView.camera.setModelMatrix(view.transform.matrix)
            }
        }

        // Process hit tests
        hitTestSource?.let { source ->
            val results = frame.getHitTestResults(source)
            if (results.isNotEmpty()) {
                val hitPose = results[0].getPose(referenceSpace)
                if (hitPose != null) {
                    onHitTest?.invoke(hitPose)
                }
            }
        }

        // Render with Filament
        if (sceneView.renderer.beginFrame(sceneView.swapChain, timestamp)) {
            sceneView.renderer.renderView(sceneView.view)
            sceneView.renderer.endFrame()
        }

        session.requestAnimationFrame(::renderFrame)
    }
}
