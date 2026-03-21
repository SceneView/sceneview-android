package io.github.sceneview.rendering

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AssetPathTest {

    @Test
    fun extensionExtracted() {
        assertEquals("glb", AssetPath("models/helmet.glb").extension)
        assertEquals("hdr", AssetPath("environments/studio.hdr").extension)
        assertEquals("", AssetPath("noextension").extension)
    }

    @Test
    fun filenameExtracted() {
        assertEquals("helmet.glb", AssetPath("models/helmet.glb").filename)
        assertEquals("file.txt", AssetPath("a/b/c/file.txt").filename)
        assertEquals("single.glb", AssetPath("single.glb").filename)
    }

    @Test
    fun stemExtracted() {
        assertEquals("helmet", AssetPath("models/helmet.glb").stem)
        assertEquals("studio", AssetPath("environments/studio.hdr").stem)
    }

    @Test
    fun directoryExtracted() {
        assertEquals("models", AssetPath("models/helmet.glb").directory)
        assertEquals("a/b/c", AssetPath("a/b/c/file.txt").directory)
        assertEquals("", AssetPath("helmet.glb").directory)
    }

    @Test
    fun isModelDetectsModelFiles() {
        assertTrue(AssetPath("model.glb").isModel)
        assertTrue(AssetPath("model.gltf").isModel)
        assertTrue(AssetPath("model.usdz").isModel)
        assertFalse(AssetPath("texture.png").isModel)
        assertFalse(AssetPath("env.hdr").isModel)
    }

    @Test
    fun isEnvironmentDetectsHdrFiles() {
        assertTrue(AssetPath("env.hdr").isEnvironment)
        assertTrue(AssetPath("env.ktx").isEnvironment)
        assertFalse(AssetPath("model.glb").isEnvironment)
    }

    @Test
    fun isTextureDetectsTextureFiles() {
        assertTrue(AssetPath("tex.png").isTexture)
        assertTrue(AssetPath("tex.jpg").isTexture)
        assertTrue(AssetPath("tex.jpeg").isTexture)
        assertTrue(AssetPath("tex.ktx2").isTexture)
        assertTrue(AssetPath("tex.basis").isTexture)
        assertFalse(AssetPath("model.glb").isTexture)
    }

    @Test
    fun toStringReturnsPath() {
        assertEquals("models/helmet.glb", AssetPath("models/helmet.glb").toString())
    }
}
