package com.google.ar.sceneform.rendering;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;

/**
 * Convenience class to perform common GL operations
 *
 * @hide
 */
public class GLHelper {
  private static final String TAG = GLHelper.class.getSimpleName();

  private static final int EGL_OPENGL_ES3_BIT = 0x40;

  public static EGLContext makeContext() {
    return makeContext(EGL14.EGL_NO_CONTEXT);
  }

  @SuppressWarnings("nullness")
  public static EGLContext makeContext(EGLContext shareContext) {
    EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);

    EGL14.eglInitialize(display, null, 0, null, 0);
    EGLConfig[] configs = new EGLConfig[1];
    int[] numConfig = {0};
    int[] attribs = {EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL14.EGL_NONE};
    EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfig, 0);

    int[] contextAttribs = {EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE};
    EGLContext context =
        EGL14.eglCreateContext(display, configs[0], shareContext, contextAttribs, 0);

    int[] surfaceAttribs = {
      EGL14.EGL_WIDTH, 1,
      EGL14.EGL_HEIGHT, 1,
      EGL14.EGL_NONE
    };

    EGLSurface surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0);

    if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
      throw new IllegalStateException("Error making GL context.");
    }

    return context;
  }

  public static int createCameraTexture() {
    int[] textures = new int[1];
    GLES30.glGenTextures(1, textures, 0);
    int result = textures[0];

    final int textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    GLES30.glBindTexture(textureTarget, result);
    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
    GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
    return result;
  }

  public static void destroyContext(EGLContext context) {
    EGLDisplay display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
    if (!EGL14.eglDestroyContext(display, context)) {
      throw new IllegalStateException("Error destroying GL context.");
    }
  }
}
