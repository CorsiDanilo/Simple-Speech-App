package com.anomalyzed.simplespeechkeyboard.data

/**
 * Describes a downloadable Whisper.cpp model.
 */
data class ModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val fileName: String,
    val minRamMb: Int,
    val quantization: String = ""
) {
    /** Human-readable size (e.g. "42 MB" or "1.5 GB"). */
    val formattedSize: String
        get() {
            val gb = sizeBytes / (1024.0 * 1024.0 * 1024.0)
            return if (gb >= 1.0) {
                String.format("%.1f GB", gb)
            } else {
                val mb = sizeBytes / (1024.0 * 1024.0)
                String.format("%.0f MB", mb)
            }
        }
}
