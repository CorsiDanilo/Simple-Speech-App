package com.anomalyzed.simplespeechkeyboard.engine

/**
 * Transcription engine using Gemma via LiteRT-LM (on-device LLM with audio support).
 *
 * LiteRT-LM SDK (com.google.ai.edge:litert-lm) is currently in early access and
 * not published to Maven Central. This stub provides the interface and will be
 * connected to the real SDK once the dependency becomes publicly available.
 *
 * When the SDK is available, add to app/build.gradle.kts:
 *   implementation("com.google.ai.edge:litert-lm:<version>")
 *
 * Then replace the stub body below with the real LiteRT Engine initialization,
 * conversation creation, and audio-bytes message sending (see simple-transcription-app
 * LiteRTEngine.kt for a reference implementation).
 */
class LiteRTEngine(
    private val modelPath: String?,
    private val modelDisplayName: String = "Gemma (LiteRT)"
) : TranscriptionEngine {

    override suspend fun transcribe(
        audioBytes: ByteArray,
        mimeType: String,
        language: String,
        onProgress: (String) -> Unit,
        onPartialText: (String) -> Unit
    ): TranscriptionResult {
        if (modelPath == null) {
            return TranscriptionResult.Error(
                "Nessun modello Gemma selezionato. Scaricane uno dal Gestore Modelli."
            )
        }
        if (!java.io.File(modelPath).exists()) {
            return TranscriptionResult.Error("File modello non trovato: $modelPath")
        }

        // TODO: Replace stub with real LiteRT-LM Engine once SDK is available on Maven Central.
        // Reference: simple-transcription-app/app/.../engine/LiteRTEngine.kt
        return TranscriptionResult.Error(
            "Motore Gemma in arrivo. L'SDK LiteRT-LM non è ancora disponibile su Maven Central. " +
            "Usa Gemini Cloud o Whisper nel frattempo."
        )
    }

    override fun isAvailable(): Boolean =
        modelPath != null && java.io.File(modelPath).exists()

    override fun displayName(): String = modelDisplayName
}
