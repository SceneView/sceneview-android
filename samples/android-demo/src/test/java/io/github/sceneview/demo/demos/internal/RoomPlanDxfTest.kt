package io.github.sceneview.demo.demos.internal

import io.github.sceneview.demo.demos.internal.RoomPlanDxf.Opening
import io.github.sceneview.demo.demos.internal.RoomPlanDxf.OpeningKind
import io.github.sceneview.demo.demos.internal.RoomPlanLayoutTest.Companion.WORDING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/**
 * The plan's DXF read back pair by pair, with a reader of its own: what a CAD tool finds in it.
 * AutoCAD 2000 in millimetres; the outline one closed polyline on WALLS; each wall's length as
 * lines and text on DIMENSIONS, never a DIMENSION entity; the room's name and area on TEXT; each
 * opening with its size on OPENINGS; the warning to check it on site on LEGEND.
 */
class RoomPlanDxfTest {

    private val room = planSurvey(0f, 0f, 4f, 0f, 4f, 5f, 0f, 5f)
    private val lShape = planSurvey(0f, 0f, 6f, 0f, 6f, 3f, 3f, 3f, 3f, 5f, 0f, 5f)

    @Test
    fun `an AutoCAD 2000 drawing in millimetres, in plain ASCII`() {
        val dxf = dxf(room)
        assertTrue("pairs of a code and a value", dxf.wellFormed && dxf.pairs.isNotEmpty())
        assertEquals("AC1015", dxf.header("\$ACADVER"))
        assertEquals("4", dxf.header("\$INSUNITS")?.trim())
        assertTrue("ASCII only", dxf.ascii)
        assertEquals(listOf("HEADER", "CLASSES", "TABLES", "BLOCKS", "ENTITIES", "OBJECTS"), dxf.sections.keys.toList())
        assertEquals(0 to "EOF", dxf.pairs.lastOrNull())
    }

    @Test
    fun `the five layers are declared and every entity is on one of them`() {
        val dxf = dxf(room, openings = OPENINGS)
        val layers = dxf.objects("TABLES", "LAYER").map { it.first(2) }
        for (name in LAYERS) assertTrue("$name in $layers", name in layers)
        val entities = dxf.objects("ENTITIES")
        assertTrue("entities", entities.isNotEmpty())
        for (entity in entities) assertTrue("${entity.type} on ${entity.layer}", entity.layer in LAYERS)
    }

    @Test
    fun `the walls are one closed polyline, in millimetres, y up`() {
        for (survey in listOf(room, lShape)) {
            val outlines = dxf(survey).entities("LWPOLYLINE", "WALLS")
            assertEquals("outlines", 1, outlines.size)
            val outline = outlines.single()
            assertEquals("closed", 1, (outline.first(70)?.trim()?.toInt() ?: 0) and 1)
            val expected = survey.walls.map { at(survey, it.ax, it.ay) }
            val vertices = outline.vertices
            assertEquals("vertices", expected.size, vertices.size)
            expected.zip(vertices).forEach { (e, v) ->
                assertTrue("${v.toList()} for ${e.toList()}", hypot(e[0] - v[0], e[1] - v[1]) < 0.5)
            }
        }
    }

