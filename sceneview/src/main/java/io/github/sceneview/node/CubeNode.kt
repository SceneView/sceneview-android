package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Cube
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class CubeNode(
    engine: Engine,
    size: Size = Cube.DEFAULT_SIZE,
    center: Position = Cube.DEFAULT_CENTER,
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
) : BaseGeometryNode<Cube>(
    engine = engine,
    geometry = Cube.Builder()
        .size(size)
        .center(center)
        .build(engine),
    materialInstances = materialInstances,
    parent = parent,
    renderableApply = renderableApply
) {
    val center get() = geometry.center
    val size get() = geometry.size

    fun updateGeometry(
        center: Position = geometry.center,
        size: Size = geometry.size
    ) = geometry.update(center, size)
}