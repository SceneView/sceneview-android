package io.github.sceneview.ar.node

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.material.setEmissiveColor
import io.github.sceneview.math.Position
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.colorOf
import kotlinx.coroutines.delay

open class CursorNode(
    context: Context,
    lifecycle: Lifecycle? = null,
    modelFileLocation: String = "sceneview/models/cursor.glb",
    autoAnimate: Boolean = true,
    scaleToUnits: Float? = null,
    centerOrigin: Position? = null,
    onError: ((error: Exception) -> Unit)? = null,
    onLoaded: ((instance: RenderableInstance) -> Unit)? = null
) : ArModelNode(
    context,
    lifecycle,
    modelFileLocation,
    autoAnimate,
    scaleToUnits,
    centerOrigin,
    onError,
    onLoaded
) {

    var clickDuration = 250L

    override var isTracking: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
            }
        }
    var isClicked = false
        set(value) {
            if (field != value) {
                field = value
                updateState()
                if (value) {
                    lifecycleScope?.launchWhenCreated {
                        delay(clickDuration)
                        isClicked = false
                    }
                }
            }
        }
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

    init {
        isSelectable = false
    }

    override fun onHitResult(hitResult: HitResult?) {
        super.onHitResult(hitResult)

        isTracking = hitResult?.isTracking ?: false
    }

    override fun onModelChanged(modelInstance: RenderableInstance?) {
        super.onModelChanged(modelInstance)

        updateState()
    }

    open fun updateState() {
        modelInstance?.materialInstance?.setEmissiveColor(
            when {
                isClicked -> colorClicked
                isTracking -> colorTracking
                else -> colorNotTracking
            }
        )
    }

    override fun createAnchor(): Anchor? {
        return (hitResult?.takeIf { it.isTracking }?.let { super.createAnchor(it) }
            ?: super.createAnchor())
            .also {
                isClicked = it != null
            }
    }
}