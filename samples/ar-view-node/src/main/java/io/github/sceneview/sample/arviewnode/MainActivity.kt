package io.github.sceneview.sample.arviewnode

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.sample.arviewnode.databinding.ActivityMainBinding
import io.github.sceneview.sample.arviewnode.nodes.events.ImageNodeEvent
import io.github.sceneview.sample.arviewnode.nodes.events.ViewNodeEvent
import io.github.sceneview.sample.arviewnode.nodes.target.ImageNodeHelper
import io.github.sceneview.sample.arviewnode.nodes.target.TrackingNode
import io.github.sceneview.sample.arviewnode.nodes.target.ViewNodeHelper
import io.github.sceneview.sample.arviewnode.utils.NodeRotationHelper


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var view: ActivityMainBinding
    private lateinit var sceneView: ARSceneView
    private lateinit var trackingNode: TrackingNode
    private lateinit var imageNodeHelper: ImageNodeHelper
    private lateinit var viewNodeHelper: ViewNodeHelper

    private val nodeList: MutableList<AnchorNode> = mutableListOf()

    private var nodeType = NodeType.VIEW_NODE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        view = ActivityMainBinding
            .inflate(
                layoutInflater,
                null,
                false
            )

        setContentView(view.root)

        sceneView = findViewById<ARSceneView?>(R.id.sceneView).apply {
            trackingNode = TrackingNode(
                this@MainActivity,
                this
            )
            imageNodeHelper = ImageNodeHelper(
                this@MainActivity,
                this,
                this@MainActivity::onEvent
            )
            viewNodeHelper = ViewNodeHelper(
                this@MainActivity,
                this,
                this@MainActivity::onEvent
            )

            configureSession { session, config ->
                config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    true -> Config.DepthMode.AUTOMATIC
                    else -> Config.DepthMode.DISABLED
                }
                config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.planeFindingMode = Config.PlaneFindingMode.DISABLED
            }

            onSessionUpdated = { _, frame ->
                trackingNode.frameUpdate(frame)
                NodeRotationHelper
                    .updateRotationOfViewNodes(
                        sceneView,
                        nodeList
                    )

                updateBtnEnabledState()
            }

            lifecycle = this@MainActivity.lifecycle
        }

        view.btnAdd.setOnClickListener(this)
        view.btnViewNode.setOnClickListener(this)
        view.btnImageNode.setOnClickListener(this)
    }

    private fun updateBtnEnabledState() {
        if (trackingNode.isTracking()) {
            if (!view.btnAdd.isEnabled) {
                view.btnAdd.isEnabled = true
            }
        } else {
            if (view.btnAdd.isEnabled) {
                view.btnAdd.isEnabled = false
            }
        }
    }

    ///////////////////////
    // region View.OnClickListener
    override fun onClick(v: View) {
        when (v.id) {
            view.btnAdd.id -> {
                handleOnAddClicked()
            }

            view.btnViewNode.id -> {
                handleOnSetViewNodeClicked()
            }

            view.btnImageNode.id -> {
                handleOnSetImageNodeClicked()
            }
        }
    }

    private fun handleOnAddClicked() {
        if (trackingNode.isHitting()) {
            trackingNode.getLastHitResult()?.let { hitResult ->
                hitResult.createAnchorOrNull().apply {
                    if (this != null) {
                        when (nodeType) {
                            NodeType.VIEW_NODE -> {
                                viewNodeHelper.addViewNode(this)
                            }

                            NodeType.IMAGE_NODE -> {
                                imageNodeHelper.addImageNode(this)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleOnSetViewNodeClicked() {
        nodeType = NodeType.VIEW_NODE

        applyActiveColor(view.btnViewNode)
        applyDisabledColor(view.btnImageNode)
    }

    private fun handleOnSetImageNodeClicked() {
        nodeType = NodeType.IMAGE_NODE

        applyActiveColor(view.btnImageNode)
        applyDisabledColor(view.btnViewNode)
    }

    private fun applyActiveColor(view: View) {
        view.backgroundTintList = ContextCompat
            .getColorStateList(
                this,
                R.color.main_color
            )
    }

    private fun applyDisabledColor(view: View) {
        view.backgroundTintList = ContextCompat
            .getColorStateList(
                this,
                R.color.gray_500
            )
    }
    // endregion View.OnClickListener
    ///////////////////////


    ///////////////////
    // region OnEvent Handling
    private fun onEvent(event: ImageNodeEvent) {
        when (event) {
            is ImageNodeEvent.NewImageNode -> {
                nodeList.add(event.anchorNode)
                sceneView.addChildNode(event.anchorNode)
            }
        }
    }

    private fun onEvent(event: ViewNodeEvent) {
        when (event) {
            is ViewNodeEvent.NewViewNode -> {
                nodeList.add(event.anchorNode)
                sceneView.addChildNode(event.anchorNode)
            }
        }
    }
    // endregion OnEvent Handling
    ///////////////////


    companion object {
        const val HITTEST_SKIP_AMOUNT = 0
    }

    enum class NodeType {
        IMAGE_NODE,
        VIEW_NODE
    }
}
