package io.github.sceneview.sample.arcloudanchor

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.CloudAnchorNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.sample.SceneviewTheme

class Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SceneviewTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    val engine = rememberEngine()
                    val modelLoader = rememberModelLoader(engine)
                    val modelInstance = rememberModelInstance(modelLoader, "models/damaged_helmet.glb")

                    var viewSize by remember { mutableStateOf(IntSize.Zero) }
                    var session by remember { mutableStateOf<Session?>(null) }
                    var frame by remember { mutableStateOf<Frame?>(null) }
                    var cloudAnchorNode by remember { mutableStateOf<CloudAnchorNode?>(null) }

                    var cloudAnchorId by remember {
                        mutableStateOf(
                            getSharedPreferences("preferences", Context.MODE_PRIVATE)
                                .getString("cloudAnchorId", null)
                        )
                    }

                    // UI mode state
                    var mode by remember { mutableStateOf(Mode.HOME) }
                    var editTextValue by remember { mutableStateOf(cloudAnchorId ?: "") }

                    ARScene(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { viewSize = it },
                        engine = engine,
                        modelLoader = modelLoader,
                        sessionConfiguration = { _, config ->
                            config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
                        },
                        onSessionCreated = { s -> session = s },
                        onSessionUpdated = { _, updatedFrame -> frame = updatedFrame }
                    ) {
                        // Cursor node — follows screen center via hit testing
                        val cx = viewSize.width / 2f
                        val cy = viewSize.height / 2f
                        if (cx > 0f && cy > 0f) {
                            HitResultNode(xPx = cx, yPx = cy) {
                                // Show model only in HOST mode
                                if (mode == Mode.HOST) {
                                    modelInstance?.let { instance ->
                                        ModelNode(modelInstance = instance)
                                    }
                                }
                            }
                        }

                        // Cloud anchor node shown when resolved
                        if (cloudAnchorNode != null) {
                            CloudAnchorNode(
                                anchor = cloudAnchorNode!!.anchor,
                                cloudAnchorId = cloudAnchorNode!!.cloudAnchorId
                            )
                        }
                    }

                    // Controls UI overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(16.dp)
                    ) {
                        when (mode) {
                            Mode.HOME -> {
                                Row(
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                ) {
                                    Button(
                                        onClick = { mode = Mode.HOST },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) { Text("Host") }
                                    Button(
                                        onClick = { mode = Mode.RESOLVE }
                                    ) { Text("Resolve") }
                                }
                            }

                            Mode.HOST -> {
                                Button(
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    onClick = {
                                        val s = session ?: return@Button
                                        val cx = viewSize.width / 2f
                                        val cy = viewSize.height / 2f
                                        val anchor = frame
                                            ?.hitTest(cx, cy)
                                            ?.firstOrNull { it.isValid(depthPoint = false, point = false) }
                                            ?.createAnchorOrNull()
                                        if (anchor == null) {
                                            Toast.makeText(
                                                this@Activity,
                                                "Cannot create anchor — point at a surface",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@Button
                                        }
                                        mode = Mode.HOSTING
                                        val newNode = CloudAnchorNode(engine, anchor)
                                        newNode.host(s) { id, state ->
                                            if (state == CloudAnchorState.SUCCESS && id != null) {
                                                cloudAnchorId = id
                                                editTextValue = id
                                                getSharedPreferences("preferences", Context.MODE_PRIVATE)
                                                    .edit().putString("cloudAnchorId", id).apply()
                                                cloudAnchorNode = newNode
                                                mode = Mode.RESET
                                            } else {
                                                Toast.makeText(
                                                    this@Activity,
                                                    "Error hosting",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                mode = Mode.HOST
                                            }
                                        }
                                    }
                                ) { Text("Host Anchor") }
                            }

                            Mode.HOSTING -> {
                                Text(
                                    text = "Hosting...",
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }

                            Mode.RESOLVE -> {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = editTextValue,
                                        onValueChange = { editTextValue = it },
                                        label = { Text("Cloud Anchor ID") },
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Button(
                                        enabled = editTextValue.isNotBlank(),
                                        onClick = {
                                            val s = session ?: return@Button
                                            mode = Mode.RESOLVING
                                            CloudAnchorNode.resolve(engine, s, editTextValue) { state, node ->
                                                if (!state.isError && node != null) {
                                                    cloudAnchorNode = node
                                                    mode = Mode.RESET
                                                } else {
                                                    Toast.makeText(
                                                        this@Activity,
                                                        "Error resolving",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    mode = Mode.RESOLVE
                                                }
                                            }
                                        }
                                    ) { Text("Resolve") }
                                }
                            }

                            Mode.RESOLVING -> {
                                Text(
                                    text = "Resolving...",
                                    modifier = Modifier.align(Alignment.BottomCenter)
                                )
                            }

                            Mode.RESET -> {
                                Button(
                                    modifier = Modifier.align(Alignment.BottomCenter),
                                    onClick = {
                                        cloudAnchorNode?.detachAnchor()
                                        cloudAnchorNode = null
                                        mode = Mode.HOME
                                    }
                                ) { Text("Reset") }
                            }
                        }
                    }
                }
            }
        }
    }

    private enum class Mode {
        HOME, HOST, HOSTING, RESOLVE, RESOLVING, RESET
    }
}
