package io.github.sceneview.ar

import com.google.ar.core.ArCoreApk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Integration-style tests for the AR permission flow using [ARPermissionHandler].
 *
 * These tests exercise realistic permission and ARCore installation sequences
 * that an AR app goes through during startup.
 */
class ARPermissionFlowTest {

    private lateinit var handler: FakeARPermissionHandler

    /**
     * Configurable test double that simulates the full permission lifecycle.
     */
    private class FakeARPermissionHandler(
        private var cameraGranted: Boolean = false,
        private var rationaleNeeded: Boolean = false,
        private var availability: ArCoreApk.Availability =
            ArCoreApk.Availability.SUPPORTED_INSTALLED,
        private var installResult: Boolean = false
    ) : ARPermissionHandler {

        var requestCameraPermissionCallCount = 0
        var openAppSettingsCallCount = 0
        var requestInstallCallCount = 0

        fun grantCameraPermission() { cameraGranted = true }
        fun denyCameraPermission() { cameraGranted = false }
        fun setAvailability(a: ArCoreApk.Availability) { availability = a }
        fun setRationaleNeeded(needed: Boolean) { rationaleNeeded = needed }
        fun setInstallResult(result: Boolean) { installResult = result }

        override fun hasCameraPermission(): Boolean = cameraGranted

        override fun requestCameraPermission(onResult: (granted: Boolean) -> Unit) {
            requestCameraPermissionCallCount++
            onResult(cameraGranted)
        }

        override fun shouldShowPermissionRationale(): Boolean = rationaleNeeded

        override fun openAppSettings() {
            openAppSettingsCallCount++
        }

        override fun checkARCoreAvailability(): ArCoreApk.Availability = availability

        override fun requestARCoreInstall(userRequestedInstall: Boolean): Boolean {
            requestInstallCallCount++
            return installResult
        }
    }

    @Before
    fun setUp() {
        handler = FakeARPermissionHandler()
    }

    // ── Happy path ──────────────────────────────────────────────────────────

    @Test
    fun `happy path - permission granted and ARCore installed`() {
        handler.grantCameraPermission()
        handler.setAvailability(ArCoreApk.Availability.SUPPORTED_INSTALLED)

        assertTrue(handler.hasCameraPermission())
        assertEquals(ArCoreApk.Availability.SUPPORTED_INSTALLED, handler.checkARCoreAvailability())
        assertFalse(handler.requestARCoreInstall(userRequestedInstall = false))
    }

    // ── Permission denied then granted ──────────────────────────────────────

    @Test
    fun `permission denied then granted on retry`() {
        handler.denyCameraPermission()
        assertFalse(handler.hasCameraPermission())

        var result: Boolean? = null
        handler.requestCameraPermission { result = it }
        assertEquals(false, result)
        assertEquals(1, handler.requestCameraPermissionCallCount)

        // User grants on second try
        handler.grantCameraPermission()
        handler.requestCameraPermission { result = it }
        assertEquals(true, result)
        assertEquals(2, handler.requestCameraPermissionCallCount)
    }

    // ── Permission permanently denied → settings ────────────────────────────

    @Test
    fun `permanently denied opens app settings`() {
        handler.denyCameraPermission()
        handler.setRationaleNeeded(true)

        assertFalse(handler.hasCameraPermission())
        assertTrue(handler.shouldShowPermissionRationale())

        handler.openAppSettings()
        assertEquals(1, handler.openAppSettingsCallCount)

        // Simulate user granting in settings
        handler.grantCameraPermission()
        assertTrue(handler.hasCameraPermission())
    }

    // ── ARCore not installed → install requested ────────────────────────────

    @Test
    fun `ARCore not installed triggers install`() {
        handler.grantCameraPermission()
        handler.setAvailability(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED)
        handler.setInstallResult(true)

        assertEquals(
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
            handler.checkARCoreAvailability()
        )
        assertTrue(handler.requestARCoreInstall(userRequestedInstall = true))
        assertEquals(1, handler.requestInstallCallCount)
    }

    // ── Device not capable ──────────────────────────────────────────────────

    @Test
    fun `unsupported device returns UNSUPPORTED`() {
        handler.setAvailability(ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE)
        assertEquals(
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE,
            handler.checkARCoreAvailability()
        )
    }

    // ── Full startup sequence ───────────────────────────────────────────────

    @Test
    fun `full startup sequence - deny then grant then install`() {
        // Step 1: No permission
        handler.denyCameraPermission()
        assertFalse(handler.hasCameraPermission())

        // Step 2: Request and get denied
        var granted = false
        handler.requestCameraPermission { granted = it }
        assertFalse(granted)

        // Step 3: User grants permission
        handler.grantCameraPermission()
        handler.requestCameraPermission { granted = it }
        assertTrue(granted)

        // Step 4: Check ARCore — needs install
        handler.setAvailability(ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED)
        handler.setInstallResult(true)
        assertEquals(
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
            handler.checkARCoreAvailability()
        )
        assertTrue(handler.requestARCoreInstall(userRequestedInstall = true))

        // Step 5: ARCore installed, ready to go
        handler.setAvailability(ArCoreApk.Availability.SUPPORTED_INSTALLED)
        handler.setInstallResult(false)
        assertEquals(
            ArCoreApk.Availability.SUPPORTED_INSTALLED,
            handler.checkARCoreAvailability()
        )
        assertFalse(handler.requestARCoreInstall(userRequestedInstall = false))
    }

    // ── Unknown checking state ──────────────────────────────────────────────

    @Test
    fun `unknown checking state during async availability check`() {
        handler.setAvailability(ArCoreApk.Availability.UNKNOWN_CHECKING)
        assertEquals(ArCoreApk.Availability.UNKNOWN_CHECKING, handler.checkARCoreAvailability())
    }
}
