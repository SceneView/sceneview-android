package io.github.sceneview.collision

/** Mathematical representation of a ray. Used to perform intersection and collision tests. */
class Ray {
    private var origin = Vector3()
    private var direction = Vector3.forward()

    /** Create a ray with an origin of (0,0,0) and a direction of Vector3.forward(). */
    constructor()

    /**
     * Create a ray with a specified origin and direction. The direction will automatically be
     * normalized.
     *
     * @param origin the ray's origin
     * @param direction the ray's direction
     */
    constructor(origin: Vector3, direction: Vector3) {
        Preconditions.checkNotNull(origin, "Parameter \"origin\" was null.")
        Preconditions.checkNotNull(direction, "Parameter \"direction\" was null.")

        setOrigin(origin)
        setDirection(direction)
    }

    /**
     * Set the origin of the ray in world coordinates.
     *
     * @param origin the new origin of the ray.
     */
    fun setOrigin(origin: Vector3) {
        Preconditions.checkNotNull(origin, "Parameter \"origin\" was null.")
        this.origin.set(origin)
    }

    /**
     * Get the origin of the ray.
     *
     * @return a new vector that represents the ray's origin
     */
    fun getOrigin(): Vector3 = Vector3(origin)

    /**
     * Set the direction of the ray. The direction will automatically be normalized.
     *
     * @param direction the new direction of the ray
     */
    fun setDirection(direction: Vector3) {
        Preconditions.checkNotNull(direction, "Parameter \"direction\" was null.")

        this.direction.set(direction.normalized())
    }

    /**
     * Get the direction of the ray.
     *
     * @return a new vector that represents the ray's direction
     */
    fun getDirection(): Vector3 = Vector3(direction)

    /**
     * Get a point at a distance along the ray.
     *
     * @param distance distance along the ray of the point
     * @return a new vector that represents a point at a distance along the ray.
     */
    fun getPoint(distance: Float): Vector3 = Vector3.add(origin, direction.scaled(distance))

    override fun toString(): String = "[Origin:$origin, Direction:$direction]"
}
