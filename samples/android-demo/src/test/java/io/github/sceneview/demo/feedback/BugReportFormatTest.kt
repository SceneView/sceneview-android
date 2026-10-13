package io.github.sceneview.demo.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder

/**
 * Pure-JVM coverage of the lightweight bug-report formatting: the share text,
 * the GitHub-issue markdown, and the pre-filled `issues/new` URL (including
 * its length cap). No Robolectric needed — the formatting layer takes plain
 * data on purpose.
 */
class BugReportFormatTest {

    private val metadata = linkedMapOf(
        "App version" to "4.19.0 (350)",
        "Screen" to "Demo · model-viewer",
        "Demo" to "model-viewer",
        "Device" to "Google Pixel 7a",
        "Android" to "15 (API 35)",
    )

    private fun info(
        logcat: List<String> = listOf("06-01 12:00:00.000 I/Demo: started"),
    ) = BugReportInfo(metadata = metadata, logcat = logcat)

    // ── Title ────────────────────────────────────────────────────────────────

    @Test
    fun `title uses the note's first line when present`() {
        val title = formatReportTitle(info(), "Model turns black\nafter rotating")
        assertEquals("[Demo app] Model turns black", title)
    }

    @Test
    fun `title falls back to the demo id then to a generic title`() {
        assertEquals("[Demo app] Bug report — model-viewer", formatReportTitle(info(), " "))
        val noDemo = BugReportInfo(metadata = linkedMapOf("Device" to "x"), logcat = emptyList())
        assertEquals("[Demo app] Bug report", formatReportTitle(noDemo, ""))
    }

    @Test
    fun `title caps a very long first line`() {
        val title = formatReportTitle(info(), "x".repeat(300))
        assertTrue(title.length <= "[Demo app] ".length + 80)
    }

    // ── Share text ───────────────────────────────────────────────────────────

    @Test
    fun `share text carries note, metadata and logcat`() {
        val text = formatShareText(info(), "It crashed")
        assertTrue(text.contains("It crashed"))
        assertTrue(text.contains("App version: 4.19.0 (350)"))
        assertTrue(text.contains("Demo: model-viewer"))
        assertTrue(text.contains("I/Demo: started"))
    }

    @Test
    fun `share text omits the log section when logcat is empty`() {
        val text = formatShareText(info(logcat = emptyList()), "note")
        assertFalse(text.contains("Recent app log"))
    }

    @Test
    fun `share text carries the whole captured window, not the issue cap`() {
        // The share path has no URL budget: it is the one that must survive a
        // repeating warning long enough to measure its period (#3390).
        val lines = (1..LOGCAT_TAIL_LINES).map { "line $it" }
        val text = formatShareText(info(logcat = lines), "")
        assertTrue(text.contains("last $LOGCAT_TAIL_LINES lines"))
        assertTrue(text.contains("line 1\n"))
        assertTrue(text.contains("line $LOGCAT_TAIL_LINES"))
    }

    @Test
    fun `share text keeps the newest lines when the capture overflows`() {
        val lines = (1..SHARE_LOGCAT_MAX_LINES + 50).map { "line $it" }
        val text = formatShareText(info(logcat = lines), "")
        assertTrue(text.contains("last $SHARE_LOGCAT_MAX_LINES lines"))
        assertFalse(text.contains("line 1\n"))                        // oldest dropped
        assertTrue(text.contains("line ${SHARE_LOGCAT_MAX_LINES + 50}")) // newest kept
    }

    // ── Issue body ───────────────────────────────────────────────────────────

    @Test
    fun `issue body is markdown with a context table and a details log block`() {
        val body = formatIssueBody(info(), "Steps: open the demo")
        assertTrue(body.startsWith("Steps: open the demo"))
        assertTrue(body.contains("| App version | 4.19.0 (350) |"))
        assertTrue(body.contains("<details><summary>App log (last 1 lines)</summary>"))
        assertTrue(body.contains("```"))
        assertTrue(body.contains("</details>"))
    }

    @Test
    fun `issue body escapes pipes in metadata and fences in log lines`() {
        val tricky = BugReportInfo(
            metadata = linkedMapOf("Device" to "a|b"),
            logcat = listOf("evil ``` fence"),
        )
        val body = formatIssueBody(tricky, "")
        assertTrue(body.contains("| Device | a\\|b |"))
        assertFalse(body.contains("evil ``` fence"))
        assertTrue(body.contains("evil ''' fence"))
    }

