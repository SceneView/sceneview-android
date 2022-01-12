package io.github.sceneview.ar

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.*
import com.gorisse.thomas.sceneview.*
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
    val config: Config.() -> Unit = {},
    var onException: ((Exception) -> Unit)? = null
) : ArSceneLifecycleObserver {

    private var installRequested = false
    internal var session: ArSession? = null
        private set

    companion object {
        var cameraPermissionLauncher: ActivityResultLauncher<String>? = null

        fun registerForCameraPermissionResult(activity: ComponentActivity) {
            cameraPermissionLauncher = activity.registerForCameraPermissionResult()
        }
    }

    init {
        // Must be called before on resume
        try {
            registerForCameraPermissionResult(lifecycle.activity)
        } catch (exception: Exception) {
            throw AssertionError(
                "######################################################################\n" +
                        "# Camera permission result must be registered before Activity resume #\n" +
                        "#                                                                    #\n" +
                        "# - Add the ArSceneView before onResume()                            #\n" +
                        "# OR                                                                 #\n" +
                        "# - Add this call in your Activity onCreate():                       #\n" +
                        "#                                                                    #\n" +
                        "# ARCore.registerForCameraPermissionResult(this)                     #\n" +
                        "#                                                                    #\n" +
                        "######################################################################\n" +
                        exception
            )
        }
        lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (this.session == null) {
            this.session = try {
                when {
                    // Request installation if necessary.
                    ArCoreApk.getInstance().requestInstall(lifecycle.activity, !installRequested) ==
                            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        installRequested = true
                        // createSession will be called again, so we return null for now.
                        null
                    }
                    !lifecycle.activity.hasCameraPermission -> {
                        cameraPermissionLauncher?.requestCameraPermission()
                        // createSession will be called again, so we return null for now.
                        null
                    }
                    else -> {
                        // Create a session if Google Play Services for AR is installed and up to date.
                        ArSession(cameraTextureId, lifecycle, features, config).also {
                            lifecycle.dispatchEvent<ArSceneLifecycleObserver> {
                                onArSessionCreated(it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                onException?.invoke(e)
                null
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
}