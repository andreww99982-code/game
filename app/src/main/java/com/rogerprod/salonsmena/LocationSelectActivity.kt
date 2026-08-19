package com.rogerprod.salonsmena

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class LocationSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val charId = CharacterId.valueOf(intent.getStringExtra("char_id") ?: CharacterId.KIRILL.name)
        setContentView(LocationSelectView(this, charId) { locId ->
            val intent2 = Intent(this, GameActivity::class.java)
            intent2.putExtra("char_id", charId.name)
            intent2.putExtra("loc_id", locId.name)
            intent2.putExtra("level_id", 1)
            startActivity(intent2)
        })
    }
}

class LocationSelectView(
    context: android.content.Context,
    private val charId: CharacterId,
    private val onSelect: (LocationId) -> Unit
) : View(context) {

    private var animTick = 0f
    private val locCards = mutableListOf<LocCard>()
    private val homeLocId = GameData.getHomeLocation(charId)

    init { post(::tick) }
    private fun tick() { animTick += 0.02f; invalidate(); postDelayed(::tick, 16) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        locCards.clear()
        val locs = GameData.locations
        val cardW = w * 0.85f
        val cardH = h * 0.12f
        locs.forEachIndexed { i, loc ->
            locCards += LocCard(loc, w * 0.5f, h * 0.25f + i * (cardH + h * 0.02f), cardW, cardH)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val bgP = Paint(); bgP.shader = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Color.rgb(10, 5, 30), Color.rgb(20, 8, 50)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgP)

        val char = GameData.getCharacter(charId)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = h * 0.04f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.WHITE }
        canvas.drawText("Выбери локацию", w / 2f, h * 0.1f, tp)

        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = h * 0.025f; color = Color.argb(180, 233, 150, 100) }
        canvas.drawText("Персонаж: ${char.name}", w / 2f, h * 0.16f, sp)

        for (card in locCards) card.draw(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            for (card in locCards) {
                if (card.contains(event.x, event.y)) {
                    onSelect(card.loc.id)
                    return true
                }
            }
        }
        return true
    }

    private inner class LocCard(val loc: Location, val cx: Float, val cy: Float, val cw: Float, val ch: Float) {
        fun contains(x: Float, y: Float) = RectF(cx - cw/2, cy - ch/2, cx + cw/2, cy + ch/2).contains(x, y)
        fun draw(canvas: Canvas) {
            val r = RectF(cx - cw/2, cy - ch/2, cx + cw/2, cy + ch/2)
            val isHome = loc.id == homeLocId
            val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(r.left, r.top, r.right, r.bottom,
                    if (isHome) intArrayOf(Color.rgb(233, 69, 96), Color.rgb(120, 20, 50))
                    else intArrayOf(Color.rgb(40, 20, 80), Color.rgb(20, 10, 40)),
                    null, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(r, 16f, 16f, bgP)
            val np = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = ch * 0.38f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.WHITE }
            canvas.drawText(loc.name, r.left + 24f, cy + ch * 0.12f, np)
            val dp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = ch * 0.25f; color = Color.argb(180, 220, 220, 220) }
            canvas.drawText(loc.description, r.left + 24f, cy + ch * 0.43f, dp)
            if (isHome) {
                val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    textSize = ch * 0.25f; color = Color.rgb(255, 220, 80) }
                canvas.drawText("★ Профильная локация (+20% бонус)", r.right - 20f - 280f, cy + ch * 0.12f, bp)
            }
        }
    }
}
