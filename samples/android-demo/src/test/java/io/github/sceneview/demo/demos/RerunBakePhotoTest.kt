package io.github.sceneview.demo.demos

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import io.github.sceneview.demo.demos.internal.DebugPose
import io.github.sceneview.demo.demos.internal.ScanArchive
import io.github.sceneview.demo.demos.internal.ScanPhotoPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/**
 * The plane bake reads each photo at [ScanPhotoPolicy.BAKE_LONG_SIDE], not whole: a sharp photo
 * is held at a quarter of its pixels for the whole build, and still frames the same scene.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RerunBakePhotoTest {

    @Test
    fun `a sharp photo is decoded at the useful side`() {
        val photo = RerunCaptureBuilder.decodePhoto(scanPhoto(540, 960))!!

        assertEquals(270, photo.width)
        assertEquals(ScanPhotoPolicy.BAKE_LONG_SIDE, photo.height)
        assertEquals(270 * 480, photo.argb.size)
    }

    @Test
    fun `an ordinary photo is decoded whole`() {
        val photo = RerunCaptureBuilder.decodePhoto(scanPhoto(240, 320))!!

        assertEquals(240, photo.width)
        assertEquals(320, photo.height)
    }

    @Test
    fun `a reduced photo keeps its framing`() {
        // Left half red, right half blue: the bake samples a photo by its normalized position,
        // so the halves must still split the reduced photo down the middle.
        val photo = RerunCaptureBuilder.decodePhoto(scanPhoto(540, 960))!!
        val row = photo.height / 2 * photo.width

        val left = photo.argb[row + photo.width / 4]
        val right = photo.argb[row + photo.width * 3 / 4]
        assertTrue(Color.red(left) > 200 && Color.blue(left) < 60)
        assertTrue(Color.blue(right) > 200 && Color.red(right) < 60)
    }

    private fun scanPhoto(width: Int, height: Int): ScanPhoto {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        canvas.drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = Color.BLUE })
        val jpeg = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        bitmap.recycle()
        return ScanPhoto(ScanArchive.photoPath(0), jpeg, DebugPose(0f, 0f, 0f))
    }
}
