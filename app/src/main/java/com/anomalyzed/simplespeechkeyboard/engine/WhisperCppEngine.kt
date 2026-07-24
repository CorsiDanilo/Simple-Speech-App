package com.anomalyzed.simplespeechkeyboard.engine

import com.anomalyzed.simplespeechkeyboard.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Transcription engine using Whisper.cpp via JNI.
 *
 * Whisper.cpp is included as a git submodule in third_party/whisper.cpp.
 * The native library is compiled as `speech_whisper` and loaded by [WhisperContext].
 *
 * Input format expected: raw PCM 16-bit mono 16 kHz (produced directly by [AudioRecorder]).
 * No additional resampling or decoding is required.
 */
class WhisperCppEngine(
    private val modelPath: String?,
    private val modelDisplayName: String = "Whisper.cpp"
) : TranscriptionEngine {

    private var whisperContext: WhisperContext? = null

    override suspend fun transcribe(
        audioBytes: ByteArray,
        mimeType: String,
        language: String,
        onProgress: (String) -> Unit,
        onPartialText: (String) -> Unit
    ): TranscriptionResult {
        if (modelPath == null) {
            return TranscriptionResult.Error(
                "Nessun modello Whisper selezionato. Scaricane uno dal Gestore Modelli."
            )
        }
        if (!File(modelPath).exists()) {
            return TranscriptionResult.Error("File modello Whisper non trovato: $modelPath")
        }
        if (audioBytes.isEmpty()) {
            return TranscriptionResult.Error("Nessun audio registrato.")
        }

        return try {
            onProgress("Preparazione audio...")
            val floatSamples = pcm16ToFloatArray(audioBytes)

            onProgress("Caricamento modello Whisper...")
            val context = getOrCreateContext()

            onProgress("Trascrizione in corso...")
            val transcript = context.transcribeData(
                data = floatSamples,
                languageCode = language.toWhisperLanguageCode(),
                onProgress = { progress ->
                    onProgress("Trascrizione Whisper... $progress%")
                },
                onNewSegment = { partialText ->
                    if (partialText.isNotBlank()) {
                        onPartialText(partialText)
                    }
                }
            )

            if (transcript.isNotBlank()) {
                TranscriptionResult.Success(transcript)
            } else {
                TranscriptionResult.Error("Whisper non ha prodotto testo. Verifica volume e contenuto audio.")
            }
        } catch (e: InterruptedException) {
            TranscriptionResult.Error("Trascrizione annullata.")
        } catch (e: Exception) {
            TranscriptionResult.Error("Errore Whisper.cpp: ${e.localizedMessage ?: e.message}")
        }
    }

    override fun isAvailable(): Boolean =
        modelPath != null && File(modelPath).exists()

    override fun displayName(): String = modelDisplayName

    override fun release() {
        val ctx = whisperContext ?: return
        whisperContext = null
        kotlinx.coroutines.runBlocking {
            ctx.release()
        }
    }

    // ── Private helpers ────────────────────────────────────────────────

    private suspend fun getOrCreateContext(): WhisperContext =
        withContext(Dispatchers.Default) {
            whisperContext ?: WhisperContext.createContextFromFile(modelPath!!).also {
                whisperContext = it
            }
        }

    /** Converts PCM 16-bit little-endian bytes to a float array in [-1, 1] range. */
    private fun pcm16ToFloatArray(pcmBytes: ByteArray): FloatArray {
        val shortBuf = ByteBuffer.wrap(pcmBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        return FloatArray(shortBuf.remaining()) { i -> shortBuf.get(i) / 32768.0f }
    }

    private fun String.toWhisperLanguageCode(): String {
        return when (trim().lowercase()) {
            "italian", "italiano", "it" -> "it"
            "english", "inglese", "en" -> "en"
            "spanish", "spagnolo", "es" -> "es"
            "french", "francese", "fr" -> "fr"
            "german", "tedesco", "de" -> "de"
            "portuguese", "portoghese", "pt" -> "pt"
            "russian", "russo", "ru" -> "ru"
            "chinese", "cinese", "zh" -> "zh"
            "japanese", "giapponese", "ja" -> "ja"
            "arabic", "arabo", "ar" -> "ar"
            else -> "auto"
        }
    }
}
