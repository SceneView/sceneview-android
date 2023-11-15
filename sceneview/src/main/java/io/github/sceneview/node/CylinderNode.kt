package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.Entity
import io.github.sceneview.geometries.Cylinder
import io.github.sceneview.math.Position

open class CylinderNode private constructor(
    engine: Engine,
    entity: Entity = EntityManager.get().create(),
    parent: Node? = null,
    override val geometry: Cylinder,
    materialInstances: List<MaterialInstance?>,
    primitivesOffsets: List<IntRange> = geometry.primitivesOffsets,
    builder: RenderableManager.Builder.() -> Unit = {}
) : GeometryNode(engine, entity, parent, geometry, materialInstances, primitivesOffsets, builder) {

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        geometry: Cylinder,
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        entity = entity,
        parent = parent,
        geometry = geometry,
        materialInstances = listOf(materialInstance),
        primitivesOffsets = listOf(0..geometry.primitivesOffsets.last().last),
        builder = builder
    )

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        radius: Float = Cylinder.DEFAULT_RADIUS,
        height: Float = Cylinder.DEFAULT_HEIGHT,
        center: Position = Cylinder.DEFAULT_CENTER,
        sideCount: Int = Cylinder.DEFAULT_SIDE_COUNT,
        materialInstances: List<MaterialInstance?>,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        entity = entity,
        parent = parent,
        geometry = Cylinder.Builder()
            .radius(radius)
            .height(height)
            .center(center)
            .sideCount(sideCount)
            .build(engine),
        materialInstances = materialInstances,
        builder = builder
    )

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        radius: Float = Cylinder.DEFAULT_RADIUS,
        height: Float = Cylinder.DEFAULT_HEIGHT,
        center: Position = Cylinder.DEFAULT_CENTER,
        sideCount: Int = Cylinder.DEFAULT_SIDE_COUNT,
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        entity = entity,
        parent = parent,
        geometry = Cylinder.Builder()
            .radius(radius)
            .height(height)
            .center(center)
            .sideCount(sideCount)
            .build(engine),
        materialInstance = materialInstance,
        builder = builder
    )

    fun updateGeometry(
        radius: Float = geometry.radius,
        height: Float = geometry.height,
        center: Position = geometry.center,
        sideCount: Int = geometry.sideCount
    ) = setGeometry(geometry.update(radius, height, center, sideCount))
}