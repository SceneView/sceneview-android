package io.github.sceneview.sample.arcloudanchor

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Guideline
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Anchor.CloudAnchorState
import com.google.ar.core.Config
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.canHostCloudAnchor
import io.github.sceneview.ar.node.CloudAnchorNode
import io.github.sceneview.ar.node.HitResultNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.sample.doOnApplyWindowInsets
import kotlinx.coroutines.Job
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class MainFragment : Fragment(R.layout.fragment_main) {

    private lateinit var sceneView: ARSceneView
    private lateinit var loadingView: View
    private lateinit var editText: EditText
    private lateinit var hostButton: Button
    private lateinit var resolveButton: Button
    private lateinit var actionButton: ExtendedFloatingActionButton

    private lateinit var cursorNode: HitResultNode
    private lateinit var modelNode: ModelNode
    private lateinit var cloudAnchorNode: CloudAnchorNode

    private var cloudAnchorResolveJob: Job? = null

    private var mode = Mode.HOME
        set(value) {
            field = value
            when (value) {
                Mode.HOME -> {
                    editText.isVisible = false
                    hostButton.isVisible = true
                    resolveButton.isVisible = true
                    actionButton.isVisible = false
                    cloudAnchorNode.isVisible = false
                }

                Mode.HOST -> {
                    hostButton.isVisible = false
                    resolveButton.isVisible = false
                    actionButton.apply {
                        setIconResource(R.drawable.ic_host)
                        setText(R.string.host)
                        isVisible = true
                        isEnabled = true
                    }
                    cloudAnchorNode.isVisible = true
                }

                Mode.RESOLVE -> {
                    editText.isVisible = true
                    hostButton.isVisible = false
                    resolveButton.isVisible = false
                    actionButton.apply {
                        setIconResource(R.drawable.ic_resolve)
                        setText(R.string.resolve)
                        isVisible = true
                        isEnabled = editText.text.isNotEmpty()
                    }
                }

                Mode.RESET -> {
                    editText.isVisible = true
                    actionButton.apply {
                        setIconResource(R.drawable.ic_reset)
                        setText(R.string.reset)
                        isEnabled = true
                    }
                }
            }
        }

    private var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    var cloudAnchorId by requireContext().getSharedPreferences("preferences", Context.MODE_PRIVATE)
        .string(defaultValue = null)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val topGuideline = view.findViewById<Guideline>(R.id.topGuideline)
        topGuideline.doOnApplyWindowInsets { systemBarsInsets ->
            // Add the action bar margin
            val actionBarHeight =
                (requireActivity() as AppCompatActivity).supportActionBar?.height ?: 0
            topGuideline.setGuidelineBegin(systemBarsInsets.top + actionBarHeight)
        }
        val bottomGuideline = view.findViewById<Guideline>(R.id.bottomGuideline)
        bottomGuideline.doOnApplyWindowInsets { systemBarsInsets ->
            // Add the navigation bar margin
            bottomGuideline.setGuidelineEnd(systemBarsInsets.bottom)
        }

        sceneView = view.findViewById<ARSceneView>(R.id.sceneView).apply {
            configureSession { _, config ->
                config.cloudAnchorMode = Config.CloudAnchorMode.ENABLED
            }
            cursorNode = HitResultNode(
                engine = engine,
                xPx = width / 2.0f,
                yPx = height / 2.0f
            ).apply {
                modelNode = ModelNode(
                    modelLoader.createModelInstance("models/spiderbot.glb")
                )
                addChildNode(modelNode)
            }
            addChildNode(cursorNode)
        }

        loadingView = view.findViewById(R.id.loadingView)

        actionButton = view.findViewById<ExtendedFloatingActionButton>(R.id.actionButton).apply {
            setOnClickListener {
                actionButtonClicked()
            }
        }

        editText = view.findViewById<EditText>(R.id.editText).apply {
            addTextChangedListener {
                actionButton.isEnabled = !it.isNullOrBlank()
            }
            setText(cloudAnchorId ?: "")
        }

        hostButton = view.findViewById<Button?>(R.id.hostButton).apply {
            setOnClickListener { mode = Mode.HOST }
        }

        resolveButton = view.findViewById<Button?>(R.id.resolveButton).apply {
            setOnClickListener { mode = Mode.RESOLVE }
        }
    }

    private fun actionButtonClicked() {
        when (mode) {
            Mode.HOME -> {}
            Mode.HOST -> {
                val session = sceneView.session ?: return
                if (!session.canHostCloudAnchor(sceneView.cameraNode)) {
                    Toast.makeText(context, R.string.insufficient_visual_data, Toast.LENGTH_LONG)
                        .show()
                    return
                }
                val anchor = cursorNode.createAnchor() ?: return

                sceneView.addChildNode(CloudAnchorNode(sceneView.engine, anchor).apply {
                    host(session) { cloudAnchorId, state ->
                        mode = if (state == CloudAnchorState.SUCCESS && cloudAnchorId != null) {
                            editText.setText(cloudAnchorId)
                            Mode.RESET
                        } else {
                            Toast.makeText(context, R.string.error_occurred, Toast.LENGTH_LONG)
                                .show()
                            Mode.HOST
                        }
                    }
                })
                actionButton.apply {
                    setText(R.string.hosting)
                    isEnabled = true
                }
            }

            Mode.RESOLVE -> {
                val session = sceneView.session ?: return
                CloudAnchorNode.resolve(
                    sceneView.engine,
                    session,
                    editText.text.toString()
                ) { state, node ->
                    mode = if (!state.isError && node != null) {
                        sceneView.addChildNode(node)
                        Mode.RESET
                    } else {
                        Toast.makeText(context, R.string.error_occurred, Toast.LENGTH_LONG).show()
                        Mode.RESOLVE
                    }
                }

                actionButton.apply {
                    setText(R.string.resolving)
                    isEnabled = false
                }
            }

            Mode.RESET -> {
                cloudAnchorNode.detachAnchor()
                mode = Mode.HOME
            }
        }
    }

    private enum class Mode {
        HOME, HOST, RESOLVE, RESET
    }

    companion object {
        private const val TAG = "MainFragment"
    }
}

fun SharedPreferences.string(
    key: (KProperty<*>) -> String = KProperty<*>::name,
    defaultValue: String? = null
): ReadWriteProperty<Any, String?> = object : ReadWriteProperty<Any, String?> {
    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) = edit().apply {
        val propertyKey = key(property)
        if (value != null) {
            putString(propertyKey, value)
        } else {
            remove(propertyKey)
        }
    }.apply()

    override fun getValue(thisRef: Any, property: KProperty<*>): String? =
        getString(key(property), defaultValue)
}