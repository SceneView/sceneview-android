package io.github.sceneview.sample.gltfcamera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.sceneview.Scene
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.sample.SceneviewTheme

private const val kAperture = 16f
private const val kShutterSpeed = 1f / 125f
private const val kSensitivity = 100f

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val environmentLoader = rememberEnvironmentLoader(engine)

                    val modelInstance = rememberModelInstance(modelLoader, "models/brainstem_cameras.glb")

                    val defaultCameraNode = rememberCameraNode(engine)

                    var viewSize by remember { mutableStateOf(IntSize.Zero) }

                    // Captured from ModelNode's apply block so we can access its GLTF camera nodes
                    var modelNode by remember { mutableStateOf<ModelNode?>(null) }
                    val cameraNodes = remember(modelNode) {
                        modelNode?.cameraNodes?.also { nodes ->
                            nodes.forEach { it.setExposure(kAperture, kShutterSpeed, kSensitivity) }
                        } ?: emptyList()
                    }

                    var selectedCameraNode by remember(cameraNodes) {
                        mutableStateOf(cameraNodes.getOrNull(0))
                    }

                    // Update projection whenever the selected camera or viewport size changes
                    val aspect = if (viewSize.height > 0) viewSize.width.toDouble() / viewSize.height else 1.0
                    selectedCameraNode?.updateProjection(aspect = aspect, near = 0.05f, far = 5000.0f)

                    val environment = rememberEnvironment(environmentLoader) {
                        environmentLoader.createHDREnvironment("environments/symmetrical_garden_02_4k.hdr")!!
                    }

                    Scene(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size -> viewSize = size },
                        engine = engine,
                        modelLoader = modelLoader,
                        cameraNode = selectedCameraNode ?: defaultCameraNode,
                        environment = environment
                    ) {
                        modelInstance?.let { instance ->
                            ModelNode(
                                modelInstance = instance,
                                scaleToUnits = 1.0f,
                                autoAnimate = true,
                                apply = {
                                    if (modelNode == null) modelNode = this
                                }
                            )
                        }
                    }

                    if (cameraNodes.isNotEmpty()) {
                        ChipsGroup(
                            labels = cameraNodes.mapIndexed { i, node ->
                                node.name?.takeIf { it.isNotBlank() && it != "<unknown>" }
                                    ?: "Camera ${i + 1}"
                            },
                            onSelected = { selectedCameraNode = cameraNodes[it] }
                        )
                    }

                    TopAppBar(
                        title = {
                            Image(
                                modifier = Modifier.width(192.dp),
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Logo"
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun BoxScope.ChipsGroup(labels: List<String>, onSelected: (index: Int) -> Unit) {
        var selectedIndex by remember { mutableIntStateOf(0) }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .navigationBarsPadding()
                .padding(8.dp)
        ) {
            labels.forEachIndexed { index, label ->
                val selected = selectedIndex == index
                FilterChip(
                    label = {
                        Text(
                            style = MaterialTheme.typography.bodyLarge,
                            text = label
                        )
                    },
                    modifier = Modifier.padding(4.dp),
                    selected = selected,
                    onClick = {
                        selectedIndex = index
                        onSelected(index)
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Done,
                                contentDescription = "Done icon",
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
