package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.Entity
import io.github.sceneview.geometries.Plane
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Size

open class PlaneNode private constructor(
    engine: Engine,
    entity: Entity = EntityManager.get().create(),
    parent: Node? = null,
    override val geometry: Plane,
    materialInstances: List<MaterialInstance?>,
    primitivesOffsets: List<IntRange> = geometry.primitivesOffsets,
    builder: RenderableManager.Builder.() -> Unit = {}
) : GeometryNode(engine, entity, parent, geometry, materialInstances, primitivesOffsets, builder) {

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        geometry: Plane,
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
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        materialInstances: List<MaterialInstance?>,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        entity = entity,
        parent = parent,
        geometry = Plane.Builder()
            .size(size)
            .center(center)
            .normal(normal)
            .build(engine),
        materialInstances = materialInstances,
        builder = builder
    )

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        entity = entity,
        parent = parent,
        geometry = Plane.Builder()
            .size(size)
            .center(center)
            .normal(normal)
            .build(engine),
        materialInstance = materialInstance,
        builder = builder
    )

    fun updateGeometry(
        size: Size = geometry.size,
        center: Position = geometry.center,
        normal: Direction = geometry.normal
    ) = setGeometry(geometry.update(size, center, normal))
}