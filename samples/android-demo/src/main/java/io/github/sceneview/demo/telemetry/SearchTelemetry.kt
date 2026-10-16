package io.github.sceneview.demo.telemetry

import java.util.Locale

/**
 * What a search field may send as `search_term`: the only typed text this app's analytics
 * carries. **Shared rule for rule with the iOS demo** (`DemoAnalytics.swift`, `SearchTerm`).
 *
 * The term is trimmed, its whitespace collapsed, lowercased and cut to [MAX_LENGTH]
 * characters. Anything shorter than [MIN_LENGTH] is dropped (a single letter is a keystroke,
 * not a search), and anything that looks like an email address, a link or a long number is
 * replaced by [REDACTED] rather than sent.
 */
object SearchTerm {
    const val MIN_LENGTH = 2
    const val MAX_LENGTH = 50
    const val REDACTED = "(redacted)"

    /** How long a field must stay still before its query counts as a search. */
    const val SETTLE_MS = 1_500L

    private val whitespace = Regex("\\s+")
    private val longNumber = Regex("\\d{5,}")

    /** The term to send for [raw], or `null` when nothing should be sent. */
    fun normalize(raw: String): String? {
        val term = raw.trim().replace(whitespace, " ").lowercase(Locale.ROOT)
        if (term.length < MIN_LENGTH) return null
        if (looksPersonal(term)) return REDACTED
        return term.take(MAX_LENGTH).trimEnd()
    }

    /** An email address, a link or a long number (a phone, an order id): never sent as typed. */
    private fun looksPersonal(term: String): Boolean =
        listOf("@", "http", "www.").any { it in term } || longNumber.containsMatchIn(term)

    /** `result_bucket` for [count] results, `error` when the search never answered (`null`). */
    fun resultBucket(count: Int?): String = when {
        count == null -> "error"
        count <= 0 -> "0"
        count == 1 -> "1"
        count <= 5 -> "2-5"
        count <= 20 -> "6-20"
        else -> "21+"
    }
}

/** Which field the search was typed in. */
enum class SearchSurface(val value: String) {
    /** The Home grid's sample filter — local, instant. */
    Samples("samples"),

    /** The Explore tab's online model catalogs. */
    Models("models"),
}

/**
 * Turns settled queries into `search` events, once each: a field that settles twice on the same
 * term (a recomposition, a retry, a result tapped right after the settle fired) counts once.
 * Typing a different term, or the same term in the other field, counts again.
 */
class SearchLogger(private val log: (AnalyticsEvent) -> Unit) {
    private var last: Pair<SearchSurface, String>? = null

    /** Logs [raw] as a search on [surface] that found [resultCount] results (`null`: failed). */
    fun settled(surface: SearchSurface, raw: String, resultCount: Int?): Boolean {
        val term = SearchTerm.normalize(raw) ?: return false
        val key = surface to term
        if (key == last) return false
        last = key
        log(AnalyticsEvent.Search(term, surface, SearchTerm.resultBucket(resultCount)))
        return true
    }

    /** The field on [surface] was emptied: the next search there counts, even if it repeats. */
    fun cleared(surface: SearchSurface) {
        if (last?.first == surface) last = null
    }
}
