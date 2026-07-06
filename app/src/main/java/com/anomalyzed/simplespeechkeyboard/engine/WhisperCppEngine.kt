package com.anomalyzed.simplespeechkeyboard.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Transcription engine using Whisper.cpp via JNI.
 *
 * Whisper.cpp is included as a git submodule in third_party/whisper.cpp.
 * The JNI bindings (WhisperContext) bridge the native C++ library to Kotlin.
 *
 * NOTE: The WhisperContext JNI class will be added in a follow-up task when
 * the native CMake module is configured. This stub compiles and provides the
 * full interface so Tasks 4-7 can proceed without the native library.
 */
class WhisperCppEngine(
    private val modelPath: String?,
    private val modelDisplayName: String = "Whisper.cpp"
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
                "Nessun modello Whisper selezionato. Scaricane uno dal Gestore Modelli."
            )
        }
        if (!File(modelPath).exists()) {
            return TranscriptionResult.Error("File modello Whisper non trovato: $modelPath")
        }

        return try {
            onProgress("Preparazione audio...")
            val floatSamples = pcm16ToFloatArray(audioBytes)

            onProgress("Caricamento modello Whisper...")
            // TODO: Connect to real WhisperContext JNI once CMake native module is set up.
            // val context = WhisperContext.createContextFromFile(modelPath)
            // val transcript = context.transcribeData(floatSamples, language, onProgress, onPartialText)
            // context.release()
            // TranscriptionResult.Success(transcript)

            TranscriptionResult.Error(
                "Motore Whisper.cpp in configurazione. Il modulo JNI nativo sarà collegato nella prossima fase."
            )
        } catch (e: Exception) {
            TranscriptionResult.Error("Errore Whisper.cpp: ${e.localizedMessage ?: e.message}")
        }
    }

    override fun isAvailable(): Boolean =
        modelPath != null && File(modelPath).exists()

    override fun displayName(): String = modelDisplayName

    /** Converts PCM 16-bit little-endian bytes to a float array in [-1, 1] range. */
    private fun pcm16ToFloatArray(pcmBytes: ByteArray): FloatArray {
        val shortBuf = ByteBuffer.wrap(pcmBytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        return FloatArray(shortBuf.remaining()) { i -> shortBuf.get(i) / 32768.0f }
    }
}
