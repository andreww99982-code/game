package com.rogerprod.salonsmena

import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class LeaderboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val board = SaveManager.getLeaderboard(this)
        setContentView(LeaderboardView(this, board) { finish() })
    }
}

class LeaderboardView(
    context: android.content.Context,
    private val board: List<Pair<String, Int>>,
    private val onBack: () -> Unit
) : View(context) {

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val bgP = Paint(); bgP.shader = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Color.rgb(10,5,30), Color.rgb(20,8,50)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgP)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign=Paint.Align.CENTER; textSize=h*0.048f; color=Color.WHITE
            typeface=Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("🏆 Рекорды", w/2f, h*0.1f, tp)

        val medals = listOf("🥇", "🥈", "🥉", "4️⃣")
        board.forEachIndexed { i, (charName, total) ->
            val y = h * 0.22f + i * h * 0.15f
            val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (i) {
                    0 -> Color.argb(80, 255, 215, 0); 1 -> Color.argb(60, 200, 200, 200)
                    2 -> Color.argb(60, 200, 100, 50); else -> Color.argb(40, 100, 100, 100)
                }
            }
            canvas.drawRoundRect(RectF(w*0.08f, y-h*0.05f, w*0.92f, y+h*0.08f), 14f, 14f, rp)
            val ep = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign=Paint.Align.LEFT; textSize=h*0.06f; color=Color.WHITE }
            canvas.drawText(medals.getOrElse(i){"${i+1}."}, w*0.1f, y+h*0.04f, ep)
            val charDisplay = when (charName) {
                "KIRILL" -> "Кирилл"; "ZHENYA" -> "Женя"; "TANYA" -> "Таня"; "YULIA" -> "Юля"; else -> charName }
            val np = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign=Paint.Align.LEFT; textSize=h*0.04f; color=Color.WHITE
                typeface=Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText(charDisplay, w*0.22f, y+h*0.025f, np)
            val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign=Paint.Align.RIGHT; textSize=h*0.04f; color=Color.rgb(255,220,60) }
            canvas.drawText("${total} очков", w*0.92f, y+h*0.025f, sp)
        }

        // Back button
        val br = RectF(w*0.3f, h*0.88f, w*0.7f, h*0.95f)
        canvas.drawRoundRect(br, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.rgb(60,30,100) })
        canvas.drawText("← Назад", w/2f, h*0.925f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign=Paint.Align.CENTER; textSize=h*0.035f; color=Color.WHITE })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val br = RectF(width*0.3f, height*0.88f, width*0.7f, height*0.95f)
            if (br.contains(event.x, event.y)) { onBack(); return true }
        }
        return true
    }
}
