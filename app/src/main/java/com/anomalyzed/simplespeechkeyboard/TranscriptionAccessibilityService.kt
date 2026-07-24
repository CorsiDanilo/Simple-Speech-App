package com.anomalyzed.simplespeechkeyboard

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.anomalyzed.simplespeechkeyboard.engine.AudioRecorder
import com.anomalyzed.simplespeechkeyboard.engine.WhisperCppEngine
import com.anomalyzed.simplespeechkeyboard.engine.TranscriptionEngine
import com.anomalyzed.simplespeechkeyboard.engine.TranscriptionResult
import com.anomalyzed.simplespeechkeyboard.data.AppPreferences
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accessibility service that:
 * 1. Detects when the software keyboard is visible/hidden.
 * 2. Shows/hides the floating microphone overlay accordingly.
 * 3. Records audio, transcribes it in real-time using local Whisper.cpp engine,
 *    and streams/injects the resulting text into the currently focused input field.
 */
class TranscriptionAccessibilityService : AccessibilityService() {

    private val TAG = "TranscriptionA11y"

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var overlayManager: OverlayManager
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var prefs: AppPreferences

    private var isKeyboardVisible = false
    private var isRecording = false
    private var recordingJob: Job? = null
    private var streamingJob: Job? = null
    private val audioBuffer = mutableListOf<ByteArray>()

