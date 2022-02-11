package io.github.sceneview.ar.node

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.ar.core.Anchor
import com.google.ar.core.HitResult
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.RenderableInstance
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.material.setEmissiveColor
import io.github.sceneview.utils.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

const val clickDuration = 250L

open class CursorNode(
    context: Context,
    coroutineScope: LifecycleCoroutineScope? = null,
    glbFileLocation: String = "sceneview/models/cursor.glb"
) :
    ArModelNode() {

    var validColor: Color = Color(1.0f, 1.0f, 1.0f)
    var invalidColor: Color = Color(1.0f, 0.0f, 0.0f)
    var clickColor: Color = Color(0.0f, 0.0f, 0.0f)

    open var color: Color = validColor
        set(value) {
            if (field != value) {
                field = value
                applyColor()
            }
        }

    init {
        isFocusable = false
        loadModel(
            context = context,
            coroutineScope = coroutineScope,
            glbFileLocation = glbFileLocation
        )
    }

    override fun setRenderable(renderable: Renderable?): RenderableInstance? {
        return super.setRenderable(renderable).also {
            applyColor()
        }
    }

    open fun applyColor() {
        renderableInstance?.material?.filamentMaterialInstance?.setEmissiveColor(
            r = color.r,
            g = color.g,
            b = color.b
        )
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
            renderableInstance?.material?.filamentMaterialInstance?.setEmissiveColor(
                r = 0.0f,
                g = 0.0f,
                b = 0.0f
            )
            withContext(Dispatchers.IO) {
                delay(clickDuration)
                renderableInstance?.material?.filamentMaterialInstance?.setEmissiveColor(
                    r = 1.0f,
                    g = 1.0f,
                    b = 1.0f
                )
            }
        }
        return super.createAnchor()
    }
}