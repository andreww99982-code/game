package com.rogerprod.salonsmena

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class MainMenuView(context: Context, private val onAction: (String) -> Unit) : View(context) {

    private var animTick = 0f
    private val buttons = mutableListOf<MenuButton>()

    private val bgPaint = Paint()
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.WHITE
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.argb(180, 233, 69, 96)
    }

    init { post(::tick) }

    private fun tick() {
        animTick += 0.02f
        invalidate()
        postDelayed(::tick, 16)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        val cx = w / 2f
        buttons.clear()
        buttons += MenuButton("▶  Играть", cx, h * 0.55f, w * 0.6f, 56f, "play")
        buttons += MenuButton("🏆  Рекорды", cx, h * 0.68f, w * 0.6f, 56f, "leaderboard")
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f

        // Animated background
        val grad = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Color.rgb(10, 5, 30), Color.rgb(30, 10, 60), Color.rgb(10, 5, 30)),
            null, Shader.TileMode.CLAMP)
        bgPaint.shader = grad
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Floating orbs
        for (i in 0..4) {
            val ox = cx + sin(animTick + i * 1.2f) * w * 0.35f
            val oy = h * 0.3f + cos(animTick * 0.7f + i) * h * 0.1f
            val orbGrad = RadialGradient(ox, oy, 60f,
                intArrayOf(Color.argb(60, 233, 69, 96), Color.TRANSPARENT),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(ox, oy, 60f, Paint().apply { shader = orbGrad })
        }

        // Title
        titlePaint.textSize = h * 0.07f
        canvas.drawText("Салонная Смена", cx, h * 0.28f, titlePaint)

        // Sub
        subPaint.textSize = h * 0.028f
        canvas.drawText("Симулятор продавца", cx, h * 0.35f, subPaint)

        // Pulse ring
        val pulse = sin(animTick * 2f) * 0.5f + 0.5f
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f + pulse * 3f
            color = Color.argb((80 + (pulse * 60).toInt()), 233, 69, 96)
        }
        canvas.drawCircle(cx, h * 0.2f, 55f + pulse * 15f, ringPaint)

        // Roger Production credit
        val creditPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = h * 0.022f
            color = Color.argb(120, 200, 200, 200)
        }
        canvas.drawText("Roger Production  •  2025", cx, h * 0.94f, creditPaint)

        // Buttons
        for (btn in buttons) btn.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            for (btn in buttons) {
                if (btn.contains(event.x, event.y)) {
                    onAction(btn.action)
                    return true
                }
            }
        }
        return true
    }

    private inner class MenuButton(
        val label: String, val cx: Float, val cy: Float,
        val width: Float, val height: Float, val action: String
    ) {
        private val rect get() = RectF(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2)
        fun contains(x: Float, y: Float) = rect.contains(x, y)
        fun draw(canvas: Canvas) {
            val r = rect
            val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(r.left, r.top, r.right, r.bottom,
                    intArrayOf(Color.rgb(233, 69, 96), Color.rgb(150, 30, 60)),
                    null, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(r, 28f, 28f, bgP)
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.WHITE; textSize = height * 0.45f
            }
            canvas.drawText(label, cx, cy + height * 0.15f, tp)
        }
    }
}
