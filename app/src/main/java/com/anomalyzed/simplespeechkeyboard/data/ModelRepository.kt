package com.anomalyzed.simplespeechkeyboard.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/**
 * Manages the lifecycle of downloadable Whisper.cpp model files.
 *
 * Models are stored in [context.filesDir]/models/.
 * Downloads are streamed directly from HuggingFace with progress reporting.
 */
class ModelRepository(private val context: Context) {

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    // ── Catalog ────────────────────────────────────────────────────────────────

    /** The curated list of downloadable Whisper models. */
    val catalog: List<ModelInfo> = buildCatalog()

    private fun buildCatalog(): List<ModelInfo> {
        fun whisper(
            id: String,
            displayName: String,
            sizeMib: Int,
            minRamMb: Int,
            quantization: String = ""
        ): ModelInfo {
            val fileName = "ggml-$id.bin"
            val quantLabel = if (quantization.isNotBlank()) "$quantization quantized" else "Full precision"
            return ModelInfo(
                id = "whisper-$id",
                displayName = displayName,
                description = "Whisper.cpp • Multilingual • $quantLabel",
                sizeBytes = sizeMib.toLong() * 1024L * 1024L,
                downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$fileName",
                fileName = fileName,
                minRamMb = minRamMb,
                quantization = quantization
            )
        }

        return listOf(
            whisper("tiny-q8_0",   "Whisper Tiny Q8_0",   42,  512,  "Q8_0"),
            whisper("tiny-q5_1",   "Whisper Tiny Q5_1",   31,  512,  "Q5_1"),
            whisper("tiny",        "Whisper Tiny",         75,  512),
            whisper("base-q5_1",   "Whisper Base Q5_1",   57,  1024, "Q5_1"),
            whisper("base-q8_0",   "Whisper Base Q8_0",   78,  1024, "Q8_0"),
            whisper("base",        "Whisper Base",        142,  1024),
            whisper("small-q5_1",  "Whisper Small Q5_1", 181,  2048, "Q5_1"),
            whisper("small-q8_0",  "Whisper Small Q8_0", 252,  2048, "Q8_0"),
            whisper("small",       "Whisper Small",       466,  2048)
        )
    }

    // ── Query ──────────────────────────────────────────────────────────────────

    /** Returns true if the model file exists and is non-empty on disk. */
    fun isDownloaded(model: ModelInfo): Boolean {
        val file = File(modelsDir, model.fileName)
        return file.exists() && file.length() > 0
    }

    /** Returns the absolute path of the model file, or null if not downloaded. */
    fun getModelPath(model: ModelInfo): String? {
        val file = File(modelsDir, model.fileName)
        return if (file.exists() && file.length() > 0) file.absolutePath else null
    }

    // ── Download ───────────────────────────────────────────────────────────────

    /**
     * Downloads [model] from HuggingFace and saves it to [modelsDir].
     * Calls [onProgress] with a value in [0f, 1f] during the transfer.
     * Returns the saved [File] on success, or throws on failure/cancellation.
     */
    suspend fun downloadModel(model: ModelInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val targetFile = File(modelsDir, model.fileName)
            val tempFile  = File(modelsDir, "${model.fileName}.tmp")

            try {
                val connection = URL(model.downloadUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15_000
                connection.readTimeout    = 60_000
                connection.requestMethod  = "GET"

                try {
                    val responseCode = connection.responseCode
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw Exception("Download fallito: HTTP $responseCode")
                    }

                    val totalBytes = connection.contentLengthLong
                        .takeIf { it > 0 } ?: model.sizeBytes

                    var downloaded = 0L
                    val buffer = ByteArray(8192)

                    onProgress(0f)
                    BufferedInputStream(connection.inputStream, 8192).use { input ->
                        FileOutputStream(tempFile).use { output ->
                            while (coroutineContext.isActive) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                            }
                            if (!coroutineContext.isActive) {
                                throw CancellationException("Download annullato")
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                if (!tempFile.renameTo(targetFile)) {
                    tempFile.delete()
                    throw Exception("Impossibile finalizzare il file scaricato")
                }
                onProgress(1f)
                targetFile
            } catch (e: CancellationException) {
                tempFile.delete()
                throw e
            } catch (e: Exception) {
                tempFile.delete()
                throw e
            }
        }

    // ── Delete ─────────────────────────────────────────────────────────────────

    /** Deletes the model file from disk. Returns true if deleted successfully. */
    fun deleteModel(model: ModelInfo): Boolean {
        File(modelsDir, "${model.fileName}.tmp").delete()
        return File(modelsDir, model.fileName).delete()
    }
}
