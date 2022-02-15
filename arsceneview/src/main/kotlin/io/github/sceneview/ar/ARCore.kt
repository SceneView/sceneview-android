package io.github.sceneview.ar

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.*
import com.google.ar.core.ArCoreApk.Availability
import io.github.sceneview.ar.arcore.*

/**
 * ### Assumed distance from the device camera to the surface on which user will try to place models
 *
 * This value affects the apparent scale of objects while the tracking method of the Instant
 * Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE. Values in the [0.2, 2.0] meter range
 * are a good choice for most AR experiences. Use lower values for AR experiences where users are
 * expected to place objects on surfaces close to the camera. Use larger values for experiences
 * where the user will likely be standing and trying to place an object on the ground or floor in
 * front of them.
 */
const val defaultApproximateDistanceMeters = 2.0f

/**
 * Manages an ARCore Session using the Android Lifecycle API. Before starting a Session, this class
 * requests installation of Google Play Services for AR if it's not installed or not up to date and
 * asks the user for required permissions if necessary.
 *
 * @property onConfigure Called when the Session must be configured (Before [Session.resume])
 * @property onException Creating a session may fail. In this case, session will remain null,
 * and this callback will be called with an exception.
 */
class ARCore(
    val cameraTextureId: Int,
    val lifecycle: ArSceneLifecycle,
    val features: Set<Session.Feature> = setOf(),
    val config: Config.() -> Unit = {}
) : ArSceneLifecycleObserver {

    /**
     * //TODO: Doc
     */
    var checkCameraPermission = true

    /**
     * // TODO: Doc
     */
    var checkAvailability = true

    // TODO: See if it could be useful
//    /**
//     * ### Whether or not your SceneView can be used without ARCore
//     *
//     * This can be set to true in order to have a fallback usage when the camera is not available or
//     * the camera permission result is not granted.
//     * If set to false, the ArSceneView won't be visible until the user accept the permission ask or
//     * change the auto-displayed app permissions settings screen.
//     *
//     * **Warning:** You should not use this to limit to 3D only usage. Using SceneView instead of
//     * ArSceneView is a better choice in this case.
//     */
//    var isOptional = false

    lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var cameraPermissionRequested = false
    lateinit var appSettingsLauncher: ActivityResultLauncher<Intent>
    private var appSettingsRequested = false
    private var installRequested = false
    internal var session: ArSession? = null
        private set

    var onCameraPermissionResult: (isGranted: Boolean) -> Unit = { isGranted ->
        if (!isGranted) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    lifecycle.activity,
                    Manifest.permission.CAMERA
                )
            ) {
                appSettingsRequested = true
                showCameraPermissionSettings(lifecycle.activity)
            }
        }
    }

    var onAppSettingsResult: (result: ActivityResult) -> Unit = { result ->
        appSettingsRequested = false
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        // Must be called before on resume
        cameraPermissionLauncher = lifecycle.activity.activityResultRegistry.register(
            "sceneview_camera_permission",
            owner,
            ActivityResultContracts.RequestPermission(),
            onCameraPermissionResult
        )
        appSettingsLauncher = lifecycle.activity.activityResultRegistry.register(
            "sceneview_app_settings",
            owner,
            ActivityResultContracts.StartActivityForResult(),
            onAppSettingsResult
        )
    }

    override fun onResume(owner: LifecycleOwner) {
        if (this.session == null) {
            // Camera Permission
            if (checkCameraPermission && !cameraPermissionRequested &&
                !checkCameraPermission(lifecycle.activity, cameraPermissionLauncher)
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
                        !checkInstall(lifecycle.activity, installRequested)
                    ) {
                        // Session will be created if everything is ok on next onResume(), so we
                        // return for now
                        installRequested = true
                    } else {
                        // Create a session if Google Play Services for AR is installed and up to
                        // date.
                        session = createSession(cameraTextureId, lifecycle, features, config).also {
                            lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
                                onArSessionCreated(it)
                            }
                        }
                    }
                } catch (e: Exception) {
                    onException(e)
                }
            }
        }
    }

    /**
     * ### Explicitly close the ARCore session to release native resources.
     *
     * Review the API reference for important considerations before calling close() in apps with
     * more complicated lifecycle requirements: [Session.close]
     */
    override fun onDestroy(owner: LifecycleOwner) {
        session = null
    }

    fun onException(exception: Exception) {
        lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
            onArSessionFailed(exception)
        }
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

    fun showCameraPermissionSettings(activity: Activity) {
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

    fun checkInstall(activity: Activity, installRequested: Boolean): Boolean {
        // Request installation if necessary
        return isInstalled(activity) || !install(activity, installRequested)
    }

    /** Check to see we have the necessary permissions for this app.  */
    fun isInstalled(context: Context) = ArCoreApk.getInstance().checkAvailability(lifecycle.activity) == Availability.SUPPORTED_INSTALLED

    fun install(activity: Activity, installRequested: Boolean) : Boolean {
        return ArCoreApk.getInstance().requestInstall(
            activity,
            !installRequested
        ) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
    }

    fun createSession(
        cameraTextureId: Int,
        lifecycle: ArSceneLifecycle,
        features: Set<Session.Feature> = setOf(),
        config: Config.() -> Unit = {}
    ): ArSession {
        // Create a session if Google Play Services for AR is installed and up to date.
        return ArSession(cameraTextureId, lifecycle, features, config)
    }
}