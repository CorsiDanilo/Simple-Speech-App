package com.anomalyzed.simplespeechkeyboard

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator

/**
 * Manages the floating microphone button overlay above the software keyboard.
 *
 * The overlay uses TYPE_ACCESSIBILITY_OVERLAY (no SYSTEM_ALERT_WINDOW permission needed)
 * and FLAG_NOT_FOCUSABLE so focus stays in the active input field.
 */
class OverlayManager(
    private val context: Context,
    private val onClick: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var pulseAnimator: ObjectAnimator? = null

    enum class State { IDLE, RECORDING, PROCESSING }

    private var currentState = State.IDLE

    fun show() {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            128.dp, 128.dp,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 24.dp
            y = 300.dp
        }

        val imageView = buildMicButton(params)
        overlayView = imageView
        windowManager.addView(imageView, params)
        updateAppearance(State.IDLE)
    }

    fun hide() {
        pulseAnimator?.cancel()
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
        currentState = State.IDLE
    }

    /** Updates the button appearance to reflect the current transcription state. */
    fun setState(state: State) {
        currentState = state
        overlayView?.let { updateAppearance(state) }
    }

    private fun updateAppearance(state: State) {
        val view = overlayView ?: return
        pulseAnimator?.cancel()

        val bg = GradientDrawable().apply { shape = GradientDrawable.OVAL }

        when (state) {
            State.IDLE -> {
                bg.setColor(0xCC1A73E8.toInt()) // Blue semi-transparent
                view.background = bg
            }
            State.RECORDING -> {
                bg.setColor(0xCCE53935.toInt()) // Red semi-transparent
                view.background = bg
                // Pulse animation
                val animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.5f, 1f)
                animator.duration = 800
                animator.repeatCount = ValueAnimator.INFINITE
                animator.interpolator = LinearInterpolator()
                animator.start()
                pulseAnimator = animator
            }
            State.PROCESSING -> {
                bg.setColor(0xCCFF8F00.toInt()) // Amber semi-transparent
                view.background = bg
            }
        }
    }

    private fun buildMicButton(params: WindowManager.LayoutParams): ImageView {
        return ImageView(context).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(20, 20, 20, 20)
            setColorFilter(Color.WHITE)

            var initialX = 0
            var initialY = 0
            var touchStartX = 0f
            var touchStartY = 0f
            var moved = false

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        touchStartX = event.rawX
                        touchStartY = event.rawY
                        moved = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - touchStartX).toInt()
                        val dy = (event.rawY - touchStartY).toInt()
                        if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                            moved = true
                            params.x = initialX - dx
                            params.y = initialY + dy
                            windowManager.updateViewLayout(overlayView, params)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
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
