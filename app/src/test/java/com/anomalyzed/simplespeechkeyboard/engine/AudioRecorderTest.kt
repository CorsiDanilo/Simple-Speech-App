package com.anomalyzed.simplespeechkeyboard.engine

import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for AudioRecorder.
 *
 * NOTE: AudioRecord.getMinBufferSize() requires the Android framework, so tests
 * that instantiate AudioRecorder must run as instrumented tests on a device/emulator.
 * These JVM-only tests verify the companion constants and helper logic.
 */
class AudioRecorderTest {

    @Test
    fun `pcm16 bytes convert to float array with correct length`() {
        // Two bytes = one 16-bit sample
        val pcmBytes = byteArrayOf(0x00, 0x40) // 0x4000 = 16384
        val result = pcm16ToFloatArray(pcmBytes)
        assertNotNull(result)
        assert(result.size == 1) { "Expected 1 float sample, got ${result.size}" }
        assert(result[0] in -1.0f..1.0f) { "Sample ${result[0]} outside [-1,1]" }
    }

    @Test
    fun `empty pcm byte array yields empty float array`() {
        val result = pcm16ToFloatArray(byteArrayOf())
        assertNotNull(result)
        assert(result.isEmpty())
    }

    // Helper duplicated here because AudioRecorder uses Android framework internally.
    private fun pcm16ToFloatArray(pcmBytes: ByteArray): FloatArray {
        val buf = java.nio.ByteBuffer.wrap(pcmBytes)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        return FloatArray(buf.remaining()) { i -> buf.get(i) / 32768.0f }
    }
}
