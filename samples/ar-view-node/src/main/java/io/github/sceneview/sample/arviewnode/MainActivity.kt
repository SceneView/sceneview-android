package io.github.sceneview.sample.arviewnode

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.sample.arviewnode.nodes.events.ImageNodeEvent
import io.github.sceneview.sample.arviewnode.nodes.events.ViewNodeEvent
import io.github.sceneview.sample.arviewnode.nodes.ImageNodeHelper
import io.github.sceneview.sample.arviewnode.nodes.TrackingNode
import io.github.sceneview.sample.arviewnode.nodes.ViewNodeHelper
import io.github.sceneview.sample.arviewnode.utils.NodeRotationHelper


class MainActivity : AppCompatActivity(), MainActivityViewMvc.Listener {

    private lateinit var viewMvc: MainActivityViewMvc

    private lateinit var trackingNode: TrackingNode
    private lateinit var imageNodeHelper: ImageNodeHelper
    private lateinit var viewNodeHelper: ViewNodeHelper
    private lateinit var nodeRotationHelper: NodeRotationHelper

    private val nodeList: MutableList<AnchorNode> = mutableListOf()

    private var nodeType = NodeType.VIEW_NODE

    ////////////////////
    // region Lifecycle Methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewMvc = MainActivityViewMvcImpl(
            this,
            layoutInflater,
            null
        )
        setContentView(viewMvc.getView().root)
        viewMvc.registerListener(this)

        viewMvc.getSceneView().apply {
            initObjects(this)
            configureArSession(this)
            handleOnSessionUpdated(this)

            lifecycle = this@MainActivity.lifecycle
        }
    }

    private fun initObjects(arSceneView: ARSceneView) {
        trackingNode = TrackingNode(
            this,
            arSceneView
        )
        imageNodeHelper = ImageNodeHelper(
            this,
            arSceneView,
            this::onEvent
        )
        viewNodeHelper = ViewNodeHelper(
            this,
            arSceneView,
            this::onEvent
        )
        nodeRotationHelper = NodeRotationHelper()
    }

    private fun configureArSession(arSceneView: ARSceneView) {
        arSceneView.configureSession { session, config ->
            config.depthMode = when (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                true -> Config.DepthMode.AUTOMATIC
                else -> Config.DepthMode.DISABLED
            }
            config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
            config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
            config.planeFindingMode = Config.PlaneFindingMode.DISABLED
        }
    }

    private fun handleOnSessionUpdated(arSceneView: ARSceneView) {
        arSceneView.onSessionUpdated = { _, frame ->
            trackingNode.frameUpdate(frame)
            nodeRotationHelper
                .updateRotationOfViewNodes(
                    viewMvc.getSceneView(),
                    nodeList
                )
            viewMvc.updateBtnEnabledState(trackingNode.isTracking())
        }
    }

    override fun onDestroy() {
        viewMvc.unregisterListener(this)
        super.onDestroy()
    }
    // endregion Lifecycle Methods
    ////////////////////


    ///////////////////////////////
    // region MainActivityViewMvc.Listener
    override fun updateActiveNodeType(nodeType: NodeType) {
        this.nodeType = nodeType
    }

    override fun handleAddClicked() {
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
    // endregion MainActivityViewMvc.Listener
    ///////////////////////////////


    ///////////////////
    // region OnEvent Handling
    private fun onEvent(event: ImageNodeEvent) {
        when (event) {
            is ImageNodeEvent.NewImageNode -> {
                nodeList.add(event.anchorNode)
                viewMvc.getSceneView().addChildNode(event.anchorNode)
            }
        }
    }

    private fun onEvent(event: ViewNodeEvent) {
        when (event) {
            is ViewNodeEvent.NewViewNode -> {
                nodeList.add(event.anchorNode)
                viewMvc.getSceneView().addChildNode(event.anchorNode)
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
