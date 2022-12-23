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

package io.github.sceneview.ar.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.HardwareBuffer
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import io.github.sceneview.ar.R
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Toy class that handles all interaction with the Android camera2 API.
 * Sets the "textureTransform" and "videoTexture" parameters on the given Filament material.
 */
class CameraManager constructor(
    val context: Context,
    val captureTargets: List<Surface>,
//    private val cameraPermissionLauncher: ActivityResultLauncher<String>? = null,
//    private val appSettingsLauncher: ActivityResultLauncher<Intent>? = null,

    /**
     * Callback when the permission was denied and !shouldShowRequestPermissionRationale
     *
     * Return true to show the permission settings.
     */
    var onPermissionDenied: () -> Boolean = {
        // Set the resource to empty string if you don't want the Toast
        context.getString(R.string.sceneview_camera_permission_required)
            .takeIf { it.isNotEmpty() }
            ?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        true
    },
    /**
     * The method called when a camera device has encountered a serious error
     *
     * This indicates a failure of the camera device or camera service in some way.
     * Any attempt to call methods on this CameraDevice in the future will throw
     * a [CameraAccessException] with the [CameraAccessException.CAMERA_ERROR]
     * reason.
     *
     * Return true to finish the activity.
     *
     * @see CameraDevice.StateCallback.ERROR_CAMERA_IN_USE
     * @see CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE
     * @see CameraDevice.StateCallback.ERROR_CAMERA_DISABLED
     * @see CameraDevice.StateCallback.ERROR_CAMERA_DEVICE
     * @see CameraDevice.StateCallback.ERROR_CAMERA_SERVICE
     */
    var onError: (error: Int) -> Boolean = {
        // Set the resource to empty string if you don't want the Toast
        context.getString(R.string.sceneview_camera_unavailable_message)
            .takeIf { it.isNotEmpty() }
            ?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            }
        true
    }
) {

    var cameraDevice: CameraDevice? = null
    lateinit var cameraId: String
    lateinit var captureRequest: CaptureRequest

    private var _activity: Activity? = activity
    private val activity get() = _activity!!

    private val handler = Handler(Looper.getMainLooper())

    private val cameraOpenCloseLock = Semaphore(1)
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var captureSession: CameraCaptureSession? = null
    private var resolution = Size(640, 480)

    private val imageReader = ImageReader.newInstance(
        resolution.width,
        resolution.height,
        ImageFormat.PRIVATE,
        IMAGE_READER_MAX_IMAGES,
        HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
    )

    @Suppress("deprecation")
    private val display = if (Build.VERSION.SDK_INT >= 30) {
        activity.display
    } else {
        activity.windowManager.defaultDisplay!!
    }

    val permissionCheckState

    private var cameraPermissionLauncher: ActivityResultLauncher<String>? = null
    private var appSettingsLauncher: ActivityResultLauncher<Intent>? = null

    private var cameraPermissionRequested = false
    private var appSettingsRequested = false

    init {
        if (hasCameraPermission(context)) {
            openCamera()
        }
    }


    /**
     * Fetches the latest image (if any) from ImageReader and passes its HardwareBuffer to Filament.
     */
    fun pushExternalImageToFilament() {
        val stream = filamentStream
        if (stream != null) {
            imageReader.acquireLatestImage()?.also {
                stream.setAcquiredImage(it.hardwareBuffer, handler)) {
                    it.close()
                }
            }
        }
    }

    /**
     * Finds the front-facing Android camera, requests permission, and sets up a listener that will
     * start a capture session as soon as the camera is ready.
     */
    fun openCamera(onError: ((error: Exception) -> Unit)? = null) {
        val manager =
            activity.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue
                }

                this.cameraId = cameraId

                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    ?: continue
                resolution = map.getOutputSizes(SurfaceTexture::class.java)[0]
            }
        } catch (error: CameraAccessException) {
            onError?.invoke(error)
            Log.e(kLogTag, e.toString())
        } catch (e: NullPointerException) {
            Log.e(kLogTag, "Camera2 API is not supported on this device.")
        }

        val permission =
            ContextCompat.checkSelfPermission(this.activity, Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }
        if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
            throw RuntimeException("Time out waiting to lock camera opening.")
        }
        manager.openCamera(cameraId, CameraCallback(), backgroundHandler)
    }

    fun resume() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper!!)
    }

    fun pause() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(kLogTag, e.toString())
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(kLogTag, "Unable to obtain camera position.")
            }
            return true
        }
        return false
    }

    private fun createCaptureSession() {
//        filamentStream?.apply { filamentEngine.destroyStream(this) }
//
//        // [Re]create the Filament Stream object that gets bound to the Texture.
//        filamentStream = Stream.Builder().build(filamentEngine)
//
//        // Create the Filament Texture object if we haven't done so already.
//        if (filamentTexture == null) {
//            filamentTexture = Texture.Builder()
//                .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
//                .format(Texture.InternalFormat.RGB8)
//                .build(filamentEngine)
//        }
//
//        // We are texturing a front-facing square shape so we need to generate a matrix that transforms (u, v, 0, 1)
//        // into a new UV coordinate according to the screen rotation and the aspect ratio of the camera image.
//        val aspectRatio = resolution.width.toFloat() / resolution.height.toFloat()
//        val textureTransform = FloatArray(16)
//        Matrix.setIdentityM(textureTransform, 0)
//        when (display.rotation) {
//            Surface.ROTATION_0 -> {
//                Matrix.translateM(textureTransform, 0, 1.0f, 0.0f, 0.0f)
//                Matrix.rotateM(textureTransform, 0, 90.0f, 0.0f, 0.0f, 1.0f)
//                Matrix.translateM(textureTransform, 0, 1.0f, 0.0f, 0.0f)
//                Matrix.scaleM(textureTransform, 0, -1.0f, 1.0f / aspectRatio, 1.0f)
//            }
//            Surface.ROTATION_90 -> {
//                Matrix.translateM(textureTransform, 0, 1.0f, 1.0f, 0.0f)
//                Matrix.rotateM(textureTransform, 0, 180.0f, 0.0f, 0.0f, 1.0f)
//                Matrix.translateM(textureTransform, 0, 1.0f, 0.0f, 0.0f)
//                Matrix.scaleM(textureTransform, 0, -1.0f / aspectRatio, 1.0f, 1.0f)
//            }
//            Surface.ROTATION_270 -> {
//                Matrix.translateM(textureTransform, 0, 1.0f, 0.0f, 0.0f)
//                Matrix.scaleM(textureTransform, 0, -1.0f / aspectRatio, 1.0f, 1.0f)
//            }
//        }
//
//        // Connect the Stream to the Texture and the Texture to the MaterialInstance.
//        val sampler = TextureSampler(
//            TextureSampler.MinFilter.LINEAR,
//            TextureSampler.MagFilter.LINEAR,
//            TextureSampler.WrapMode.CLAMP_TO_EDGE
//        )
//        filamentTexture!!.setExternalStream(filamentEngine, filamentStream!!)
//        filamentMaterial.setParameter("videoTexture", filamentTexture!!, sampler)
//        filamentMaterial.setParameter(
//            "textureTransform",
//            MaterialInstance.FloatElement.MAT4,
//            textureTransform,
//            0,
//            1
//        )

        // Start the capture session. You could also use TEMPLATE_PREVIEW here.
        val captureRequestBuilder =
            cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        captureRequestBuilder.addTarget(imageReader.surface)

        cameraDevice?.createCaptureSession(
            listOf(imageReader.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = cameraCaptureSession
                    captureRequestBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    captureRequest = captureRequestBuilder.build()
                    captureSession!!.setRepeatingRequest(captureRequest, null, backgroundHandler)
                    Log.i(kLogTag, "Created CaptureRequest.")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(kLogTag, "onConfigureFailed")
                }
            }, null
        )
    }

    fun destroy() {
        _activity = null
    }

    inner class CameraCallback : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            this@CameraManager.cameraDevice = cameraDevice
            createCaptureSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice.close()
            this@CameraManager.cameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            onDisconnected(cameraDevice)
            // Fatal error
            if (onError(error)) {
                //  Quit application
                this@CameraManager.activity.finish()
            }
        }
    }

    companion object {
        // This seems a little high, but lower values cause occasional "client tried to acquire
        // more than maxImages buffers" on a Pixel 3
        var IMAGE_READER_MAX_IMAGES = 7

        private const val kLogTag = "CameraHelper"
        private const val REQUEST_CAMERA_PERMISSION = 1
    }

}
