package io.github.sceneview.ar

import android.content.Context
import android.os.Build
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.arcore.ARSession

/**
 * Assumed distance in meters from the device camera to the surface on which the user will
 * try to place models.
 *
 * This value affects the apparent scale of objects while the tracking method of the Instant
 * Placement point is `SCREENSPACE_WITH_APPROXIMATE_DISTANCE`. Values in the [0.2, 2.0] meter
 * range are a good choice for most AR experiences.
 */
const val kDefaultHitTestInstantDistance = 2.0f

/**
 * Manages an ARCore [Session] lifecycle.
 *
 * Before starting a session this class checks camera permission and ARCore availability
 * through an [ARPermissionHandler], which decouples the permission logic from
 * [android.app.Activity] and makes the class testable with a mock handler.
 *
 * @param onSessionCreated     Called once when the [Session] is created.
 * @param onSessionResumed     Called each time the session resumes.
 * @param onSessionPaused      Called each time the session pauses.
 * @param onArSessionFailed    Called when session creation or resume fails.
 * @param onSessionConfigChanged Called when the session configuration changes.
 */
class ARCore(
    val onSessionCreated: (session: Session) -> Unit,
    val onSessionResumed: (session: Session) -> Unit,
    val onSessionPaused: (session: Session) -> Unit,
    val onArSessionFailed: (exception: Exception) -> Unit,
    val onSessionConfigChanged: (session: Session, config: Config) -> Unit
) {

    /** Enable/disable the automatic camera permission check. */
    var checkCameraPermission = true

    /** Enable/disable Google Play Services for AR availability check, auto-install and update. */
    var checkAvailability = true

    lateinit var features: Set<Session.Feature>

    /** The permission handler used for camera and ARCore availability checks. */
    var permissionHandler: ARPermissionHandler? = null

    private var cameraPermissionRequested = false
    private var appSettingsRequested = false
    private var installRequested = false

    internal var session: ARSession? = null
        private set

    /**
     * Initializes the ARCore session lifecycle.
     *
     * @param context  Android context for session creation.
     * @param handler  Permission handler for camera permission and ARCore install checks.
     *                 Pass `null` to skip all permission checks (useful for tests or
     *                 contexts where the camera permission is guaranteed).
     * @param features ARCore session features to enable.
     */
    fun create(context: Context, handler: ARPermissionHandler?, features: Set<Session.Feature>) {
        this.features = features
        this.permissionHandler = handler

        if (handler != null) {
            if (checkPermissionAndInstall(handler)) {
                createSession(context)
            }
        } else {
            createSession(context)
        }
    }

    /**
     * Resumes the ARCore session, creating it first if necessary.
     *
     * @param context Android context for session creation.
     * @param handler Permission handler, or `null` to skip permission checks.
     */
    fun resume(context: Context, handler: ARPermissionHandler?) {
        if (session == null) {
            if (handler == null || checkPermissionAndInstall(handler)) {
                createSession(context)
            }
        }
        session?.resume()
    }

    /** Pauses the current ARCore session. */
    fun pause() {
        session?.pause()
    }

    /**
     * Creates the ARCore session.
     *
     * @param context Android context.
     */
    fun createSession(context: Context) {
        try {
            session = ARSession(
                context,
                features,
                onResumed = onSessionResumed,
                onPaused = onSessionPaused,
                onConfigChanged = onSessionConfigChanged
            ).also(onSessionCreated)
        } catch (exception: Exception) {
            onException(exception)
        }
    }

    /**
     * Checks camera permission and ARCore installation, requesting them if needed.
     *
     * @param handler The permission handler to delegate checks to.
     * @return `true` if all checks pass and the session can be created.
     */
    fun checkPermissionAndInstall(handler: ARPermissionHandler): Boolean {
        // Camera permission
        if (checkCameraPermission && !cameraPermissionRequested && !handler.hasCameraPermission()) {
            handler.requestCameraPermission { granted ->
                if (!granted && handler.shouldShowPermissionRationale()) {
                    appSettingsRequested = true
                    handler.openAppSettings()
                }
            }
            cameraPermissionRequested = true
        } else if (!appSettingsRequested) {
            try {
                if (checkAvailability && !installRequested &&
                    handler.checkARCoreAvailability() != Availability.SUPPORTED_INSTALLED
                ) {
                    if (handler.requestARCoreInstall(!installRequested)) {
                        installRequested = true
                    } else {
                        return true
                    }
                } else {
                    return true
                }
            } catch (e: Exception) {
                onException(e)
            }
        }
        return false
    }

    /**
     * Explicitly closes the ARCore session to release native resources.
     *
     * Review the API reference for important considerations before calling close() in apps with
     * more complicated lifecycle requirements: [Session.close]
     */
    fun destroy() {
        session?.let {
            synchronized(it) {
                if (session == null) return@synchronized
                it.close()
                session = null
            }
        }
    }

    /** Forwards an exception to the [onArSessionFailed] callback. */
    fun onException(exception: Exception) {
        onArSessionFailed(exception)
    }

    // ── Deprecated compatibility overloads ────────────────────────────────────────────────────────

    /**
     * @deprecated Use [create] with an [ARPermissionHandler] instead.
     */
    @Deprecated(
        "Use create(context, handler, features) instead",
        ReplaceWith("create(context, (context as? androidx.activity.ComponentActivity)?.let { ActivityARPermissionHandler(it) }, features)")
    )
    fun create(context: Context, features: Set<Session.Feature>) {
        val handler = (context as? androidx.activity.ComponentActivity)?.let {
            ActivityARPermissionHandler(it)
        }
        create(context, handler, features)
    }

    /**
     * @deprecated Use [resume] with an [ARPermissionHandler] instead.
     */
    @Deprecated(
        "Use resume(context, handler) instead",
        ReplaceWith("resume(context, (context as? androidx.activity.ComponentActivity)?.let { ActivityARPermissionHandler(it) })")
    )
    fun resume(context: Context) {
        val handler = (context as? androidx.activity.ComponentActivity)?.let {
            ActivityARPermissionHandler(it)
        }
        resume(context, handler)
    }
}

/**
 * Returns a human-readable description of the given [TrackingFailureReason].
 *
 * @param context Android context for string resource resolution.
 */
@Suppress("REDUNDANT_ELSE_IN_WHEN")
fun TrackingFailureReason.getDescription(context: Context) = when (this) {
    TrackingFailureReason.NONE -> ""
    TrackingFailureReason.BAD_STATE -> context.getString(R.string.sceneview_bad_state_message)
    TrackingFailureReason.INSUFFICIENT_LIGHT -> context.getString(
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            R.string.sceneview_insufficient_light_message
        } else {
            R.string.sceneview_insufficient_light_android_s_message
        }
    )
    TrackingFailureReason.EXCESSIVE_MOTION -> context.getString(R.string.sceneview_excessive_motion_message)
    TrackingFailureReason.INSUFFICIENT_FEATURES -> context.getString(R.string.sceneview_insufficient_features_message)
    TrackingFailureReason.CAMERA_UNAVAILABLE -> context.getString(R.string.sceneview_camera_unavailable_message)
    else -> context.getString(R.string.sceneview_unknown_tracking_failure, this)
}
