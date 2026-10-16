package io.github.sceneview.demo.ai.scene

import io.github.sceneview.ai.AiEndpoint
import io.github.sceneview.ai.AiPricing
import io.github.sceneview.ai.AiProvider
import io.github.sceneview.ai.AiUsageLog
import io.github.sceneview.ai.claude.ClaudeAiProvider
import io.github.sceneview.ai.gemini.GeminiAiProvider
import io.github.sceneview.demo.ai.scene.SceneToolExecutor.Companion.r2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.time.TimeSource

/**
 * The live loop, end to end, on the synthetic room: a real provider chooses, the code measures,
 * and the result is scored against the ground truth of
 * [io.github.sceneview.demo.demos.internal.RerunSyntheticRoom] — 4.0 × 3.6 m, 2.6 m high, four
 * pieces of furniture, no opening.
 *
 * Skipped unless the provider's key is in the environment (`ANTHROPIC_API_KEY`, `GEMINI_API_KEY`),
 * so CI never calls a vendor. The key is read by the provider when it builds a request and goes
 * nowhere else. Every run writes a report — tokens and latency per round, tool calls, the room as
 * measured, the score — to `build/ai-live/` (or `SCENEVIEW_AI_LOG_DIR`); `SCENEVIEW_AI_RUNS`
 * repeats the run to see how stable a provider is.
 */
class LiveReconstructionTest {

    @Test
    fun `claude rebuilds the synthetic room`() = live("ANTHROPIC_API_KEY", SONNET_5_5_PRICING) { key ->
        ClaudeAiProvider(AiEndpoint.Vendor(key))
    }

    @Test
    fun `gemini rebuilds the synthetic room`() = live("GEMINI_API_KEY", pricing = null) { key ->
        GeminiAiProvider(AiEndpoint.Vendor(key))
    }

    private fun live(envName: String, pricing: AiPricing?, provider: (() -> String?) -> AiProvider) {
        assumeTrue("$envName is not set: live run skipped", !System.getenv(envName).isNullOrBlank())
        val live = provider { System.getenv(envName) }
        val runs = System.getenv("SCENEVIEW_AI_RUNS")?.toIntOrNull()?.coerceIn(1, MAX_RUNS) ?: 1
        val outcomes = (1..runs).map { run(live, pricing, it) }
        val failed = outcomes.filter { it.failures.isNotEmpty() }
        assertTrue(
            failed.joinToString("\n") { "${it.file}: ${it.failures.joinToString("; ")}" },
            failed.isEmpty(),
        )
    }

    private class Outcome(val file: File, val failures: List<String>)

    private fun run(provider: AiProvider, pricing: AiPricing?, index: Int): Outcome = runBlocking {
        val executor = SceneToolExecutor(SyntheticScanSession.session)
        val agent = SceneAgent(executor, computeContext = Dispatchers.Default, applyContext = Dispatchers.Unconfined)
        val startedAt = LocalDateTime.now()
        val mark = TimeSource.Monotonic.markNow()
        agent.ask(provider, RecordedAiProvider.REBUILD_PROMPT)
        val wallMillis = mark.elapsedNow().inWholeMilliseconds
        val state = agent.state.value
        val score = Score(state.model, state.lines)
        val failures = buildList {
            agent.usage.errors.forEach { add("provider error: $it") }
            if (!score.closed) add("the room is not a closed polygon")
            if (score.wallCount != TRUE_WALLS) add("${score.wallCount} walls, expected $TRUE_WALLS")
            if (!score.areaOk) add("area ${score.area} m², expected $TRUE_AREA_M2 within ${TOLERANCE * 100}%")
        }
        val report = buildJsonObject {
            put("provider", provider.id)
            put("model", provider.model)
            put("prompt", RecordedAiProvider.REBUILD_PROMPT)
            put("started_at", startedAt.toString())
            put("run", index)
            put("wall_ms", wallMillis)
            put("usage", usage(agent.usage, pricing))
            put("room", room(state.model))
            put("score", score.json())
            putJsonArray("transcript") {
                state.lines.forEach { addJsonObject { put("kind", it.kind.name); put("text", it.text) } }
            }
            putJsonArray("failures") { failures.forEach { add(it) } }
        }
        val file = write(provider, index, startedAt, report)
        println("ai-live ${provider.id}: ${agent.usage.summary(pricing)}; wall $wallMillis ms; ${score.line()}")
        println("ai-live report: $file")
        Outcome(file, failures)
    }

    private fun usage(log: AiUsageLog, pricing: AiPricing?): JsonObject = buildJsonObject {
        put("rounds", log.records.size)
        put("tool_calls", log.toolCalls)
        put("input_tokens", log.inputTokens)
        put("output_tokens", log.outputTokens)
        put("latency_ms", log.latencyMillis)
        pricing?.let {
            val cost = it.costUsd(log.inputTokens, log.outputTokens)
            put("cost_usd", (cost * TEN_THOUSANDTHS).roundToLong() / TEN_THOUSANDTHS)
        }
        put("summary", log.summary(pricing))
        put("calls", log.json())
    }

