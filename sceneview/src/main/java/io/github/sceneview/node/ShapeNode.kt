package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Shape
import io.github.sceneview.geometries.UvScale
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position2

open class ShapeNode private constructor(
    engine: Engine,
    override val geometry: Shape,
    materialInstances: List<MaterialInstance?>,
    primitivesOffsets: List<IntRange>,
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
        geometry: Shape,
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        geometry,
        listOf(materialInstance),
        listOf(0..geometry.primitivesOffsets.last().last),
        builder
    )

    constructor(
        engine: Engine,
        geometry: Shape,
        materialInstances: List<MaterialInstance?>,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(engine, geometry, materialInstances, geometry.primitivesOffsets, builder)

    constructor(
        engine: Engine,
        polygonPath: List<Position2> = listOf(),
        polygonHoles: List<Int> = listOf(),
        delaunayPoints: List<Position2> = listOf(),
        normal: Direction = Shape.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        color: Color? = null,
        materialInstances: List<MaterialInstance?>,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        Shape.Builder()
            .polygonPath(polygonPath, polygonHoles)
            .delaunayPoints(delaunayPoints)
            .normal(normal)
            .uvScale(uvScale)
            .color(color)
            .build(engine),
        materialInstances,
        builder
    )

    constructor(
        engine: Engine,
        polygonPath: List<Position2> = listOf(),
        polygonHoles: List<Int> = listOf(),
        delaunayPoints: List<Position2> = listOf(),
        normal: Direction = Shape.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        color: Color? = null,
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        Shape.Builder()
            .polygonPath(polygonPath, polygonHoles)
            .delaunayPoints(delaunayPoints)
            .normal(normal)
            .uvScale(uvScale)
            .color(color)
            .build(engine),
        materialInstance,
        builder
    )

    fun updateGeometry(
        polygonPath: List<Position2> = geometry.polygonPath,
        polygonHoles: List<Int> = geometry.polygonHoles,
        delaunayPoints: List<Position2> = geometry.delaunayPoints,
        normal: Direction = geometry.normal,
        uvScale: UvScale = geometry.uvScale,
        color: Color? = geometry.color
    ) = setGeometry(
        geometry.update(polygonPath, polygonHoles, delaunayPoints, normal, uvScale, color)
    )
}