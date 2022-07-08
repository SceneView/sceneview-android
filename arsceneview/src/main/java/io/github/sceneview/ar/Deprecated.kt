package io.github.sceneview.ar

import com.google.ar.core.*
import io.github.sceneview.SceneView
import io.github.sceneview.node.Node
import io.github.sceneview.utils.FrameTime

// Deprecated

/** ## Deprecated: Use [ArSceneView] */
@Deprecated("Use ArSceneView", replaceWith = ReplaceWith(expression = "ArSceneView(context)"))
open class BaseArFragment {

    /** ## Deprecated: Use [ArSceneView] */
    @Deprecated(
        "Use ArSceneView",
        replaceWith = ReplaceWith(expression = "findViewById<ArSceneView>(R.id.arSceneView)")
    )
    fun getArSceneView() = null

    /** ## Deprecated: Use [ArSceneView.onARCoreException] */
    @Deprecated(
        "Use ArSceneView.onARCoreException",
        replaceWith = ReplaceWith(expression = "arSceneView.onARCoreException = { exception -> }")
    )
    fun setOnArUnavailableListener(any: Any?) = null

    /** ## Deprecated: Use [ArSceneView.instructions] */
    @Deprecated(
        "Use ArSceneView.instructions",
        replaceWith = ReplaceWith(expression = "arSceneView.instructions")
    )
    fun getInstructionsController() = null

    /** ## Deprecated: Use [ArSceneView.nodeGestureRecognizer] */
    @Deprecated(
        "Use ArSceneView.nodeGestureRecognizer",
        replaceWith = ReplaceWith(expression = "arSceneView.nodeGestureRecognizer")
    )
    fun getTransformationSystem() = null

    /** ## Deprecated: Use [ArSceneView.configureSession] */
    @Deprecated(
        "Use ArSceneView.configureSession",
        replaceWith = ReplaceWith(expression = "arSceneView.configureSession = { config -> }")
    )
    fun setOnSessionConfigurationListener(any: Any?) = null

    /** ## Deprecated: Use [ArSceneView.onTouchAr] */
    @Deprecated(
        "Use ArSceneView.onTouchAr",
        replaceWith = ReplaceWith(expression = "arSceneView.onTouchAr = { hitResult, _ -> }")
    )
    fun setOnTapArPlaneListener(listener: Any?) = null

    /** ## Deprecated: Use [ArSceneView.onAugmentedImageUpdate] */
    @Deprecated(
        "Use ArSceneView.onAugmentedImageUpdate",
        replaceWith = ReplaceWith(expression = "arSceneView.onAugmentedImageUpdate = { augmentedImage -> }")
    )
    fun setOnAugmentedImageUpdateListener(listener: Any?) = null

    /** ## Deprecated: Use [ArSceneView.onAugmentedFaceUpdate] */
    @Deprecated(
        "Use ArSceneView.onAugmentedFaceUpdate",
        replaceWith = ReplaceWith(expression = "arSceneView.onAugmentedFaceUpdate = { augmentedFace -> }")
    )
    fun setOnAugmentedFaceUpdateListener(listener: Any?) = null
}

/** ## Deprecated: Use [ArSceneView] */
@Deprecated("Use ArSceneView", replaceWith = ReplaceWith(expression = "ArSceneView(context)"))
class ArFragment : BaseArFragment() {

    /** ## Deprecated: Use [ArSceneView.onArSessionResumed] */
    @Deprecated(
        "Use ArSceneView",
        replaceWith = ReplaceWith(expression = "arSceneView")
    )
    fun setOnViewCreatedListener(any: Any?) = null

    /** ## Deprecated: Use [ArSceneView.setOnTapArPlaneGlbModel] */
    @Deprecated(
        "Use ArSceneView.setOnTapArPlaneGlbModel",
        replaceWith = ReplaceWith(expression = "arSceneView.setOnTapArPlaneGlbModel(glbSource)")
    )
    fun setOnTapPlaneGlbModel(glbSource: String) {
    }

    /** ## Deprecated: Use [ArSceneView.setOnTapArPlaneGlbModel] */
    @Deprecated(
        "Use ArSceneView.setOnTapArPlaneGlbModel",
        replaceWith = ReplaceWith(expression = "arSceneView.setOnTapArPlaneGlbModel(glbSource)")
    )
    fun ArFragment.setOnTapPlaneGlbModel(
        glbSource: String,
        listener: OnTapModelListener
    ) {
    }