    @Test
    fun `issue body notes a missing description and omits an empty log`() {
        val body = formatIssueBody(info(logcat = emptyList()), "  ")
        assertTrue(body.contains("_No description provided._"))
        assertFalse(body.contains("<details>"))
    }

    // ── GitHub URL ───────────────────────────────────────────────────────────

    @Test
    fun `issue url points at issues-new with an encoded title and body`() {
        val url = buildGitHubIssueUrl(info(), "Model turns black")
        assertTrue(url.startsWith("$GITHUB_NEW_ISSUE_URL?title="))
        val decodedBody = URLDecoder.decode(url.substringAfter("&body="), "UTF-8")
        assertTrue(decodedBody.contains("| Demo | model-viewer |"))
        assertTrue(decodedBody.contains("Model turns black"))
    }

    @Test
    fun `issue url stays under the length cap even with a huge logcat`() {
        val lines = (1..400).map { "06-01 12:00:00.000 W/Filament: " + "x".repeat(180) }
        val url = buildGitHubIssueUrl(info(logcat = lines), "note")
        assertTrue("url length ${url.length}", url.length <= ISSUE_URL_MAX_LENGTH)
    }

    @Test
    fun `issue url packs a usable log window, not a couple of dozen lines`() {
        // Regression guard for #3390: the coarse fallback ladder used to snap to
        // 30 lines — too short to see a repeating warning twice and measure its
        // period. Realistic threadtime lines, so the budget arithmetic is real.
        val lines = (1..ISSUE_LOGCAT_MAX_LINES).map {
            val millis = (it % 1000).toString().padStart(3, '0')
            "06-01 12:00:00.$millis  6543  6560 W Filament: throttled draw call skipped"
        }
        val url = buildGitHubIssueUrl(info(logcat = lines), "Repeating warning in the AR view")
        assertTrue("url length ${url.length}", url.length <= ISSUE_URL_MAX_LENGTH)

        val body = URLDecoder.decode(url.substringAfter("&body="), "UTF-8")
        val packed = Regex("""App log \(last (\d+) lines\)""").find(body)!!.groupValues[1].toInt()
        assertTrue("packed only $packed lines", packed >= 60)
        assertTrue(body.contains("| Screen | Demo · model-viewer |"))
    }

    // ── Log compaction ───────────────────────────────────────────────────────

    @Test
    fun `compaction drops the date and pid columns but keeps the milliseconds`() {
        // Milliseconds are what makes a repeating warning measurable; the date
        // and the pid/tid pair are constant across the whole capture.
        assertEquals(
            "12:00:00.123 W Filament: msg",
            compactLogLine("06-01 12:00:00.123  6543  6560 W Filament: msg"),
        )
        // Other shapes only lose their leading date.
        assertEquals("12:00:00.123 I/Demo: msg", compactLogLine("06-01 12:00:00.123 I/Demo: msg"))
        assertEquals("a free-form line", compactLogLine("a free-form line"))
    }

    @Test
    fun `issue url survives a pathological note`() {
        val url = buildGitHubIssueUrl(info(logcat = emptyList()), "y".repeat(50_000))
        assertTrue("url length ${url.length}", url.length <= ISSUE_URL_MAX_LENGTH)
    }

    // ── AR tracking lines (#4510) ────────────────────────────────────────────

    /** [count] lines as `ARSceneView` logs them, numbered through their `for=` field. */
    private fun trackingLines(count: Int): List<String> = (1..count).map {
        "10-13 16:55:46.108 29383 29383 I SceneView: AR tracking: state=PAUSED reason=NONE for=${it}0s"
    }

    private val appLine = "10-13 16:56:00.000 29383 29716 I native  : VisualInertialState is kNotTracking."

    @Test
    fun `only AR tracking lines are kept from the SceneView tag, the newest sixty`() {
        val read = listOf(
            "10-13 16:55:40.000 29383 29383 E SceneView: rememberSplatCloud: cannot load " +
                "content://com.android.providers.downloads.documents/document/42",
            "10-13 16:55:40.001 29383 29383 E SceneView: java.io.FileNotFoundException: /sdcard/a.spz",
        ) + trackingLines(AR_TRACKING_MAX_LINES + 5)

        val kept = arTrackingLines(read)

        assertEquals(AR_TRACKING_MAX_LINES, kept.size)
        assertTrue(kept.all { AR_TRACKING_LOG_PREFIX in it })
        assertTrue(kept.first().endsWith("for=60s"))   // the five oldest are dropped
        assertTrue(kept.last().endsWith("for=650s"))
    }

