package com.google.ar.sceneform;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import io.github.sceneview.BuildConfig;


/**
 * Global static Sceneform access class
 */
public class Sceneform {
    private static final String TAG = Sceneform.class.getSimpleName();

    private static final double MIN_BUILD_VERSION = Build.VERSION_CODES.N;
    private static final double MIN_OPENGL_VERSION = 3.0;

    /**
     * Returns true if Sceneform can run and is compatible on this device.
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     */
    public static boolean isSupported(Context context) {
        if (Build.VERSION.SDK_INT < MIN_BUILD_VERSION) {
            Log.e(TAG, "Sceneform requires Android N or later");
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            return false;
        }
        return true;
    }

    public static String versionName() {
        return BuildConfig.VERSION_NAME;
    }
}