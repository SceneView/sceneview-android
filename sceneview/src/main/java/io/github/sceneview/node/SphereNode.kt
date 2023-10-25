package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.Sphere
import io.github.sceneview.math.Position

open class SphereNode(
    engine: Engine,
    radius: Float,
    center: Position,
    stacks: Int,
    slices: Int,
    /**
     * Binds a material instance to all primitives.
     */
    materialInstance: MaterialInstance? = null,
    /**
     * Binds a material instance to the specified primitive.
     *
     * If no material is specified for a given primitive, Filament will fall back to a basic
     * default material.
     *
     * Should return the material to bind for the zero-based index of the primitive, must be less
     * than the [Geometry.submeshes] size passed to constructor.
     */
    materialInstances: (index: Int) -> MaterialInstance? = { materialInstance },
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
) : BaseGeometryNode<Sphere>(
    engine = engine,
    geometry = Sphere.Builder()
        .radius(radius)
        .center(center)
        .stacks(stacks)
        .slices(slices)
        .build(engine),
    materialInstance = materialInstance,
    materialInstances = materialInstances,
    parent = parent,
    renderableApply = renderableApply
) {
    val radius get() = geometry.radius
    val center get() = geometry.center
    val stacks get() = geometry.stacks
    val slices get() = geometry.slices

    fun updateGeometry(
        radius: Float = this.radius,
        center: Position = this.center,
        stacks: Int = this.stacks,
        slices: Int = this.slices
    ) = geometry.update(radius, center, stacks, slices)
}