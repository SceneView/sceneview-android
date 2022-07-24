package io.github.sceneview.ar

import com.google.ar.core.Config
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.ar.node.infos.AugmentedImageInfoNode
import io.github.sceneview.ar.node.infos.SearchPlaneInfoNode
import io.github.sceneview.ar.node.infos.TapArPlaneInfoNode
import io.github.sceneview.node.Node

class Instructions(private val lifecycle: ArSceneLifecycle) : ArSceneLifecycleObserver {

    private val sceneView get() = lifecycle.sceneView

    var enabled = true
        set(value) {
            field = value
            infoNode?.isVisible = value
        }

    val searchPlaneInfoNode by lazy {
        SearchPlaneInfoNode(
            lifecycle.context,
            lifecycle
        )
    }
    var searchPlaneInfoEnabled = true
        set(value) {
            field = value
            searchPlaneInfoNode.isVisible = value
        }
    val tapArPlaneInfoNode by lazy {
        TapArPlaneInfoNode(
            lifecycle.context,
            lifecycle
        )
    }
    var tapArPlaneInfoEnabled = true
        set(value) {
            field = value
            tapArPlaneInfoNode.isVisible = value
        }
    val augmentedImageInfoNode by lazy {
        AugmentedImageInfoNode(
            lifecycle.context,
            lifecycle
        )
    }
    var augmentedImageInfoEnabled = true
        set(value) {
            field = value
            augmentedImageInfoNode.isVisible = value
        }
    var infoNode: Node? = null
        set(value) {
            if (field != value) {
                field?.let { it.parent = null }
                field = value?.apply {
                    isVisible = this@Instructions.enabled
                    parent = sceneView.cameraNode
                }
                field = value
            }
        }

    init {
        lifecycle.addObserver(this)
    }

    override fun onArSessionConfigChanged(session: ArSession, config: Config) {
        infoNode = when {
            session.hasAugmentedImageDatabase -> augmentedImageInfoNode
            session.planeFindingEnabled -> searchPlaneInfoNode
            else -> null
        }
    }

    override fun onArFrame(arFrame: ArFrame) {
        super.onArFrame(arFrame)

        infoNode = when {
            augmentedImageInfoEnabled &&
                    arFrame.session.hasAugmentedImageDatabase &&
                    !arFrame.isTrackingAugmentedImage -> {
                augmentedImageInfoNode
            }
            arFrame.session.planeFindingEnabled -> when {
                searchPlaneInfoEnabled && !arFrame.session.hasTrackedPlane -> {
                    searchPlaneInfoNode
                }
                tapArPlaneInfoEnabled && sceneView.children.count { it is ArNode } == 0 -> {
                    null
                }
                else -> null
            }
            else -> null
        }
    }
}