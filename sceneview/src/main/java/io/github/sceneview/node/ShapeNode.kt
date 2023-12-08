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
    override val geometry: Shape,
    materialInstances: List<MaterialInstance?>,
    primitivesOffsets: List<IntRange>,
    builderApply: RenderableManager.Builder.() -> Unit = {}
) : GeometryNode(
    geometry = geometry,
    materialInstances = materialInstances,
    primitivesOffsets = primitivesOffsets,
    builderApply = builderApply
) {
    constructor(
        geometry: Shape,
        materialInstance: MaterialInstance? = null,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        geometry = geometry,
        materialInstances = listOf(materialInstance),
        primitivesOffsets = listOf(0..geometry.primitivesOffsets.last().last),
        builderApply = builderApply
    )

    constructor(
        geometry: Shape,
        materialInstances: List<MaterialInstance?>,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        geometry = geometry,
        materialInstances = materialInstances,
        primitivesOffsets = geometry.primitivesOffsets,
        builderApply = builderApply
    )

    constructor(
        engine: Engine,
        polygonPath: List<Position2> = listOf(),
        polygonHoles: List<Int> = listOf(),
        delaunayPoints: List<Position2> = listOf(),
        normal: Direction = Shape.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        color: Color? = null,
        materialInstances: List<MaterialInstance?>,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        geometry = Shape.Builder()
            .polygonPath(polygonPath, polygonHoles)
            .delaunayPoints(delaunayPoints)
            .normal(normal)
            .uvScale(uvScale)
            .color(color)
            .build(engine),
        materialInstances = materialInstances,
        builderApply = builderApply
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
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        geometry = Shape.Builder()
            .polygonPath(polygonPath, polygonHoles)
            .delaunayPoints(delaunayPoints)
            .normal(normal)
            .uvScale(uvScale)
            .color(color)
            .build(engine),
        materialInstance = materialInstance,
        builderApply = builderApply
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