    @Test
    fun `each wall is dimensioned outside it with lines and text, never a DIMENSION entity`() {
        for (survey in listOf(room, lShape)) {
            val dxf = dxf(survey)
            assertTrue("no DIMENSION entity", dxf.entities("DIMENSION").isEmpty())
            val labels = SurveyReport.planTexts(survey, WORDING).walls
            val lines = dxf.entities("LINE", "DIMENSIONS")
            val texts = dxf.entities("TEXT", "DIMENSIONS")
            survey.walls.forEachIndexed { i, wall ->
                val side = Side(at(survey, wall.ax, wall.ay), at(survey, wall.bx, wall.by))
                val line = lines.any { line ->
                    val a = line.point(10)
                    val b = line.point(11)
                    val mid = doubleArrayOf((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
                    side.parallel(a, b) && abs(hypot(b[0] - a[0], b[1] - a[1]) - side.length) < 1.0 &&
                        side.out(mid) > 0 && abs(side.along(mid) - side.length / 2) < 1.0
                }
                assertTrue("wall ${wall.tag}: its dimension line", line)
                val text = texts.any { text ->
                    val p = text.point(11)
                    text.text == labels[i] && side.out(p) > 0 && abs(side.along(p) - side.length / 2) < 1.0 &&
                        side.readsAlong(text.number(50) ?: 0.0)
                }
                assertTrue("wall ${wall.tag}: ${labels[i]} in ${texts.map { it.text }}", text)
            }
        }
    }

    @Test
    fun `the room's name and floor area stand inside it, on TEXT`() {
        for (survey in listOf(room, lShape)) {
            val texts = dxf(survey).entities("TEXT", "TEXT")
            val outline = survey.walls.map { at(survey, it.ax, it.ay) }
            for (label in listOf(WORDING.room.format(1), SurveyReport.area(survey.area!!, WORDING.units))) {
                val text = texts.singleOrNull { it.text == label }
                assertNotNull("$label in ${texts.map { it.text }}", text)
                assertTrue("$label inside the room", inside(outline, text!!.point(11)))
            }
        }
    }

    @Test
    fun `openings carry their width, and their height and sill when known`() {
        val dxf = dxf(room, openings = OPENINGS)
        assertEquals(
            setOf("Door 0.90 × 2.10", "Window 1.20 × 1.20, sill 0.90", "Door 0.80"),
            dxf.entities("TEXT", "OPENINGS").map { it.text }.toSet(),
        )
        // The door swings into the room: a quarter circle as wide as the door, hinged on its wall.
        val top = Side(at(room, 0f, 0f), at(room, 4f, 0f))
        val swings = dxf.entities("ARC", "OPENINGS").filter { abs((it.number(40) ?: 0.0) - 900.0) < 1.0 }
        assertEquals("the door's swing", 1, swings.size)
        val swing = swings.single()
        val hinge = swing.point(10)
        assertTrue("hinged on its wall", abs(top.out(hinge)) < 1.0)
        assertTrue("at one of its jambs", abs(top.along(hinge) - 1000.0) < 1.0 || abs(top.along(hinge) - 1900.0) < 1.0)
        val start = swing.number(50)!!
        assertEquals("a quarter turn", 90.0, ((swing.number(51)!! - start) % 360 + 360) % 360, 0.5)
        val half = Math.toRadians(start + 45)
        assertTrue("into the room", top.out(doubleArrayOf(hinge[0] + 900 * cos(half), hinge[1] + 900 * sin(half))) < 0)
        // The window's frame runs along its wall, over its width.
        val right = Side(at(room, 4f, 0f), at(room, 4f, 5f))
        val frame = dxf.entities("LINE", "OPENINGS").any { line ->
            val a = line.point(10)
            val b = line.point(11)
            right.parallel(a, b) && abs(hypot(b[0] - a[0], b[1] - a[1]) - 1200.0) < 1.0 &&
                abs(min(right.along(a), right.along(b)) - 2000.0) < 1.0 && abs(right.out(a)) < 100.0
        }
        assertTrue("the window's frame", frame)
    }

    @Test
    fun `the legend says to check the plan on site, under it`() {
        val dxf = dxf(room)
        val legend = dxf.entities("TEXT", "LEGEND")
        assertEquals(WORDS.legend, legend.map { it.text })
        val plan = dxf.objects("ENTITIES").filter { it.layer != "LEGEND" }
        assertTrue("the plan", plan.isNotEmpty())
        val bottom = plan.minOf { entity ->
            (entity.all(20) + entity.all(21)).mapNotNull { it.trim().toDoubleOrNull() }.minOrNull() ?: 0.0
        }
        for (line in legend) assertTrue("${line.text} under the plan", line.point(10)[1] < bottom)
    }

    @Test
    fun `an estimate keeps its mark, escaped for AutoCAD 2000`() {
        val base = room
        val walls = base.walls.mapIndexed { i, wall ->
            if (i != 0) {
                wall
            } else {
                SurveyWall(wall.tag, wall.ax, wall.ay, wall.bx, wall.by, SurveyFigure(4f, 0f, estimate = true), 0f, false, listOf(0f..1f))
            }
        }
        val survey = RoomSurvey(
            walls, base.planLength, base.planWidth, base.length, base.width, SurveyFigure(20f, 0f, estimate = true),
            base.perimeter, base.ceilingHeight, base.gaps, 0f, 0f, 0f,
        )
        val dxf = dxf(survey)
        assertTrue("ASCII only", dxf.ascii)
        val dimensions = dxf.entities("TEXT", "DIMENSIONS").map { it.first(1) }
        assertTrue("$dimensions", "\\U+2248 4.00" in dimensions)
        val texts = dxf.entities("TEXT", "TEXT").map { it.first(1) }
        assertTrue("$texts", "\\U+2248 20.0 m\\U+00B2" in texts)
    }

    @Test
    fun `figures in feet and inches when the wording is, the drawing still in millimetres`() {
        val wording = imperialWording()
        val dxf = dxf(room, wording)
        assertEquals("4", dxf.header("\$INSUNITS")?.trim())
        val dimensions = dxf.entities("TEXT", "DIMENSIONS").map { it.text }
        assertTrue("$dimensions", dimensions.containsAll(listOf("13' 1\"", "16' 5\"")))
        val area = SurveyReport.area(room.area!!, MeasureUnitSystem.Imperial)
        assertTrue(area, dxf.entities("TEXT", "TEXT").any { it.text == area })
    }

    @Test
    fun `handles are unique and under the seed, entities owned by the model space`() {
        val dxf = dxf(room, openings = OPENINGS)
        val handles = dxf.sections.filterKeys { it != "HEADER" }.values.flatten().mapNotNull { it.handle }
        assertTrue("handles", handles.isNotEmpty())
        assertEquals("unique", handles.size, handles.toSet().size)
        val seed = dxf.header("\$HANDSEED")?.toLongOrNull(16)
        assertNotNull("\$HANDSEED", seed)
        for (handle in handles) assertTrue("$handle under $seed", handle.toLong(16) in 1 until seed!!)
        assertEquals(listOf("*Model_Space", "*Paper_Space"), dxf.objects("TABLES", "BLOCK_RECORD").map { it.first(2) })
        assertEquals(listOf("*Model_Space", "*Paper_Space"), dxf.objects("BLOCKS", "BLOCK").map { it.first(2) })
        val model = dxf.objects("TABLES", "BLOCK_RECORD").first().handle
        for (entity in dxf.objects("ENTITIES")) assertEquals("${entity.type}'s owner", model, entity.first(330))
    }

    @Test
    fun `a survey with no plan still gives a drawing, its legend alone`() {
        val dxf = dxf(RoomSurvey.empty(SurveyGap.NoWalls))
        assertEquals("AC1015", dxf.header("\$ACADVER"))
        assertEquals(WORDS.legend, dxf.entities("TEXT", "LEGEND").map { it.text })
        assertTrue("the legend alone", dxf.objects("ENTITIES").all { it.layer == "LEGEND" })
    }

    private fun dxf(survey: RoomSurvey, wording: SurveyWording = WORDING, openings: List<Opening> = emptyList()): DxfRead {
        val out = ByteArrayOutputStream()
        RoomPlanDxf.write(out, survey, wording, WORDS, openings)
        return DxfRead(out.toByteArray())
    }

    /** A point of the plan (metres, y down the page) where the drawing has it: millimetres, y up. */
    private fun at(survey: RoomSurvey, x: Float, y: Float) = doubleArrayOf(x * 1000.0, (survey.planWidth - y) * 1000.0)

    /** Whether [p] is inside the outline [corners]. */
    private fun inside(corners: List<DoubleArray>, p: DoubleArray): Boolean {
        var crossings = 0
        for (i in corners.indices) {
            val a = corners[i]
            val b = corners[(i + 1) % corners.size]
            if ((a[1] > p[1]) == (b[1] > p[1])) continue
            if (a[0] + (p[1] - a[1]) * (b[0] - a[0]) / (b[1] - a[1]) > p[0]) crossings++
        }
        return crossings % 2 == 1
    }

    /** A wall on the drawing, from [a] to [b]: clockwise there as on the page, the outside on its left. */
    private class Side(val a: DoubleArray, b: DoubleArray) {
        val length = hypot(b[0] - a[0], b[1] - a[1])
        private val ux = (b[0] - a[0]) / length
        private val uy = (b[1] - a[1]) / length

        /** How far out of the room [p] is from the wall's line. */
        fun out(p: DoubleArray) = -uy * (p[0] - a[0]) + ux * (p[1] - a[1])

        /** How far along the wall [p] is from its start. */
        fun along(p: DoubleArray) = ux * (p[0] - a[0]) + uy * (p[1] - a[1])

        fun parallel(p: DoubleArray, q: DoubleArray): Boolean {
            val l = hypot(q[0] - p[0], q[1] - p[1])
            return l > 0 && abs(ux * (q[1] - p[1]) - uy * (q[0] - p[0])) / l < 1e-3
        }

        /** Whether a text turned [degrees] reads along the wall, either way. */
        fun readsAlong(degrees: Double): Boolean {
            val wall = Math.toDegrees(atan2(uy, ux))
            val off = ((degrees - wall) % 180 + 360) % 180
            return off < 0.5 || off > 179.5
        }
    }

    /** A DXF file read back: its pairs of a group code and a value, then each section's objects. */
    private class DxfRead(bytes: ByteArray) {
        /** One object of a section, from its group code 0 to the next; a section's leading pairs have no [type]. */
        class Obj(val type: String, val pairs: List<Pair<Int, String>>) {
            fun first(code: Int) = pairs.firstOrNull { it.first == code }?.second
            fun all(code: Int) = pairs.filter { it.first == code }.map { it.second }
            fun number(code: Int) = first(code)?.trim()?.toDoubleOrNull()
            fun point(code: Int) = doubleArrayOf(number(code) ?: Double.NaN, number(code + 10) ?: Double.NaN)
            val layer get() = first(8)
            val handle get() = first(if (type == "DIMSTYLE") 105 else 5)
            val vertices get() = all(10).zip(all(20)) { x, y -> doubleArrayOf(x.trim().toDouble(), y.trim().toDouble()) }

            /** The text's string, its `\U+XXXX` escapes read back. */
            val text get() = first(1)?.let { s -> ESCAPE.replace(s) { it.groupValues[1].toInt(16).toChar().toString() } }
        }

        val ascii = bytes.all { it >= 0 }
        private val lines = String(bytes, Charsets.US_ASCII).split("\n").map { it.trimEnd('\r') }
            .let { if (it.lastOrNull() == "") it.dropLast(1) else it }
        val wellFormed = lines.size % 2 == 0 && (lines.indices step 2).all { lines[it].trim().toIntOrNull() != null }
        val pairs = (0 until lines.size / 2).mapNotNull { i -> lines[i * 2].trim().toIntOrNull()?.let { it to lines[i * 2 + 1] } }
        val sections: Map<String, List<Obj>> = LinkedHashMap<String, List<Obj>>().also { sections ->
            var i = 0
            while (i < pairs.size) {
                if (pairs[i] != (0 to "SECTION") || i + 1 >= pairs.size) {
                    i++
                    continue
                }
                val name = pairs[i + 1].second
                val objects = ArrayList<Obj>()
                var type = ""
                var current = ArrayList<Pair<Int, String>>()
                i += 2
                while (i < pairs.size && pairs[i] != (0 to "ENDSEC")) {
                    if (pairs[i].first == 0) {
                        if (type.isNotEmpty() || current.isNotEmpty()) objects += Obj(type, current)
                        type = pairs[i].second
                        current = ArrayList()
                    } else {
                        current += pairs[i]
                    }
                    i++
                }
                if (type.isNotEmpty() || current.isNotEmpty()) objects += Obj(type, current)
                sections[name] = objects
            }
        }

        fun objects(section: String, type: String? = null) =
            sections[section].orEmpty().filter { type == null || it.type == type }

        fun entities(type: String, layer: String? = null) =
            objects("ENTITIES", type).filter { layer == null || it.layer == layer }

        /** The value of the header variable [name]. */
        fun header(name: String): String? {
            val variables = sections["HEADER"]?.firstOrNull { it.type.isEmpty() }?.pairs ?: return null
            val at = variables.indexOf(9 to name)
            return variables.getOrNull(at + 1)?.takeIf { at >= 0 && it.first != 9 }?.second
        }

        private companion object {
            val ESCAPE = Regex("""\\U\+([0-9A-Fa-f]{4})""")
        }
    }

    private companion object {
        val LAYERS = listOf("WALLS", "OPENINGS", "DIMENSIONS", "TEXT", "LEGEND")

        val WORDS = RoomPlanDxf.Words(
            legend = listOf(
                "Living room plan",
                "Indicative plan: verify every figure on site.",
                "Drawing in millimetres, figures in metres.",
            ),
            door = "Door %1\$s",
            window = "Window %1\$s",
            sill = "sill %1\$s",
        )

        val OPENINGS = listOf(
            Opening(OpeningKind.Door, wall = 0, from = 1f, width = 0.9f, height = 2.1f),
            Opening(OpeningKind.Window, wall = 1, from = 2f, width = 1.2f, height = 1.2f, sill = 0.9f),
            Opening(OpeningKind.Door, wall = 2, from = 0.5f, width = 0.8f),
        )
    }
}