    /** ## Deprecated: Use [ArSceneView.setOnTapArPlaneGlbModel] */
    @Deprecated(
        "Use ArSceneView.setOnTapArPlaneGlbModel",
        replaceWith = ReplaceWith(expression = "arSceneView.setOnTapArPlaneGlbModel(glbSource)")
    )
    interface OnTapModelListener
}


/**
 * ## Deprecated: Use [ArSceneView]
 *
 * ### All functions are directly on [SceneView] and [ArSceneview]*
 * *= [SceneView]/[ArSceneView] is a Scene (NodeParent)* that can contain
 * - Camera Node
 * - Lights Nodes
 * - Model Nodes
 * - ...
 */
@Deprecated("Use ArSceneView directly", replaceWith = ReplaceWith("SceneView(context)"))
class Scene {
    /** ## Deprecated: Use [ArSceneView.addChild] */
    @Deprecated(
        "Use ArSceneView.addChild",
        replaceWith = ReplaceWith(expression = "arSceneView.addChild(child)")
    )
    fun onAddChild(child: Any?) = null

    /** ## Deprecated: Use [ArSceneView.removeChild] */
    @Deprecated(
        "Use ArSceneView.removeChild",
        replaceWith = ReplaceWith(expression = "arSceneView.removeChild(child)")
    )
    fun onRemoveChild(child: Any?) = null

    /** ## Deprecated: Use [ArSceneView.onArFrame] */
    @Deprecated(
        "Use ArSceneView.onArFrame",
        replaceWith = ReplaceWith(expression = "onArFrame = { session, frame, camera -> }")
    )
    fun addOnUpdateListener(listener: Any?) = null
}

// SceneView

/**
 * ## Deprecated: Use [ArSceneView]
 *
 * ### All functions are directly on [SceneView] and [ArSceneview]*
 * *= [SceneView]/[ArSceneView] is a Scene (NodeParent)* that can contain
 * - Camera Node
 * - Lights Nodes
 * - Model Nodes
 * - ...
 */
@Deprecated("Use SceneView directly", replaceWith = ReplaceWith("sceneView"))
fun SceneView.getScene() = null


// Session

/** ## Deprecated: Use [ArSceneView.arSessionConfig] */
@Deprecated(
    "Use ArSceneView.sessionConfig",
    replaceWith = ReplaceWith(expression = "arSceneView.sessionConfig()")
)
fun Session.getConfig() = config

/** ## Deprecated: Use [ArSceneView.configureSession] */
@Deprecated(
    "Use ArSceneView.configureSession()",
    replaceWith = ReplaceWith(expression = "arSceneView.configureSession { config -> }")
)
fun Session.configure(config: Config) = null


// Node

/** ## Deprecated: Use [Node.position] */
@Deprecated(
    "Use position",
    replaceWith = ReplaceWith(expression = "position")
)
var Node.localPosition
    get() = position
    set(value) {
        position = value
    }


/** ## Deprecated: Use [Node.rotation] */
@Deprecated(
    "Use rotation",
    replaceWith = ReplaceWith(expression = "rotation")
)
var Node.localRotation
    get() = rotation
    set(value) {
        rotation = value
    }


/** ## Deprecated: Use [Node.scale] */
@Deprecated(
    "Use scale",
    replaceWith = ReplaceWith(expression = "scale")
)
var Node.localScale
    get() = scale
    set(value) {
        scale = value
    }


/** ## Deprecated: Use [Node.isSelectable] */
@Deprecated(
    "Use isSelectable",
    replaceWith = ReplaceWith(expression = "isSelectable")
)
var Node.isFocusable
    get() = isSelectable
    set(value) {
        isSelectable = value
    }

/** ## Deprecated: Use [Node.onFrameUpdate] */
@Deprecated(
    "Use Node.onFrameUpdated()",
    replaceWith = ReplaceWith(expression = "onFrameUpdated()")
)
fun Node.onUpdated(node: Node, frameTime: FrameTime) = null

/** ## Deprecated: Use [Node.onFrameUpdate] */
@Deprecated(
    "Use Node.onFrameUpdated()",
    replaceWith = ReplaceWith(expression = "onFrameUpdated = {}")
)
fun Node.addLifecycleListener(listener: Any?) = null

/** ## Deprecated: Use [Node.onTransformChanged] */
@Deprecated(
    "Use Node.onTransformChanged",
    replaceWith = ReplaceWith(expression = "node.onTransformChanged = {}")
)
fun Node.addTransformChangedListener(listener: Any?) = null