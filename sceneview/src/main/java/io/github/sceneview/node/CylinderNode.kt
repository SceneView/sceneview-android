package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Cylinder
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.math.Position

open class CylinderNode(
    engine: Engine,
    radius: Float,
    height: Float,
    center: Position,
    sideCount: Int,
    /**
     * Binds a material instance to the specified primitive.
     *
     * If no material is specified for a given primitive, Filament will fall back to a basic
     * default material.
     *
     * Should return the material to bind for the zero-based index of the primitive, must be less
     * than the [Geometry.submeshes] size passed to constructor.
     */
    materialInstances: (index: Int) -> MaterialInstance,
    /**
     * The parent node.
     *
     * If set to null, this node will not be attached.
     *
     * The local position, rotation, and scale of this node will remain the same.
     * Therefore, the world position, rotation, and scale of this node may be different after the
     * parent changes.
     */
    parent: Node? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : BaseGeometryNode<Cylinder>(
    engine = engine,
    geometry = Cylinder.Builder()
        .radius(radius)
        .height(height)
        .center(center)
        .sideCount(sideCount)
        .build(engine),
    materialInstances = materialInstances,
    parent = parent,
    renderableApply = renderableApply
) {

    constructor(
        engine: Engine,
        radius: Float,
        height: Float,
        center: Position,
        sideCount: Int,
        /**
         * Binds a material instance to all primitives.
         */
        materialInstance: MaterialInstance,
        parent: Node? = null,
        renderableApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        radius = radius,
        height = height,
        center = center,
        sideCount = sideCount,
        materialInstances = { materialInstance },
        parent = parent,
        renderableApply = renderableApply
    )

    val radius get() = geometry.radius
    val height get() = geometry.height
    val center get() = geometry.center
    val sideCount get() = geometry.sideCount

    fun updateGeometry(
        radius: Float = this.radius,
        height: Float = this.height,
        center: Position = this.center,
        sideCount: Int = this.sideCount
    ) = geometry.update(radius, height, center, sideCount)
}