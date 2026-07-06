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
import com.anomalyzed.simplespeechkeyboard.engine.CloudEngine
import com.anomalyzed.simplespeechkeyboard.engine.AICoreEngine
import com.anomalyzed.simplespeechkeyboard.engine.LiteRTEngine
import com.anomalyzed.simplespeechkeyboard.engine.WhisperCppEngine
import com.anomalyzed.simplespeechkeyboard.engine.TranscriptionEngine
import com.anomalyzed.simplespeechkeyboard.engine.TranscriptionResult
import com.anomalyzed.simplespeechkeyboard.data.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Accessibility service that:
 * 1. Detects when the software keyboard is visible/hidden.
 * 2. Shows/hides the floating microphone overlay accordingly.
 * 3. Records audio, transcribes it via the selected engine, and injects the
 *    resulting text into the currently focused input field.
 */
class TranscriptionAccessibilityService : AccessibilityService() {

    private val TAG = "TranscriptionA11y"

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var overlayManager: OverlayManager
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var prefs: AppPreferences

    private var isKeyboardVisible = false
    private var isRecording = false
    private var recordingJob: Job? = null
    private val audioBuffer = mutableListOf<ByteArray>()

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
        Log.i(TAG, "onAccessibilityEvent eventType: ${event.eventType}, package: ${event.packageName}, class: ${event.className}")
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

    // ─── Recording ────────────────────────────────────────────────────────────

    private fun onMicClicked() {
        if (isRecording) stopRecording() else startRecording()
    }

    private fun startRecording() {
        isRecording = true
        audioBuffer.clear()
        overlayManager.setState(OverlayManager.State.RECORDING)

        recordingJob = serviceScope.launch {
            audioRecorder.startRecording()
                .onEach { chunk -> audioBuffer.add(chunk) }
                .launchIn(this)
        }
    }

    private fun stopRecording() {
        isRecording = false
        audioRecorder.stopRecording()
        recordingJob?.cancel()
        recordingJob = null

        if (audioBuffer.isNotEmpty()) {
            transcribeBuffer()
        } else {
            overlayManager.setState(OverlayManager.State.IDLE)
        }
    }

    // ─── Transcription ────────────────────────────────────────────────────────

    private fun transcribeBuffer() {
        overlayManager.setState(OverlayManager.State.PROCESSING)
        val combined = combineBuffers(audioBuffer)
        audioBuffer.clear()

        serviceScope.launch(Dispatchers.IO) {
            val engine = buildEngine()
            val result = engine.transcribe(
                audioBytes = combined,
                mimeType = "audio/wav",
                language = prefs.language,
                onProgress = { Log.i(TAG, "Progress: $it") },
                onPartialText = { partial ->
                    serviceScope.launch(Dispatchers.Main) {
                        injectText(partial)
                    }
                }
            )
            serviceScope.launch(Dispatchers.Main) {
                when (result) {
                    is TranscriptionResult.Success -> injectText(result.text)
                    is TranscriptionResult.Error -> Log.e(TAG, "Transcription error: ${result.message}")
                }
                overlayManager.setState(OverlayManager.State.IDLE)
            }
        }
    }

    private fun buildEngine(): TranscriptionEngine = when (prefs.selectedEngine) {
        AppPreferences.ENGINE_WHISPER -> WhisperCppEngine(prefs.whisperModelPath)
        AppPreferences.ENGINE_GEMMA -> LiteRTEngine(prefs.gemmaModelPath)
        AppPreferences.ENGINE_AICORE -> AICoreEngine(applicationContext)
        else -> CloudEngine(prefs.geminiApiKey)
    }

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

    // ─── Text Injection ───────────────────────────────────────────────────────

    /**
     * Injects [text] into the currently focused accessibility node.
     *
     * Strategy:
     * 1. Try ACTION_SET_TEXT (API 21+) — works in most standard EditText fields.
     * 2. Fallback: copy to clipboard and paste — works in WebViews and complex fields.
     */
    private fun injectText(text: String) {
        val rootNode = rootInActiveWindow ?: run {
            Log.w(TAG, "rootInActiveWindow is null, cannot inject text")
            return
        }
        val focusedNode = findFocusedEditableNode(rootNode) ?: run {
            Log.w(TAG, "No focused editable node found, falling back to clipboard paste")
            pasteViaClipboard(text)
            return
        }

        // Append to existing text rather than replacing it
        val existing = focusedNode.text?.toString() ?: ""
        val newText = if (existing.isBlank()) text else "$existing $text"

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        if (!success) {
            Log.w(TAG, "ACTION_SET_TEXT failed, falling back to clipboard paste")
            pasteViaClipboard(text)
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
        serviceScope.cancel()
    }
}
