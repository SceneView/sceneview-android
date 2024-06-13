package io.github.sceneview.sample.arviewnode

import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.sample.arviewnode.databinding.ActivityMainBinding

interface MainActivityViewMvc {

    fun registerListener(listener: Listener)

    fun unregisterListener(listener: Listener)

    fun getView(): ActivityMainBinding

    fun getSceneView(): ARSceneView

    fun updateBtnEnabledState(isTracking: Boolean)

    interface Listener {
        fun updateActiveNodeType(nodeType: MainActivity.NodeType)

        fun handleOnAddClicked()
    }
}