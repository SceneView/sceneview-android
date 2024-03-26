package io.github.sceneview.node

import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import io.github.sceneview.geometries.Sphere
import io.github.sceneview.math.Position

open class SphereNode private constructor(
    engine: Engine,
    override val geometry: Sphere,
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
        geometry: Sphere,
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
        radius: Float = Sphere.DEFAULT_RADIUS,
        center: Position = Sphere.DEFAULT_CENTER,
        stacks: Int = Sphere.DEFAULT_STACKS,
        slices: Int = Sphere.DEFAULT_SLICES,
        materialInstances: List<MaterialInstance?>,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        geometry = Sphere.Builder()
            .radius(radius)
            .center(center)
            .stacks(stacks)
            .slices(slices)
            .build(engine),
        materialInstances = materialInstances,
        builderApply = builderApply
    )

    constructor(
        engine: Engine,
        radius: Float = Sphere.DEFAULT_RADIUS,
        center: Position = Sphere.DEFAULT_CENTER,
        stacks: Int = Sphere.DEFAULT_STACKS,
        slices: Int = Sphere.DEFAULT_SLICES,
        materialInstance: MaterialInstance? = null,
        builderApply: RenderableManager.Builder.() -> Unit = {}
    ) : this(
        engine = engine,
        geometry = Sphere.Builder()
            .radius(radius)
            .center(center)
            .stacks(stacks)
            .slices(slices)
            .build(engine),
        materialInstance = materialInstance,
        builderApply = builderApply
    )

    fun updateGeometry(
        radius: Float = geometry.radius,
        center: Position = geometry.center,
        stacks: Int = geometry.stacks,
        slices: Int = geometry.slices
    ) = setGeometry(geometry.update(engine, radius, center, stacks, slices))
}