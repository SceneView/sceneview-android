package io.github.sceneview.ar.node

import android.content.Context
import androidx.lifecycle.Lifecycle
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.material.setEmissiveColor
import io.github.sceneview.math.Position
import io.github.sceneview.utils.Color
import io.github.sceneview.utils.colorOf
import kotlinx.coroutines.delay

const val clickDuration = 250L

open class CursorNode(
    context: Context,
    lifecycle: Lifecycle? = null,
    glbFileLocation: String = "sceneview/models/cursor.glb"
) : ArModelNode(placementMode = PlacementMode.BEST_AVAILABLE) {

    override var isTracking: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                applyColor()
            }
        }
    var isClicked = false
        set(value) {
            if (field != value) {
                field = value
                applyColor()
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
            applyColor()
        }
    var colorNotTracking: Color = colorOf(r = 1.0f)
        set(value) {
            field = value
            applyColor()
        }
    var colorClicked: Color = colorOf(rgb = 0.1f)
        set(value) {
            field = value
            applyColor()
        }

    init {
        isFocusable = false
        loadModelAsync(context, lifecycle, glbFileLocation)
    }

    override fun onArFrameHitResult(hitResult: HitResult?) {
        super.onArFrameHitResult(hitResult)

        isTracking = hitResult?.isTracking ?: false
    }

    override fun setModel(
        renderable: Renderable?,
        autoAnimate: Boolean,
        autoScale: Boolean,
        centerOrigin: Position?
    ): RenderableInstance? {
        return super.setModel(renderable, autoAnimate, autoScale, centerOrigin).also {
            applyColor()
        }
    }

    open fun applyColor() {
        modelInstance?.material?.filamentMaterialInstance?.setEmissiveColor(
            when {
                isClicked -> colorClicked
                isTracking -> colorTracking
                else -> colorNotTracking
            }
        )
    }

    override fun createAnchor(): Anchor? {
        return (lastHitResult?.takeIf { it.isTracking }?.let { super.createAnchor(it) }
            ?: super.createAnchor())
            .also {
                isClicked = it != null
            }
    }
}