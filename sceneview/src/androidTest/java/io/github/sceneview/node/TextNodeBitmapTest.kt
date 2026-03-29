package io.github.sceneview.node

import android.graphics.Color
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for [TextNode.Companion.renderTextBitmap] — the pure bitmap rendering logic.
 *
 * These tests do NOT require Filament — they validate Android Canvas text rendering.
 */
@RunWith(AndroidJUnit4::class)
class TextNodeBitmapTest {

    @Test
    fun renderTextBitmap_returnsCorrectSize() {
        val bitmap = TextNode.renderTextBitmap(
            text = "Hello",
            fontSize = 48f,
            textColor = Color.WHITE,
            backgroundColor = Color.BLACK,
            bitmapWidth = 512,
            bitmapHeight = 128
        )
        assertEquals("width should be 512", 512, bitmap.width)
        assertEquals("height should be 128", 128, bitmap.height)
    }

    @Test
    fun renderTextBitmap_withCustomTypeface_doesNotCrash() {
        val bitmap = TextNode.renderTextBitmap(
            text = "Bold Italic",
            fontSize = 64f,
            textColor = Color.YELLOW,
            backgroundColor = Color.TRANSPARENT,
            typeface = Typeface.create("serif", Typeface.BOLD_ITALIC),
            bitmapWidth = 1024,
            bitmapHeight = 256
        )
        assertNotNull("bitmap should not be null", bitmap)
        assertEquals(1024, bitmap.width)
        assertEquals(256, bitmap.height)
    }

    @Test
    fun renderTextBitmap_emptyString_doesNotCrash() {
        val bitmap = TextNode.renderTextBitmap(
            text = "",
            fontSize = 48f,
            textColor = Color.WHITE,
            backgroundColor = Color.BLACK,
            bitmapWidth = 256,
            bitmapHeight = 64
        )
        assertNotNull("bitmap should not be null for empty string", bitmap)
    }

    @Test
    fun renderTextBitmap_transparentBackground_hasAlpha() {
        val bitmap = TextNode.renderTextBitmap(
            text = "Alpha",
            fontSize = 48f,
            textColor = Color.WHITE,
            backgroundColor = Color.TRANSPARENT,
            bitmapWidth = 256,
            bitmapHeight = 64
        )
        // The corners should be transparent (outside the rounded rect)
        val cornerPixel = bitmap.getPixel(0, 0)
        assertEquals("corner should be transparent", Color.TRANSPARENT, cornerPixel)
    }

    @Test
    fun renderTextBitmap_opaqueBackground_isOpaque() {
        val bitmap = TextNode.renderTextBitmap(
            text = "Opaque",
            fontSize = 48f,
            textColor = Color.WHITE,
            backgroundColor = Color.BLACK,
            bitmapWidth = 256,
            bitmapHeight = 64
        )
        // The centre should be opaque black (background)
        val centrePixel = bitmap.getPixel(128, 32)
        val alpha = Color.alpha(centrePixel)
        assertEquals("centre should have full alpha", 255, alpha)
    }
}
