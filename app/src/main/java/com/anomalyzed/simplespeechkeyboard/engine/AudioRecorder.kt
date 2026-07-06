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
        audioRecord = record
        record.startRecording()

        try {
            val buffer = ByteArray(bufferSize)
            while (coroutineContext.isActive &&
                record.recordingState == AudioRecord.RECORDSTATE_RECORDING
            ) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    emit(buffer.copyOf(read))
                }
            }
        } finally {
            record.stop()
            record.release()
            audioRecord = null
        }
    }

    /** Signals the active [AudioRecord] to stop; the flow will terminate on its next iteration. */
    fun stopRecording() {
        audioRecord?.stop()
    }

    /** Returns the sample rate used for recording (16 000 Hz). */
    val recordingSampleRate: Int get() = sampleRate
}
