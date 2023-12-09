package io.github.sceneview.sample.modelviewer.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import io.github.sceneview.Scene
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.sample.SceneViewTheme

private const val MODEL_FILE = "models/damaged_helmet.glb"
private const val ENVIRONMENT_FILE = "environments/kloofendal_48d_partly_cloudy_puresky_2k.hdr"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SceneViewTheme {
                // A surface container using the 'background' color from the theme
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // The destroy calls are automatically made when their disposable effect leaves
                    // the composition or its key changes.
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val environmentLoader = rememberEnvironmentLoader(engine)
                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        modelLoader = modelLoader,
                        childNodes = rememberNodes {
                            add(ModelNode(modelLoader.createModelInstance(MODEL_FILE)).apply {
                                // Move the node 4 units in Camera front direction
                                position = Position(z = -4.0f)
                            })
                        },
                        environment = environmentLoader.createHDREnvironment(ENVIRONMENT_FILE)!!
                    )
                }
            }
        }
    }
}