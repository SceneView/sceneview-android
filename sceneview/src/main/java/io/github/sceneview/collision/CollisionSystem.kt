package io.github.sceneview.collision

import android.view.MotionEvent
import io.github.sceneview.node.CameraNode
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier

// TODO: Only moved to kotlin without cleaning it
class CollisionSystem {

    lateinit var cameraNode: CameraNode

    private val colliders = mutableListOf<Collider>()

    fun addCollider(collider: Collider) {
        colliders.add(collider)
    }

    fun removeCollider(collider: Collider) {
        colliders.remove(collider)
    }

    /**
     * Tests to see if a ray starting from the screen/camera position is hitting any nodes within
     * the scene and returns a list of HitTestResults containing all of the nodes that were hit,
     * sorted by distance.
     *
     * Specify the camera/screen/view position where the hit ray will start from in screen
     * coordinates.
     *
     * @param xPx x view coordinate in pixels
     * @param yPx y view coordinate in pixels
     *
     * @return PickHitResult list for each nodes that was hit sorted by distance.
     * Empty if no nodes were hit.
     */
    fun hitTest(xPx: Float, yPx: Float) = hitTest(cameraNode.screenPointToRay(xPx, yPx))

    /**
     * Tests to see if a ray starting from the screen/camera position is hitting any nodes within
     * the scene and returns a list of HitTestResults containing all of the nodes that were hit,
     * sorted by distance.
     *
     * Specify the camera/screen/view position where the hit ray will start from in screen
     * coordinates.
     *
     * @param motionEvent view motion event
     *
     * @return PickHitResult list for each nodes that was hit sorted by distance.
     * Empty if no nodes were hit.
     */
    fun hitTest(motionEvent: MotionEvent) = hitTest(motionEvent.x, motionEvent.y)

    /**
     * Tests to see if a ray is hitting any nodes within the scene and returns a list of
     * HitTestResults containing all of the nodes that were hit, sorted by distance.
     *
     * @param ray The ray to use for the test.
     *
     * @return PickHitResult list for each nodes that was hit sorted by distance.
     * Empty if no nodes were hit.
     */
    fun hitTest(ray: Ray) = arrayListOf<HitResult>().apply {
        raycastAll(ray, this, { resultPick, collider ->
            resultPick.node = collider.node
        }, { HitResult() })
    }.toList()

    fun raycast(ray: Ray, resultHit: RayHit): Collider? {
        resultHit.reset()
        var result: Collider? = null
        val tempResult = RayHit()
        for (collider in colliders) {
            val collisionShape = collider.transformedShape ?: continue
            if (collisionShape.rayIntersection(ray, tempResult)) {
                if (tempResult.distance < resultHit.distance) {
                    resultHit.set(tempResult)
                    result = collider
                }
            }
        }
        return result
    }

    fun <T : RayHit?> raycastAll(
        ray: Ray,
        resultBuffer: ArrayList<T>,
        processResult: BiConsumer<T, Collider>?,
        allocateResult: Supplier<T>
    ): Int {
        val tempResult = RayHit()
        var hitCount = 0

        // Check the ray against all the colliders.
        for (collider in colliders) {
            val collisionShape = collider.transformedShape ?: continue
            if (collisionShape.rayIntersection(ray, tempResult)) {
                hitCount++
                var result: T? = null
                if (resultBuffer.size >= hitCount) {
                    result = resultBuffer[hitCount - 1]
                } else {
                    result = allocateResult.get()
                    resultBuffer.add(result)
                }
                result!!.reset()
                result.set(tempResult)
                processResult?.accept(result, collider)
            }
        }

        // Reset extra hits in the buffer.
        for (i in hitCount until resultBuffer.size) {
            resultBuffer[i]!!.reset()
        }

        // Sort the hits by distance.
        resultBuffer.sortWith(Comparator { a: T?, b: T? ->
            a!!.distance.compareTo(b!!.distance)
        })
        return hitCount
    }

    fun intersects(collider: Collider): Collider? {
        val collisionShape = collider.transformedShape ?: return null
        for (otherCollider in colliders) {
            if (otherCollider === collider) {
                continue
            }
            val otherCollisionShape = otherCollider.transformedShape ?: continue
            if (collisionShape.shapeIntersection(otherCollisionShape)) {
                return otherCollider
            }
        }
        return null
    }

    fun intersectsAll(collider: Collider, processResult: Consumer<Collider>) {
        val collisionShape = collider.transformedShape ?: return
        for (otherCollider in colliders) {
            if (otherCollider === collider) {
                continue
            }
            val otherCollisionShape = otherCollider.transformedShape ?: continue
            if (collisionShape.shapeIntersection(otherCollisionShape)) {
                processResult.accept(otherCollider)
            }
        }
    }

    companion object {
        private val TAG = CollisionSystem::class.java.simpleName
    }
}