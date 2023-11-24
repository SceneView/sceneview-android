package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
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
        geometry: Plane,
        materialInstance: MaterialInstance? = null,
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine, geometry, listOf(0..geometry.primitivesOffsets.last().last),
        listOf(materialInstance), parent, builder
    )

    constructor(
        engine: Engine,
        geometry: Plane,
        materialInstances: List<MaterialInstance?>,
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(engine, geometry, geometry.primitivesOffsets, materialInstances, parent, builder)

    constructor(
        engine: Engine,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        materialInstances: List<MaterialInstance?>,
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        Plane.Builder()
            .size(size)
            .center(center)
            .normal(normal)
            .uvScale(uvScale)
            .build(engine),
        materialInstances, parent, builder
    )

    constructor(
        engine: Engine,
        size: Size = Plane.DEFAULT_SIZE,
        center: Position = Plane.DEFAULT_CENTER,
        normal: Direction = Plane.DEFAULT_NORMAL,
        uvScale: UvScale = UvScale(1.0f),
        materialInstance: MaterialInstance? = null,
        parent: Node? = null,
        builder: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine,
        Plane.Builder()
            .size(size)
            .center(center)
            .normal(normal)
            .uvScale(uvScale)
            .build(engine),
        materialInstance, parent, builder
    )

    fun updateGeometry(
        size: Size = geometry.size,
        center: Position = geometry.center,
        normal: Direction = geometry.normal,
        uvScale: UvScale = geometry.uvScale,
    ) = setGeometry(geometry.update(size, center, normal, uvScale))
}