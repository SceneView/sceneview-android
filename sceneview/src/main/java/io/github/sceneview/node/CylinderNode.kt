package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Cylinder
import io.github.sceneview.math.Position

open class CylinderNode private constructor(
    engine: Engine,
    override val geometry: Cylinder,
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
        geometry: Cylinder,
        materialInstance: MaterialInstance? = null,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        geometry = geometry,
        materialInstances = listOf(materialInstance),
        primitivesOffsets = listOf(0..geometry.primitivesOffsets.last().last),
        builderApply = builderApply
    )

    constructor(
        engine: Engine,
        radius: Float = Cylinder.DEFAULT_RADIUS,
        height: Float = Cylinder.DEFAULT_HEIGHT,
        center: Position = Cylinder.DEFAULT_CENTER,
        sideCount: Int = Cylinder.DEFAULT_SIDE_COUNT,
        materialInstances: List<MaterialInstance?>,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        geometry = Cylinder.Builder()
            .radius(radius)
            .height(height)
            .center(center)
            .sideCount(sideCount)
            .build(engine),
        materialInstances = materialInstances,
        builderApply = builderApply
    )

    constructor(
        engine: Engine,
        radius: Float = Cylinder.DEFAULT_RADIUS,
        height: Float = Cylinder.DEFAULT_HEIGHT,
        center: Position = Cylinder.DEFAULT_CENTER,
        sideCount: Int = Cylinder.DEFAULT_SIDE_COUNT,
        materialInstance: MaterialInstance? = null,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        geometry = Cylinder.Builder()
            .radius(radius)
            .height(height)
            .center(center)
            .sideCount(sideCount)
            .build(engine),
        materialInstance = materialInstance,
        builderApply = builderApply
    )

    fun updateGeometry(
        radius: Float = geometry.radius,
        height: Float = geometry.height,
        center: Position = geometry.center,
        sideCount: Int = geometry.sideCount
    ) = setGeometry(geometry.update(engine, radius, height, center, sideCount))
}