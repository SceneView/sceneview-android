package io.github.sceneview.demo.ai.scene

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The user's own provider key, encrypted on the phone: AES-GCM under a key that lives in the
 * Android Keystore and cannot be exported. Only the ciphertext and its IV reach the app's
 * preferences; the clear key exists in memory only while a request is built. Nothing here logs.
 */
class ApiKeyStore(context: Context, private val alias: String = ALIAS, prefsName: String = PREFS) {
    private val prefs = context.applicationContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    /** Whether a key is saved. */
    val hasKey: Boolean get() = prefs.contains(CIPHERTEXT)

    /** The saved key's last four characters, for the settings sheet ("…a1b2"), never the key. */
    val hint: String? get() = prefs.getString(HINT, null)

    fun save(key: String) {
        val trimmed = key.trim()
        require(trimmed.isNotEmpty()) { "empty key" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secret())
        val sealed = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(CIPHERTEXT, Base64.encodeToString(sealed, Base64.NO_WRAP))
            .putString(HINT, trimmed.takeLast(HINT_LENGTH))
            .apply()
    }

    /** The clear key, or `null` without one — or when the Keystore lost its key (a restore). */
    fun load(): String? {
        val iv = prefs.getString(IV, null) ?: return null
        val sealed = prefs.getString(CIPHERTEXT, null) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secret(), GCMParameterSpec(TAG_BITS, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(sealed, Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().remove(IV).remove(CIPHERTEXT).remove(HINT).apply()
        runCatching { keyStore().deleteEntry(alias) }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun secret(): SecretKey {
        (keyStore().getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "ai_scene_claude_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFS = "ai_scene_key"
        const val IV = "iv"
        const val CIPHERTEXT = "ciphertext"
        const val HINT = "hint"
        const val HINT_LENGTH = 4
        const val TAG_BITS = 128
        const val KEY_BITS = 256
    }
}
