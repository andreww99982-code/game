package com.rogerprod.salonsmena

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.*
import kotlin.random.Random

class SplashView(context: Context) : View(context) {

    private var animProgress = 0f  // 0..1
    private var callback: (() -> Unit)? = null
    private var started = false

    private val particles = List(60) { Particle() }

    private val bgPaint = Paint()
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.WHITE
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(180, 233, 69, 96)
    }

    fun startAnimation(onDone: () -> Unit) {
        callback = onDone
        started = true
        startAnimating()
    }

    private fun startAnimating() {
        val startTime = System.currentTimeMillis()
        val duration = 2000L
        post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                animProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                particles.forEach { it.update(width, height) }
                invalidate()
                if (animProgress < 1f) postDelayed(this, 16)
                else callback?.invoke()
            }
        })
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val cx = w / 2f; val cy = h / 2f

        // Background gradient
        val grad = RadialGradient(cx, cy, h * 0.6f,
            intArrayOf(Color.rgb(40, 10, 60), Color.rgb(10, 5, 20), Color.BLACK),
            floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        bgPaint.shader = grad
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Particles
        for (p in particles) {
            val alpha = (p.life * 255 * animProgress).toInt().coerceIn(0, 255)
            val pPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(alpha, p.r, p.g, p.b)
                style = Paint.Style.FILL
            }
            canvas.drawCircle(p.x, p.y, p.size, pPaint)
        }

        // Cinematic bars (top/bottom)
        val barH = h * 0.1f * (1f - animProgress.coerceAtMost(0.5f) * 2)
        canvas.drawRect(0f, 0f, w, barH, Paint().apply { color = Color.BLACK })
        canvas.drawRect(0f, h - barH, w, h, Paint().apply { color = Color.BLACK })

        // Glowing circle behind text
        val circleAlpha = (animProgress * 220).toInt().coerceIn(0, 220)
        val circleRadius = 80f + animProgress * 60f
        val glowGrad = RadialGradient(cx, cy, circleRadius,
            intArrayOf(Color.argb(circleAlpha, 233, 69, 96), Color.TRANSPARENT),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
        glowPaint.shader = glowGrad
        canvas.drawCircle(cx, cy, circleRadius, glowPaint)

        // Decorative lines
        if (animProgress > 0.2f) {
            val lineAlpha = ((animProgress - 0.2f) / 0.8f * 180).toInt().coerceIn(0, 180)
            linePaint.alpha = lineAlpha
            val len = w * 0.3f * ((animProgress - 0.2f) / 0.8f)
            canvas.drawLine(cx - len, cy, cx - 20f, cy, linePaint)
            canvas.drawLine(cx + 20f, cy, cx + len, cy, linePaint)
        }

        // "Roger" text
        if (animProgress > 0.15f) {
            val a1 = ((animProgress - 0.15f) / 0.35f).coerceIn(0f, 1f)
            textPaint.alpha = (a1 * 255).toInt()
            textPaint.textSize = h * 0.07f
            val yOff = cy - h * 0.06f - (1f - a1) * 30f
            canvas.drawText("Roger", cx, yOff, textPaint)
        }

        // "Production" text
        if (animProgress > 0.4f) {
            val a2 = ((animProgress - 0.4f) / 0.35f).coerceIn(0f, 1f)
            textPaint.alpha = (a2 * 255).toInt()
            textPaint.textSize = h * 0.04f
            val letterSpacingPaint = Paint(textPaint).apply {
                textSize = h * 0.04f; letterSpacing = 0.3f; alpha = (a2 * 255).toInt()
                color = Color.argb((a2 * 200).toInt(), 233, 69, 96)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("P R O D U C T I O N", cx, cy + h * 0.04f, letterSpacingPaint)
        }

        // Presents text fade-in at end
        if (animProgress > 0.75f) {
            val a3 = ((animProgress - 0.75f) / 0.25f).coerceIn(0f, 1f)
            textPaint.alpha = (a3 * 180).toInt()
            textPaint.textSize = h * 0.025f
            textPaint.color = Color.WHITE
            canvas.drawText("представляет", cx, cy + h * 0.12f, textPaint)
        }
    }

    private inner class Particle {
        var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
        var size = 0f; var life = 0f
        var r = 0; var g = 0; var b = 0
        init { respawn(800, 1600) }
        fun respawn(w: Int, h: Int) {
            x = Random.nextFloat() * w; y = Random.nextFloat() * h
            vx = (Random.nextFloat() - 0.5f) * 1.5f
            vy = (Random.nextFloat() - 0.5f) * 1.5f
            size = Random.nextFloat() * 4f + 1f; life = Random.nextFloat()
            val colors = listOf(
                Triple(233, 69, 96), Triple(150, 50, 200),
                Triple(80, 130, 255), Triple(255, 200, 80))
            val c = colors.random()
            r = c.first; g = c.second; b = c.third
        }
        fun update(w: Int, h: Int) {
            x += vx; y += vy; life -= 0.008f
            if (life <= 0f) respawn(w, h)
        }
    }
}
