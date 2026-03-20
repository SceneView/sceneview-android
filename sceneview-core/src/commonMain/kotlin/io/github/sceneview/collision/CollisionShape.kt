package io.github.sceneview.collision

/** Base class for all types of shapes that collision checks can be performed against. */
abstract class CollisionShape {
    private val changeId = ChangeId()

    abstract fun makeCopy(): CollisionShape

    /**
     * Must be called by subclasses when the shape changes to inform listeners of the change.
     */
    protected fun onChanged() {
        changeId.update()
    }

    abstract fun rayIntersection(ray: Ray, result: RayHit): Boolean

    abstract fun shapeIntersection(shape: CollisionShape): Boolean

    internal abstract fun sphereIntersection(sphere: Sphere): Boolean

    internal abstract fun boxIntersection(box: Box): Boolean

    init {
        changeId.update()
    }

    internal fun getId(): ChangeId = changeId

    internal abstract fun transform(transformProvider: TransformProvider): CollisionShape

    internal abstract fun transform(transformProvider: TransformProvider, result: CollisionShape)
}
