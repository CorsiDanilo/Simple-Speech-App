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
    private var pulseAnimator: ObjectAnimator? = null

    enum class State { IDLE, RECORDING, PROCESSING }

    private var currentState = State.IDLE

    // Standard floating button diameter: 54dp
    private val buttonSizeDp = 54

    fun show() {
        if (overlayView != null) return

        val sizePx = buttonSizeDp.dp
        val savedX = prefs.overlayX
        val savedY = prefs.overlayY

        val defaultX = 16.dp
        val defaultY = 280.dp

        val params = WindowManager.LayoutParams(
            sizePx, sizePx,
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

        val imageView = buildMicButton(params)
        overlayView = imageView

        try {
            windowManager.addView(imageView, params)
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
        currentState = State.IDLE
    }

    /** Updates button background and animation to reflect the current transcription state. */
    fun setState(state: State) {
        currentState = state
        overlayView?.let { updateAppearance(state) }
    }

    private fun updateAppearance(state: State) {
        val view = overlayView ?: return
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
                view.background = bg
                view.alpha = 0.92f
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
            State.RECORDING -> {
                // Vivid Crimson Red recording state with pulse
                bg.setColor(Color.parseColor("#D32F2F"))
                bg.setStroke(strokeWidth, Color.parseColor("#FF8A80"))
                view.background = bg
                view.alpha = 1.0f

                // Pulse scale & alpha animation
                val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.15f, 1.0f)
                val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.15f, 1.0f)
                val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.75f, 1.0f)

                val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
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
                view.background = bg
                view.alpha = 0.95f
                view.scaleX = 1.0f
                view.scaleY = 1.0f
            }
        }
    }

    private fun buildMicButton(params: WindowManager.LayoutParams): ImageView {
        return ImageView(context).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val pad = 12.dp
            setPadding(pad, pad, pad, pad)
            setColorFilter(Color.WHITE)
            elevation = 8.dp.toFloat()

            var initialX = 0
            var initialY = 0
            var touchStartX = 0f
            var touchStartY = 0f
            var isDragging = false

            val touchSlop = 10.dp

            setOnTouchListener { v, event ->
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
                            // Gravity is Gravity.BOTTOM or Gravity.END:
                            // Moving RIGHT (dx > 0) reduces distance to right edge (x)
                            // Moving DOWN (dy > 0) reduces distance to bottom edge (y)
                            val displayMetrics = context.resources.displayMetrics
                            val maxX = displayMetrics.widthPixels - params.width
                            val maxY = displayMetrics.heightPixels - params.height

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
                            // Save user's position persistently
                            prefs.overlayX = params.x
                            prefs.overlayY = params.y
                        } else {
                            v.performClick()
                        }
                        true
                    }
                    else -> false
                }
            }

            setOnClickListener { onClick() }
        }
    }

    private val Int.dp: Int get() = (this * context.resources.displayMetrics.density).toInt()
}
