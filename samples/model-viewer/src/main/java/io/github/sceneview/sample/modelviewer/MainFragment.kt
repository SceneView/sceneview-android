package io.github.sceneview.sample.modelviewer

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.launch

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var sceneView: SceneView
    private lateinit var loadingView: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById(R.id.sceneView)
        loadingView = view.findViewById(R.id.loadingView)

        viewLifecycleOwner.lifecycleScope.launch {
            val hdrFile = "environments/studio_small_09_2k.hdr"
            sceneView.environmentLoader.loadHDREnvironment(hdrFile)
            val modelFile = "models/MaterialSuite.glb"
            val modelInstance = sceneView.modelLoader.createModelInstance(modelFile)

            val modelNode = ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 2.0f,
            ).apply {
                transform(
                    position = Position(z = -4.0f),
                    rotation = Rotation(x = 15.0f)
                )
            }
            sceneView.addChildNode(modelNode)
            loadingView.isGone = true
        }
    }
}