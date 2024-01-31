package io.github.sceneview.managers

import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.RenderableManager
import com.google.android.filament.VertexBuffer
import io.github.sceneview.Entity
import io.github.sceneview.EntityInstance
import io.github.sceneview.SceneView
import io.github.sceneview.geometries.Cube
import io.github.sceneview.geometries.Cylinder
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.Plane
import io.github.sceneview.geometries.Sphere

/**
 * Specifies the geometry data for a primitive.
 *
 * Filament primitives must have an associated [VertexBuffer] and [IndexBuffer].
 * Typically, each primitive is specified with a pair of daisy-chained calls:
 * [geometry] and [RenderableManager.Builder.material].
 * @see Geometry
 * @see Plane
 * @see Cube
 * @see Sphere
 * @see Cylinder
 * @see RenderableManager.setGeometry
 */
fun RenderableManager.Builder.geometry(geometry: Geometry) = apply {
    geometry.offsetsCounts.forEachIndexed { primitiveIndex, (offset, count) ->
        geometry(
            primitiveIndex,
            RenderableManager.PrimitiveType.TRIANGLES,
            geometry.vertexBuffer,
            geometry.indexBuffer,
            offset,
            count
        )
    }
    // Overall bounding box of the renderable
    boundingBox(geometry.boundingBox)
}

/**
 * Changes the geometry for the given renderable instance.
 *
 * @see Geometry
 * @see Plane
 * @see Cube
 * @see Sphere
 * @see Cylinder
 * @see RenderableManager.Builder.geometry
 */
fun RenderableManager.setGeometry(
    instance: EntityInstance,
    geometry: Geometry
) {
    // Update the geometry and material instances
    geometry.offsetsCounts.forEachIndexed { primitiveIndex, (offset, count) ->
        setGeometryAt(
            instance,
            primitiveIndex,
            RenderableManager.PrimitiveType.TRIANGLES,
            geometry.vertexBuffer,
            geometry.indexBuffer,
            offset,
            count
        )
    }
    setAxisAlignedBoundingBox(instance, geometry.boundingBox)
}

fun RenderableManager.Builder.build(engine: Engine, entity: Entity = EntityManager.get().create()) =
    build(engine, entity)

fun RenderableManager.Builder.build(
    sceneView: SceneView,
    entity: Entity = EntityManager.get().create()
) = build(sceneView.engine, entity).also {
    sceneView.renderables += entity
}

fun SceneView.destroyRenderable(entity: Entity) {
    renderableManager.destroy(entity)
    renderables -= entity
}