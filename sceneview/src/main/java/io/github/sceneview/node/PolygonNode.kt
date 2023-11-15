package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.Entity
import io.github.sceneview.geometries.Polygon
import io.github.sceneview.geometries.UvCoordinate
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position

open class PolygonNode private constructor(
    engine: Engine,
    entity: Entity = EntityManager.get().create(),
    parent: Node? = null,
    override val geometry: Polygon,
    materialInstances: List<MaterialInstance?>,
    primitivesOffsets: List<IntRange> = geometry.primitivesOffsets,
    builder: RenderableManager.Builder.() -> Unit = {}
) : GeometryNode(engine, entity, parent, geometry, materialInstances, primitivesOffsets, builder) {

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        geometry: Polygon,
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
        path: List<Position>,
        normal: Direction? = Polygon.DEFAULT_NORMAL,
        uvCoordinate: UvCoordinate? = Polygon.DEFAULT_UV_COORDINATE,
        color: Color? = null,
        materialInstances: List<MaterialInstance?>,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        entity = entity,
        parent = parent,
        geometry = Polygon.Builder()
            .boundary(path, normal, uvCoordinate, color)
            .build(engine),
        materialInstances = materialInstances,
        builder = builder
    )

    constructor(
        engine: Engine,
        entity: Entity = EntityManager.get().create(),
        parent: Node? = null,
        path: List<Position>,
        normal: Direction? = Polygon.DEFAULT_NORMAL,
        uvCoordinate: UvCoordinate? = Polygon.DEFAULT_UV_COORDINATE,
        color: Color? = null,
        materialInstance: MaterialInstance? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        entity = entity,
        parent = parent,
        geometry = Polygon.Builder()
            .boundary(path, normal, uvCoordinate, color)
            .build(engine),
        materialInstance = materialInstance,
        builder = builder
    )

    var boundary
        get() = geometry.boundary
        set(value) {
            setGeometry(geometry.apply { boundary = value })
        }

    fun updateGeometry(
        positions: List<Position> = boundary.map { it.position },
        normal: Direction? = Polygon.DEFAULT_NORMAL,
        uvCoordinate: UvCoordinate? = Polygon.DEFAULT_UV_COORDINATE,
        color: Color? = null
    ) = setGeometry(geometry.update(positions, normal, uvCoordinate, color))
}