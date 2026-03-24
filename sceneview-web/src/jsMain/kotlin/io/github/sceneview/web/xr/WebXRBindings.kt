@file:Suppress("INTERFACE_WITH_SUPERCLASS")

package io.github.sceneview.web.xr

import org.w3c.dom.events.EventTarget

/**
 * Kotlin/JS external declarations for the WebXR Device API.
 *
 * These bindings provide access to immersive AR/VR sessions in the browser.
 * WebXR is supported by Chrome 79+, Edge 79+, Safari 18+, Opera 58+.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/WebXR_Device_API">MDN WebXR</a>
 */

// navigator.xr
external interface XRSystem {
    fun isSessionSupported(mode: String): dynamic // Promise<Boolean>
    fun requestSession(mode: String, options: dynamic = definedExternally): dynamic // Promise<XRSession>
}

external interface XRSession : EventTarget {
    val renderState: XRRenderState
    val inputSources: dynamic // XRInputSourceArray
    fun requestReferenceSpace(type: String): dynamic // Promise<XRReferenceSpace>
    fun requestAnimationFrame(callback: (Double, XRFrame) -> Unit): Int
    fun cancelAnimationFrame(handle: Int)
    fun end(): dynamic // Promise<void>
    fun updateRenderState(state: dynamic)
    var onend: ((dynamic) -> Unit)?
    var oninputsourceschange: ((dynamic) -> Unit)?
    var onselect: ((dynamic) -> Unit)?
}

external interface XRRenderState {
    val baseLayer: dynamic // XRWebGLLayer?
}

external interface XRFrame {
    val session: XRSession
    fun getViewerPose(referenceSpace: XRReferenceSpace): XRViewerPose?
    fun getPose(space: XRSpace, baseSpace: XRReferenceSpace): XRPose?
    fun getHitTestResults(source: XRHitTestSource): Array<XRHitTestResult>
}

external interface XRReferenceSpace : XRSpace

external interface XRSpace

external interface XRViewerPose : XRPose {
    val views: Array<XRView>
}

external interface XRPose {
    val transform: XRRigidTransform
}

external interface XRView {
    val eye: String // "left", "right", "none"
    val projectionMatrix: dynamic // Float32Array
    val transform: XRRigidTransform
}

external interface XRRigidTransform {
    val position: DOMPointReadOnly
    val orientation: DOMPointReadOnly
    val matrix: dynamic // Float32Array
}

external interface DOMPointReadOnly {
    val x: Double
    val y: Double
    val z: Double
    val w: Double
}

// Hit testing (AR)
external interface XRHitTestSource

external interface XRHitTestResult {
    fun getPose(baseSpace: XRReferenceSpace): XRPose?
}

// WebGL Layer
external class XRWebGLLayer(session: XRSession, context: dynamic, options: dynamic = definedExternally) {
    val framebuffer: dynamic
    val framebufferWidth: Int
    val framebufferHeight: Int
    fun getViewport(view: XRView): XRViewport
}

external interface XRViewport {
    val x: Int
    val y: Int
    val width: Int
    val height: Int
}
