package com.google.ar.sceneform.rendering

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import com.google.android.filament.Engine
import com.google.ar.sceneform.resources.ResourceRegistry
import com.google.ar.sceneform.utilities.AndroidPreconditions
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.collision.Box
import io.github.sceneview.collision.Matrix
import io.github.sceneview.collision.Plane
import io.github.sceneview.collision.Preconditions
import io.github.sceneview.collision.Quaternion
import io.github.sceneview.collision.Ray
import io.github.sceneview.collision.RayHit
import io.github.sceneview.collision.Vector3
import io.github.sceneview.material.setExternalTexture
import io.github.sceneview.material.setParameter
import io.github.sceneview.math.toMatrix
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ViewNode
import java.util.Arrays
import java.util.OptionalInt
import java.util.concurrent.CompletableFuture

/**
 * Renders a 2D Android view in 3D space by attaching it to a [io.github.sceneview.node.Node].
 *
 * By default, the size of the view is 1 meter in the SceneView per 250dp in the layout. Use a
 * [ViewSizer] to control how the size of the view in the SceneView is calculated.
 *
 * ```
 * future = ViewRenderable.builder().setView(context, R.layout.view).build();
 * viewRenderable = future.thenAccept(...);
 * ```
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class ViewRenderable : Renderable {
    /**
     * Controls the horizontal alignment of the [ViewRenderable] relative to the
     * [io.github.sceneview.node.Node] it is attached to. The default value is CENTER.
     */
    enum class HorizontalAlignment {
        LEFT, CENTER, RIGHT
    }

    /**
     * Controls the vertical alignment of the [ViewRenderable] relative to the
     * [io.github.sceneview.node.Node] it is attached to. The default value is BOTTOM.
     */
    enum class VerticalAlignment {
        BOTTOM, CENTER, TOP
    }

    var viewRenderableData: ViewRenderableInternalData? = null
    private val view: View

    // Used to apply a final scale to the renderable that makes it render at an appropriate size based
    // on the size of the view.
    private val viewScaleMatrix = Matrix()

    private var viewSizer: ViewSizer
    private var verticalAlignment = VerticalAlignment.BOTTOM
    private var horizontalAlignment = HorizontalAlignment.CENTER

    private var isInitialized = false

    private var engine: Engine

    @Suppress("initialization")
    private val onViewSizeChangedListener =
        RenderViewToExternalTexture.OnViewSizeChangedListener { _, _ ->
            if (isInitialized) {
                updateSuggestedCollisionShapeAsync()
            }
        }

    /** The 2D Android [View] that is rendered by this [ViewRenderable]. */
    fun getView(): View = view

    /**
     * Creates a new instance of this ViewRenderable.
     *
     * The new renderable will have unique copy of all mutable state. All materials referenced by
     * the ViewRenderable will also be instanced. Immutable data will be shared between the instances.
     * The new ViewRenderable will reference the same getFilamentEngine View as the original
     * ViewRenderable.
     */
    override fun makeCopy(): ViewRenderable = ViewRenderable(engine, this)

    /** @hide */
    @Suppress("initialization")
    internal constructor(engine: Engine, builder: Builder, view: View) : super(builder) {
        this.engine = engine

        Preconditions.checkNotNull(view, "Parameter \"view\" was null.")

        this.view = view
        viewSizer = builder.viewSizer
        horizontalAlignment = builder.horizontalAlignment
        verticalAlignment = builder.verticalAlignment
        val renderView = RenderViewToExternalTexture(engine, view.context, view)
        renderView.addOnViewSizeChangedListener(onViewSizeChangedListener)
        viewRenderableData = ViewRenderableInternalData(renderView)

        // Empty collision box. Will be modified to fit the size of the view after the view is measured.
        collisionShape = Box(Vector3.zero())
    }

    internal constructor(engine: Engine, other: ViewRenderable) : super(other) {
        this.engine = engine

        view = other.view
        viewSizer = other.viewSizer
        horizontalAlignment = other.horizontalAlignment
        verticalAlignment = other.verticalAlignment
        viewRenderableData = Preconditions.checkNotNull(other.viewRenderableData)
        viewRenderableData!!.getRenderView().addOnViewSizeChangedListener(onViewSizeChangedListener)
    }

    /**
     * Gets the [ViewSizer] that controls the size of this [ViewRenderable] in the SceneView.
     */
    fun getSizer(): ViewSizer = viewSizer

    /**
     * Sets the [ViewSizer] that controls the size of this [ViewRenderable] in the SceneView.
     */
    fun setSizer(viewSizer: ViewSizer) {
        Preconditions.checkNotNull(viewSizer, "Parameter \"viewSizer\" was null.")
        this.viewSizer = viewSizer
        updateSuggestedCollisionShape()
    }

    /**
     * Gets the [HorizontalAlignment] that controls where the [ViewRenderable] is
     * positioned relative to the node it is attached to along the x-axis.
     */
    fun getHorizontalAlignment(): HorizontalAlignment = horizontalAlignment

    /**
     * Sets the [HorizontalAlignment] that controls where the [ViewRenderable] is
     * positioned relative to the node it is attached to along the x-axis.
     */
    fun setHorizontalAlignment(horizontalAlignment: HorizontalAlignment) {
        this.horizontalAlignment = horizontalAlignment
        updateSuggestedCollisionShape()
    }

    /**
     * Gets the [VerticalAlignment] that controls where the [ViewRenderable] is positioned
     * relative to the node it is attached to along the y-axis.
     */
    fun getVerticalAlignment(): VerticalAlignment = verticalAlignment

    /**
     * Sets the [VerticalAlignment] that controls where the [ViewRenderable] is positioned
     * relative to the node it is attached to along the y-axis.
     */
    fun setVerticalAlignment(verticalAlignment: VerticalAlignment) {
        this.verticalAlignment = verticalAlignment
        updateSuggestedCollisionShape()
    }

    /**
     * Takes the model matrix from the TransformProvider for rendering this node and scales it to
     * size it appropriately based on the meters to pixel ratio for the view.
     *
     * @hide
     */
    override fun getFinalModelMatrix(originalMatrix: Matrix): Matrix {
        Preconditions.checkNotNull(originalMatrix, "Parameter \"originalMatrix\" was null.")

        val size = viewSizer.getSize(view)
        viewScaleMatrix.makeScale(Vector3(size.x, size.y, 1.0f))

        viewScaleMatrix.setTranslation(
            Vector3(
                getOffsetRatioForAlignment(horizontalAlignment) * size.x,
                getOffsetRatioForAlignment(verticalAlignment) * size.y,
                0.0f
            )
        )

        Matrix.multiply(originalMatrix, viewScaleMatrix, viewScaleMatrix)

        return viewScaleMatrix
    }

    /** @hide */
    @Suppress("AndroidApiChecker")
    override fun prepareForDraw(engine: Engine) {
        if (getId().isEmpty()) {
            return
        }

        val data = Preconditions.checkNotNull(viewRenderableData)
        val renderViewToExternalTexture = data.getRenderView()

        getMaterial().setParameter("viewTextureReady", renderViewToExternalTexture.isViewTextureReady())

        if (!renderViewToExternalTexture.isAttachedToWindow
            || !renderViewToExternalTexture.isLaidOut
        ) {
            // Wait for the view to finish attachment.
            return
        }

        // Wait until one frame after the surface texture has been drawn to for the first time.
        val hasDrawnToSurfaceTexture = renderViewToExternalTexture.hasDrawnToSurfaceTexture()
        if (!hasDrawnToSurfaceTexture) {
            return
        }

        if (!isInitialized) {
            getMaterial().setExternalTexture(
                "viewTexture",
                renderViewToExternalTexture.externalTexture.getFilamentTexture()
            )
            updateSuggestedCollisionShape()

            isInitialized = true
        }

        getMaterial().setParameter("offsetUv", Float2(1.0f, 0.0f))

        super.prepareForDraw(engine)
    }

    fun attachView(attachmentManager: ViewAttachmentManager) {
        Preconditions.checkNotNull(viewRenderableData)
            .getRenderView()
            .attachView(attachmentManager)
    }

    fun detachView() {
        Preconditions.checkNotNull(viewRenderableData).getRenderView().detachView()
    }

    private fun updateSuggestedCollisionShapeAsync() {
        view.post { updateSuggestedCollisionShape() }
    }

    private fun updateSuggestedCollisionShape() {
        if (getId().isEmpty()) {
            return
        }

        val box = collisionShape as? Box ?: return

        val renderableData = getRenderableData()
        val viewSize = viewSizer.getSize(view)

        val size = renderableData.getSizeAabb()
        size.x *= viewSize.x
        size.y *= viewSize.y

        val center = renderableData.getCenterAabb()
        center.x *= viewSize.x
        center.y *= viewSize.y

        // Offset the collision shape based on the alignment.
        center.x += getOffsetRatioForAlignment(horizontalAlignment) * size.x
        center.y += getOffsetRatioForAlignment(verticalAlignment) * size.y

        box.setSize(size)
        box.setCenter(center)
    }

    private fun getOffsetRatioForAlignment(horizontalAlignment: HorizontalAlignment): Float {
        val data = getRenderableData()
        val centerAabb = data.getCenterAabb()
        val extentsAabb = data.getExtentsAabb()

        return when (horizontalAlignment) {
            HorizontalAlignment.LEFT -> -centerAabb.x + extentsAabb.x
            HorizontalAlignment.CENTER -> -centerAabb.x
            HorizontalAlignment.RIGHT -> -centerAabb.x - extentsAabb.x
        }
    }

    private fun getOffsetRatioForAlignment(verticalAlignment: VerticalAlignment): Float {
        val data = getRenderableData()
        val centerAabb = data.getCenterAabb()
        val extentsAabb = data.getExtentsAabb()

        return when (verticalAlignment) {
            VerticalAlignment.BOTTOM -> -centerAabb.y + extentsAabb.y
            VerticalAlignment.CENTER -> -centerAabb.y
            VerticalAlignment.TOP -> -centerAabb.y - extentsAabb.y
        }
    }

    /**
     * Dispatches a touch event to a node's ViewRenderable if that node has a ViewRenderable by
     * converting the touch event into the local coordinate space of the view.
     */
    fun dispatchTouchEventToView(node: ViewNode, cameraNode: CameraNode, motionEvent: MotionEvent): Boolean {
        val pointerCount = motionEvent.pointerCount

        val pointerProperties = Array(pointerCount) { MotionEvent.PointerProperties() }
        val pointerCoords = Array(pointerCount) { MotionEvent.PointerCoords() }

        val nodeTransformMatrix = node.worldTransform.toMatrix()
        val nodePosition = Vector3()
        nodeTransformMatrix.decomposeTranslation(nodePosition)
        val nodeScale = Vector3()
        nodeTransformMatrix.decomposeScale(nodeScale)
        val nodeRotation = Quaternion()
        nodeTransformMatrix.decomposeRotation(nodeScale, nodeRotation)

        val nodeForward = Quaternion.rotateVector(nodeRotation, Vector3.forward())
        val nodeBack = Quaternion.rotateVector(nodeRotation, Vector3.back())

        val plane = Plane(nodePosition, nodeForward)
        val rayHit = RayHit()

        val backPlane = Plane(nodePosition, nodeBack)

        // Convert the pointer coordinates for each pointer into the view's local coordinate space.
        for (i in 0 until pointerCount) {
            val props = MotionEvent.PointerProperties()
            val coords = MotionEvent.PointerCoords()

            motionEvent.getPointerProperties(i, props)
            motionEvent.getPointerCoords(i, coords)

            val ray = cameraNode.screenPointToRay(coords.x, coords.y)
            if (plane.rayIntersection(ray, rayHit)) {
                val viewPosition = convertWorldPositionToLocalView(node, rayHit.getPoint())
                coords.x = viewPosition.x
                coords.y = viewPosition.y
            } else if (backPlane.rayIntersection(ray, rayHit)) {
                val viewPosition = convertWorldPositionToLocalView(node, rayHit.getPoint())
                // Flip the x coordinate for the back-facing plane.
                coords.x = getView().width - viewPosition.x
                coords.y = viewPosition.y
            } else {
                coords.clear()
                props.clear()
            }

            pointerProperties[i] = props
            pointerCoords[i] = coords
        }

        // We must copy the touch event with the new coordinates and dispatch it to the view.
        val me = MotionEvent.obtain(
            motionEvent.downTime,
            motionEvent.eventTime,
            motionEvent.action,
            pointerCount,
            pointerProperties,
            pointerCoords,
            motionEvent.metaState,
            motionEvent.buttonState,
            motionEvent.xPrecision,
            motionEvent.yPrecision,
            motionEvent.deviceId,
            motionEvent.edgeFlags,
            motionEvent.source,
            motionEvent.flags
        )

        return getView().dispatchTouchEvent(me)
    }

    fun convertWorldPositionToLocalView(node: ViewNode, worldPos: Vector3): Vector3 {
        Preconditions.checkNotNull(node, "Parameter \"node\" was null.")
        Preconditions.checkNotNull(worldPos, "Parameter \"worldPos\" was null.")

        val localPos = node.transformationMatrixInverted.transformPoint(worldPos)

        val view = getView()
        val width = view.width
        val height = view.height
        val pixelsToMetersRatio = getPixelsToMetersRatio()

        var xPixels = (localPos.x * pixelsToMetersRatio).toInt()
        var yPixels = (localPos.y * pixelsToMetersRatio).toInt()

        val halfWidth = width / 2
        val halfHeight = height / 2

        when (getVerticalAlignment()) {
            VerticalAlignment.BOTTOM -> yPixels = height - yPixels
            VerticalAlignment.CENTER -> yPixels = height - (yPixels + halfHeight)
            VerticalAlignment.TOP -> yPixels = height - (yPixels + height)
        }

        when (getHorizontalAlignment()) {
            HorizontalAlignment.LEFT -> { /* Do nothing. */ }
            HorizontalAlignment.CENTER -> xPixels = xPixels + halfWidth
            HorizontalAlignment.RIGHT -> xPixels = xPixels + width
        }

        return Vector3(xPixels.toFloat(), yPixels.toFloat(), 0.0f)
    }

    private fun getPixelsToMetersRatio(): Float {
        val view = getView()
        val width = view.width
        val size = getSizer().getSize(getView())

        if (size.x == 0.0f) {
            return 0.0f
        }

        return width.toFloat() / size.x
    }

    /** @hide */
    @Throws(Throwable::class)
    protected fun finalize() {
        try {
            ThreadPools.getMainExecutor().execute { dispose() }
        } catch (e: Exception) {
            Log.e(TAG, "Error while Finalizing View Renderable.", e)
        }
    }

    /** @hide */
    internal fun dispose() {
        AndroidPreconditions.checkUiThread()

        val viewRenderableData = this.viewRenderableData
        if (viewRenderableData != null) {
            viewRenderableData.getRenderView().removeOnViewSizeChangedListener(onViewSizeChangedListener)
            this.viewRenderableData = null
        }
    }

    /** Factory class for [ViewRenderable] */
    class Builder : Renderable.Builder<ViewRenderable, Builder>() {
        private var view: View? = null
        var viewSizer: ViewSizer = DpToMetersViewSizer(DEFAULT_DP_TO_METERS)
        var verticalAlignment = VerticalAlignment.BOTTOM
        var horizontalAlignment = HorizontalAlignment.CENTER

        @Suppress("AndroidApiChecker")
        private var resourceId: OptionalInt = OptionalInt.empty()

        fun setView(context: Context, view: View): Builder {
            this.view = view
            this.context = context
            registryId = view
            return this
        }

        @Suppress("AndroidApiChecker")
        fun setView(context: Context, resourceId: Int): Builder {
            this.resourceId = OptionalInt.of(resourceId)
            this.context = context
            registryId = null
            return this
        }

        /**
         * Set the [ViewSizer] that controls the size of the built [ViewRenderable] in the SceneView.
         */
        fun setSizer(viewSizer: ViewSizer): Builder {
            Preconditions.checkNotNull(viewSizer, "Parameter \"viewSizer\" was null.")
            this.viewSizer = viewSizer
            return this
        }

        /**
         * Sets the [HorizontalAlignment] that controls where the [ViewRenderable] is
         * positioned relative to the node it is attached to along the x-axis.
         */
        fun setHorizontalAlignment(horizontalAlignment: HorizontalAlignment): Builder {
            this.horizontalAlignment = horizontalAlignment
            return this
        }

        /**
         * Sets the [VerticalAlignment] that controls where the [ViewRenderable] is
         * positioned relative to the node it is attached to along the y-axis.
         */
        fun setVerticalAlignment(verticalAlignment: VerticalAlignment): Builder {
            this.verticalAlignment = verticalAlignment
            return this
        }

        @Suppress("AndroidApiChecker")
        override fun build(engine: Engine): CompletableFuture<ViewRenderable> {
            if (!hasSource() && context != null) {
                // For ViewRenderables, the registryId must come from the View, not the RCB source.
                registryId = view

                val setSourceFuture = Material.builder()
                    .setSource(context!!, Uri.parse("materials/view_renderable.filamat"))
                    .build(engine)
                    .thenAccept { material ->
                        val vertices = ArrayList<Vertex>()
                        vertices.add(
                            Vertex.builder()
                                .setPosition(Vector3(-0.5f, 0.0f, 0.0f))
                                .setNormal(Vector3(0.0f, 0.0f, 1.0f))
                                .setUvCoordinate(Vertex.UvCoordinate(0.0f, 0.0f))
                                .build()
                        )
                        vertices.add(
                            Vertex.builder()
                                .setPosition(Vector3(0.5f, 0.0f, 0.0f))
                                .setNormal(Vector3(0.0f, 0.0f, 1.0f))
                                .setUvCoordinate(Vertex.UvCoordinate(1.0f, 0.0f))
                                .build()
                        )
                        vertices.add(
                            Vertex.builder()
                                .setPosition(Vector3(-0.5f, 1.0f, 0.0f))
                                .setNormal(Vector3(0.0f, 0.0f, 1.0f))
                                .setUvCoordinate(Vertex.UvCoordinate(0.0f, 1.0f))
                                .build()
                        )
                        vertices.add(
                            Vertex.builder()
                                .setPosition(Vector3(0.5f, 1.0f, 0.0f))
                                .setNormal(Vector3(0.0f, 0.0f, 1.0f))
                                .setUvCoordinate(Vertex.UvCoordinate(1.0f, 1.0f))
                                .build()
                        )
                        val triangleIndices = ArrayList<Int>()
                        triangleIndices.add(0)
                        triangleIndices.add(1)
                        triangleIndices.add(2)
                        triangleIndices.add(1)
                        triangleIndices.add(3)
                        triangleIndices.add(2)
                        val submesh = RenderableDefinition.Submesh.builder()
                            .setTriangleIndices(triangleIndices)
                            .setMaterial(material.filamentMaterialInstance)
                            .build(engine)
                        setSource(
                            RenderableDefinition.builder()
                                .setVertices(vertices)
                                .setSubmeshes(listOf(submesh))
                                .build(engine)
                        )
                    }
                return setSourceFuture.thenCompose { super.build(engine) }
            }

            return super.build(engine)
        }

        override fun makeRenderable(engine: Engine): ViewRenderable {
            return if (this.view != null) {
                ViewRenderable(engine, this, view!!)
            } else {
                ViewRenderable(engine, this, inflateViewFromResourceId())
            }
        }

        /** @hide */
        override fun getRenderableClass(): Class<ViewRenderable> = ViewRenderable::class.java

        /** @hide */
        override fun getRenderableRegistry(): ResourceRegistry<ViewRenderable> =
            ResourceManager.getInstance().getViewRenderableRegistry()

        /** @hide */
        override fun getSelf(): Builder = this

        /** @hide */
        @Suppress("AndroidApiChecker")
        override fun checkPreconditions() {
            super.checkPreconditions()

            val hasView = resourceId.isPresent || view != null

            if (!hasView) {
                throw AssertionError("ViewRenderable must have a source.")
            }

            if (resourceId.isPresent && view != null) {
                throw AssertionError(
                    "ViewRenderable must have a resourceId or a view as a source. This one has both."
                )
            }
        }

        @Suppress("AndroidApiChecker")
        private fun inflateViewFromResourceId(): View {
            val ctx = context ?: throw AssertionError("Context cannot be null")

            // Inflate the view in a detached state.
            val dummy: ViewGroup = FrameLayout(ctx)
            return LayoutInflater.from(ctx).inflate(resourceId.asInt, dummy, false)
        }

        companion object {
            private const val DEFAULT_DP_TO_METERS = 250
        }
    }

    companion object {
        private val TAG = ViewRenderable::class.java.simpleName

        /** Constructs a [ViewRenderable] */
        @JvmStatic
        fun builder(): Builder {
            AndroidPreconditions.checkMinAndroidApiLevel()
            return Builder()
        }
    }
}
