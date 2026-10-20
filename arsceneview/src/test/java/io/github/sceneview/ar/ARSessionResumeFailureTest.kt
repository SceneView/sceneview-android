package io.github.sceneview.ar

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.WindowManager
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.FatalException
import io.github.sceneview.ar.arcore.ARSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * A failed `Session.resume()` reaches [ARCore.onArSessionFailed] instead of the host app.
 *
 * ARCore throws from `resume()` when another app holds the camera
 * ([CameraNotAvailableException]), when the permission was revoked ([SecurityException]), or on
 * an internal error ([FatalException], [IllegalStateException]). [ARCore] resumes from the host's
 * `onResume`, from a permission answer and from [ARCore.retrySession]: before the fix the
 * exception escaped all three and crashed the app.
 *
 * Robolectric instruments `com.google.ar.core` and [ResumeFailingSession] replaces the two
 * `Session` methods the path reaches: the native constructor (a no-op) and `resume()` (throws
 * [resumeFailure], when set). [ARSession] itself, and every line of [ARCore], run for real.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    instrumentedPackages = ["com.google.ar.core"],
    shadows = [ARSessionResumeFailureTest.ResumeFailingSession::class],
)
class ARSessionResumeFailureTest {

    private lateinit var context: Context
    private lateinit var arCore: ARCore
    private lateinit var failures: MutableList<Exception>
    private var created: Session? = null

    @Before
    fun setUp() {
        // A window context: `ARSession` reads the display, which an Application context lacks.
        val app = RuntimeEnvironment.getApplication()
        val display = app.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        context = app.createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        failures = mutableListOf()
        created = null
        resumeFailure = null
        arCore = ARCore(
            onSessionCreated = { created = it },
            onSessionResumed = {},
            onSessionPaused = {},
            onArSessionFailed = { failures += it },
            onSessionConfigChanged = { _, _ -> },
        ).apply {
            checkCameraPermission = false
            features = emptySet()
        }
    }

    @Test
    fun `every documented resume failure reaches onArSessionFailed from the host resume`() {
        listOf(
            CameraNotAvailableException("camera held by another app"),
            SecurityException("camera permission revoked"),
            FatalException("ARCore internal error"),
            IllegalStateException("ARCore internal state"),
        ).forEach { failure ->
            setUp()
            resumeFailure = failure

            arCore.resume(context, handler = null)

            assertNotNull("The session is created before it is resumed", created)
            assertEquals(listOf<Exception>(failure), failures)
            assertFalse(
                "A session ARCore refused to resume must not read as resumed (${failure::class.simpleName})",
                (created as ARSession).isResumed,
            )
        }
    }

    @Test
    fun `a resume failure from retrySession reaches onArSessionFailed`() {
        resumeFailure = null
        arCore.resume(context, handler = null)
        assertEquals(emptyList<Exception>(), failures)
        val failure = CameraNotAvailableException("camera held by another app")
        resumeFailure = failure

        arCore.retrySession(context)

        assertEquals(listOf<Exception>(failure), failures)
    }

    @Test
    fun `a resume failure from a session started outside a host resume reaches onArSessionFailed`() {
        val failure = CameraNotAvailableException("camera held by another app")
        resumeFailure = failure

        arCore.startSession(context)

        assertEquals(listOf<Exception>(failure), failures)
    }

    @Implements(Session::class)
    class ResumeFailingSession {
        @Suppress("FunctionName", "UNUSED_PARAMETER")
        @Implementation
        protected fun __constructor__(context: Context, features: Set<Session.Feature>) = Unit

        @Implementation
        protected fun resume() {
            resumeFailure?.let { throw it }
        }
    }

    private companion object {
        var resumeFailure: Exception? = null
    }
}
