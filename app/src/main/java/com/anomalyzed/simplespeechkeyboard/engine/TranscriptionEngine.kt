package com.anomalyzed.simplespeechkeyboard.engine

sealed class TranscriptionResult {
    data class Success(val text: String) : TranscriptionResult()
    data class Error(val message: String) : TranscriptionResult()
}

interface TranscriptionEngine {
    suspend fun transcribe(
        audioBytes: ByteArray,
        mimeType: String,
        language: String,
        onProgress: (String) -> Unit,
        onPartialText: (String) -> Unit
    ): TranscriptionResult

    fun isAvailable(): Boolean
    fun displayName(): String
    fun release() {}
}
