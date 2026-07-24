package com.anomalyzed.simplespeechkeyboard

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.anomalyzed.simplespeechkeyboard.data.AppPreferences

/**
 * Manages the floating microphone overlay above the software keyboard.
 *
 * Uses TYPE_ACCESSIBILITY_OVERLAY and FLAG_NOT_FOCUSABLE so focus stays in the active input field.
 * Allows smooth 2D dragging across the screen and saves the position persistently.
 */
class OverlayManager(
    private val context: Context,
    private val onClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = AppPreferences(context)
    private var overlayView: View? = null
    private var micButton: ImageView? = null
    private var previewTextView: android.widget.TextView? = null
    private var pulseAnimator: ObjectAnimator? = null

    enum class State { IDLE, RECORDING, PROCESSING }

    private var currentState = State.IDLE

    // Standard floating button diameter: 54dp
    private val buttonSizeDp = 54

    fun show() {
        if (overlayView != null) return

        val savedX = prefs.overlayX
        val savedY = prefs.overlayY

        val defaultX = 16.dp
        val defaultY = 280.dp

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = if (savedX >= 0) savedX else defaultX
            y = if (savedY >= 0) savedY else defaultY
        }

        val container = buildOverlayContainer(params)
        overlayView = container

        try {
            windowManager.addView(container, params)
            updateAppearance(State.IDLE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
        micButton = null
        previewTextView = null
        currentState = State.IDLE
    }

    /** Updates live text preview chip next to mic button. */
    fun updateLivePreview(text: String) {
        val tv = previewTextView ?: return
        if (text.isBlank() || currentState == State.IDLE) {
            tv.visibility = View.GONE
        } else {
            tv.text = text
            tv.visibility = View.VISIBLE
        }
    }

    /** Updates button background and animation to reflect the current transcription state. */
    fun setState(state: State) {
        currentState = state
        if (state == State.IDLE) {
            previewTextView?.visibility = View.GONE
            previewTextView?.text = ""
        }
        overlayView?.let { updateAppearance(state) }
    }

    private fun updateAppearance(state: State) {
        val btn = micButton ?: return
        pulseAnimator?.cancel()
        pulseAnimator = null

        val strokeWidth = 2.dp
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
        }

        when (state) {
            State.IDLE -> {
                // Sleek dark grey button with subtle Gold border
                bg.setColor(Color.parseColor("#1E1E1E"))
                bg.setStroke(strokeWidth, Color.parseColor("#D4AF37")) // Gold stroke
                btn.background = bg
                btn.alpha = 0.92f
                btn.scaleX = 1.0f
                btn.scaleY = 1.0f
            }
            State.RECORDING -> {
                // Vivid Crimson Red recording state with pulse
                bg.setColor(Color.parseColor("#D32F2F"))
                bg.setStroke(strokeWidth, Color.parseColor("#FF8A80"))
                btn.background = bg
                btn.alpha = 1.0f

                // Pulse scale & alpha animation
                val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f, 1.0f)
                val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f, 1.0f)
                val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.75f, 1.0f)

                val animator = ObjectAnimator.ofPropertyValuesHolder(btn, scaleX, scaleY, alpha).apply {
                    duration = 900
                    repeatCount = ValueAnimator.INFINITE
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                pulseAnimator = animator
            }
            State.PROCESSING -> {
                // Amber processing state
                bg.setColor(Color.parseColor("#F57C00"))
                bg.setStroke(strokeWidth, Color.parseColor("#FFE082"))
                btn.background = bg
                btn.alpha = 0.95f
                btn.scaleX = 1.0f
                btn.scaleY = 1.0f
            }
        }
    }

    private fun buildOverlayContainer(params: WindowManager.LayoutParams): View {
        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            clipChildren = false
            clipToPadding = false
            val p = 6.dp
            setPadding(p, p, p, p)
        }

        // Live text preview chip (placed to the left of the mic button)
        val textView = android.widget.TextView(context).apply {
            visibility = View.GONE
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            val padH = 12.dp
            val padV = 8.dp
            setPadding(padH, padV, padH, padV)

            val textBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16.dp.toFloat()
                setColor(Color.parseColor("#CC121212")) // 80% dark background
                setStroke(1.dp, Color.parseColor("#44FFFFFF"))
            }
            background = textBg
            elevation = 6.dp.toFloat()

            val lp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8.dp
            }
            layoutParams = lp
        }
        previewTextView = textView
        container.addView(textView)

        // Mic Button
        val buttonSizePx = buttonSizeDp.dp
        val button = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val pad = 12.dp
            setPadding(pad, pad, pad, pad)
            setColorFilter(Color.WHITE)
            elevation = 8.dp.toFloat()

            val lp = android.widget.LinearLayout.LayoutParams(buttonSizePx, buttonSizePx)
            layoutParams = lp
        }
        micButton = button
        container.addView(button)

        // Touch & Drag Handling
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false
        val touchSlop = 10.dp

        container.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()

                    if (!isDragging && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        val displayMetrics = context.resources.displayMetrics
                        val maxX = displayMetrics.widthPixels - container.width
                        val maxY = displayMetrics.heightPixels - container.height

                        val newX = (initialX - dx).coerceIn(0, maxX)
                        val newY = (initialY - dy).coerceIn(0, maxY)

                        params.x = newX
                        params.y = newY
                        windowManager.updateViewLayout(overlayView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        prefs.overlayX = params.x
                        prefs.overlayY = params.y
                    } else {
                        onClick()
                    }
                    true
                }
                else -> false
            }
        }

        return container
    }

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()
}
