package io.github.sceneview.ar.node

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
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
    coroutineScope: LifecycleCoroutineScope? = null,
    glbFileLocation: String = "sceneview/models/cursor.glb"
) : ArModelNode(placementMode = PlacementMode.BEST_AVAILABLE) {

    var validColor: Color = colorOf(rgb = 1.0f)
    var invalidColor: Color = colorOf(r = 1.0f, g = 0.0f, b = 0.0f)

    open var color: Color = validColor
        set(value) {
            if (field != value) {
                field = value
                applyColor()
            }
        }

    init {
        isFocusable = false
        loadModelAsync(context, glbFileLocation, coroutineScope)
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
        modelInstance?.material?.filamentMaterialInstance?.setEmissiveColor(color)
    }

    override fun onArFrameHitResult(hitResult: HitResult?) {
        super.onArFrameHitResult(hitResult)

        color = if (hitResult?.isTracking == true) {
            validColor
        } else {
            invalidColor
        }
    }

    override fun createAnchor(): Anchor? {
        lifecycleScope?.launchWhenCreated {
            modelInstance?.material?.filamentMaterialInstance?.setEmissiveColor(
                colorOf(rgb = 0.0f)
            )
            delay(clickDuration)
            modelInstance?.material?.filamentMaterialInstance?.setEmissiveColor(
                colorOf(rgb = 1.0f)
            )
        }
        return super.createAnchor()
    }
}