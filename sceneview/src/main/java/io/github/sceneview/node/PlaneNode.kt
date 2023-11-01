package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.Plane
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class PlaneNode(
    engine: Engine,
    size: Size = Plane.DEFAULT_SIZE,
    center: Position = Plane.DEFAULT_CENTER,
    normal: Direction = Plane.DEFAULT_NORMAL,
    /**
     * Binds a material instance to the specified primitive.
     *
     * If no material is specified for a given primitive, Filament will fall back to a basic
     * default material.
     *
     * Should return the material to bind for the zero-based index of the primitive, must be less
     * than the [Geometry.submeshes] size passed to constructor.
     */
    materialInstances: (index: Int) -> MaterialInstance?,
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
) : BaseGeometryNode<Plane>(
    engine = engine,
    geometry = Plane.Builder()
        .size(size)
        .center(center)
        .normal(normal)
        .build(engine),
    materialInstances = materialInstances,
    parent = parent,
    renderableApply = renderableApply
) {

    constructor(
        engine: Engine,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        /**
         * Binds a material instance to all primitives.
         */
        materialInstance: MaterialInstance?,
        parent: Node? = null,
        renderableApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        size = size,
        center = center,
        normal = normal,
        materialInstances = { materialInstance },
        parent = parent,
        renderableApply = renderableApply
    )

    val size get() = geometry.size
    val center get() = geometry.center
    val normal get() = geometry.normal

    fun updateGeometry(
        size: Size = this.size,
        center: Position = this.center,
        normal: Direction = this.normal
    ) = geometry.update(size, center, normal)
}