    private fun write(provider: AiProvider, index: Int, startedAt: LocalDateTime, report: JsonObject): File {
        val dir = File(System.getenv("SCENEVIEW_AI_LOG_DIR") ?: "build/ai-live").apply { mkdirs() }
        val stamp = startedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        val file = File(dir, "${provider.id}-${provider.model}-$stamp-run$index.json")
        file.writeText(report.toString())
        return file
    }

    private fun room(model: RoomModel): JsonObject = buildJsonObject {
        putJsonArray("walls") {
            model.walls.forEach { w ->
                addJsonObject {
                    put("id", w.id)
                    put("length_m", r2(w.length))
                    put("height_m", r2(w.height))
                    put("evidence", w.evidence)
                }
            }
        }
        model.plan?.let { plan ->
            putJsonObject("plan") {
                put("closed", plan.closed)
                plan.area?.let { put("area_m2", r2(it)) }
                put("perimeter_m", r2(plan.perimeter))
                plan.ceilingHeight?.let { put("ceiling_height_m", r2(it)) }
            }
        }
        putJsonArray("openings") {
            model.openings.forEach { o ->
                addJsonObject {
                    put("id", o.id)
                    put("wall_id", o.wallId)
                    put("kind", o.kind.wire)
                    put("width_m", r2(o.width))
                }
            }
        }
        putJsonArray("objects") {
            model.objects.forEach { o ->
                addJsonObject {
                    put("id", o.id)
                    put("category", o.category)
                    putJsonArray("center") { add(r2(o.box.center.x)); add(r2(o.box.center.z)) }
                    putJsonArray("size") { add(r2(o.box.size.x)); add(r2(o.box.size.y)); add(r2(o.box.size.z)) }
                    put("evidence", o.evidence)
                }
            }
        }
        putJsonArray("rejections") { model.rejections.forEach { add("${it.what}: ${it.reason}") } }
    }

    /** The room against the truth: what the code measured once the model had chosen. */
    private class Score(model: RoomModel, lines: List<ChatLine>) {
        private val plan = model.plan
        val closed = plan?.closed == true
        val wallCount = model.walls.size
        private val lengths = model.walls.map { it.length }.sorted()
        val lengthsOk = wallCount == TRUE_WALLS &&
            lengths.zip(TRUE_LENGTHS_M.sorted()).all { (got, truth) -> within(got, truth) }
        val area = plan?.area
        val areaOk = area?.let { within(it, TRUE_AREA_M2) } == true
        val perimeterOk = plan?.let { within(it.perimeter, TRUE_PERIMETER_M) } == true
        val ceilingOk = plan?.ceilingHeight?.let { within(it, TRUE_CEILING_M) } == true
        val openings = model.openings.size

        /** For each true piece, the category of the object standing where it stands, or `null`. */
        val labels: Map<String, String?> = TRUE_OBJECTS.associate { truth ->
            val found = model.objects.firstOrNull { near(it, truth) }
            truth.category to found?.category
        }
        val objectsFound = labels.values.count { it != null }
        val objectsLabelled = labels.count { (truth, got) -> got == truth }
        val refused = lines.count { it.kind == ChatLine.Kind.ToolRefused }

        fun json(): JsonObject = buildJsonObject {
            put("closed", closed)
            put("walls", wallCount)
            put("wall_lengths_within_3pc", lengthsOk)
            put("area_m2", area?.let { r2(it) })
            put("area_within_3pc", areaOk)
            put("perimeter_within_3pc", perimeterOk)
            put("ceiling_within_3pc", ceilingOk)
            put("openings", openings)
            put("objects_found_of_4", objectsFound)
            put("objects_labelled_of_4", objectsLabelled)
            putJsonObject("labels") { labels.forEach { (truth, got) -> put(truth, got) } }
            put("tools_refused", refused)
        }

        fun line() = "closed=$closed walls=$wallCount area=$area found=$objectsFound/4 labelled=$objectsLabelled/4"

        private fun near(o: RoomObject, truth: Truth) =
            hypot(o.box.center.x - truth.x, o.box.center.z - truth.z) <= NEAR_M

        private fun within(got: Float, truth: Float) = abs(got - truth) <= truth * TOLERANCE
    }

    private class Truth(val category: String, val x: Float, val z: Float)

    private companion object {
        /** Sonnet 5.5 list price, US dollars per million tokens. Gemini is reported in tokens only. */
        val SONNET_5_5_PRICING = AiPricing(2.0, 10.0)
        const val TEN_THOUSANDTHS = 10_000.0
        const val MAX_RUNS = 5
        const val TOLERANCE = 0.03f
        const val TRUE_WALLS = 4
        const val TRUE_AREA_M2 = 14.4f
        const val TRUE_PERIMETER_M = 15.2f
        const val TRUE_CEILING_M = 2.6f
        val TRUE_LENGTHS_M = listOf(3.6f, 3.6f, 4f, 4f)

        /** An object counts as found when a box centre lies this close to the piece's true centre. */
        const val NEAR_M = 0.3f
        val TRUE_OBJECTS = listOf(
            Truth("sofa", -1f, -1.375f),
            Truth("table", 0.8f, 0.05f),
            Truth("cabinet", 1.775f, 1.1f),
            Truth("other", -1.1f, 1f),
        )
    }
}
