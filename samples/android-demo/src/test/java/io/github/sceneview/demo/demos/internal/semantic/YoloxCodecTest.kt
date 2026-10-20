package io.github.sceneview.demo.demos.internal.semantic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ln

/** The YOLOX letterbox and decoding, on hand-made outputs: what runs round the model on the phone. */
class YoloxCodecTest {
    @Test
    fun `a 416 input has one row per cell of the three grids`() {
        assertEquals(52 * 52 + 26 * 26 + 13 * 13, YoloxCodec.rowsFor(416))
    }

    @Test
    fun `the photo is drawn top left, BGR, unscaled 0-255, and the rest is grey`() {
        // A red pixel beside a blue one, into a 4 × 4 input: drawn 2× into the top two rows.
        val image = SceneImage(2, 1, intArrayOf(0xFFFF0000.toInt(), 0xFF0000FF.toInt()))
        val input = FloatArray(3 * 16)
        val ratio = YoloxCodec.input(image, 4, input)
        assertEquals(2f, ratio, 0f)
        val plane = 16
        assertEquals("blue of the red pixel", 0f, input[0], 0f)
        assertEquals("red of the red pixel", 255f, input[2 * plane], 0f)
        assertEquals("blue of the blue pixel", 255f, input[3], 0f)
        assertEquals("red of the blue pixel", 0f, input[2 * plane + 3], 0f)
        for (at in 8 until 16) assertEquals("row ${at / 4} is padding", YoloxCodec.PAD, input[at], 0f)
    }

    @Test
    fun `a row decodes to its box in the photo's fractions, the best class named`() {
        val out = Output(size = 32)
        // Grid of stride 8, cell (1, 2): centre (1.5, 2.5) cells = (12, 20) px, 8 px square.
        out.row(gx = 1, gy = 2, dx = 0.5f, dy = 0.5f, objectness = 0.9f, "couch" to 0.8f, "chair" to 0.1f)
        val found = out.decode()
        assertEquals(1, found.size)
        val d = found.single()
        assertEquals("couch", d.name)
        assertEquals(SceneLabel.Sofa, d.label)
        assertEquals(0.72f, d.score, 1e-5f)
        assertEquals(0.25f, d.left, 1e-5f)
        assertEquals(0.5f, d.top, 1e-5f)
        assertEquals(0.5f, d.right, 1e-5f)
        assertEquals(0.75f, d.bottom, 1e-5f)
    }

    @Test
    fun `the box is scaled back by the letterbox ratio`() {
        val out = Output(size = 32)
        out.row(gx = 1, gy = 2, dx = 0.5f, dy = 0.5f, objectness = 0.9f, "bed" to 0.9f)
        // The photo was drawn at half size: 64 × 64 px, the box twice as large in pixels, the same fraction.
        val d = out.decode(ratio = 0.5f, width = 64, height = 64).single()
        assertEquals(0.25f, d.left, 1e-5f)
        assertEquals(0.75f, d.bottom, 1e-5f)
    }

    @Test
    fun `overlapping boxes of one class merge, of two classes both stay, faint ones drop`() {
        val out = Output(size = 32)
        out.row(gx = 1, gy = 2, dx = 0.5f, dy = 0.5f, objectness = 0.9f, "couch" to 0.8f)
        out.row(gx = 2, gy = 2, dx = -0.4f, dy = 0.5f, objectness = 0.9f, "couch" to 0.5f)
        out.row(gx = 1, gy = 3, dx = 0.5f, dy = -0.5f, objectness = 0.9f, "chair" to 0.6f)
        out.row(gx = 3, gy = 0, dx = 0.5f, dy = 0.5f, objectness = 0.4f, "tv" to 0.5f)
        val found = out.decode()
        assertEquals(listOf("couch", "chair"), found.map { it.name })
        assertEquals(listOf("couch"), out.decode(keep = { it != "chair" }).map { it.name })
    }

    @Test
    fun `COCO names map to the room's labels, the rest to none`() {
        assertEquals(SceneLabel.Table, SceneLabel.ofDetector("dining table"))
        assertEquals(SceneLabel.Television, SceneLabel.ofDetector("tv"))
        assertEquals(SceneLabel.Plant, SceneLabel.ofDetector("potted plant"))
        assertNull(SceneLabel.ofDetector("person"))
        assertNull(SceneLabel.ofDetector("sports ball"))
        for (label in SceneLabel.entries) assertEquals(label, SceneLabel.ofWire(label.wireName))
        assertTrue(SceneLabel.Door.isOpening && SceneLabel.Window.isOpening && !SceneLabel.Bed.isOpening)
    }

    /** A model output for a [size]² input, every row empty until written. */
    private class Output(val size: Int) {
        val rows = YoloxCodec.rowsFor(size)
        val columns = 5 + YoloxCodec.COCO.size
        val data = FloatArray(rows * columns)

        /** Writes the row of the stride-8 grid's cell ([gx], [gy]): an 8 px box, scores as given. */
        @Suppress("LongParameterList")
        fun row(gx: Int, gy: Int, dx: Float, dy: Float, objectness: Float, vararg scores: Pair<String, Float>) {
            val at = (gy * (size / 8) + gx) * columns
            data[at] = dx
            data[at + 1] = dy
            data[at + 2] = ln(1f)
            data[at + 3] = ln(1f)
            data[at + 4] = objectness
            for ((name, score) in scores) data[at + 5 + YoloxCodec.COCO.indexOf(name)] = score
        }

        fun decode(ratio: Float = 1f, width: Int = size, height: Int = size, keep: (String) -> Boolean = { true }) =
            YoloxCodec.decode(data, rows, columns, size, ratio, width, height, keep = keep)
    }
}
