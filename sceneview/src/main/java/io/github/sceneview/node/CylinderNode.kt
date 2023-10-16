package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Cylinder
import io.github.sceneview.math.Position

open class CylinderNode(
    engine: Engine,
    radius: Float,
    height: Float,
    center: Position,
    sideCount: Int,
    materialInstance: MaterialInstance? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : BaseGeometryNode<Cylinder>(
    engine = engine,
    geometry = Cylinder.Builder()
        .radius(radius)
        .height(height)
        .center(center)
        .sideCount(sideCount)
        .build(engine),
    materialInstance = materialInstance,
    renderableApply = renderableApply
) {
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