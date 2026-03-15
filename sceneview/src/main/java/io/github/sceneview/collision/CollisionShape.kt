package io.github.sceneview.collision

/** Base class for all types of shapes that collision checks can be performed against. */
abstract class CollisionShape {
    private val changeId = ChangeId()

    abstract fun makeCopy(): CollisionShape

    /**
     * Must be called by subclasses when the shape changes to inform listeners of the change.
     *
     * @hide
     */
    protected fun onChanged() {
        changeId.update()
    }

    /** @hide */
    abstract fun rayIntersection(ray: Ray, result: RayHit): Boolean

    /** @hide */
    abstract fun shapeIntersection(shape: CollisionShape): Boolean

    /** @hide */
    internal abstract fun sphereIntersection(sphere: Sphere): Boolean

    /** @hide */
    internal abstract fun boxIntersection(box: Box): Boolean

    init {
        changeId.update()
    }

    internal fun getId(): ChangeId = changeId

    internal abstract fun transform(transformProvider: TransformProvider): CollisionShape

    internal abstract fun transform(transformProvider: TransformProvider, result: CollisionShape)
}
