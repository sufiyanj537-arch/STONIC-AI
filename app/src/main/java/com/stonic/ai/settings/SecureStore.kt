package com.stonic.ai.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Encrypted storage for settings and the API key.
 * Values are encrypted with a key kept in the Android Keystore.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "stonic_secure_prefs",
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var assistantName: String
        get() = prefs.getString(KEY_ASSISTANT_NAME, DEFAULT_NAME) ?: DEFAULT_NAME
        set(value) {
            prefs.edit().putString(KEY_ASSISTANT_NAME, value.ifBlank { DEFAULT_NAME }).apply()
        }

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()
        }

    var avatarIndex: Int
        get() = prefs.getInt("avatar_index", 0).coerceIn(0, 3)
        set(value) { prefs.edit().putInt("avatar_index", value.coerceIn(0, 3)).apply() }
    var language: String
        get() = prefs.getString("language", "Roman Urdu") ?: "Roman Urdu"
        set(value) { prefs.edit().putString("language", value).apply() }
    var personality: String
        get() = prefs.getString("personality", "Caring, friendly and natural") ?: "Caring, friendly and natural"
        set(value) { prefs.edit().putString("personality", value).apply() }
    var voiceEnabled: Boolean
        get() = prefs.getBoolean("voice_enabled", true)
        set(value) { prefs.edit().putBoolean("voice_enabled", value).apply() }
    var voiceSpeed: Float
        get() = prefs.getFloat("voice_speed", 1.0f)
        set(value) { prefs.edit().putFloat("voice_speed", value.coerceIn(0.7f, 1.3f)).apply() }

    fun hasGeminiKey(): Boolean = geminiApiKey.isNotBlank()

    fun clearGeminiKey() {
        prefs.edit().remove(KEY_GEMINI_API_KEY).apply()
    }

    companion object {
        const val DEFAULT_NAME = "Stonic"
        private const val KEY_ASSISTANT_NAME = "assistant_name"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }
}
