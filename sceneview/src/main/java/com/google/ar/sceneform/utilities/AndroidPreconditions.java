package com.google.ar.sceneform.utilities;

import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Looper;
import androidx.annotation.VisibleForTesting;

/**
 * Helper class for common android specific preconditions used inside of RenderCore.
 *
 * @hide
 */
public class AndroidPreconditions {
  private static final boolean IS_ANDROID_API_AVAILABLE = checkAndroidApiAvailable();
  private static final boolean IS_MIN_ANDROID_API_LEVEL = isMinAndroidApiLevelImpl();
  private static boolean isUnderTesting = false;

  /**
   * Ensure that the code is being executed on Android's UI thread. Null-Op if the Android API isn't
   * available (i.e. for unit tests.
   */
  public static void checkUiThread() {
    if (!isAndroidApiAvailable() || isUnderTesting()) {
      return;
    }

    boolean isOnUIThread = Looper.getMainLooper().getThread() == Thread.currentThread();
    Preconditions.checkState(isOnUIThread, "Must be called from the UI thread.");
  }

  /**
   * Enforce the minimum Android api level
   *
   * @throws IllegalStateException if the api level is not high enough
   */
  public static void checkMinAndroidApiLevel() {
    Preconditions.checkState(isMinAndroidApiLevel(), "Sceneform requires Android N or later");
  }

  /**
   * Returns true if the Android API is currently available. Useful for branching functionality to
   * make it testable via junit. The android API is available for Robolectric tests and android
   * emulator tests.
   */
  public static boolean isAndroidApiAvailable() {
    return IS_ANDROID_API_AVAILABLE;
  }

  public static boolean isUnderTesting() {
    return isUnderTesting;
  }

  /**
   * Returns true if the Android api level is above the minimum or if not on Android.
   *
   * <p>Also returns true if not on Android or in a test.
   */
  public static boolean isMinAndroidApiLevel() {
    return isUnderTesting() || IS_MIN_ANDROID_API_LEVEL;
  }

  @VisibleForTesting
  public static void setUnderTesting(boolean isUnderTesting) {
    AndroidPreconditions.isUnderTesting = isUnderTesting;
  }

  private static boolean isMinAndroidApiLevelImpl() {
    return !isAndroidApiAvailable() || (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP);
  }

  private static boolean checkAndroidApiAvailable() {
    try {
      Class.forName("android.app.Activity");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
