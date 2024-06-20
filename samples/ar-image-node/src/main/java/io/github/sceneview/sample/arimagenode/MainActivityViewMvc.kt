package io.github.sceneview.sample.arimagenode

import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.sample.arimagenode.databinding.ActivityMainBinding

interface MainActivityViewMvc {

    fun registerListener(listener: Listener)

    fun unregisterListener(listener: Listener)

    fun getView(): ActivityMainBinding

    fun getSceneView(): ARSceneView

    fun updateBtnEnabledState(isTracking: Boolean)

    interface Listener {
        fun handleAddClicked()
    }
}