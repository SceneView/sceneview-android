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

    fun setOrigin(origin: Vector3) {
        Preconditions.checkNotNull(origin, "Parameter \"origin\" was null.")
        this.origin.set(origin)
    }

    fun getOrigin(): Vector3 = Vector3(origin)

    fun setDirection(direction: Vector3) {
        Preconditions.checkNotNull(direction, "Parameter \"direction\" was null.")
        this.direction.set(direction.normalized())
    }

    fun getDirection(): Vector3 = Vector3(direction)

    fun getPoint(distance: Float): Vector3 = Vector3.add(origin, direction.scaled(distance))

    override fun toString(): String = "[Origin:$origin, Direction:$direction]"
}
