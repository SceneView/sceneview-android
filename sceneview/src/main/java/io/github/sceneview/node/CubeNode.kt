package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Cube
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class CubeNode private constructor(
    engine: Engine,
    override val geometry: Cube,
    materialInstances: List<MaterialInstance?>,
    primitivesOffsets: List<IntRange> = geometry.primitivesOffsets,
    builderApply: RenderableManager.Builder.() -> Unit = {}
) : GeometryNode(
    engine = engine,
    geometry = geometry,
    materialInstances = materialInstances,
    primitivesOffsets = primitivesOffsets,
    builderApply = builderApply
) {

    constructor(
        engine: Engine,
        geometry: Cube,
        materialInstance: MaterialInstance? = null,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        geometry = geometry,
        materialInstances = listOf(materialInstance),
        primitivesOffsets = listOf(0..geometry.primitivesOffsets.last().last),
        builderApply = builderApply
    )

    constructor(
        engine: Engine,
        size: Size = Cube.DEFAULT_SIZE,
        center: Position = Cube.DEFAULT_CENTER,
        materialInstances: List<MaterialInstance?>,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        geometry = Cube.Builder()
            .size(size)
            .center(center)
            .build(engine),
        materialInstances = materialInstances,
        builderApply = builderApply
    )

    constructor(
        engine: Engine,
        size: Size = Cube.DEFAULT_SIZE,
        center: Position = Cube.DEFAULT_CENTER,
        materialInstance: MaterialInstance? = null,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        geometry = Cube.Builder()
            .size(size)
            .center(center)
            .build(engine),
        materialInstance = materialInstance,
        builderApply = builderApply
    )

    fun updateGeometry(
        center: Position = geometry.center,
        size: Size = geometry.size
    ) = setGeometry(geometry.update(engine, center, size))
}