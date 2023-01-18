package io.github.sceneview.utils

import android.opengl.*

/**
 * Convenience class to perform common GL operations
 */
object OpenGL {
    fun createEglContext(): EGLContext {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

        EGL14.eglInitialize(display, null, 0, null, 0)

        val configs = arrayOfNulls<EGLConfig?>(1)
        EGL14.eglChooseConfig(
            display,
            intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE,
                // EGL OPENGL ES3 BIT
                0x40,
                EGL14.EGL_NONE
            ),
            0,
            configs,
            0,
            1,
            intArrayOf(0),
            0,
        )

        val context = EGL14.eglCreateContext(
            display,
            configs[0], EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE),
            0
        )

        val surface = EGL14.eglCreatePbufferSurface(
            display,
            configs[0],
            intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE),
            0,
        )

        EGL14.eglMakeCurrent(display, surface, surface, context)
        return context
    }

    fun createExternalTextureId(): Int = IntArray(1)
        .apply { GLES30.glGenTextures(1, this, 0) }
        .first()
        .apply {
            val target = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
            GLES30.glBindTexture(target, this)
            val useMipmaps = true
            val minFilter = if (useMipmaps) GLES30.GL_LINEAR_MIPMAP_LINEAR else GLES30.GL_LINEAR
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MIN_FILTER, minFilter)
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(target, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        }

    fun destroyEglContext(context: EGLContext) =
        EGL14.eglDestroyContext(EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY), context)
}