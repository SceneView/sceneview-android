package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Sphere
import io.github.sceneview.math.Position

open class SphereNode(
    engine: Engine,
    radius: Float,
    center: Position,
    stacks: Int,
    slices: Int,
    materialInstance: MaterialInstance? = null,
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