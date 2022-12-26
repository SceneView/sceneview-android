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
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Session
import io.github.sceneview.ar.arcore.ArSessionOld



/**
 * Manages an ARCore Session using the Android Lifecycle API.
 */
class ARCore(
    val activity: androidx.activity.ComponentActivity,
    val lifecycle: ArSceneLifecycle,
    val features: Set<Session.Feature> = setOf()
) : ArSceneLifecycleObserver {


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
    internal var session: ArSessionOld? = null
        private set


    var onCameraPermissionResult: (isGranted: Boolean) -> Unit = { isGranted ->
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

    var onAppSettingsResult: (result: ActivityResult) -> Unit = { _ ->
        appSettingsRequested = false
    }

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)

        // Must be called before on resume
        cameraPermissionLauncher = activity.activityResultRegistry.register(
            "sceneview_camera_permission",
            owner,
            ActivityResultContracts.RequestPermission(),
            onCameraPermissionResult
        )
        appSettingsLauncher = activity.activityResultRegistry.register(
            "sceneview_app_settings",
            owner,
            ActivityResultContracts.StartActivityForResult(),
            onAppSettingsResult
        )

    }

    override fun onResume(owner: LifecycleOwner) {
        if (session == null) {
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
                    if (checkARCoreInstall && !installRequested &&
                        !checkInstall(activity, installRequested)
                    ) {
                        // Session will be created if everything is ok on next onResume(), so we
                        // return for now
                        installRequested = true
                    } else {
                        // Create a session if Google Play Services for AR is installed and up to
                        // date.
                        session = createSession(lifecycle, features)
                        session?.let {
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
        super.onDestroy(owner)
    }

    fun onException(exception: Exception) {
        lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
            onArSessionFailed(exception)
        }
    }

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

    /**
     * Check to see we have the necessary permissions for this app
     */
    fun hasCameraPermission(context: Context) = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

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

    /**
     * Check to see we have the necessary permissions for this app.
     */
    fun isInstalled(context: Context) =
        ArCoreApk.getInstance().checkAvailability(context) == Availability.SUPPORTED_INSTALLED

    /**
     * Request ARCore installation or update if needed.
     */
    fun install(activity: Activity, installRequested: Boolean): Boolean {
        return ArCoreApk.getInstance().requestInstall(
            activity,
            !installRequested
        ) == ArCoreApk.InstallStatus.INSTALL_REQUESTED
    }

    fun createSession(
        lifecycle: ArSceneLifecycle,
        features: Set<Session.Feature> = setOf()
    ): ArSessionOld {
        // Create a session if Google Play Services for AR is installed and up to date.
        return ArSessionOld(lifecycle, features)
    }
}