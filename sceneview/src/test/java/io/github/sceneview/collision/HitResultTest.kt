package io.github.sceneview.collision

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [HitResult] and its parent [RayHit].
 *
 * [HitResult] stores a [Node] reference but also inherits distance/point tracking from [RayHit].
 * The RayHit distance/point functionality is testable without Filament. The Node reference
 * requires a Filament Engine to construct, so we test only the non-Node parts here.
 *
 * NOTE: HitResult.node getter throws IllegalStateException after reset() or when constructed
 * with null. We cannot test node assignment without a real Node (requires Engine).
 */
class HitResultTest {

    @Test
    fun `default distance is Float MAX_VALUE`() {
        val hit = HitResult()
        assertEquals(Float.MAX_VALUE, hit.getDistance(), 0f)
    }

    @Test
    fun `setDistance and getDistance round-trip`() {
        val hit = HitResult()
        hit.setDistance(42.5f)
        assertEquals(42.5f, hit.getDistance(), 0f)
    }

    @Test
    fun `reset restores distance to MAX_VALUE`() {
        val hit = HitResult()
        hit.setDistance(1.0f)
        hit.reset()
        assertEquals(Float.MAX_VALUE, hit.getDistance(), 0f)
    }

    @Test
    fun `getPoint returns zero vector after construction`() {
        val hit = HitResult()
        val point = hit.getPoint()
        assertEquals(0f, point.x, 0f)
        assertEquals(0f, point.y, 0f)
        assertEquals(0f, point.z, 0f)
    }

    @Test
    fun `setPoint and getPoint round-trip`() {
        val hit = HitResult()
        val p = Vector3(1f, 2f, 3f)
        hit.setPoint(p)

        val result = hit.getPoint()
        assertEquals(1f, result.x, 0f)
        assertEquals(2f, result.y, 0f)
        assertEquals(3f, result.z, 0f)
    }

    @Test
    fun `getWorldPosition returns Float3 from point`() {
        val hit = HitResult()
        hit.setPoint(Vector3(7f, 8f, 9f))

        val wp = hit.getWorldPosition()
        assertEquals(7f, wp.x, 0f)
        assertEquals(8f, wp.y, 0f)
        assertEquals(9f, wp.z, 0f)
    }

    @Test
    fun `reset clears point to zero`() {
        val hit = HitResult()
        hit.setPoint(Vector3(5f, 5f, 5f))
        hit.reset()

        val point = hit.getPoint()
        assertEquals(0f, point.x, 0f)
        assertEquals(0f, point.y, 0f)
        assertEquals(0f, point.z, 0f)
    }

    @Test(expected = IllegalStateException::class)
    fun `accessing node after default construction throws`() {
        val hit = HitResult()
        hit.node // should throw
    }

    @Test(expected = IllegalStateException::class)
    fun `accessing node after reset throws`() {
        val hit = HitResult()
        hit.reset()
        hit.node // should throw
    }
}
