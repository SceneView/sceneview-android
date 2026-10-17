package io.github.sceneview.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** What one request to a provider cost: tokens, time, what the model chose. Never its content. */
data class AiCallRecord(
    val provider: String,
    val model: String,
    val round: Int,
    val inputTokens: Int,
    val outputTokens: Int,
    val latencyMillis: Long,
    val stopReason: String?,
    val toolNames: List<String>,
    val error: String?,
)

/** A vendor's list price, US dollars per million tokens. */
data class AiPricing(val inputPerMillionUsd: Double, val outputPerMillionUsd: Double) {
    fun costUsd(inputTokens: Int, outputTokens: Int): Double =
        inputTokens * inputPerMillionUsd / MILLION + outputTokens * outputPerMillionUsd / MILLION

    private companion object {
        const val MILLION = 1_000_000.0
    }
}

/**
 * The requests of one run, for the numbers a comparison needs — tokens, latency, rounds, tool
 * calls — and a JSON dump for the record.
 *
 * ```kotlin
 * val log = AiUsageLog()
 * val round = conversation.round(provider)
 * log.add(provider.id, model, roundIndex, round)
 * println(log.summary(AiPricing(2.0, 10.0)))
 * ```
 */
class AiUsageLog {
    private val mutable = ArrayList<AiCallRecord>()
    val records: List<AiCallRecord> get() = mutable

    fun add(provider: String, model: String, round: Int, result: AiRound) {
        mutable += AiCallRecord(
            provider = provider,
            model = model,
            round = round,
            inputTokens = result.usage?.inputTokens ?: 0,
            outputTokens = result.usage?.outputTokens ?: 0,
            latencyMillis = result.latencyMillis,
            stopReason = result.stopReason,
            toolNames = result.calls.map { it.name },
            error = result.error,
        )
    }

    val inputTokens: Int get() = mutable.sumOf { it.inputTokens }
    val outputTokens: Int get() = mutable.sumOf { it.outputTokens }
    val latencyMillis: Long get() = mutable.sumOf { it.latencyMillis }
    val toolCalls: Int get() = mutable.sumOf { it.toolNames.size }
    val errors: List<String> get() = mutable.mapNotNull { it.error }

    fun clear() = mutable.clear()

    /** One line: rounds, calls, tokens, time and — with [pricing] — the cost. */
    fun summary(pricing: AiPricing? = null): String = buildString {
        append("${mutable.size} rounds, $toolCalls tool calls, $inputTokens in / $outputTokens out tokens, ")
        append("${latencyMillis} ms")
        if (pricing != null) {
            val cost = pricing.costUsd(inputTokens, outputTokens)
            append(", ~$").append(((cost * CENTS).toLong() / CENTS).toString()).append(" USD")
        }
        if (errors.isNotEmpty()) append(", errors: ").append(errors.joinToString("; "))
    }

    fun json(): JsonArray = buildJsonArray {
        for (r in mutable) addJsonObject {
            put("provider", r.provider)
            put("model", r.model)
            put("round", r.round)
            put("input_tokens", r.inputTokens)
            put("output_tokens", r.outputTokens)
            put("latency_ms", r.latencyMillis)
            put("stop_reason", r.stopReason)
            putJsonArray("tools") { r.toolNames.forEach { add(it) } }
            put("error", r.error)
        }
    }

    private companion object {
        const val CENTS = 10_000.0
    }
}
