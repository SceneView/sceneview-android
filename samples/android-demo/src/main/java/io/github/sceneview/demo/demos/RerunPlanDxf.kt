package io.github.sceneview.demo.demos

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.sceneview.demo.R
import io.github.sceneview.demo.demos.internal.MeasureUnitSystem
import io.github.sceneview.demo.demos.internal.RoomPlanDxf
import io.github.sceneview.demo.demos.internal.RoomSurvey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [send], the share of the plan's [pdf], with the same plan as a DXF drawing beside it, for a CAD
 * tool to take it up: both files in one share. [send] as it is when the survey has no plan to
 * draw, or when the drawing could not be written.
 */
internal suspend fun withPlanDxf(context: Context, send: Intent, pdf: File, survey: RoomSurvey, words: PlanWords): Intent {
    if (!survey.hasPlan) return send
    val title = send.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
    val drawing = planDxfWords(context, survey, words, title)
    val dxf = withContext(Dispatchers.IO) {
        runCatching {
            File(pdf.parentFile, pdf.nameWithoutExtension + DXF_EXTENSION).also { target ->
                target.outputStream().use { RoomPlanDxf.write(it, survey, words.survey, drawing) }
            }
        }.getOrNull()
    } ?: return send
    val authority = "${context.packageName}.fileprovider"
    val files = arrayListOf<Uri>(
        FileProvider.getUriForFile(context, authority, pdf),
        FileProvider.getUriForFile(context, authority, dxf),
    )
    return Intent(send).apply {
        action = Intent.ACTION_SEND_MULTIPLE
        // The type both files share: a PDF and a DXF drawing are both application files.
        type = SHARED_MIME_TYPE
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, files)
        clipData = ClipData.newRawUri(pdf.name, files[0]).apply { addItem(ClipData.Item(files[1])) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

/** The drawing's words: its openings' names and the lines under the plan, the warning to check it on site first among the notes. */
private fun planDxfWords(context: Context, survey: RoomSurvey, words: PlanWords, title: String) = RoomPlanDxf.Words(
    legend = listOfNotNull(
        context.getString(R.string.demo_ar_rerun_plan_dxf_heading, title).takeIf { title.isNotBlank() },
        context.getString(R.string.demo_ar_rerun_plan_dxf_verify),
        context.getString(
            when (words.survey.units) {
                MeasureUnitSystem.Metric -> R.string.demo_ar_rerun_plan_dxf_units
                MeasureUnitSystem.Imperial -> R.string.demo_ar_rerun_plan_dxf_units_feet
            },
        ),
        context.getString(R.string.demo_ar_rerun_plan_dxf_estimates).takeIf { survey.hasEstimates },
        words.survey.spreadNote.takeIf { survey.hasSpread },
        words.survey.madeWith,
    ),
    door = context.getString(R.string.demo_ar_rerun_plan_dxf_door),
    window = context.getString(R.string.demo_ar_rerun_plan_dxf_window),
    sill = context.getString(R.string.demo_ar_rerun_plan_dxf_sill),
)

private const val DXF_EXTENSION = ".dxf"
private const val SHARED_MIME_TYPE = "application/*"
