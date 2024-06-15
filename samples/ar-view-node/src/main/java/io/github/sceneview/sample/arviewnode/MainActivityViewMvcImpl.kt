package io.github.sceneview.sample.arviewnode

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.sample.arviewnode.databinding.ActivityMainBinding
import java.util.Collections

class MainActivityViewMvcImpl(
    private val context: Context,
    inflater: LayoutInflater,
    parent: ViewGroup?,
) : View.OnClickListener, MainActivityViewMvc {
    private val binding: ActivityMainBinding

    private val _listeners: MutableSet<MainActivityViewMvc.Listener> = HashSet()

    private val listeners: Set<MainActivityViewMvc.Listener>
        get() = Collections.unmodifiableSet(_listeners)

    ///////
    // region Init
    init {
        binding = ActivityMainBinding
            .inflate(
                inflater,
                parent,
                false
            )
    }

    init {
        binding.btnAdd.setOnClickListener(this)
        binding.btnViewNode.setOnClickListener(this)
        binding.btnImageNode.setOnClickListener(this)
    }
    // endregion Init
    ///////


    ///////////////////////
    // region View.OnClickListener
    override fun onClick(v: View) {

        when (v.id) {
            binding.btnAdd.id -> {
                listeners.forEach {
                    it.handleAddClicked()
                }
            }

            binding.btnViewNode.id -> {
                handleSetViewNodeClicked()
            }

            binding.btnImageNode.id -> {
                handleSetImageNodeClicked()
            }
        }
    }

    private fun handleSetViewNodeClicked() {
        listeners.forEach {
            it.updateActiveNodeType(MainActivity.NodeType.VIEW_NODE)
        }

        applyActiveColor(binding.btnViewNode)
        applyDisabledColor(binding.btnImageNode)
    }

    private fun handleSetImageNodeClicked() {
        listeners.forEach {
            it.updateActiveNodeType(MainActivity.NodeType.IMAGE_NODE)
        }

        applyActiveColor(binding.btnImageNode)
        applyDisabledColor(binding.btnViewNode)
    }

    private fun applyActiveColor(view: View) {
        view.backgroundTintList = ContextCompat
            .getColorStateList(
                context,
                R.color.main_color
            )
    }

    private fun applyDisabledColor(view: View) {
        view.backgroundTintList = ContextCompat
            .getColorStateList(
                context,
                R.color.gray_500
            )
    }
    // endregion View.OnClickListener
    ///////////////////////


    //////////////////////
    // region MainActivityViewMvc
    override fun registerListener(listener: MainActivityViewMvc.Listener) {
        _listeners.add(listener)
    }

    override fun unregisterListener(listener: MainActivityViewMvc.Listener) {
        _listeners.remove(listener)
    }

    override fun getView(): ActivityMainBinding {
        return binding
    }

    override fun getSceneView(): ARSceneView {
        return binding.sceneView
    }

    override fun updateBtnEnabledState(isTracking: Boolean) {
        if (isTracking) {
            if (!binding.btnAdd.isEnabled) {
                binding.btnAdd.isEnabled = true
            }
        } else {
            if (binding.btnAdd.isEnabled) {
                binding.btnAdd.isEnabled = false
            }
        }
    }
    // endregion MainActivityViewMvc
    //////////////////////
}