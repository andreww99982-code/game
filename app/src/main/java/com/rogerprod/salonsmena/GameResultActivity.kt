package com.rogerprod.salonsmena

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class GameResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val score   = intent.getIntExtra("score", 0)
        val rank    = Rank.valueOf(intent.getStringExtra("rank") ?: Rank.C.name)
        val revenue = intent.getIntExtra("revenue", 0)
        val combo   = intent.getIntExtra("combo", 0)
        val charId  = CharacterId.valueOf(intent.getStringExtra("char_id") ?: CharacterId.KIRILL.name)
        val locId   = LocationId.valueOf(intent.getStringExtra("loc_id") ?: LocationId.MEGAFONCHIK.name)
        val levelId = intent.getIntExtra("level_id", 1)

        setContentView(ResultView(this, score, rank, revenue, combo, charId, locId, levelId) { action ->
            when (action) {
                "retry" -> {
                    val i = Intent(this, GameActivity::class.java).apply {
                        putExtra("char_id", charId.name); putExtra("loc_id", locId.name)
                        putExtra("level_id", levelId)
                    }
                    startActivity(i); finish()
                }
                "menu"  -> { startActivity(Intent(this, MainActivity::class.java)); finish() }
                "next"  -> {
                    val nextLevel = GameData.levels.firstOrNull { it.id == levelId + 1 }
                    if (nextLevel != null) {
                        val i = Intent(this, GameActivity::class.java).apply {
                            putExtra("char_id", charId.name); putExtra("loc_id", nextLevel.locationId.name)
                            putExtra("level_id", nextLevel.id)
                        }
                        startActivity(i); finish()
                    } else {
                        startActivity(Intent(this, MainActivity::class.java)); finish()
                    }
                }
            }
        })
    }
}

class ResultView(
    context: android.content.Context,
    private val score: Int, private val rank: Rank,
    private val revenue: Int, private val combo: Int,
    private val charId: CharacterId, private val locId: LocationId, private val levelId: Int,
    private val onAction: (String) -> Unit
) : View(context) {

    private var animTick = 0f
    private var scoreAnim = 0f
    private val buttons = mutableListOf<Triple<String, Float, Float>>()

    init { post(::tick) }
    private fun tick() {
        animTick += 0.03f
        scoreAnim = (scoreAnim + score / 60f).coerceAtMost(score.toFloat())
        invalidate(); postDelayed(::tick, 16)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        buttons.clear()
        buttons += Triple("retry", w * 0.22f, h * 0.82f)
        buttons += Triple("menu",  w * 0.5f,  h * 0.82f)
        buttons += Triple("next",  w * 0.78f, h * 0.82f)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        // BG
        val bgP = Paint(); bgP.shader = RadialGradient(w/2f, h/2f, h*0.7f,
            intArrayOf(Color.rgb(30,10,60), Color.rgb(10,5,20)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgP)

        // Rank badge
        val rankColor = when (rank) { Rank.S -> Color.rgb(255,215,0); Rank.A -> Color.rgb(200,120,255);
            Rank.B -> Color.rgb(100,200,255); Rank.C -> Color.rgb(180,180,180) }
        val rp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = rankColor }
        canvas.drawCircle(w/2f, h*0.22f, 72f, rp)
        val rkP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign=Paint.Align.CENTER; textSize=80f; color=Color.rgb(20,10,40)
            typeface=Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText(rank.name, w/2f, h*0.22f+28f, rkP)

        // Title
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign=Paint.Align.CENTER; textSize=h*0.05f; color=Color.WHITE
            typeface=Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("Смена окончена!", w/2f, h*0.38f, tp)

        // Stats
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign=Paint.Align.CENTER; textSize=h*0.038f; color=Color.rgb(255,220,60) }
        canvas.drawText("${scoreAnim.toInt()} очков", w/2f, h*0.48f, sp)
        sp.color = Color.rgb(80,200,80)
        canvas.drawText("Выручка: ${revenue}₽", w/2f, h*0.55f, sp)
        sp.color = Color.rgb(255,140,50)
        if (combo > 1) canvas.drawText("Макс. комбо: ×${combo}", w/2f, h*0.62f, sp)

        // Char/loc
        val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign=Paint.Align.CENTER; textSize=h*0.025f; color=Color.argb(160,200,200,200) }
        val char = GameData.getCharacter(charId); val loc = GameData.getLocation(locId)
        canvas.drawText("${char.name} • ${loc.name}", w/2f, h*0.69f, cp)

        // Buttons
        for ((act, bx, by) in buttons) drawButton(canvas, act, bx, by, w*0.28f, h*0.07f)
    }

    private fun drawButton(canvas: Canvas, action: String, cx: Float, cy: Float, bw: Float, bh: Float) {
        val label = when (action) { "retry" -> "↺ Ещё раз"; "menu" -> "⌂ Меню"; else -> "▶ Дальше" }
        val color = when (action) { "next" -> Color.rgb(233, 69, 96); else -> Color.rgb(60,40,100) }
        val r = RectF(cx-bw/2, cy-bh/2, cx+bw/2, cy+bh/2)
        canvas.drawRoundRect(r, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
        canvas.drawText(label, cx, cy+bh*0.18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign=Paint.Align.CENTER; textSize=bh*0.4f; this.color=Color.WHITE
            typeface=Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            for ((act, bx, by) in buttons) {
                val bw = width * 0.28f; val bh = height * 0.07f
                if (RectF(bx-bw/2, by-bh/2, bx+bw/2, by+bh/2).contains(event.x, event.y)) {
                    onAction(act); return true
                }
            }
        }
        return true
    }
}
