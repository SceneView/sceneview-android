package io.github.sceneview.ar

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode

// TODO: Use it for more actual ArNode and use view layout parameters for placement and glb file
open class ArModelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ArSceneView(
    context,
    attrs,
    defStyleAttr,
    defStyleRes
) {

    val modelNode = ArModelNode()

    /**
     * TODO: Doc
     *
     * @see io.github.sceneview.ar.node.ArModelNode.glbFileLocation
     */
    var model: String?
        get() = modelNode.glbFileLocation
        set(value) {
            modelNode.glbFileLocation = value
        }

    /**
     * TODO: Doc
     *
     * @see io.github.sceneview.ar.node.ArModelNode.placementMode
     */
    var placementMode: PlacementMode
        get() = modelNode.placementMode
        set(value) {
            modelNode.placementMode = value
        }

    /**
     * TODO: Doc
     *
     * @see io.github.sceneview.ar.node.ArModelNode.autoAnchor
     */
    var autoAnchor: Boolean
        get() = modelNode.autoAnchor
        set(value) {
            modelNode.autoAnchor = value
        }

    /**
     * TODO: Doc
     *
     * @see io.github.sceneview.ar.node.ArModelNode.autoAnchor
     */
    var planeVisible: Boolean
        get() = planeRenderer.isVisible
        set(value) {
            planeRenderer.isVisible = value
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ArModelView, defStyleAttr, defStyleRes)
            .use { typedArray ->
                if (typedArray.hasValue(R.styleable.ArModelView_model)) {
                    model = typedArray.getString(R.styleable.ArModelView_model)
                }
                if (typedArray.hasValue(R.styleable.ArModelView_placementMode)) {
                    placementMode = PlacementMode.values()[typedArray.getInt(
                        R.styleable.ArModelView_placementMode,
                        placementMode.ordinal
                    )]
                }
                if (typedArray.hasValue(R.styleable.ArModelView_autoAnchor)) {
                    autoAnchor =
                        typedArray.getBoolean(R.styleable.ArModelView_autoAnchor, autoAnchor)
                }
                if (typedArray.hasValue(R.styleable.ArModelView_planeVisible)) {
                    planeVisible =
                        typedArray.getBoolean(R.styleable.ArModelView_planeVisible, planeVisible)
                }
            }
    }
}