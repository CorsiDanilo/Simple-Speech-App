package com.anomalyzed.simplespeechkeyboard.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistent app preferences backed by SharedPreferences.
 * Stores the Whisper model path, app UI language, and transcription language.
 */
class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("simplespeechkeyboard_prefs", Context.MODE_PRIVATE)

    var selectedEngine: String
        get() = ENGINE_WHISPER
        set(_) {}

    var whisperModelPath: String?
        get() = prefs.getString(KEY_WHISPER_MODEL_PATH, null)
        set(value) = prefs.edit().putString(KEY_WHISPER_MODEL_PATH, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "it") ?: "it"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    var overlayX: Int
        get() = prefs.getInt(KEY_OVERLAY_X, -1)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_X, value).apply()

    var overlayY: Int
        get() = prefs.getInt(KEY_OVERLAY_Y, -1)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_Y, value).apply()

    var threadCount: Int
        get() = prefs.getInt(KEY_THREAD_COUNT, com.anomalyzed.simplespeechkeyboard.whisper.WhisperCpuConfig.preferredThreadCount)
        set(value) = prefs.edit().putInt(KEY_THREAD_COUNT, value).apply()

    var overlaySizeDp: Int
        get() = prefs.getInt(KEY_OVERLAY_SIZE, 54)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_SIZE, value).apply()

    var overlayOpacityPercent: Int
        get() = prefs.getInt(KEY_OVERLAY_OPACITY, 90)
        set(value) = prefs.edit().putInt(KEY_OVERLAY_OPACITY, value).apply()

    var overlayColorHue: Float
        get() = prefs.getFloat(KEY_OVERLAY_HUE, 45.0f)
        set(value) = prefs.edit().putFloat(KEY_OVERLAY_HUE, value).apply()

    fun resetOverlayCustomizations() {
        prefs.edit()
            .putInt(KEY_OVERLAY_SIZE, 54)
            .putInt(KEY_OVERLAY_OPACITY, 90)
            .putFloat(KEY_OVERLAY_HUE, 45.0f)
            .apply()
    }

    companion object {
        const val ENGINE_WHISPER = "whisper"

        private const val KEY_WHISPER_MODEL_PATH = "whisper_model_path"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_OVERLAY_X = "overlay_x"
        private const val KEY_OVERLAY_Y = "overlay_y"
        private const val KEY_THREAD_COUNT = "thread_count"
        private const val KEY_OVERLAY_SIZE = "overlay_size_dp"
        private const val KEY_OVERLAY_OPACITY = "overlay_opacity_percent"
        private const val KEY_OVERLAY_HUE = "overlay_color_hue"
    }
}
