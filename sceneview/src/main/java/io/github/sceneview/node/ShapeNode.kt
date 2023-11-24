package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
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
    parent: Node? = null,
    builder: RenderableManager.Builder.() -> Unit = {}
) : GeometryNode(
    engine,
    EntityManager.get().create(),
    parent,
    geometry,
    materialInstances,
    primitivesOffsets,
    builder
) {

    constructor(
        engine: Engine,
        geometry: Shape,
        materialInstance: MaterialInstance? = null,
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        geometry,
        listOf(materialInstance),
        listOf(0..geometry.primitivesOffsets.last().last),
        parent,
        builder
    )

    constructor(
        engine: Engine,
        geometry: Shape,
        materialInstances: List<MaterialInstance?>,
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(engine, geometry, materialInstances, geometry.primitivesOffsets, parent, builder)

    constructor(
        engine: Engine,
        polygonPath: List<Position2> = listOf(),
        polygonHoles: List<Int> = listOf(),
        delaunayPoints: List<Position2> = listOf(),
        normal: Direction = Shape.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        color: Color? = null,
        materialInstances: List<MaterialInstance?>,
        parent: Node? = null,
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
        parent,
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
        parent: Node? = null,
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
        parent,
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