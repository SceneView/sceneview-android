package io.github.sceneview.sample.modelviewer

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.sceneview.SceneView
import io.github.sceneview.loaders.loadHdrIndirectLight
import io.github.sceneview.loaders.loadHdrSkybox
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.nodes.ModelNode
import io.github.sceneview.nodes.ViewNode

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: SceneView
    lateinit var loadingView: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById<SceneView>(R.id.sceneView).apply {
            setLifecycle(lifecycle)
        }
        loadingView = view.findViewById(R.id.loadingView)

        lifecycleScope.launchWhenCreated {
            val hdrFile = "environments/studio_small_09_2k.hdr"
            sceneView.loadHdrIndirectLight(hdrFile, specularFilter = true) {
                intensity(30_000f)
            }
            sceneView.loadHdrSkybox(hdrFile) {
                intensity(50_000f)
            }

            val model = sceneView.modelLoader.loadModel("models/MaterialSuite.glb")!!
            val modelNode = ModelNode(sceneView, model).apply {
                transform(
                    position = Position(z = -4.0f),
                    rotation = Rotation(x = 15.0f)
                )
                scaleToUnitsCube(2.0f)
                // TODO: Fix centerOrigin
                //  centerOrigin(Position(x=-1.0f, y=-1.0f))
                playAnimation()
            }
            sceneView.addChildNode(modelNode)

            val viewNode = ViewNode(
                sceneView = sceneView,
                viewResourceId = R.layout.view_node_layout
            ).apply {
                transform(
                    position = Position(z = -4f),
                    rotation = Rotation()
                )
            }
            sceneView.addChildNode(viewNode)

            loadingView.isGone = true
        }
    }
}