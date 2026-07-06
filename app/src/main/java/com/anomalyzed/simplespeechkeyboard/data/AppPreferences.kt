package com.anomalyzed.simplespeechkeyboard.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistent app preferences backed by SharedPreferences.
 * Stores the selected engine, model paths, API key, and transcription language.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("simplespeechkeyboard_prefs", Context.MODE_PRIVATE)

    var selectedEngine: String
        get() = prefs.getString(KEY_ENGINE, ENGINE_CLOUD) ?: ENGINE_CLOUD
        set(value) = prefs.edit().putString(KEY_ENGINE, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var whisperModelPath: String?
        get() = prefs.getString(KEY_WHISPER_MODEL_PATH, null)
        set(value) = prefs.edit().putString(KEY_WHISPER_MODEL_PATH, value).apply()

    var gemmaModelPath: String?
        get() = prefs.getString(KEY_GEMMA_MODEL_PATH, null)
        set(value) = prefs.edit().putString(KEY_GEMMA_MODEL_PATH, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "it") ?: "it"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    companion object {
        const val ENGINE_CLOUD = "gemini_cloud"
        const val ENGINE_WHISPER = "whisper"
        const val ENGINE_GEMMA = "gemma"
        const val ENGINE_AICORE = "aicore"

        private const val KEY_ENGINE = "engine"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_WHISPER_MODEL_PATH = "whisper_model_path"
        private const val KEY_GEMMA_MODEL_PATH = "gemma_model_path"
        private const val KEY_LANGUAGE = "language"
    }
}
