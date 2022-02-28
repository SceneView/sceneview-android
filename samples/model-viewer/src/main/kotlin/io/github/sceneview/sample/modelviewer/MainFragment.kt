package io.github.sceneview.sample.modelviewer

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.utils.HDRLoader
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.SceneView
import io.github.sceneview.environment.loadEnvironment
import io.github.sceneview.node.ModelNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: SceneView
    lateinit var loadingView: View

    var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById(R.id.sceneView)
        loadingView = view.findViewById(R.id.loadingView)

        isLoading = true
        sceneView.camera.position = Position(x = 4.0f, y = -1.0f)
        sceneView.camera.quaternion = Quaternion.fromEuler(Rotation(x = 0.0f, y = 80.0f))

        val modelNode = ModelNode()
        sceneView.addChild(modelNode)

        lifecycleScope.launchWhenCreated {
            sceneView.environment = HDRLoader.loadEnvironment(
                context = requireContext(),
                hdrFileLocation = "environments/studio_small_09_2k.hdr",
                specularFilter = false
            )
            modelNode.loadModel(
                context = requireContext(),
                glbFileLocation = "https://sceneview.github.io/assets/models/MaterialSuite.glb",
                autoAnimate = true,
                autoScale = true
            )

            // We actually have an issue while the model render is not completely loaded
            withContext(Dispatchers.IO) {
                delay(200)
            }
            isLoading = false
            sceneView.camera.smooth(
                position = Position(x = -1.0f, y = 1.5f, z = -3.5f),
                rotation = Quaternion.fromEuler(Rotation(x = -60.0f, y = -50.0f)),
                smoothSpeed = 0.5f
            )
        }
    }
}