    // Initial text captured from focused node when dictation starts
    private var initialText = ""
    private var activeEngine: TranscriptionEngine? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate called")
        audioRecorder = AudioRecorder()
        prefs = AppPreferences(applicationContext)
        overlayManager = OverlayManager(this) { onMicClicked() }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "onServiceConnected called")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            updateKeyboardVisibility()
        }
    }

    private fun updateKeyboardVisibility() {
        val visible = windows.any { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
        if (visible == isKeyboardVisible) return
        isKeyboardVisible = visible
        Log.i(TAG, "Keyboard visible: $isKeyboardVisible")
        if (isKeyboardVisible) {
            overlayManager.show()
        } else {
            if (isRecording) stopRecording()
            overlayManager.hide()
        }
    }

    // ─── Recording & Real-Time Dictation ──────────────────────────────────────

    private fun onMicClicked() {
        if (isRecording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start recording: RECORD_AUDIO permission not granted")
            Toast.makeText(this, "Permesso microfono non concesso. Apri l'app per autorizzarlo.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            isRecording = true
            synchronized(audioBuffer) { audioBuffer.clear() }

            // Capture initial text in focused field before dictation, ignoring placeholders
            initialText = captureCleanFocusedText()
            Log.i(TAG, "Captured initial text before dictation: '$initialText'")

            overlayManager.setState(OverlayManager.State.RECORDING)
            activeEngine = buildEngine()

            // 1. Audio recording job
            recordingJob = serviceScope.launch(Dispatchers.IO) {
                try {
                    audioRecorder.startRecording().collect { chunk ->
                        synchronized(audioBuffer) {
                            audioBuffer.add(chunk)
                        }
                    }
                } catch (e: CancellationException) {
                    // Normal cancellation on stop
                } catch (e: Exception) {
                    Log.e(TAG, "Recording error in flow", e)
                    withContext(Dispatchers.Main) {
                        isRecording = false
                        overlayManager.setState(OverlayManager.State.IDLE)
                        Toast.makeText(this@TranscriptionAccessibilityService, "Errore registrazione: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }

            // 2. Real-time streaming transcription job while speaking
            streamingJob = serviceScope.launch(Dispatchers.IO) {
                delay(800)
                while (isRecording) {
                    val currentAudio = synchronized(audioBuffer) { combineBuffers(audioBuffer) }
                    if (currentAudio.size >= 25000) {
                        try {
                            val result = activeEngine?.transcribe(
                                audioBytes = currentAudio,
                                mimeType = "audio/wav",
                                language = prefs.language,
                                initialPrompt = initialText,
                                onProgress = {},
                                onPartialText = { partial ->
                                    if (partial.isNotBlank() && isRecording) {
                                        serviceScope.launch(Dispatchers.Main) {
                                            injectLiveText(partial)
                                        }
                                    }
                                }
                            )
                            if (result is TranscriptionResult.Success && result.text.isNotBlank() && isRecording) {
                                serviceScope.launch(Dispatchers.Main) {
                                    injectLiveText(result.text)
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Streaming interim exception: ${e.message}")
                        }
                    }
                    delay(1000)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            isRecording = false
            overlayManager.setState(OverlayManager.State.IDLE)
            Toast.makeText(this, "Errore avvio microfono: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopRecording() {
        isRecording = false
        try {
            audioRecorder.stopRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recorder", e)
        }

        streamingJob?.cancel()
        streamingJob = null
        recordingJob?.cancel()
        recordingJob = null

        if (audioBuffer.isNotEmpty()) {
            finalTranscribeBuffer()
        } else {
            overlayManager.setState(OverlayManager.State.IDLE)
            activeEngine?.release()
            activeEngine = null
        }
    }

    // ─── Final Pass Transcription ─────────────────────────────────────────────

    private fun finalTranscribeBuffer() {
        overlayManager.setState(OverlayManager.State.PROCESSING)
        val combined = synchronized(audioBuffer) {
            val buf = combineBuffers(audioBuffer)
            audioBuffer.clear()
            buf
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val engine = activeEngine ?: buildEngine()
                val result = engine.transcribe(
                    audioBytes = combined,
                    mimeType = "audio/wav",
                    language = prefs.language,
                    initialPrompt = initialText,
                    onProgress = { Log.i(TAG, "Final progress: $it") },
                    onPartialText = { partial ->
                        serviceScope.launch(Dispatchers.Main) {
                            injectLiveText(partial)
                        }
                    }
                )
                serviceScope.launch(Dispatchers.Main) {
                    when (result) {
                        is TranscriptionResult.Success -> {
                            if (result.text.isNotBlank()) {
                                injectLiveText(result.text)
                                copyToClipboard(result.text)
                            }
                        }
                        is TranscriptionResult.Error -> {
                            Log.e(TAG, "Transcription error: ${result.message}")
                            Toast.makeText(this@TranscriptionAccessibilityService, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                    overlayManager.setState(OverlayManager.State.IDLE)
                    engine.release()
                    activeEngine = null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Transcription exception", e)
                serviceScope.launch(Dispatchers.Main) {
                    overlayManager.setState(OverlayManager.State.IDLE)
                    Toast.makeText(this@TranscriptionAccessibilityService, "Errore trascrizione: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    activeEngine?.release()
                    activeEngine = null
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        if (text.isBlank()) return
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Trascrizione", text))
            Log.i(TAG, "Copied transcription to clipboard: '$text'")
        } catch (e: Exception) {
            Log.e(TAG, "Error copying to clipboard", e)
        }
    }

    private fun buildEngine(): TranscriptionEngine = WhisperCppEngine(prefs.whisperModelPath)

    private fun combineBuffers(buffers: List<ByteArray>): ByteArray {
        val totalSize = buffers.sumOf { it.size }
        val combined = ByteArray(totalSize)
        var offset = 0
        for (buf in buffers) {
            buf.copyInto(combined, offset)
            offset += buf.size
        }
        return combined
    }

    // ─── Text Injection & Cleaning ────────────────────────────────────────────

    private fun captureCleanFocusedText(): String {
        val rootNode = rootInActiveWindow ?: return ""
        val focusedNode = findFocusedEditableNode(rootNode) ?: return ""

        val clean = extractCleanText(focusedNode)
        focusedNode.recycle()
        return clean
    }

    private fun extractCleanText(focusedNode: AccessibilityNodeInfo): String {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && focusedNode.isShowingHintText) {
            return ""
        }
        val text = focusedNode.text?.toString() ?: ""
        val hint = focusedNode.hintText?.toString() ?: ""

        if (text.isBlank() || text == hint) return ""

        val trimmed = text.trim()
        val commonPlaceholders = listOf(
            "message", "messaggio", "type a message", "scrivi un messaggio",
            "cerca", "search", "write a message", "send message"
        )
        if (commonPlaceholders.any { trimmed.equals(it, ignoreCase = true) || trimmed.startsWith("$it ", ignoreCase = true) }) {
            return ""
        }
        return text
    }

    private fun injectLiveText(streamedText: String) {
        if (streamedText.isBlank()) return
        val rootNode = rootInActiveWindow ?: run {
            pasteViaClipboard(streamedText)
            return
        }
        val focusedNode = findFocusedEditableNode(rootNode) ?: run {
            pasteViaClipboard(streamedText)
            return
        }

        val targetText = if (initialText.isBlank()) {
            streamedText
        } else {
            "$initialText $streamedText"
        }

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, targetText)
        }
        val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!success) {
            pasteViaClipboard(targetText)
        }
        focusedNode.recycle()
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isFocused && root.isEditable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFocusedEditableNode(child)
            if (found != null) {
                if (found != child) {
                    child.recycle()
                }
                return found
            }
            child.recycle()
        }
        return null
    }

    private fun pasteViaClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("transcription", text))
        val root = rootInActiveWindow ?: return
        val node = findFocusedEditableNode(root) ?: return
        node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        node.recycle()
    }

    override fun onInterrupt() {
        Log.i(TAG, "onInterrupt called")
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayManager.hide()
        activeEngine?.release()
        serviceScope.cancel()
    }
}
