package io.github.sceneview.ar.node

import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import io.github.sceneview.material.setEmissiveColor
import io.github.sceneview.math.Position
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.colorOf
import kotlinx.coroutines.delay

open class CursorNode(
    modelFileLocation: String = "sceneview/models/cursor.glb",
    autoAnimate: Boolean = true,
    scaleToUnits: Float? = null,
    centerOrigin: Position? = null,
    onError: ((error: Exception) -> Unit)? = null,
    onLoaded: ((modelInstance: ModelInstance) -> Unit)? = null
) : ArModelNode(
    modelFileLocation,
    autoAnimate,
    scaleToUnits,
    centerOrigin,
    onError,
    onLoaded
) {

    var colorTracking: Color = colorOf(rgb = 1.0f)
        set(value) {
            field = value
            updateState()
        }
    var colorNotTracking: Color = colorOf(r = 1.0f)
        set(value) {
            field = value
            updateState()
        }
    var colorClicked: Color = colorOf(rgb = 0.1f)
        set(value) {
            field = value
            updateState()
        }

    var clickDuration = 250L

    var isClicked = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }

    override var isSelectable = false

    override var hitResult: HitResult?
        get() = super.hitResult
        set(value) {
            super.hitResult = value
            updateState()
        }

    override fun onModelChanged(modelInstance: ModelInstance?) {
        super.onModelChanged(modelInstance)
        updateState()
    }

    override fun createAnchor(): Anchor? {
        return super.createAnchor().also {
            if (it != null) {
                click()
            }
        }
    }

    fun click() {
        isClicked = true
        lifecycleScope?.launchWhenCreated {
            delay(clickDuration)
            isClicked = false
        }
    }

    open fun updateState() {
        modelInstance?.materialInstances?.forEach {
            it.setEmissiveColor(
                when {
                    isClicked -> colorClicked
                    isTracking -> colorTracking
                    else -> colorNotTracking
                }
            )
        }
    }
}