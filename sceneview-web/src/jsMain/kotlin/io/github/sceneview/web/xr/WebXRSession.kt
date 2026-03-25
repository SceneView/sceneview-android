package io.github.sceneview.web.xr

import io.github.sceneview.web.SceneView
import io.github.sceneview.web.bindings.*
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement

/**
 * WebXR session manager — handles the XR session lifecycle with Filament rendering.
 *
 * This class bridges the WebXR Device API with the Filament.js renderer,
 * managing session creation, the XR render loop, pose tracking, hit testing,
 * and input handling.
 *
 * Supports both AR (immersive-ar) and VR (immersive-vr) sessions.
 *
 * Usage:
 * ```kotlin
 * WebXRSession.create(
 *     canvas = canvas,
 *     mode = XRSessionMode.IMMERSIVE_AR,
 *     features = WebXRSession.Features(
 *         required = arrayOf(XRFeature.HIT_TEST),
 *         optional = arrayOf(XRFeature.LIGHT_ESTIMATION, XRFeature.DOM_OVERLAY)
 *     )
 * ) { xrSession ->
 *     xrSession.onHitTest = { pose -> /* place content */ }
 *     xrSession.onInputSelect = { source, pose -> /* handle input */ }
 *     xrSession.start()
 * }
 * ```
 */
class WebXRSession private constructor(
    val sceneView: SceneView,
    val xrSession: XRSession,
    val referenceSpace: XRReferenceSpace,
    val mode: String,
    private val glLayer: XRWebGLLayer
) {
    // -- Callbacks --

    /** Called each frame with the viewer pose (head tracking). */
    var onFrame: ((XRFrame, XRViewerPose?) -> Unit)? = null

    /** Called when a hit test finds a surface (AR). Provides the closest hit pose. */
    var onHitTest: ((XRPose) -> Unit)? = null

    /** Called when the user performs a primary select action (tap, trigger). */
    var onInputSelect: ((XRInputSource, XRPose?) -> Unit)? = null

    /** Called when a squeeze action is performed (grip button on controllers). */
    var onInputSqueeze: ((XRInputSource, XRPose?) -> Unit)? = null

    /** Called when input sources change (controllers connected/disconnected). */
    var onInputSourcesChange: ((Array<XRInputSource>, Array<XRInputSource>) -> Unit)? = null

    /** Called when the session ends. */
    var onSessionEnd: (() -> Unit)? = null

    // -- State --

    private var hitTestSource: XRHitTestSource? = null
    private var isRunning = false

    /** Whether this is an AR session. */
    val isAR: Boolean get() = mode == XRSessionMode.IMMERSIVE_AR

    /** Whether this is a VR session. */
    val isVR: Boolean get() = mode == XRSessionMode.IMMERSIVE_VR

    /**
     * Feature configuration for requesting an XR session.
     */
    data class Features(
        val required: Array<String> = emptyArray(),
        val optional: Array<String> = emptyArray()
    )

    companion object {

        /**
         * Check if a session mode is supported in the current browser.
         *
         * @param mode One of [XRSessionMode] constants
         * @param callback Receives true if supported
         */
        fun checkSupport(mode: String = XRSessionMode.IMMERSIVE_AR, callback: (Boolean) -> Unit) {
            val xr = Navigator.xr
            if (xr == null) {
                callback(false)
                return
            }
            xr.isSessionSupported(mode).then { supported: Boolean ->
                callback(supported)
            }.catch {
                callback(false)
            }
        }

        /**
         * Create and configure a WebXR session with Filament rendering.
         *
         * Must be called from a user gesture (click/tap event handler).
         *
         * @param canvas The HTML canvas element for rendering
         * @param mode Session mode — "immersive-ar" or "immersive-vr"
         * @param features Required and optional features to request
         * @param referenceSpaceType The reference space type — defaults to "local-floor"
         * @param onError Called if session creation fails
         * @param onReady Called with the configured session
         */
        fun create(
            canvas: HTMLCanvasElement,
            mode: String = XRSessionMode.IMMERSIVE_AR,
            features: Features = Features(
                required = if (mode == XRSessionMode.IMMERSIVE_AR) arrayOf(XRFeature.HIT_TEST) else emptyArray(),
                optional = arrayOf(XRFeature.DOM_OVERLAY, XRFeature.LIGHT_ESTIMATION, XRFeature.HAND_TRACKING)
            ),
            referenceSpaceType: String = XRReferenceSpaceType.LOCAL_FLOOR,
            onError: ((String) -> Unit)? = null,
            onReady: (WebXRSession) -> Unit
        ) {
            val xr = Navigator.xr
            if (xr == null) {
                onError?.invoke("WebXR not supported in this browser")
                return
            }

            // Build session options
            val options = js("{}")
            if (features.required.isNotEmpty()) {
                options.requiredFeatures = features.required
            }
            if (features.optional.isNotEmpty()) {
                options.optionalFeatures = features.optional
            }

            xr.requestSession(mode, options).then { session: XRSession ->
                // Create SceneView with Filament
                SceneView.create(
                    canvas = canvas,
                    configure = {
                        // XR sessions manage their own camera — disable orbit controls
                        cameraControls(false)
                    },
                    onReady = { sceneView ->
                        // Get WebGL2 context with XR compatibility
                        val gl = canvas.asDynamic().getContext("webgl2", js("{xrCompatible: true}"))
                        val xrLayer = XRWebGLLayer(session, gl)

                        val renderStateInit = js("{}")
                        renderStateInit.baseLayer = xrLayer
                        session.updateRenderState(renderStateInit)

                        session.requestReferenceSpace(referenceSpaceType).then { refSpace: XRReferenceSpace ->
                            val xrSession = WebXRSession(sceneView, session, refSpace, mode, xrLayer)
                            xrSession.setupEventHandlers()

                            // Set up hit testing for AR sessions
                            if (mode == XRSessionMode.IMMERSIVE_AR) {
                                xrSession.setupHitTesting(session)
                            }

                            onReady(xrSession)
                        }
                    }
                )
            }.catch { error: dynamic ->
                val message = error?.message?.toString() ?: "Failed to start XR session"
                onError?.invoke(message)
                console.error("WebXRSession: Failed to start $mode session:", error)
            }
        }
    }

    /** Start the XR render loop. */
    fun start() {
        isRunning = true
        xrSession.requestAnimationFrame(::renderFrame)
    }

    /** Stop the XR session and clean up. */
    fun stop() {
        isRunning = false
        hitTestSource?.cancel()
        hitTestSource = null
        xrSession.end()
    }

    /**
     * Load a 3D model into the AR/VR scene.
     *
     * @param url URL of the glTF/GLB model
     * @param onLoaded Callback when the model is loaded
     */
    fun loadModel(url: String, onLoaded: ((FilamentAsset) -> Unit)? = null) {
        sceneView.loadModel(url, onLoaded)
    }

    /**
     * Set the position of an entity using a 4x4 matrix from an [XRRigidTransform].
     *
     * @param entity The Filament entity ID
     * @param transform The XR transform to apply
     */
    fun setEntityTransform(entity: Int, transform: XRRigidTransform) {
        sceneView.engine.transformManager.let { tm ->
            val instance = tm.getInstance(entity)
            if (instance != 0) {
                tm.setTransform(instance, transform.matrix)
            }
        }
    }

    // -- Private --

    private fun setupEventHandlers() {
        xrSession.onend = {
            isRunning = false
            onSessionEnd?.invoke()
        }

        xrSession.onselect = { event ->
            val inputSource = event.asDynamic().inputSource as? XRInputSource
            if (inputSource != null) {
                val pose = event.asDynamic().frame?.let { frame ->
                    (frame as? XRFrame)?.getPose(inputSource.targetRaySpace, referenceSpace)
                }
                onInputSelect?.invoke(inputSource, pose as? XRPose)
            }
        }

        xrSession.onsqueeze = { event ->
            val inputSource = event.asDynamic().inputSource as? XRInputSource
            if (inputSource != null) {
                val pose = event.asDynamic().frame?.let { frame ->
                    (frame as? XRFrame)?.getPose(inputSource.targetRaySpace, referenceSpace)
                }
                onInputSqueeze?.invoke(inputSource, pose as? XRPose)
            }
        }

        xrSession.oninputsourceschange = { event ->
            val added = event.asDynamic().added as? Array<XRInputSource> ?: emptyArray()
            val removed = event.asDynamic().removed as? Array<XRInputSource> ?: emptyArray()
            onInputSourcesChange?.invoke(added, removed)
        }
    }

    private fun setupHitTesting(session: XRSession) {
        session.requestReferenceSpace(XRReferenceSpaceType.VIEWER).then { viewerSpace: XRReferenceSpace ->
            val hitTestOptions = js("{}")
            hitTestOptions.space = viewerSpace
            session.asDynamic().requestHitTestSource(hitTestOptions).then { source: XRHitTestSource ->
                hitTestSource = source
            }
        }
    }

    private fun renderFrame(timestamp: Double, frame: XRFrame) {
        if (!isRunning) return

        val pose = frame.getViewerPose(referenceSpace)

        // Update Filament camera from XR pose
        if (pose != null) {
            val views = pose.views
            if (views.isNotEmpty()) {
                val primaryView = views[0]

                // Set camera projection from XR view
                sceneView.camera.setModelMatrix(primaryView.transform.matrix)

                // For stereo VR, set projection per-eye
                // (Filament's single-pass stereo is configured via the View)
                val viewport = glLayer.getViewport(primaryView)
                val viewportArray = js("[]")
                viewportArray.push(viewport.x, viewport.y, viewport.width, viewport.height)
                sceneView.view.setViewport(viewportArray)
            }
        }

        // Process hit tests (AR only)
        hitTestSource?.let { source ->
            val results = frame.getHitTestResults(source)
            if (results.isNotEmpty()) {
                val hitPose = results[0].getPose(referenceSpace)
                if (hitPose != null) {
                    onHitTest?.invoke(hitPose)
                }
            }
        }

        // Dispatch frame callback
        onFrame?.invoke(frame, pose)

        // Render with Filament
        if (sceneView.renderer.beginFrame(sceneView.swapChain, timestamp)) {
            sceneView.renderer.renderView(sceneView.view)
            sceneView.renderer.endFrame()
        }

        xrSession.requestAnimationFrame(::renderFrame)
    }
}
