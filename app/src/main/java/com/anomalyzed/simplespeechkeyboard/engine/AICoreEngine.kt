package com.anomalyzed.simplespeechkeyboard.engine

import android.content.Context
import android.content.pm.PackageManager

/**
 * Transcription engine using Gemini Nano via AICore (on-device, system-managed).
 * Available only on Pixel 8+ and selected devices with the AICore package installed.
 */
class AICoreEngine(private val context: Context) : TranscriptionEngine {

    override suspend fun transcribe(
        audioBytes: ByteArray,
        mimeType: String,
        language: String,
        onProgress: (String) -> Unit,
        onPartialText: (String) -> Unit
    ): TranscriptionResult {
        if (!isAvailable()) {
            return TranscriptionResult.Error(
                "AICore non disponibile su questo dispositivo. " +
                "Richiede Pixel 8+ o dispositivo equivalente con Google AICore installato."
            )
        }

        return TranscriptionResult.Error(
            "Integrazione AICore in arrivo nella prossima versione. " +
            "Usa Gemini Cloud, Gemma o Whisper nel frattempo."
        )
    }

    /**
     * Returns true if the AICore package (Gemini Nano host) is installed on this device.
     */
    override fun isAvailable(): Boolean = try {
        context.packageManager.getPackageInfo(
            "com.google.android.aicore",
            PackageManager.GET_META_DATA
        )
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    override fun displayName(): String = "Gemini Nano (AICore)"
}
