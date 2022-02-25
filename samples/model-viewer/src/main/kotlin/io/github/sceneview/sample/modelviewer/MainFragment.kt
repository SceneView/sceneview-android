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
import io.github.sceneview.utils.Position
import io.github.sceneview.utils.Rotation
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
        lifecycleScope.launchWhenCreated {
            sceneView.environment = HDRLoader.loadEnvironment(
                context = requireContext(),
                hdrFileLocation = "environments/studio_small_09_2k.hdr"
            )
        }
        sceneView.addChild(
            ModelNode(
                context = requireContext(),
                coroutineScope = lifecycleScope,
                glbFileLocation = "https://sceneview.github.io/assets/models/MaterialSuite.glb",
                autoAnimate = true,
                autoScale = true,
                onModelLoaded = {
                    lifecycleScope.launchWhenCreated {
                        // We actually have an issue while the model render not completely loaded
                        withContext(Dispatchers.IO) {
                            delay(500)
                        }
                        isLoading = false
                        sceneView.camera.smooth(
                            position = Position(x = -2.0f, y = 1.0f, z = -2.5f),
                            rotation = Quaternion.fromEuler(Rotation(x = -15.0f, y = -50.0f)),
                            smoothSpeed = 0.5f
                        )
                    }
                }
            )
        )
    }
}