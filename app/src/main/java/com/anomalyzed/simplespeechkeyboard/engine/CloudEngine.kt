package com.anomalyzed.simplespeechkeyboard.engine

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.collect

/**
 * Transcription engine using Gemini Cloud API.
 * Sends the full WAV audio to Gemini and streams back the transcript.
 */
class CloudEngine(
    private val apiKey: String,
    private val modelName: String = "gemini-2.5-flash"
) : TranscriptionEngine {

    override suspend fun transcribe(
        audioBytes: ByteArray,
        mimeType: String,
        language: String,
        onProgress: (String) -> Unit,
        onPartialText: (String) -> Unit
    ): TranscriptionResult {
        if (apiKey.isBlank()) {
            return TranscriptionResult.Error("API Key mancante. Inseriscila nelle Impostazioni.")
        }

        return try {
            onProgress("Connessione a Gemini Cloud...")
            val model = GenerativeModel(modelName = modelName, apiKey = apiKey)

            val requestContent = content {
                blob(mimeType, audioBytes)
                text(
                    "Trascrivi questo audio fedelmente in lingua $language. " +
                    "Correggi poi la punteggiatura, la sintassi e gli errori grammaticali " +
                    "senza cambiare il significato originale. " +
                    "Rispondi SOLO con il testo finale corretto in $language, " +
                    "senza frasi introduttive o conclusive."
                )
            }

            onProgress("Trascrizione in corso...")
            val stream = model.generateContentStream(requestContent)
            var text = ""
            stream.collect { chunk ->
                text += chunk.text ?: ""
                onPartialText(text)
            }
            text = text.trim()

            if (text.isNotBlank()) {
                TranscriptionResult.Success(text)
            } else {
                TranscriptionResult.Error("Gemini non ha restituito testo. Controlla il volume o il contenuto audio.")
            }
        } catch (e: Exception) {
            TranscriptionResult.Error("Errore Gemini Cloud: ${e.localizedMessage ?: e.message}")
        }
    }

    override fun isAvailable(): Boolean = apiKey.isNotBlank()

    override fun displayName(): String = "Gemini Cloud ($modelName)"
}
