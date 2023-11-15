package io.github.sceneview.collision

import android.view.MotionEvent
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.math.toCollisionRay
import io.github.sceneview.utils.motionEventToRay
import io.github.sceneview.utils.screenToRay
import io.github.sceneview.utils.viewToRay
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Supplier

// TODO: Completely move to Kotlin
class CollisionSystem(var view: View) {

    private val colliders = mutableListOf<Collider>()

    fun addCollider(collider: Collider) {
        colliders.add(collider)
    }

    fun removeCollider(collider: Collider) {
        colliders.remove(collider)
    }

    @Deprecated(message = "Use hitTest", replaceWith = ReplaceWith("hitTest(ray)"))
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

    @Deprecated(message = "Use hitTest", replaceWith = ReplaceWith("hitTest(ray)"))
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

    /**
     * Tests to see if a ray is hitting any nodes within the scene.
     *
     * @param motionEvent motionEvent The motion event where you want the hit ray to happen from.
     *
     * @return [HitResult] list containing nodes that were hit sorted by distance.
     * Empty if no nodes were hit.
     */
    fun hitTest(motionEvent: MotionEvent) = hitTest(view.motionEventToRay(motionEvent))

    /**
     * Tests to see if a ray starting from the screen/camera position is hitting any nodes within
     * the scene and returns a list of HitResult containing all the nodes that were hit, sorted by
     * distance.
     *
     * Specify the camera/screen/view position where the hit ray will start from in screen
     * coordinates.
     *
     * @param xPx x screen coordinate in pixels
     * @param yPx y screen coordinate in pixels
     *
     * @return [HitResult] list containing nodes that were hit sorted by distance.
     * Empty if no nodes were hit.
     */
    fun hitTest(xPx: Float, yPx: Float) = hitTest(view.screenToRay(xPx, yPx))

    /**
     * Tests to see if a ray starting from the view position is hitting any nodes within the scene
     * and returns a list of HitResult containing all the nodes that were hit, sorted by distance.
     *
     * @param viewPosition normalized view coordinate
     * x = (0 = left, 0.5 = center, 1 = right)
     * y = (0 = bottom, 0.5 = center, 1 = top)
     *
     * @return [HitResult] list containing the nodes that were hit sorted by distance.
     * Empty if no nodes were hit.
     */
    fun hitTest(viewPosition: Float2) = hitTest(view.camera!!.viewToRay(viewPosition))

    /**
     * Tests to see if a ray starting from the view position is hitting any nodes within the scene
     * and returns a list of HitResult containing all the nodes that were hit, sorted by distance.
     *
     * @param ray The ray to use for the test.
     *
     * @return [HitResult] list containing the nodes that were hit sorted by distance.
     * Empty if no nodes were hit.
     */
    fun hitTest(ray: dev.romainguy.kotlin.math.Ray): List<HitResult> =
        colliders.mapNotNull { collider ->
            HitResult().takeIf {
                collider.transformedShape?.rayIntersection(ray.toCollisionRay(), it) == true
            }?.apply {
                node = collider.node
            }?.takeIf {
                it.node.isHittable
            }
        }.sortedWith { a: HitResult, b: HitResult ->
            // Sort the hits by distance.
            a.distance.compareTo(b.distance)
        }

    fun intersects(collider: Collider): Collider? {
        val collisionShape = collider.transformedShape ?: return null
        for (otherCollider in colliders) {
            if (otherCollider == collider) {
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
            if (otherCollider == collider) {
                continue
            }
            val otherCollisionShape = otherCollider.transformedShape ?: continue
            if (collisionShape.shapeIntersection(otherCollisionShape)) {
                processResult.accept(otherCollider)
            }
        }
    }

    fun destroy() {
    }

    companion object {
        private val TAG = CollisionSystem::class.java.simpleName
    }
}