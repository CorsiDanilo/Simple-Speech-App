package com.anomalyzed.simplespeechkeyboard.engine

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Records audio from the microphone as a stream of PCM 16-bit mono 16kHz byte arrays.
 * The format is compatible with both Whisper.cpp and LiteRT-LM (Gemma).
 */
class AudioRecorder {

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding),
        4096
    )

    private var audioRecord: AudioRecord? = null

    /**
     * Starts recording and emits PCM byte chunks while the coroutine is active.
     * The caller cancels the coroutine to stop recording.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(): Flow<ByteArray> = flow {
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioEncoding,
            bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            try { record.release() } catch (_: Exception) {}
            throw IllegalStateException("AudioRecord non inizializzato. Il microfono potrebbe essere occupato o bloccato dal sistema.")
        }

        audioRecord = record
        try {
            record.startRecording()
        } catch (e: Exception) {
            try { record.release() } catch (_: Exception) {}
            audioRecord = null
            throw e
        }

        try {
            val buffer = ByteArray(bufferSize)
            while (coroutineContext.isActive &&
                record.recordingState == AudioRecord.RECORDSTATE_RECORDING
            ) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    emit(buffer.copyOf(read))
                } else if (read < 0) {
                    break
                }
            }
        } finally {
            try {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
            } catch (_: Exception) {}
            try {
                record.release()
            } catch (_: Exception) {}
            audioRecord = null
        }
    }

    /** Signals the active [AudioRecord] to stop; the flow will terminate on its next iteration. */
    fun stopRecording() {
        try {
            val rec = audioRecord
            if (rec != null && rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                rec.stop()
            }
        } catch (_: Exception) {}
    }

    /** Returns the sample rate used for recording (16 000 Hz). */
    val recordingSampleRate: Int get() = sampleRate

    companion object {
        /**
         * Calculates the Root Mean Square (RMS) amplitude of a 16-bit PCM little-endian byte array.
         * Returns a value between 0.0 (total silence) and 1.0 (maximum volume).
         */
        fun calculateRms(pcmBytes: ByteArray): Float {
            if (pcmBytes.size < 2) return 0f
            var sumSquare = 0.0
            val sampleCount = pcmBytes.size / 2
            for (i in 0 until sampleCount) {
                val low = pcmBytes[i * 2].toInt() and 0xFF
                val high = pcmBytes[i * 2 + 1].toInt()
                val sample = ((high shl 8) or low).toShort()
                val normalized = sample / 32768.0
                sumSquare += normalized * normalized
            }
            return Math.sqrt(sumSquare / sampleCount).toFloat()
        }

        /**
         * Checks if the PCM audio chunk contains speech above a specified RMS threshold.
         * Default threshold is 0.025 (~ -32 dB), suitable for detecting spoken words.
         */
        fun isSpeech(pcmBytes: ByteArray, threshold: Float = 0.025f): Boolean {
            return calculateRms(pcmBytes) >= threshold
        }
    }
}

