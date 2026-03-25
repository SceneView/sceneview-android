package io.github.sceneview.web.xr

import io.github.sceneview.web.SceneView
import io.github.sceneview.web.bindings.*
import org.w3c.dom.HTMLCanvasElement

/**
 * AR SceneView for Web — WebXR-based augmented reality in the browser.
 *
 * Uses the WebXR Device API for camera passthrough AR on supported devices
 * (Chrome Android, Safari iOS 18+, Quest Browser, etc.) combined with
 * Filament.js for 3D rendering.
 *
 * This is a high-level convenience wrapper around [WebXRSession] for AR-only use.
 * For VR or more control, use [WebXRSession] directly.
 *
 * Usage:
 * ```kotlin
 * ARSceneView.checkSupport { supported ->
 *     if (supported) {
 *         ARSceneView.create(canvas) { arSession ->
 *             arSession.onHitTest = { pose ->
 *                 // Place model at hit position
 *                 arSession.sceneView.loadModel("models/chair.glb")
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

    /** Callback when select (tap) input occurs. */
    var onSelect: ((XRInputSource) -> Unit)? = null

    private var hitTestSource: XRHitTestSource? = null
    private var isRunning = false

    companion object {

        /**
         * Check if AR is supported in the current browser.
         */
        fun checkSupport(callback: (Boolean) -> Unit) {
            WebXRSession.checkSupport(XRSessionMode.IMMERSIVE_AR, callback)
        }

        /**
         * Create an AR session with Filament rendering.
         *
         * Must be called from a user gesture (click/tap event handler).
         *
         * @param canvas The HTML canvas for rendering
         * @param features Additional features to request (hit-test is always required)
         * @param onError Called if AR session creation fails
         * @param onReady Callback with the configured ARSceneView
         */
        fun create(
            canvas: HTMLCanvasElement,
            features: WebXRSession.Features = WebXRSession.Features(
                required = arrayOf(XRFeature.HIT_TEST),
                optional = arrayOf(XRFeature.DOM_OVERLAY, XRFeature.LIGHT_ESTIMATION)
            ),
            onError: ((String) -> Unit)? = null,
            onReady: (ARSceneView) -> Unit
        ) {
            val xr = Navigator.xr
            if (xr == null) {
                onError?.invoke("WebXR not supported in this browser")
                return
            }

            // Build session options
            val options = js("{}")
            options.requiredFeatures = features.required
            if (features.optional.isNotEmpty()) {
                options.optionalFeatures = features.optional
            }

            xr.requestSession(XRSessionMode.IMMERSIVE_AR, options).then { session: XRSession ->
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

                        val renderStateInit = js("{}")
                        renderStateInit.baseLayer = xrLayer
                        session.updateRenderState(renderStateInit)

                        session.requestReferenceSpace(XRReferenceSpaceType.LOCAL_FLOOR).then { refSpace: XRReferenceSpace ->
                            val arView = ARSceneView(sceneView, session, refSpace)

                            // Set up hit test source from viewer space
                            session.requestReferenceSpace(XRReferenceSpaceType.VIEWER).then { viewerSpace: XRReferenceSpace ->
                                val hitTestOptions = js("{}")
                                hitTestOptions.space = viewerSpace
                                session.asDynamic().requestHitTestSource(hitTestOptions).then { source: XRHitTestSource ->
                                    arView.hitTestSource = source
                                }
                            }

                            // Handle select (tap) events
                            session.onselect = { event ->
                                val inputSource = event.asDynamic().inputSource as? XRInputSource
                                if (inputSource != null) {
                                    arView.onSelect?.invoke(inputSource)
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
                val message = error?.message?.toString() ?: "Failed to start AR session"
                onError?.invoke(message)
                console.error("ARSceneView: Failed to start AR session:", error)
            }
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
        hitTestSource?.cancel()
        hitTestSource = null
        session.end()
    }

    /** Load a 3D model into the AR scene. */
    fun loadModel(url: String, onLoaded: ((FilamentAsset) -> Unit)? = null) {
        sceneView.loadModel(url, onLoaded)
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

/**
 * VR SceneView for Web — WebXR-based virtual reality in the browser.
 *
 * Uses the WebXR Device API for immersive VR on headsets (Quest, Vive, etc.)
 * combined with Filament.js for 3D rendering.
 *
 * Usage:
 * ```kotlin
 * VRSceneView.checkSupport { supported ->
 *     if (supported) {
 *         VRSceneView.create(canvas) { vrSession ->
 *             vrSession.sceneView.loadModel("models/room.glb")
 *             vrSession.onInputSelect = { source, pose ->
 *                 // Handle controller trigger
 *             }
 *             vrSession.start()
 *         }
 *     }
 * }
 * ```
 */
class VRSceneView private constructor(
    val sceneView: SceneView,
    val session: XRSession,
    val referenceSpace: XRReferenceSpace,
    private val glLayer: XRWebGLLayer
) {
    /** Called each frame with the viewer pose. */
    var onFrame: ((XRFrame, XRViewerPose?) -> Unit)? = null

    /** Called when the user performs a primary select action (trigger press). */
    var onInputSelect: ((XRInputSource, XRPose?) -> Unit)? = null

    /** Called when a squeeze action is performed (grip button). */
    var onInputSqueeze: ((XRInputSource, XRPose?) -> Unit)? = null

    /** Called when session ends. */
    var onSessionEnd: (() -> Unit)? = null

    private var isRunning = false

    companion object {

        /** Check if VR is supported. */
        fun checkSupport(callback: (Boolean) -> Unit) {
            WebXRSession.checkSupport(XRSessionMode.IMMERSIVE_VR, callback)
        }

        /**
         * Create a VR session with Filament rendering.
         *
         * Must be called from a user gesture.
         *
         * @param canvas The HTML canvas for rendering
         * @param features Optional features to request
         * @param referenceSpaceType Reference space — defaults to "local-floor" for room-scale
         * @param onError Called if session creation fails
         * @param onReady Callback with the configured VRSceneView
         */
        fun create(
            canvas: HTMLCanvasElement,
            features: WebXRSession.Features = WebXRSession.Features(
                optional = arrayOf(XRFeature.HAND_TRACKING)
            ),
            referenceSpaceType: String = XRReferenceSpaceType.LOCAL_FLOOR,
            onError: ((String) -> Unit)? = null,
            onReady: (VRSceneView) -> Unit
        ) {
            val xr = Navigator.xr
            if (xr == null) {
                onError?.invoke("WebXR not supported in this browser")
                return
            }

            val options = js("{}")
            if (features.required.isNotEmpty()) {
                options.requiredFeatures = features.required
            }
            if (features.optional.isNotEmpty()) {
                options.optionalFeatures = features.optional
            }

            xr.requestSession(XRSessionMode.IMMERSIVE_VR, options).then { session: XRSession ->
                SceneView.create(
                    canvas = canvas,
                    configure = {
                        cameraControls(false)
                    },
                    onReady = { sceneView ->
                        val gl = canvas.asDynamic().getContext("webgl2", js("{xrCompatible: true}"))
                        val xrLayer = XRWebGLLayer(session, gl)

                        val renderStateInit = js("{}")
                        renderStateInit.baseLayer = xrLayer
                        session.updateRenderState(renderStateInit)

                        session.requestReferenceSpace(referenceSpaceType).then { refSpace: XRReferenceSpace ->
                            val vrView = VRSceneView(sceneView, session, refSpace, xrLayer)

                            session.onselect = { event ->
                                val inputSource = event.asDynamic().inputSource as? XRInputSource
                                if (inputSource != null) {
                                    val frame = event.asDynamic().frame as? XRFrame
                                    val pose = frame?.getPose(inputSource.targetRaySpace, refSpace)
                                    vrView.onInputSelect?.invoke(inputSource, pose)
                                }
                            }

                            session.onsqueeze = { event ->
                                val inputSource = event.asDynamic().inputSource as? XRInputSource
                                if (inputSource != null) {
                                    val frame = event.asDynamic().frame as? XRFrame
                                    val pose = frame?.getPose(inputSource.targetRaySpace, refSpace)
                                    vrView.onInputSqueeze?.invoke(inputSource, pose)
                                }
                            }

                            session.onend = {
                                vrView.isRunning = false
                                vrView.onSessionEnd?.invoke()
                            }

                            onReady(vrView)
                        }
                    }
                )
            }.catch { error: dynamic ->
                val message = error?.message?.toString() ?: "Failed to start VR session"
                onError?.invoke(message)
                console.error("VRSceneView: Failed to start VR session:", error)
            }
        }
    }

    /** Start the VR render loop. */
    fun start() {
        isRunning = true
        session.requestAnimationFrame(::renderFrame)
    }

    /** Stop the VR session. */
    fun stop() {
        isRunning = false
        session.end()
    }

    /** Load a 3D model into the VR scene. */
    fun loadModel(url: String, onLoaded: ((FilamentAsset) -> Unit)? = null) {
        sceneView.loadModel(url, onLoaded)
    }

    private fun renderFrame(timestamp: Double, frame: XRFrame) {
        if (!isRunning) return

        val pose = frame.getViewerPose(referenceSpace)

        if (pose != null) {
            val views = pose.views
            if (views.isNotEmpty()) {
                // For stereo VR, update camera from the first view
                val primaryView = views[0]
                sceneView.camera.setModelMatrix(primaryView.transform.matrix)

                val viewport = glLayer.getViewport(primaryView)
                val viewportArray = js("[]")
                viewportArray.push(viewport.x, viewport.y, viewport.width, viewport.height)
                sceneView.view.setViewport(viewportArray)
            }
        }

        // Dispatch frame callback
        onFrame?.invoke(frame, pose)

        // Render with Filament
        if (sceneView.renderer.beginFrame(sceneView.swapChain, timestamp)) {
            sceneView.renderer.renderView(sceneView.view)
            sceneView.renderer.endFrame()
        }

        session.requestAnimationFrame(::renderFrame)
    }
}
