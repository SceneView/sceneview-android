package io.github.sceneview.demo.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.ui.platform.LocalDensity
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val density = LocalDensity.current
            val planHeight = Space.x4l * 2 + Space.xl
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { planHeight.toPx() }
            val center = Offset(widthPx / 2, heightPx / 2)
            val stroke = with(density) { SceneViewTokens.Glass.borderWidth.toPx() }
            val wallStroke = with(density) { Space.xs.toPx() }
            val labelGap = with(density) { Space.md.toPx() }
            val tick = with(density) { Space.xs.toPx() }
            val sample = textMeasurer.measure("0.00 m", style)
            val margin = maxOf(labelGap * 2, sample.size.width / 2f + labelGap + tick)
            val scale = minOf(
                (widthPx - margin * 2) / projection.width,
                (heightPx - margin * 2) / projection.depth,
            )
            if (scale <= 0f) {
                // At extreme font scales the measurements leave no room for a drawing.
                Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    model.objects.forEachIndexed { index, obj ->
                        Text("${index + 1} · ${obj.category.replace('_', ' ')}", style = style)
                    }
                }
                return@BoxWithConstraints
            }
            fun screen(point: FloorPoint): Offset = projection.at(point) * scale + center
            fun polygon(points: List<Offset>) = Path().apply {
                points.firstOrNull()?.let { moveTo(it.x, it.y) }
                points.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            fun normal(from: Offset, to: Offset): Offset {
                val delta = to - from
                val length = delta.getDistance()
                if (length <= 0f) return Offset.Zero
                var result = Offset(-delta.y, delta.x) / length
                if (((from + to) / 2f - center).let { it.x * result.x + it.y * result.y } < 0f) {
                    result = -result
                }
                return result
            }
            val footprints = model.objects.map { obj ->
                val footprint = obj.box.footprint()
                (footprint.indices step 2).map { screen(FloorPoint(footprint[it], footprint[it + 1])) }
            }
            val labels = model.objects.map { textMeasurer.measure(it.category.replace('_', ' '), style) }
            val markers = model.objects.indices.map { textMeasurer.measure("${it + 1}", style) }
            val dimensions = model.walls.map { wall ->
                val from = screen(wall.from)
                val to = screen(wall.to)
                val label = textMeasurer.measure("${SceneToolExecutor.fmt(wall.length)} m", style)
                val n = normal(from, to)
                // Horizontal even on rotated walls; reserve its full screen-space rectangle.
                val distance = labelGap + tick +
                    kotlin.math.abs(n.x) * label.size.width / 2f + kotlin.math.abs(n.y) * label.size.height / 2f
                val at = (from + to) / 2f + n * distance
                PlanRect.around(at.planPoint(), label.size.width.toFloat(), label.size.height.toFloat()) to label
            }
            val obstacles = dimensions.map { it.first } + model.walls.map { wall ->
                PlanRect.enclosing(listOf(screen(wall.from).planPoint(), screen(wall.to).planPoint()))
            }
            val placements = remember(footprints, labels, markers, obstacles, widthPx, heightPx, tick) {
                placePlanLabels(
                    labels = labels.mapIndexed { index, label ->
                        PlanLabel(
                            footprints[index].map { it.planPoint() },
                            label.size.width.toFloat(), label.size.height.toFloat(),
                            markers[index].size.width.toFloat(), markers[index].size.height.toFloat(),
                        )
                    },
                    viewport = PlanRect(tick, tick, widthPx - tick, heightPx - tick),
                    obstacles = obstacles,
                    gap = tick,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Canvas(
                    Modifier.fillMaxWidth().height(planHeight)
                        .semantics { contentDescription = description },
                ) {
                    if (plan?.closed == true) {
                        drawPath(polygon(plan.corners.map(::screen)), chrome.track)
                    }
                    footprints.forEach { points ->
                        val path = polygon(points)
                        drawPath(path, chrome.track)
                        drawPath(path, chrome.onCardMuted, style = Stroke(stroke))
                    }
                    model.walls.forEachIndexed { index, wall ->
                        val from = screen(wall.from)
                        val to = screen(wall.to)
                        drawLine(chrome.onCard, from, to, wallStroke)
                        val normal = normal(from, to)
                        val a = from + normal * labelGap
                        val b = to + normal * labelGap
                        drawLine(chrome.onCardMuted, a, b, stroke)
                        drawLine(chrome.onCardMuted, a - normal * tick, a + normal * tick, stroke)
                        drawLine(chrome.onCardMuted, b - normal * tick, b + normal * tick, stroke)
                        val (bounds, label) = dimensions[index]
                        drawText(label, topLeft = Offset(bounds.left, bounds.top))
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
                    placements.forEachIndexed { index, placement ->
                        val bounds = placement.bounds ?: return@forEachIndexed
                        if (placement.leader) {
                            val origin = PlanRect.enclosing(footprints[index].map { it.planPoint() }).center
                            val end = Offset(
                                origin.x.coerceIn(bounds.left, bounds.right),
                                origin.y.coerceIn(bounds.top, bounds.bottom),
                            )
                            drawLine(chrome.onCardMuted, Offset(origin.x, origin.y), end, stroke)
                        }
                    }
                    // Draw labels last on an opaque themed ground: leader lines cannot cross the text.
                    placements.forEachIndexed { index, placement ->
                        val bounds = placement.bounds ?: return@forEachIndexed
                        val label = if (placement.number == null) labels[index] else markers[index]
                        val topLeft = Offset(bounds.left, bounds.top)
                        drawRect(chrome.ground, topLeft, Size(label.size.width.toFloat(), label.size.height.toFloat()))
                        drawText(label, topLeft = topLeft)
                    }
                }
                placements.forEachIndexed { index, placement ->
                    if (placement.number != null) {
                        Text("${placement.number} · ${model.objects[index].category.replace('_', ' ')}", style = style)
                    }
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

private fun Offset.planPoint() = PlanPoint(x, y)

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
