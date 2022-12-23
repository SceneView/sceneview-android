/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.sceneview.ar.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.github.sceneview.ar.R
import io.github.sceneview.utils.getComponentActivity
import io.github.sceneview.utils.getFragment

/**
 * Helper to ask camera permission
 */
object PermissionHelper {

    /**
     * Check to see if we have the necessary permissions for Camera
     */
    fun hasCameraPermission(context: Context) = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Request the Camera permission
     *
     * Must be called at the onCreate() time (before onResume()).
     * Use your own permission check if you need to call it elsewhere.
     *
     * @param onDenied Callback on permission denied and !shouldShowRequestPermissionRationale.
     * Return true to show the permission settings.
     * @param onGranted Callback to start using the camera
     */
    fun requestCameraPermission(
        activity: ComponentActivity,
        onDenied: () -> Boolean = {
            Toast.makeText(
                activity,
                R.string.sceneview_camera_permission_required,
                Toast.LENGTH_LONG
            ).show()
            true
        }, onGranted: () -> Unit = { }
    ) {
        // Must be called before on resume
        val activityResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (hasCameraPermission(activity)) {
                onGranted()
            }
        }
        // Must be called before on resume
        val permissionResultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                onGranted()
            } else if (!activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                if (onDenied()) {
                    // Launch Application Setting to grant permission
                    activityResultLauncher.launch(
                        Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", activity.packageName, null)
                        })
                }
            }
        }
        permissionResultLauncher.launch(Manifest.permission.CAMERA)
    }

    /**
     * Request the Camera permission
     *
     * Must be called at the onCreate() time (before onResume()).
     * Use your own permission check if you need to call it elsewhere.
     *
     * @param onDenied Callback on permission denied and !shouldShowRequestPermissionRationale.
     * Return true to show the permission settings.
     * @param onGranted Callback to start using the camera
     */
    fun requestCameraPermission(
        view: View,
        onDenied: () -> Boolean = { true },
        onGranted: () -> Unit = { }
    ) {
        view.getFragment()?.let { requestCameraPermission(it, onDenied, onGranted) }
            ?: view.getComponentActivity()?.let { requestCameraPermission(it, onDenied, onGranted) }
    }
}
