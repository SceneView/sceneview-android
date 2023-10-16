package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Plane
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class PlaneNode(
    engine: Engine,
    size: Size = Plane.DEFAULT_SIZE,
    center: Position = Plane.DEFAULT_CENTER,
    normal: Direction = Plane.DEFAULT_NORMAL,
    materialInstance: MaterialInstance? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : BaseGeometryNode<Plane>(
    engine = engine,
    geometry = Plane.Builder()
        .size(size)
        .center(center)
        .normal(normal)
        .build(engine),
    materialInstance = materialInstance,
    renderableApply = renderableApply
) {
    val size get() = geometry.size
    val center get() = geometry.center
    val normal get() = geometry.normal

    fun updateGeometry(
        size: Size = this.size,
        center: Position = this.center,
        normal: Direction = this.normal
    ) = geometry.update(size, center, normal)
}