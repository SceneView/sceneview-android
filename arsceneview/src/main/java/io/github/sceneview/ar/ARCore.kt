package io.github.sceneview.ar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.arcore.ARSession

/**
 * ### Assumed distance in meters from the device camera to the surface on which user will try to
 * place models
 *
 * This value affects the apparent scale of objects while the tracking method of the Instant
 * Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE. Values in the [0.2, 2.0] meter range
 * are a good choice for most AR experiences. Use lower values for AR experiences where users are
 * expected to place objects on surfaces close to the camera. Use larger values for experiences
 * where the user will likely be standing and trying to place an object on the ground or floor in
 * front of them.
 */
const val kDefaultHitTestInstantDistance = 2.0f

/**
 * Manages an ARCore Session using the Android Lifecycle API. Before starting a Session, this class
 * requests installation of Google Play Services for AR if it's not installed or not up to date and
 * asks the user for required permissions if necessary.
 */
class ARCore(
    val onSessionCreated: (session: Session) -> Unit,
    val onSessionResumed: (session: Session) -> Unit,
    val onSessionPaused: (session: Session) -> Unit,
    val onArSessionFailed: (exception: Exception) -> Unit,
    val onSessionConfigChanged: (session: Session, config: Config) -> Unit
) {

    /**
     * ### Enable/Disable the auto camera permission check
     */
    var checkCameraPermission = true

    /**
     * Enable/Disable Google Play Services for AR availability check, auto install and update
     */
    var checkAvailability = true

    lateinit var features: Set<Session.Feature>
    lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var cameraPermissionRequested = false
    lateinit var appSettingsLauncher: ActivityResultLauncher<Intent>

    private var appSettingsRequested = false
    private var installRequested = false
    internal var session: ARSession? = null
        private set

    fun create(context: Context, activity: ComponentActivity?, features: Set<Session.Feature>) {
        this.features = features
        // Must be called before on resume
        if (activity != null) {
            cameraPermissionLauncher = activity.activityResultRegistry.register(
                "sceneview_camera_permission",
                ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (!isGranted) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.CAMERA
                        )
                    ) {
                        appSettingsRequested = true
                        showCameraPermissionSettings(activity)
                    }
                }
            }
            appSettingsLauncher = activity.activityResultRegistry.register(
                "sceneview_app_settings",
                ActivityResultContracts.StartActivityForResult()
            ) {
                appSettingsRequested = false
            }
            // If no activity, no permission check
            if (checkPermissionAndInstall(activity)) {
                createSession(context)
            }
        } else {
            createSession(context)
        }
    }

    fun resume(context: Context, activity: ComponentActivity?) {
        if (session == null) {
            // If no activity, no permission check
            if (activity == null || checkPermissionAndInstall(activity)) {
                // Create a session if Google Play Services for AR is installed and up to
                // date.
                createSession(context)
            }
        }
        session?.resume()
    }

    fun pause() {
        session?.pause()
    }

    fun createSession(context: Context) {
        // Create a session if Google Play Services for AR is installed and up to date.
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

    fun checkPermissionAndInstall(activity: ComponentActivity): Boolean {
        // Camera Permission
        if (checkCameraPermission && !cameraPermissionRequested &&
            !checkCameraPermission(activity, cameraPermissionLauncher)
        ) {
            cameraPermissionRequested = true
        } else if (!appSettingsRequested) {
            // In case of Camera permission previously denied, the allow popup won't show but
            // the onResume will be called anyway.
            // So if we launch the app settings screen, the onResume will be called twice.
            // In order to avoid multiple session creation failing because camera permission is
            // still not granted, we check if the app settings screen is displayed.
            // In last case, if the camera permission is still not granted a SecurityException
            // will be thrown when trying to create session.
            try {
                // ARCore install and update if camera permission is granted.
                // For now, ARCore session will throw an exception if the camera is not
                // accessible (ARCore cannot be used without camera.
                // Request installation if necessary
                if (checkAvailability && !installRequested &&
                    !checkInstall(activity, installRequested)
                ) {
                    // Session will be created if everything is ok on next onResume(), so we
                    // return for now
                    installRequested = true
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
     * ### Explicitly close the ARCore session to release native resources.
     *
     * Review the API reference for important considerations before calling close() in apps with
     * more complicated lifecycle requirements: [Session.close]
     */
    fun destroy() {
        session?.close()
        session = null
    }

    fun onException(exception: Exception) {
        onArSessionFailed(exception)
    }

    /** Check to see we have the necessary permissions for this app.  */
    fun hasCameraPermission(context: Context) = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    fun checkCameraPermission(
        context: Context,
        permissionLauncher: ActivityResultLauncher<String>
    ): Boolean {
        return if (!hasCameraPermission(context)) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            false
        } else {
            true
        }
    }

    fun showCameraPermissionSettings(activity: ComponentActivity) {
        // Permission denied with checking "Do not ask again".
        Toast.makeText(
            activity,
            activity.getString(R.string.sceneview_camera_permission_required),
            Toast.LENGTH_LONG
        ).show()
        // Launch Application Setting to grant permission
        appSettingsLauncher.launch(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", activity.packageName, null)
        })
    }

    fun checkInstall(activity: ComponentActivity, installRequested: Boolean): Boolean {
        // Request installation if necessary
        return isInstalled(activity) || !install(activity, installRequested)
    }

    /** Check to see we have the necessary permissions for this app.  */
    fun isInstalled(context: Context) =
        ArCoreApk.getInstance().checkAvailability(context) == Availability.SUPPORTED_INSTALLED

    fun install(activity: ComponentActivity, installRequested: Boolean): Boolean {
        return ArCoreApk.getInstance().requestInstall(
            activity,
            !installRequested
        ) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
    }
}

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