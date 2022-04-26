package io.github.sceneview.sample.modelviewer

import android.graphics.drawable.*
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.Colors
import com.google.android.filament.LightManager
import com.google.android.filament.utils.KTXLoader
import com.google.ar.sceneform.Camera
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import io.github.sceneview.Filament
import io.github.sceneview.SceneView
import io.github.sceneview.environment.loadEnvironment
import io.github.sceneview.light.build
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.delay
import java.util.*

class MainFragment : Fragment(R.layout.fragment_main) {
    enum class Prop { CHAIR, TABLE, WALL, DOORFRAME }

    lateinit var sceneView: SceneView
    lateinit var loadingView: View

    lateinit var cameraOrbit: Node;
    lateinit var camera: Camera;

    var modelNode = ModelNode();

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

        sceneView.addChild(modelNode)

        lifecycleScope.launchWhenCreated {
            sceneView.environment = KTXLoader.loadEnvironment(
                context = requireContext(),
                lifecycle = lifecycle,
                iblKtxFileLocation = "environments/default_environment_ibl.ktx",
                skyboxKtxFileLocation = null
            )

            sceneView.mainLight = LightManager.Builder(LightManager.Type.DIRECTIONAL).apply {
                val (r, g, b) = Colors.cct(6_500.0f)
                color(r, g, b)
                intensity(100_000.0f)
                direction(0.28f, -0.6f, -0.76f)
                castShadows(false)
            }.build(lifecycle)

            sceneView.background = null

            modelNode.loadModel(
                context = requireContext(),
                lifecycle = lifecycle,
                glbFileLocation = "models/out.glb",
                autoAnimate = true,
                autoScale = true,
                centerOrigin = Position(x = 0.0f, y = 0.0f, z = 0.0f)
            )
            modelNode.worldPosition = Float3(0f, 0f, 0f)
            // We currently have an issue while the model render is not completely loaded
            delay(200)
            isLoading = false

            // Initialize cameraOrbit and camera
            setupCamera();
        }
    }

    fun setCameraPosition(position: Float3) {
        cameraOrbit.worldPosition = position
    }

    fun setCameraRotation(vRotationRadians: Float, hRotationRadians: Float) {
        // Reset camera rotation first
        cameraOrbit.rotation = Float3(0f, 0f, 0f);

        this.setCameraVerticalRotation(vRotationRadians);
        this.setCameraHorizontalRotation(hRotationRadians);
    }

    private fun setCameraVerticalRotation(radians: Float) {
        val rotation = cameraOrbit.rotation

        val cameraOrbitRotation = Quaternion
            .fromEuler(Float3(Math.toDegrees(radians.toDouble()).toFloat(), 0f, 0f))
            .times(Quaternion.fromEuler(rotation))

        System.out.println("Setting Vertical Rotation: " + cameraOrbitRotation.toString() + " | " + Math.toDegrees(radians.toDouble()));
        cameraOrbit.rotation = cameraOrbitRotation.toEulerAngles()
    }

    private fun setCameraHorizontalRotation(radians: Float) {
        val cameraOrbitRotation = Quaternion
            .fromEuler(Float3(0f, Math.toDegrees(radians.toDouble()).toFloat(), 0f))
            .times(Quaternion.fromEuler(cameraOrbit.rotation))

        System.out.println("Setting Horizontal Rotation: " + cameraOrbitRotation.toString() + " | " + Math.toDegrees(radians.toDouble()) + " | " + radians);
        cameraOrbit.rotation = cameraOrbitRotation.toEulerAngles()
    }

    fun setCameraZoom(zoom: Float) {
        camera.position = Float3(0f, 0f, zoom)
    }

    private fun setupCamera() {
        cameraOrbit = Node()
        cameraOrbit.worldPosition = Float3(0f, 0f, 0f)

        camera = sceneView.camera
        camera.position = Float3(0f, 0f, 5f)

        // Following iOS implementation; only the camera moves around the model.
        this.setCameraZoom(3f);

        // Set initial values
        val vRotationRadians = -0.374f;
        val hRotationRadians = -1.566f;

        this.setCameraRotation(vRotationRadians, hRotationRadians);

        // Add nodes to scene
        sceneView.addChild(cameraOrbit);
        sceneView.removeChild(camera)
        cameraOrbit.addChild(camera)

        // Make sure orbit is selected so that we can control it
        sceneView.gestureDetector.onTouchNode(cameraOrbit)

//        enablePropMeshVisibility(EnumSet.noneOf(Prop::class.java))
        enablePropMeshVisibility(EnumSet.of(Prop.CHAIR))
    }

    fun enablePropMeshVisibility(requiredProps: EnumSet<Prop>) {
        // Access sub-meshes
        val renderableManager = Filament.renderableManager
        val asset = modelNode.modelInstance?.filamentAsset

        if (asset !== null) {
            for (entity in asset.entities) {
                val renderableInstance = renderableManager.getInstance(entity)
                if (renderableInstance == 0) {
                    continue;
                }

                // Set visible first
                renderableManager.setLayerMask(renderableInstance, 0xff, 0xff);

                if (asset.getName(entity).equals("Chair01") && !requiredProps.contains(Prop.CHAIR)) {
                    renderableManager.setLayerMask(renderableInstance, 0xff, 0x00);
                }
                if (asset.getName(entity).equals("Doorway01") && !requiredProps.contains(Prop.DOORFRAME)) {
                    renderableManager.setLayerMask(renderableInstance, 0xff, 0x00);
                }
                if (asset.getName(entity).equals("Table01") && !requiredProps.contains(Prop.TABLE)) {
                    renderableManager.setLayerMask(renderableInstance, 0xff, 0x00);
                }
                if (asset.getName(entity).equals("Wall01") && !requiredProps.contains(Prop.WALL)) {
                    renderableManager.setLayerMask(renderableInstance, 0xff, 0x00);
                }
            }
        }
    }
}