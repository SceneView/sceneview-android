package io.github.sceneview.ar.rerun

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Edge-case tests for [RerunWireFormat] that complement the golden-JSON tests
 * in [RerunWireFormatTest].
 *
 * Focus areas:
 * - JSON string escaping for all control characters and special chars.
 * - Boundary conditions for the packed point-cloud buffer (non-multiple-of-4 tolerance).
 * - Timestamp extremes (0, Long.MAX_VALUE).
 * - Anchor/hit entity path uniqueness for different ids.
 * - All JSON lines contain the mandatory `"t"` and `"type"` keys.
 */
class RerunWireFormatEdgeCaseTest {

    // ── Escape sequences ─────────────────────────────────────────────────────

    @Test
    fun `backslash in entity path is escaped`() {
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = 1L,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
            entity = "world\\camera",
        )
        assertTrue(
            "backslash must be escaped as \\\\",
            line.contains("world\\\\camera")
        )
    }

    @Test
    fun `newline in entity path is escaped`() {
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = 1L,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
            entity = "world/bad\npath",
        )
        assertFalse("raw newline must not appear inside entity value", line.count { it == '\n' } > 1)
        assertTrue("newline should be escaped as \\n", line.contains("\\n"))
    }

    @Test
    fun `carriage-return in entity path is escaped`() {
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = 1L,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
            entity = "world/bad\rpath",
        )
        assertFalse("raw CR must not appear in JSON", line.contains("\r"))
        assertTrue("CR must be escaped as \\r", line.contains("\\r"))
    }

    @Test
    fun `tab in entity path is escaped`() {
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = 1L,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
            entity = "world/bad\tpath",
        )
        assertFalse("raw tab must not appear in JSON", line.contains("\t"))
        assertTrue("tab must be escaped as \\t", line.contains("\\t"))
    }

    @Test
    fun `control character U+0001 in entity path is unicode-escaped`() {
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = 1L,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
            entity = "world/\u0001path",
        )
        assertTrue("control char must be \\u-escaped", line.contains("\\u0001"))
    }

    // ── Timestamp boundaries ─────────────────────────────────────────────────

    @Test
    fun `timestamp zero is emitted verbatim`() {
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = 0L,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
        )
        assertTrue("zero timestamp must appear in JSON", line.contains("\"t\":0"))
    }

    @Test
    fun `large timestamp is emitted without scientific notation`() {
        val ts = 9_876_543_210_123_456L
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = ts,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
        )
        assertTrue(
            "large timestamp $ts must appear literally in JSON",
            line.contains("\"t\":$ts")
        )
        assertFalse("timestamp must not use 'E' notation", line.contains("E") && line.indexOf("E") < line.indexOf("\"type\""))
    }

    @Test
    fun `Long MAX_VALUE timestamp does not crash serializer`() {
        val line = RerunWireFormat.cameraPoseJson(
            timestampNanos = Long.MAX_VALUE,
            tx = 0f, ty = 0f, tz = 0f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
        )
        assertTrue(line.endsWith("\n"))
        assertTrue(line.contains("\"t\":${Long.MAX_VALUE}"))
    }

    // ── Anchor / hit entity path uniqueness ──────────────────────────────────

    @Test
    fun `different anchor ids produce different entity paths`() {
        val line1 = RerunWireFormat.anchorJson(1L, 1, 0f, 0f, 0f, 0f, 0f, 0f, 1f)
        val line2 = RerunWireFormat.anchorJson(1L, 2, 0f, 0f, 0f, 0f, 0f, 0f, 1f)
        assertFalse("anchor lines for different ids must differ", line1 == line2)
        assertTrue(line1.contains("world/anchors/1"))
        assertTrue(line2.contains("world/anchors/2"))
    }

    @Test
    fun `different hit ids produce different entity paths`() {
        val line1 = RerunWireFormat.hitResultJson(1L, 10, 0f, 0f, 0f, 1f)
        val line2 = RerunWireFormat.hitResultJson(1L, 20, 0f, 0f, 0f, 1f)
        assertTrue(line1.contains("world/hits/10"))
        assertTrue(line2.contains("world/hits/20"))
    }

    // ── Mandatory JSON keys ───────────────────────────────────────────────────

    @Test
    fun `all serialized line types contain t and type keys`() {
        val lines = listOf(
            RerunWireFormat.cameraPoseJson(1L, 0f, 0f, 0f, 0f, 0f, 0f, 1f),
            RerunWireFormat.anchorJson(1L, 1, 0f, 0f, 0f, 0f, 0f, 0f, 1f),
            RerunWireFormat.hitResultJson(1L, 1, 0f, 0f, 0f, 1f),
            RerunWireFormat.pointCloudJson(1L, floatArrayOf(0f, 0f, 0f, 1f)),
            RerunWireFormat.planeJson(1L, 1, "horizontal_upward", emptyList()),
        )
        for (line in lines) {
            assertTrue("missing \"t\" key in: $line", line.contains("\"t\":"))
            assertTrue("missing \"type\" key in: $line", line.contains("\"type\":"))
            assertTrue("missing \"entity\" key in: $line", line.contains("\"entity\":"))
        }
    }

    // ── point-cloud: single point ─────────────────────────────────────────────

    @Test
    fun `pointCloud with single point parses correctly`() {
        val packed = floatArrayOf(7f, 8f, 9f, 0.5f)
        val line = RerunWireFormat.pointCloudJson(1L, packed)
        assertTrue(line.contains("\"positions\":[[7.0,8.0,9.0]]"))
        assertTrue(line.contains("\"confidences\":[0.5]"))
    }

    // ── plane: single-vertex polygon ─────────────────────────────────────────

    @Test
    fun `plane with single polygon vertex serializes as one-element array`() {
        val poly = listOf(floatArrayOf(1f, 0f, 2f))
        val line = RerunWireFormat.planeJson(1L, 5, "vertical", poly)
        assertTrue(line.contains("\"polygon\":[[1.0,0.0,2.0]]"))
    }

    // ── anchor: negative floats ───────────────────────────────────────────────

    @Test
    fun `anchor serializes negative translation components`() {
        val line = RerunWireFormat.anchorJson(
            timestampNanos = 1L,
            id = 3,
            tx = -1.5f, ty = -2.0f, tz = -3.14f,
            qx = 0f, qy = 0f, qz = 0f, qw = 1f,
        )
        assertTrue(line.contains("-1.5"))
        assertTrue(line.contains("-2.0"))
        assertTrue(line.contains("-3.14"))
    }

    // ── hit result: non-finite distance clamped to 0 ─────────────────────────

    @Test
    fun `hitResult with NaN distance does not leak NaN into JSON`() {
        // hitResultJson calls appendFloat internally, which clamps non-finite to 0.
        val line = RerunWireFormat.hitResultJson(
            timestampNanos = 1L,
            id = 1,
            tx = 0f, ty = 0f, tz = 0f,
            distance = Float.NaN,
        )
        assertFalse("NaN must not appear in JSON", line.contains("NaN"))
        assertTrue("clamped NaN distance should be 0", line.contains("\"distance\":0"))
    }

    @Test
    fun `hitResult with Infinity distance is clamped to 0`() {
        val line = RerunWireFormat.hitResultJson(
            timestampNanos = 1L,
            id = 1,
            tx = 0f, ty = 0f, tz = 0f,
            distance = Float.POSITIVE_INFINITY,
        )
        assertFalse("Infinity must not appear in JSON", line.contains("Infinity"))
        assertTrue("clamped Infinity distance should be 0", line.contains("\"distance\":0"))
    }
}
