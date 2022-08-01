package io.github.sceneview.sample.arviewnode

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.ViewNode
import io.github.sceneview.utils.doOnApplyWindowInsets
import io.github.sceneview.utils.setFullScreen

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    private lateinit var sceneView: ArSceneView
    private lateinit var loadingView: View
    private lateinit var placeModelButton: ExtendedFloatingActionButton

    private lateinit var arNode: ArModelNode
    private lateinit var viewNode: ViewNode
    private lateinit var modelNode: ModelNode

    private var isLoading = false
        set(value) {
            field = value
            loadingView.isGone = !value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFullScreen(
            findViewById(R.id.rootView),
            fullScreen = true,
            hideSystemBars = false,
            fitsSystemWindows = false
        )

        setSupportActionBar(findViewById<Toolbar>(R.id.toolbar)?.apply {
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).topMargin = systemBarsInsets.top
            }
            title = ""
        })

        sceneView = findViewById(R.id.sceneView)
        loadingView = findViewById(R.id.loadingView)

        placeModelButton = findViewById<ExtendedFloatingActionButton>(R.id.placeModelButton).apply {
            // Add system bar margins
            val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBarsInsets.bottom + bottomMargin
            }
            setOnClickListener { placeArNode() }
        }

        arNode = ArModelNode(placementMode = PlacementMode.PLANE_HORIZONTAL).apply {
            parent = sceneView
        }

        viewNode = ViewNode().apply {
            parent = arNode
            loadView(this@MainActivity, lifecycle, R.layout.view_node)
            isEditable = false
        }

        isLoading = true

        // "Planet" (https://skfb.ly/o9w6U) by userwaniroll is licensed under Creative Commons Attribution (http://creativecommons.org/licenses/by/4.0/)
        modelNode = ModelNode(this, lifecycle, "models/planet.glb", scaleUnits = 1f).apply {
            parent = arNode
            position.y = 0.8f
            onModelLoaded += {
                isLoading = false
            }
        }
    }

    private fun placeArNode() {
        arNode.anchor()
        placeModelButton.isVisible = false
        sceneView.planeRenderer.isVisible = false
    }
}