package io.github.sceneview.sample.arimagenode


import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.sample.arimagenode.nodes.ImageNodeHelper
import io.github.sceneview.sample.arimagenode.nodes.TrackingNode
import io.github.sceneview.sample.arimagenode.nodes.events.ImageNodeEvent
import io.github.sceneview.sample.arimagenode.utils.NodeRotationHelper


class MainActivity : AppCompatActivity(), MainActivityViewMvc.Listener {
    private lateinit var viewMvc: MainActivityViewMvc

    private lateinit var trackingNode: TrackingNode
    private lateinit var imageNodeHelper: ImageNodeHelper
    private lateinit var nodeRotationHelper: NodeRotationHelper

    private val nodeList: MutableList<AnchorNode> = mutableListOf()

    ////////////////////
    // region Lifecycle Methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewMvc = MainActivityViewMvcImpl(
            layoutInflater,
            null
        )
        setContentView(viewMvc.getView().root)
        viewMvc.registerListener(this)

        viewMvc.getSceneView().let {
            initObjects(it)
            configureArSession(it)
            handleOnSessionUpdated(it)

            it.lifecycle = this.lifecycle
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
            this::onImageNodeEvent
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
    override fun handleAddClicked() {
        if (trackingNode.isHitting()) {
            trackingNode.getLastHitResult()?.let { hitResult ->
                hitResult.createAnchorOrNull().apply {
                    if (this != null) {
                        imageNodeHelper.addImageNode(this)
                    }
                }
            }
        }
    }
    // endregion MainActivityViewMvc.Listener
    ///////////////////////////////


    ///////////////////
    // region OnEvent Handling
    private fun onImageNodeEvent(event: ImageNodeEvent) {
        when (event) {
            is ImageNodeEvent.NewImageNode -> {
                nodeList.add(event.anchorNode)
                viewMvc.getSceneView().addChildNode(event.anchorNode)
            }
        }
    }
    // endregion OnEvent Handling
    ///////////////////
}