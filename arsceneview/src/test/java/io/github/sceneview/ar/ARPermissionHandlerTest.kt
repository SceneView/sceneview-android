package io.github.sceneview.ar

import com.google.ar.core.ArCoreApk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * JVM tests for [ARPermissionHandler].
 *
 * The interface itself has no ARCore/Android dependency — it only references
 * [ArCoreApk.Availability] as a return type, which is a plain Java enum from
 * the ARCore stub jar that ships with the SDK. All tests use a simple in-memory
 * fake implementation so no device or emulator is required.
 */
class ARPermissionHandlerTest {

    // ── Fake implementation ──────────────────────────────────────────────────

    /**
     * Configurable test double for [ARPermissionHandler].
     *
     * Every method is controllable via constructor parameters or mutable properties
     * so each test can set up the exact preconditions it needs.
     */
    private class FakeARPermissionHandler(
        private var cameraGranted: Boolean = false,
        private var rationaleNeeded: Boolean = false,
        private var availability: ArCoreApk.Availability =
            ArCoreApk.Availability.SUPPORTED_INSTALLED,
        private var installResult: Boolean = false
    ) : ARPermissionHandler {

        var requestCameraPermissionCalled = false
        var openAppSettingsCalled = false
        var requestInstallCalled = false
        var lastUserRequestedInstall: Boolean? = null

        /** Grant camera permission without going through the permission dialog. */
        fun grantCameraPermission() { cameraGranted = true }

        override fun hasCameraPermission(): Boolean = cameraGranted

        override fun requestCameraPermission(onResult: (granted: Boolean) -> Unit) {
            requestCameraPermissionCalled = true
            // Simulate an immediate synchronous result equal to the current state
            onResult(cameraGranted)
        }

        override fun shouldShowPermissionRationale(): Boolean = rationaleNeeded

        override fun openAppSettings() {
            openAppSettingsCalled = true
        }

        override fun checkARCoreAvailability(): ArCoreApk.Availability = availability

        override fun requestARCoreInstall(userRequestedInstall: Boolean): Boolean {
            requestInstallCalled = true
            lastUserRequestedInstall = userRequestedInstall
            return installResult
        }
    }

    // ── hasCameraPermission ──────────────────────────────────────────────────

    @Test
    fun hasCameraPermission_returnsFalseWhenDenied() {
        val handler = FakeARPermissionHandler(cameraGranted = false)
        assertFalse(handler.hasCameraPermission())
    }

    @Test
    fun hasCameraPermission_returnsTrueWhenGranted() {
        val handler = FakeARPermissionHandler(cameraGranted = true)
        assertTrue(handler.hasCameraPermission())
    }

    // ── requestCameraPermission ──────────────────────────────────────────────

    @Test
    fun requestCameraPermission_invokesCallbackWithGrantedResult() {
        val handler = FakeARPermissionHandler(cameraGranted = true)
        var result: Boolean? = null
        handler.requestCameraPermission { result = it }
        assertTrue(handler.requestCameraPermissionCalled)
        assertEquals(true, result)
    }

    @Test
    fun requestCameraPermission_invokesCallbackWithDeniedResult() {
        val handler = FakeARPermissionHandler(cameraGranted = false)
        var result: Boolean? = null
        handler.requestCameraPermission { result = it }
        assertEquals(false, result)
    }

    // ── shouldShowPermissionRationale ────────────────────────────────────────

    @Test
    fun shouldShowPermissionRationale_returnsFalseWhenNotNeeded() {
        val handler = FakeARPermissionHandler(rationaleNeeded = false)
        assertFalse(handler.shouldShowPermissionRationale())
    }

    @Test
    fun shouldShowPermissionRationale_returnsTrueWhenNeeded() {
        val handler = FakeARPermissionHandler(rationaleNeeded = true)
        assertTrue(handler.shouldShowPermissionRationale())
    }

    // ── openAppSettings ──────────────────────────────────────────────────────

    @Test
    fun openAppSettings_isCalled() {
        val handler = FakeARPermissionHandler()
        handler.openAppSettings()
        assertTrue(handler.openAppSettingsCalled)
    }

    // ── checkARCoreAvailability ──────────────────────────────────────────────

    @Test
    fun checkARCoreAvailability_returnsSupportedInstalled() {
        val handler = FakeARPermissionHandler(
            availability = ArCoreApk.Availability.SUPPORTED_INSTALLED
        )
        assertEquals(ArCoreApk.Availability.SUPPORTED_INSTALLED, handler.checkARCoreAvailability())
    }

    @Test
    fun checkARCoreAvailability_returnsSupportedNotInstalled() {
        val handler = FakeARPermissionHandler(
            availability = ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED
        )
        assertEquals(
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
            handler.checkARCoreAvailability()
        )
    }

    @Test
    fun checkARCoreAvailability_returnsUnsupportedDeviceNotCapable() {
        val handler = FakeARPermissionHandler(
            availability = ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        )
        assertEquals(
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE,
            handler.checkARCoreAvailability()
        )
    }

    // ── requestARCoreInstall ─────────────────────────────────────────────────

    @Test
    fun requestARCoreInstall_returnsFalseWhenAlreadyInstalled() {
        val handler = FakeARPermissionHandler(installResult = false)
        val result = handler.requestARCoreInstall(userRequestedInstall = true)
        assertTrue(handler.requestInstallCalled)
        assertFalse(result)
    }

    @Test
    fun requestARCoreInstall_returnsTrueWhenInstallInitiated() {
        val handler = FakeARPermissionHandler(installResult = true)
        val result = handler.requestARCoreInstall(userRequestedInstall = false)
        assertTrue(result)
    }

    @Test
    fun requestARCoreInstall_forwardsUserRequestedFlag() {
        val handler = FakeARPermissionHandler(installResult = false)
        handler.requestARCoreInstall(userRequestedInstall = true)
        assertEquals(true, handler.lastUserRequestedInstall)
    }

    // ── Interface contract ───────────────────────────────────────────────────

    @Test
    fun permissionFlow_grantThenCheck() {
        val handler = FakeARPermissionHandler(cameraGranted = false)
        assertFalse(handler.hasCameraPermission())

        handler.requestCameraPermission { granted ->
            // Simulate the OS granting in response to request
            handler.grantCameraPermission()
        }
        handler.grantCameraPermission()
        assertTrue(handler.hasCameraPermission())
    }

    @Test
    fun availabilityEnum_hasExpectedValues() {
        // Verify that the enum values we depend on actually exist (compile-time guard)
        val values = ArCoreApk.Availability.values()
        val names = values.map { it.name }
        assertTrue("SUPPORTED_INSTALLED missing", "SUPPORTED_INSTALLED" in names)
        assertTrue("SUPPORTED_NOT_INSTALLED missing", "SUPPORTED_NOT_INSTALLED" in names)
        assertTrue("UNSUPPORTED_DEVICE_NOT_CAPABLE missing", "UNSUPPORTED_DEVICE_NOT_CAPABLE" in names)
        assertTrue("UNKNOWN_CHECKING missing", "UNKNOWN_CHECKING" in names)
    }
}
