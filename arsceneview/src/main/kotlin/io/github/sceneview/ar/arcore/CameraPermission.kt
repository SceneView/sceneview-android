package io.github.sceneview.ar.arcore

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.sceneview.ar.R

/** Check to see we have the necessary permissions for this app.  */
val Activity.hasCameraPermission
    get() = (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED)

/** Must be called before onResume() */
fun ComponentActivity.registerForCameraPermissionResult() =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            if (!shouldShowCameraPermissionRationale) {
                // Permission denied with checking "Do not ask again".
                Toast.makeText(
                    this,
                    getString(R.string.sceneview_camera_permission_required),
                    Toast.LENGTH_LONG
                ).show()
                launchPermissionSettings()
            }
        }
    }

/** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
fun ActivityResultLauncher<String>.requestCameraPermission() = launch(Manifest.permission.CAMERA)

/** Check to see if we need to show the rationale for this permission.  */
val ComponentActivity.shouldShowCameraPermissionRationale
    get() = ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.CAMERA
    )

/** Launch Application Setting to grant permission.  */
fun ComponentActivity.launchPermissionSettings() {
    startActivity(Intent().apply {
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        data = Uri.fromParts("package", packageName, null)
    })
}
