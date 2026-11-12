package com.example.data.prefs

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ApiKeyState(
    val hasKey: Boolean = false,
    val maskedPreview: String = ""
)

class SecureKeyStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _keyState = MutableStateFlow(readCurrentState())
    val keyState: StateFlow<ApiKeyState> = _keyState.asStateFlow()

    private fun readCurrentState(): ApiKeyState {
        val raw = decryptApiKeyOrNull()
        return if (raw.isNullOrBlank()) {
            ApiKeyState(hasKey = false, maskedPreview = "")
        } else {
            ApiKeyState(hasKey = true, maskedPreview = maskKey(raw))
        }
    }

    fun saveApiKey(rawKey: String): Boolean {
        val cleaned = rawKey.trim()
        if (cleaned.isEmpty()) return false
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(cleaned.toByteArray(Charsets.UTF_8))
            prefs.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .commit()
            _keyState.value = ApiKeyState(hasKey = true, maskedPreview = maskKey(cleaned))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deleteApiKey() {
        prefs.edit()
            .remove(KEY_CIPHERTEXT)
            .remove(KEY_IV)
            .commit()
        _keyState.value = ApiKeyState(hasKey = false, maskedPreview = "")
    }

    fun decryptApiKeyOrNull(): String? {
        val cipherB64 = prefs.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivB64 = prefs.getString(KEY_IV, null) ?: return null
        return try {
            val secretKey = getExistingSecretKey() ?: return null
            val cipherBytes = Base64.decode(cipherB64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivB64, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, ivBytes))
            String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    private fun getExistingSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return entry?.secretKey
    }

    private fun getOrCreateSecretKey(): SecretKey {
        getExistingSecretKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun maskKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        val tail = key.takeLast(4)
        return "••••••••••••$tail"
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nara_ai_credential_aes_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFS_NAME = "nara_secure_vault"
        private const val KEY_CIPHERTEXT = "vault_ct"
        private const val KEY_IV = "vault_iv"
    }
}
