package io.github.sceneview.ar

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.ar.scene.PlaneRenderer

// TODO: Use it for more actual ArNode and use view layout parameters for placement and glb file
internal class ArModelView @JvmOverloads constructor(
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

//    /**
//     * TODO: Doc
//     *
//     * @see io.github.sceneview.ar.node.ArModelNode.modelFileLocation
//     */
//    var model: String?
//        get() = modelNode.modelFileLocation
//        set(value) {
//            modelNode.modelFileLocation = value
//        }

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
     * @see ArModelNode.instantAnchor
     */
    var instantAnchor: Boolean
        get() = modelNode.instantAnchor
        set(value) {
            modelNode.instantAnchor = value
        }

    /**
     * TODO: Doc
     * @see PlaneRenderer.isVisible
     */
    var isPlaneVisible: Boolean
        get() = planeRenderer.isVisible
        set(value) {
            planeRenderer.isVisible = value
        }

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ArModelView, defStyleAttr, defStyleRes)
            .use { typedArray ->
//                if (typedArray.hasValue(R.styleable.ArModelView_model)) {
//                    typedArray.getString(R.styleable.ArModelView_model)
//                }
                if (typedArray.hasValue(R.styleable.ArModelView_placementMode)) {
                    placementMode = PlacementMode.values()[typedArray.getInt(
                        R.styleable.ArModelView_placementMode,
                        placementMode.ordinal
                    )]
                }
                if (typedArray.hasValue(R.styleable.ArModelView_instantAnchor)) {
                    instantAnchor =
                        typedArray.getBoolean(R.styleable.ArModelView_instantAnchor, instantAnchor)
                }
                if (typedArray.hasValue(R.styleable.ArModelView_isPlaneVisible)) {
                    isPlaneVisible = typedArray.getBoolean(
                        R.styleable.ArModelView_isPlaneVisible,
                        isPlaneVisible
                    )
                }
            }
    }
}