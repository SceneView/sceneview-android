package io.github.sceneview.ar

import androidx.lifecycle.coroutineScope
import com.google.ar.core.Config
import io.github.sceneview.ar.arcore.ArFrame
import io.github.sceneview.ar.arcore.ArSession
import io.github.sceneview.ar.node.ArNode
import io.github.sceneview.node.Node
import io.github.sceneview.ar.node.infos.AugmentedImageInfoNode
import io.github.sceneview.ar.node.infos.SearchPlaneInfoNode
import io.github.sceneview.ar.node.infos.TapArPlaneInfoNode
import io.github.sceneview.SceneView

class Instructions(val sceneView: SceneView, val lifecycle: ArSceneLifecycle) :
    ArSceneLifecycleObserver {

    var enabled = true
        set(value) {
            field = value
            infoNode?.isVisible = value
        }

    val searchPlaneInfoNode by lazy {
        SearchPlaneInfoNode(
            lifecycle.context,
            lifecycle.coroutineScope
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
            lifecycle.coroutineScope
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
            lifecycle.coroutineScope
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
                    parent = sceneView.camera
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
            arFrame.session.hasAugmentedImageDatabase && !arFrame.isTrackingAugmentedImage -> {
                augmentedImageInfoNode
            }
            arFrame.session.planeFindingEnabled -> when {
                !arFrame.session.hasTrackedPlane -> {
                    searchPlaneInfoNode
                }
                sceneView.children.count { it is ArNode } == 0 -> {
                    tapArPlaneInfoNode
                }
                else -> null
            }
            else -> null
        }
    }
}