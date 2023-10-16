package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Cube
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class CubeNode(
    engine: Engine,
    size: Size = Cube.DEFAULT_SIZE,
    center: Position = Cube.DEFAULT_CENTER,
    materialInstance: MaterialInstance? = null,
    renderableApply: RenderableManager.Builder.() -> Unit = {}
) : BaseGeometryNode<Cube>(
    engine = engine,
    geometry = Cube.Builder()
        .size(size)
        .center(center)
        .build(engine),
    materialInstance = materialInstance,
    renderableApply = renderableApply
) {
    val center get() = geometry.center
    val size get() = geometry.size

    fun updateGeometry(
        center: Position = geometry.center,
        size: Size = geometry.size
    ) = geometry.update(center, size)
}