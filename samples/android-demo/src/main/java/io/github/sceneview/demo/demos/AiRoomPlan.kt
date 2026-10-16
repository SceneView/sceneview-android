package io.github.sceneview.demo.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import io.github.sceneview.demo.ai.scene.FloorPoint
import io.github.sceneview.demo.ai.scene.OpeningKind
import io.github.sceneview.demo.ai.scene.RoomModel
import io.github.sceneview.demo.ai.scene.RoomWall
import io.github.sceneview.demo.ai.scene.SceneToolExecutor
import io.github.sceneview.demo.theme.LocalStageChrome
import io.github.sceneview.demo.theme.SceneViewTokens
import io.github.sceneview.demo.theme.SceneViewTokens.Space
import kotlin.math.atan2

/** A measured plan, in the scan's scale: the longest wall sets the horizontal axis. */
@Composable
internal fun AiRoomPlan(model: RoomModel) {
    val longest = model.walls.maxByOrNull { it.length } ?: return
    if (longest.length <= 0f) return
    val chrome = LocalStageChrome.current
    val textMeasurer = rememberTextMeasurer()
    val style = SceneViewTokens.Type.caption.copy(color = chrome.onCard, fontFeatureSettings = "tnum")
    val projection = remember(model.walls, model.objects) { PlanProjection(longest, model) }
    val plan = model.plan
    val area = plan?.takeIf { it.closed }?.area?.let { "${SceneToolExecutor.fmt(it)} m²" } ?: "—"
    val perimeterM = plan?.perimeter ?: model.walls.sumOf { it.length.toDouble() }.toFloat()
    val perimeter = "${SceneToolExecutor.fmt(perimeterM)} m"
    val ceiling = plan?.ceilingHeight?.let { "${SceneToolExecutor.fmt(it)} m" } ?: "—"
    val description = buildString {
        append("Measured floor plan. Area $area. Perimeter $perimeter. Ceiling $ceiling. ")
        model.walls.forEach { append("${it.id}: ${SceneToolExecutor.fmt(it.length)} metres. ") }
        model.openings.forEach { append("${it.kind.wire} on ${it.wallId}. ") }
        model.objects.forEach { append("${it.category.replace('_', ' ')}. ") }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        Text(
            text = if (plan?.closed == true) "Measured floor plan" else "Measured walls · open outline",
            style = style,
        )
        Canvas(
            Modifier.fillMaxWidth().height(Space.x4l * 2 + Space.xl)
                .semantics { contentDescription = description },
        ) {
            // Leave a full label and its dimension ticks outside each edge of the room.
            val margin = maxOf(Space.xl.toPx(), textMeasurer.measure("0.00 m", style).size.height * 2f)
            val scale = minOf(
                (size.width - margin * 2) / projection.width,
                (size.height - margin * 2) / projection.depth,
            )
            if (scale <= 0f) return@Canvas
            fun screen(point: FloorPoint): Offset = projection.at(point) * scale + center
            fun polygon(points: List<Offset>) = Path().apply {
                points.firstOrNull()?.let { moveTo(it.x, it.y) }
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            val stroke = SceneViewTokens.Glass.borderWidth.toPx()
            val wallStroke = Space.xs.toPx()
            val labelGap = Space.md.toPx()
            val tick = Space.xs.toPx()
            if (plan?.closed == true) {
                drawPath(polygon(plan.corners.map(::screen)), chrome.track)
            }
            model.objects.forEach { obj ->
                val footprint = obj.box.footprint()
                val points = (footprint.indices step 2).map { screen(FloorPoint(footprint[it], footprint[it + 1])) }
                val path = polygon(points)
                drawPath(path, chrome.track)
                drawPath(path, chrome.onCardMuted, style = Stroke(stroke))
                val label = textMeasurer.measure(obj.category.replace('_', ' '), style)
                val at = screen(FloorPoint(obj.box.center.x, obj.box.center.z))
                val edge = if (obj.box.size.x >= obj.box.size.z) points[1] - points[0] else points[3] - points[0]
                rotate(readableAngle(edge), at) {
                    drawText(label, topLeft = at - Offset(label.size.width / 2f, label.size.height / 2f))
                }
            }
            model.walls.forEach { wall ->
                val from = screen(wall.from)
                val to = screen(wall.to)
                drawLine(chrome.onCard, from, to, wallStroke)
                val delta = to - from
                val length = delta.getDistance()
                if (length <= 0f) return@forEach
                val middle = (from + to) / 2f
                var normal = Offset(-delta.y, delta.x) / length
                if ((middle - center).let { it.x * normal.x + it.y * normal.y } < 0f) normal = -normal
                val a = from + normal * labelGap
                val b = to + normal * labelGap
                drawLine(chrome.onCardMuted, a, b, stroke)
                drawLine(chrome.onCardMuted, a - normal * tick, a + normal * tick, stroke)
                drawLine(chrome.onCardMuted, b - normal * tick, b + normal * tick, stroke)
                val label = textMeasurer.measure("${SceneToolExecutor.fmt(wall.length)} m", style)
                val at = middle + normal * (labelGap + label.size.height / 2f + tick)
                rotate(readableAngle(delta), at) {
                    drawText(label, topLeft = at - Offset(label.size.width / 2f, label.size.height / 2f))
                }
            }
            model.openings.forEach { opening ->
                val wall = model.wall(opening.wallId) ?: return@forEach
                val from = screen(wall.at(opening.from))
                val to = screen(wall.at(opening.to))
                // Erase the solid wall, then distinguish glazing from an inward door leaf.
                drawLine(chrome.ground, from, to, wallStroke + stroke)
                if (opening.kind == OpeningKind.Window) {
                    drawLine(chrome.accent, from, to, stroke * 2)
                } else {
                    val delta = to - from
                    var leaf = Offset(-delta.y, delta.x)
                    if ((center - from).let { it.x * leaf.x + it.y * leaf.y } < 0f) leaf = -leaf
                    drawLine(chrome.accent, from, from + leaf, stroke)
                    val radius = delta.getDistance()
                    val angle = Math.toDegrees(atan2(delta.y, delta.x).toDouble()).toFloat()
                    val sweep = if (delta.x * leaf.y - delta.y * leaf.x > 0f) 90f else -90f
                    drawArc(
                        chrome.accent, angle, sweep, useCenter = false,
                        topLeft = from - Offset(radius, radius), size = Size(radius * 2, radius * 2),
                        style = Stroke(stroke),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            listOf("Area" to area, "Perimeter" to perimeter, "Ceiling" to ceiling).forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(label, style = style.copy(color = chrome.onCardMuted))
                    Text(
                        value,
                        style = SceneViewTokens.Type.card.copy(color = chrome.onCard, fontFeatureSettings = "tnum"),
                    )
                }
            }
        }
    }
}

private fun readableAngle(delta: Offset): Float {
    val angle = Math.toDegrees(atan2(delta.y, delta.x).toDouble()).toFloat()
    return when {
        angle > 90f -> angle - 180f
        angle < -90f -> angle + 180f
        else -> angle
    }
}

/** Rotation and bounds only; measurements always remain those of [RoomModel]. */
private class PlanProjection(longest: RoomWall, model: RoomModel) {
    private val ux = (longest.to.x - longest.from.x) / longest.length
    private val uz = (longest.to.z - longest.from.z) / longest.length
    private fun rotated(p: FloorPoint) = Offset(p.x * ux + p.z * uz, -p.x * uz + p.z * ux)
    private val ends = model.walls.flatMap { listOf(rotated(it.from), rotated(it.to)) } +
        model.objects.flatMap { obj ->
            val footprint = obj.box.footprint()
            (footprint.indices step 2).map { rotated(FloorPoint(footprint[it], footprint[it + 1])) }
        }
    private val minX = ends.minOf { it.x }
    private val maxX = ends.maxOf { it.x }
    private val minY = ends.minOf { it.y }
    private val maxY = ends.maxOf { it.y }
    val width = (maxX - minX).coerceAtLeast(MIN_EXTENT_M)
    val depth = (maxY - minY).coerceAtLeast(MIN_EXTENT_M)
    fun at(point: FloorPoint): Offset = rotated(point) - Offset((minX + maxX) / 2f, (minY + maxY) / 2f)

    private companion object {
        const val MIN_EXTENT_M = 0.1f
    }
}
