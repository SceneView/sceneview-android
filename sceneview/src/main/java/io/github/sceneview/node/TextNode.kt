package io.github.sceneview.node

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.math.Position

/**
 * A node that displays a text label in 3D world space.
 *
 * [TextNode] internally renders a [Bitmap] with [android.graphics.Canvas] and delegates to
 * [BillboardNode] for camera-facing behaviour. The bitmap is re-rendered whenever [text],
 * [fontSize], [textColor], or [backgroundColor] changes.
 *
 * Usage inside a [io.github.sceneview.SceneScope]:
 * ```kotlin
 * Scene(onFrame = { cameraPos = cameraNode.worldPosition }) {
 *     TextNode(
 *         materialLoader = materialLoader,
 *         text = "Hello 3D!",
 *         fontSize = 48f,
 *         textColor = android.graphics.Color.WHITE,
 *         backgroundColor = 0xCC000000.toInt(),
 *         widthMeters = 0.6f,
 *         heightMeters = 0.2f,
 *         cameraPositionProvider = { cameraPos }
 *     )
 * }
 * ```
 *
 * @param materialLoader         MaterialLoader used to create the image material instance.
 * @param text                   The string to display.
 * @param fontSize               Font size in pixels used when rendering the bitmap texture.
 * @param textColor              ARGB text colour (default opaque white).
 * @param backgroundColor        ARGB background fill colour (default semi-transparent black).
 * @param widthMeters            Width of the quad in world-space meters.
 * @param heightMeters           Height of the quad in world-space meters.
 * @param cameraPositionProvider Lambda invoked every frame to obtain the current camera world
 *                               position so the label can face the camera.
 * @param bitmapWidth            Resolution width of the rendered bitmap in pixels (default 512).
 * @param bitmapHeight           Resolution height of the rendered bitmap in pixels (default 128).
 */
open class TextNode(
    materialLoader: MaterialLoader,
    text: String,
    fontSize: Float = 48f,
    textColor: Int = android.graphics.Color.WHITE,
    backgroundColor: Int = 0xCC000000.toInt(),
    widthMeters: Float = 0.6f,
    heightMeters: Float = 0.2f,
    cameraPositionProvider: (() -> Position)? = null,
    val bitmapWidth: Int = 512,
    val bitmapHeight: Int = 128,
) : BillboardNode(
    materialLoader = materialLoader,
    bitmap = renderTextBitmap(
        text = text,
        fontSize = fontSize,
        textColor = textColor,
        backgroundColor = backgroundColor,
        bitmapWidth = 512,
        bitmapHeight = 128
    ),
    widthMeters = widthMeters,
    heightMeters = heightMeters,
    cameraPositionProvider = cameraPositionProvider
) {

    var text: String = text
        set(value) {
            if (field != value) {
                field = value
                refreshBitmap()
            }
        }

    var fontSize: Float = fontSize
        set(value) {
            if (field != value) {
                field = value
                refreshBitmap()
            }
        }

    var textColor: Int = textColor
        set(value) {
            if (field != value) {
                field = value
                refreshBitmap()
            }
        }

    var backgroundColor: Int = backgroundColor
        set(value) {
            if (field != value) {
                field = value
                refreshBitmap()
            }
        }

    private fun refreshBitmap() {
        bitmap = renderTextBitmap(
            text = text,
            fontSize = fontSize,
            textColor = textColor,
            backgroundColor = backgroundColor,
            bitmapWidth = bitmapWidth,
            bitmapHeight = bitmapHeight
        )
    }

    companion object {
        /**
         * Renders [text] into a new [Bitmap] using Android [Canvas].
         *
         * The bitmap has a rounded-rectangle background and horizontally/vertically centred text.
         * Alpha is preserved so the material's blending mode controls transparency.
         */
        fun renderTextBitmap(
            text: String,
            fontSize: Float,
            textColor: Int,
            backgroundColor: Int,
            bitmapWidth: Int,
            bitmapHeight: Int
        ): Bitmap {
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background with rounded corners
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = backgroundColor
                style = Paint.Style.FILL
            }
            val cornerRadius = bitmapHeight * 0.2f
            canvas.drawRoundRect(
                RectF(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat()),
                cornerRadius,
                cornerRadius,
                bgPaint
            )

            // Centred bold text
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = fontSize
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val xPos = bitmapWidth / 2f
            val yPos = (bitmapHeight / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
            canvas.drawText(text, xPos, yPos, textPaint)

            return bitmap
        }
    }
}