    @Test
    fun `share text puts the AR tracking lines ahead of the general log`() {
        val text = formatShareText(
            BugReportInfo(metadata, logcat = listOf(appLine), arTracking = trackingLines(3)),
            "Stuck on Getting ready",
        )

        val tracking = text.indexOf("AR tracking (last 3 lines):")
        val general = text.indexOf("Recent app log (last 1 lines):")
        assertTrue(tracking > text.indexOf("Android: 15 (API 35)"))
        assertTrue("tracking block at $tracking, general log at $general", tracking in 0 until general)
        assertTrue(text.indexOf("for=30s") < general)
    }

    @Test
    fun `share text carries no more than sixty AR tracking lines, the newest`() {
        val info = BugReportInfo(metadata, logcat = emptyList(), arTracking = trackingLines(75))

        val text = formatShareText(info, "")

        assertTrue(text.contains("AR tracking (last $AR_TRACKING_MAX_LINES lines):"))
        assertFalse(text.contains("for=150s"))
        assertTrue(text.contains("for=160s"))
        assertTrue(text.contains("for=750s"))
    }

    @Test
    fun `a report without AR tracking lines reads as it did`() {
        val info = info()

        assertFalse(formatShareText(info, "note").contains("AR tracking"))
        assertFalse(formatIssueBody(info, "note").contains("AR tracking"))
    }

    @Test
    fun `issue body puts the AR tracking block ahead of the app log`() {
        val body = formatIssueBody(
            BugReportInfo(metadata, logcat = listOf(appLine), arTracking = trackingLines(2)),
            "Stuck on Getting ready",
        )

        val tracking = body.indexOf("<details><summary>AR tracking (last 2 lines)</summary>")
        val general = body.indexOf("<details><summary>App log (last 1 lines)</summary>")
        assertTrue("tracking block at $tracking, app log at $general", tracking in 0 until general)
        assertTrue(tracking > body.indexOf("### Context"))
    }

    @Test
    fun `issue body takes the AR tracking lines out of the log ceiling instead of adding to it`() {
        val info = BugReportInfo(
            metadata,
            logcat = (1..LOGCAT_TAIL_LINES).map { "line $it" },
            arTracking = trackingLines(AR_TRACKING_MAX_LINES),
        )

        val body = formatIssueBody(info, "")

        val general = ISSUE_LOGCAT_MAX_LINES - ISSUE_AR_TRACKING_MAX_LINES
        assertTrue(body.contains("AR tracking (last $ISSUE_AR_TRACKING_MAX_LINES lines)"))
        assertTrue(body.contains("App log (last $general lines)"))
        // The newest of each, and nothing older than the ceiling allows.
        assertTrue(body.contains("for=600s"))
        assertFalse(body.contains("for=480s"))
        assertTrue(body.contains("line ${LOGCAT_TAIL_LINES - general + 1}\n"))
        assertFalse(body.contains("line ${LOGCAT_TAIL_LINES - general}\n"))
    }

    @Test
    fun `issue url keeps the AR tracking lines, compacted, under the length cap`() {
        val general = (1..ISSUE_LOGCAT_MAX_LINES).map {
            "10-13 16:56:00.000 29383 29716 I native  : " + "x".repeat(120)
        }
        val info = BugReportInfo(metadata, logcat = general, arTracking = trackingLines(40))

        val url = buildGitHubIssueUrl(info, "Stuck on Getting ready")

        assertTrue("url length ${url.length}", url.length <= ISSUE_URL_MAX_LENGTH)
        val body = URLDecoder.decode(url.substringAfter("&body="), "UTF-8")
        assertTrue(body.contains("AR tracking (last $ISSUE_AR_TRACKING_MAX_LINES lines)"))
        assertTrue(body.contains("16:55:46.108 I SceneView: AR tracking: state=PAUSED reason=NONE for=400s"))
        assertFalse(body.contains("29383"))
        val packed = Regex("""App log \(last (\d+) lines\)""").find(body)!!.groupValues[1].toInt()
        assertTrue("packed only $packed general lines", packed >= 10)
    }

    @Test
    fun `issue url drops every log block for a pathological note`() {
        val info = BugReportInfo(metadata, logcat = listOf(appLine), arTracking = trackingLines(12))

        val url = buildGitHubIssueUrl(info, "y".repeat(50_000))

        assertTrue("url length ${url.length}", url.length <= ISSUE_URL_MAX_LENGTH)
        assertFalse(URLDecoder.decode(url.substringAfter("&body="), "UTF-8").contains("<details>"))
    }
}
