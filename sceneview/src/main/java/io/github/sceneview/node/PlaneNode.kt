package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Plane
import io.github.sceneview.geometries.UvScale
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class PlaneNode private constructor(
    engine: Engine,
    override val geometry: Plane,
    primitivesOffsets: List<IntRange>,
    materialInstances: List<MaterialInstance?>,
    builder: RenderableManager.Builder.() -> Unit = {}
) : GeometryNode(
    engine,
    geometry,
    materialInstances,
    primitivesOffsets,
    builder
) {
    constructor(
        engine: Engine,
        geometry: Plane,
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine, geometry, listOf(0..geometry.primitivesOffsets.last().last),
        listOf(materialInstance), builder
    )

    constructor(
        engine: Engine,
        geometry: Plane,
        materialInstances: List<MaterialInstance?>,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(engine, geometry, geometry.primitivesOffsets, materialInstances, builder)

    constructor(
        engine: Engine,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        materialInstances: List<MaterialInstance?>,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        Plane.Builder()
            .size(size)
            .center(center)
            .normal(normal)
            .uvScale(uvScale)
            .build(engine),
        materialInstances, builder
    )

    constructor(
        engine: Engine,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        Plane.Builder()
            .size(size)
            .center(center)
            .normal(normal)
            .uvScale(uvScale)
            .build(engine),
        materialInstance, builder
    )

    fun updateGeometry(
        size: Size = geometry.size,
        center: Position = geometry.center,
        normal: Direction = geometry.normal,
        uvScale: UvScale = geometry.uvScale,
    ) = setGeometry(geometry.update(size, center, normal, uvScale))
}