package io.github.sceneview.demo.demos

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.test.core.app.ApplicationProvider
import io.github.sceneview.demo.R
import io.github.sceneview.demo.demos.internal.RoomSurvey
import io.github.sceneview.demo.demos.internal.SurveyGap
import io.github.sceneview.demo.demos.internal.planSurvey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter
import java.io.File
import java.io.OutputStream
import java.lang.reflect.Modifier

/**
 * The floor plan's file shares, each tapped from the plan's share menu as a user would. "Share
 * PDF" hands over the PDF alone, typed `application/pdf`, so every app that takes a PDF is
 * offered and none is left out. "Share DXF (CAD)", right after it in the same menu, hands over
 * the DXF drawing alone, under its own type; a survey with no plan has no DXF to offer. Each kind
 * keeps its latest file, so the PDF stays readable to the app it went to while the DXF is shared
 * right after. Robolectric has no PDF backend: [PlanPdfStandIn] stands in for the platform's
 * writer, so the PDF's tap runs to the share sheet too.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xhdpi", shadows = [PlanPdfStandIn::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RerunPlanShareTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    /**
     * FileProvider keeps each authority's roots in its static `sCache` map, resolved once against
     * the cache folder of the first test that shared a file. Each test has a cache folder of its
     * own, so the map is emptied before each one, as a fresh process would have it. Should androidx
     * rename or drop that field, this fails here rather than let the shares flake again.
     */
    @Before
    fun forgetFileProviderRoots() {
        val cache = FileProvider::class.java.declaredFields.singleOrNull {
            it.name == "sCache" && Modifier.isStatic(it.modifiers) && Map::class.java.isAssignableFrom(it.type)
        }
        assertNotNull("FileProvider has no static sCache map any more: find where it keeps its roots", cache)
        cache!!.isAccessible = true
        (cache.get(null) as MutableMap<*, *>).clear()
    }

    /** 4 × 3 m with its top-right corner cut at 45°, 2.50 m high: a room with a plan to draw. */
    private val traced = planSurvey(0f, 0f, 3f, 0f, 4f, 1f, 4f, 3f, 0f, 3f)

    private fun showPlan() {
        composeRule.setContent { RerunPlanView(survey = traced, title = TITLE, date = null) }
        composeRule.waitForIdle()
    }

    /** Taps [button] and returns the share sheet it opened. */
    private fun shareSheetOf(button: SemanticsNodeInteraction): Intent {
        val started = shadowOf(app)
        // Whatever started before the tap is not the share.
        while (started.peekNextStartedActivity() != null) started.nextStartedActivity
        button.performScrollTo().performClick()
        composeRule.waitUntil(SHARE_TIMEOUT_MS) { started.peekNextStartedActivity() != null }
        return started.nextStartedActivity
    }

    /** What [sheet] offers to the apps it lists. */
    private fun sentBy(sheet: Intent): Intent {
        assertEquals(Intent.ACTION_CHOOSER, sheet.action)
        val send = IntentCompat.getParcelableExtra(sheet, Intent.EXTRA_INTENT, Intent::class.java)
        assertNotNull("the share sheet offers nothing", send)
        return send!!
    }

    /** The file [send] carries. */
    private fun streamOf(send: Intent): Uri {
        val stream = IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java)
        assertNotNull("the share carries no file", stream)
        return stream!!
    }

    /** Opens [uri] as the app it was shared with would, through the app's FileProvider. */
    private fun readShared(uri: Uri): Result<String> {
        Robolectric.setupContentProvider(FileProvider::class.java, "${app.packageName}.fileprovider")
        return runCatching { app.contentResolver.openInputStream(uri)!!.use { String(it.readBytes(), Charsets.ISO_8859_1) } }
    }

    /** [uri] still opens, and reads as a file that starts with [start] and holds [holds]. */
    private fun assertStillReadable(uri: Uri, start: String, holds: String = start) {
        val read = readShared(uri)
        assertTrue("$uri no longer opens: ${read.exceptionOrNull()}", read.isSuccess)
        val text = read.getOrThrow()
        assertTrue("$uri reads ${text.take(16)}", text.startsWith(start) && text.contains(holds))
    }

    /** The files the share wrote for the share sheet to read. */
    private fun sharedFiles(): List<File> =
        rerunShareDirectory(app, RERUN_SHARE_PLAN).walk().filter { it.isFile }.toList()

    /** [send] carries one file, readable by the app it reaches, and the share wrote that file alone. */
    private fun assertOneFile(send: Intent, extension: String): File {
        assertEquals(Intent.ACTION_SEND, send.action)
        val stream = IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java)
        assertNotNull("the share carries no file", stream)
        assertTrue("$stream is not a $extension file", stream!!.lastPathSegment.orEmpty().endsWith(extension))
        val clip = send.clipData
        assertNotNull("the share grants no file", clip)
        assertEquals(1, clip!!.itemCount)
        assertEquals(stream, clip.getItemAt(0).uri)
        assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        val written = sharedFiles()
        assertEquals(listOf(stream.lastPathSegment), written.map { it.name })
        return written.single()
    }

    /** [sheet] offers [send] to every app that takes its type: no app hidden, none added, none chosen. */
    private fun assertNoAppLeftOut(sheet: Intent, send: Intent) {
        for (extra in listOf(Intent.EXTRA_EXCLUDE_COMPONENTS, Intent.EXTRA_INITIAL_INTENTS, Intent.EXTRA_CHOOSER_TARGETS)) {
            assertFalse("the share sheet carries $extra", sheet.hasExtra(extra))
        }
        assertFalse(send.hasExtra(Intent.EXTRA_EXCLUDE_COMPONENTS))
        assertNull(send.component)
        assertNull(send.`package`)
    }

    @Test
    fun `Share PDF hands over the PDF alone, typed as a PDF, and leaves no app out`() {
        showPlan()
        val sheet = shareSheetOf(composeRule.onNodeWithTag(RERUN_PLAN_SHARE_PDF_TAG))
        val send = sentBy(sheet)
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("application/pdf", send.type)
        val pdf = assertOneFile(send, ".pdf")
        assertTrue(pdf.readText().startsWith("%PDF"))
        assertNoAppLeftOut(sheet, send)
    }

    @Test
    fun `Share DXF hands over the DXF drawing alone, typed as a DXF`() {
        showPlan()
        val sheet = shareSheetOf(composeRule.onNodeWithText(DXF_LABEL))
        val send = sentBy(sheet)
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals(DXF_TYPE, send.type)
        val dxf = assertOneFile(send, ".dxf")
        assertTrue("not a DXF AC1015 (R2000) drawing", dxf.readText().contains("AC1015"))
        assertNoAppLeftOut(sheet, send)
    }

    @Test
    fun `Share DXF sits right after Share PDF in the plan's share menu`() {
        showPlan()
        val pdf = composeRule.onNodeWithTag(RERUN_PLAN_SHARE_PDF_TAG).fetchSemanticsNode()
        val dxf = composeRule.onNodeWithText(DXF_LABEL).fetchSemanticsNode()
        val menu = pdf.parent
        assertNotNull("Share PDF is in no menu", menu)
        assertEquals("Share DXF is not in Share PDF's menu", menu!!.id, dxf.parent?.id)
        val items = menu.children
        assertEquals(items.indexOfFirst { it.id == pdf.id } + 1, items.indexOfFirst { it.id == dxf.id })
        // The menu is the shares and nothing else: PDF, DXF, then the text.
        assertEquals(
            listOf(app.getString(R.string.demo_ar_rerun_plan_share_pdf), DXF_LABEL, app.getString(R.string.demo_ar_rerun_plan_share_text)),
            items.map { it.label() },
        )
        assertTrue(items.all { it.config.getOrNull(SemanticsProperties.Role) == Role.Button })
    }

    @Test
    fun `a DXF shared right after the PDF leaves the PDF readable, each kind keeping its latest file`() {
        showPlan()
        val pdf = streamOf(sentBy(shareSheetOf(composeRule.onNodeWithTag(RERUN_PLAN_SHARE_PDF_TAG))))
        val dxf = streamOf(sentBy(shareSheetOf(composeRule.onNodeWithText(DXF_LABEL))))
        // The app the PDF went to may read it only now: the DXF's share must not have taken it away.
        assertStillReadable(pdf, "%PDF")
        assertStillReadable(dxf, "  0", holds = "AC1015")
        // A second PDF replaces the first, and leaves the DXF be.
        val again = streamOf(sentBy(shareSheetOf(composeRule.onNodeWithTag(RERUN_PLAN_SHARE_PDF_TAG))))
        assertStillReadable(again, "%PDF")
        assertStillReadable(dxf, "  0", holds = "AC1015")
        assertEquals(
            listOf(again.lastPathSegment, dxf.lastPathSegment).sortedBy { it.orEmpty() },
            sharedFiles().map { it.name }.sorted(),
        )
    }

    @Test
    fun `a survey with no plan offers no DXF, its share menu holding the PDF and the text`() {
        composeRule.setContent { RerunPlanView(survey = RoomSurvey.empty(SurveyGap.NoWalls), title = TITLE, date = null) }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(DXF_LABEL).assertDoesNotExist()
        composeRule.onNodeWithTag(RERUN_PLAN_SHARE_DXF_TAG).assertDoesNotExist()
        val menu = composeRule.onNodeWithTag(RERUN_PLAN_SHARES_TAG).fetchSemanticsNode()
        assertEquals(
            listOf(app.getString(R.string.demo_ar_rerun_plan_share_pdf), app.getString(R.string.demo_ar_rerun_plan_share_text)),
            menu.children.map { it.label() },
        )
    }

    private fun SemanticsNode.label(): String =
        config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }

    private companion object {
        const val TITLE = "Traced room"
        const val DXF_LABEL = "Share DXF (CAD)"
        const val DXF_TYPE = "application/dxf"
        const val SHARE_TIMEOUT_MS = 20_000L
    }
}

/**
 * The platform's PDF writer, which Robolectric cannot run: each page is drawn on a bitmap and the
 * document comes out as a bare PDF header, enough for the share to go through.
 */
@Implements(PdfDocument::class)
class PlanPdfStandIn {

    @Implementation
    fun startPage(info: PdfDocument.PageInfo): PdfDocument.Page = ReflectionHelpers.callConstructor(
        PdfDocument.Page::class.java,
        ClassParameter.from(Canvas::class.java, Canvas(Bitmap.createBitmap(info.pageWidth, info.pageHeight, Bitmap.Config.ARGB_8888))),
        ClassParameter.from(PdfDocument.PageInfo::class.java, info),
    )

    @Implementation
    fun finishPage(ignored: PdfDocument.Page) = Unit

    @Implementation
    fun writeTo(out: OutputStream) = out.write("%PDF-1.4\n%%EOF\n".toByteArray())

    @Implementation
    fun close() = Unit
}
