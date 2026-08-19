package com.rogerprod.salonsmena

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class CharacterSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(CharacterSelectView(this) { charId ->
            val intent = Intent(this, LocationSelectActivity::class.java)
            intent.putExtra("char_id", charId.name)
            startActivity(intent)
        })
    }
}

class CharacterSelectView(context: android.content.Context, private val onSelect: (CharacterId) -> Unit) : View(context) {
    private var animTick = 0f
    private var selectedCard = -1
    private val cards = mutableListOf<CharCard>()

    init { post(::tick) }
    private fun tick() { animTick += 0.025f; invalidate(); postDelayed(::tick, 16) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        cards.clear()
        val cardW = w * 0.44f
        val cardH = h * 0.34f
        val chars = GameData.characters
        val positions = listOf(
            Pair(w * 0.25f, h * 0.35f), Pair(w * 0.75f, h * 0.35f),
            Pair(w * 0.25f, h * 0.72f), Pair(w * 0.75f, h * 0.72f)
        )
        chars.forEachIndexed { i, c ->
            cards += CharCard(c, positions[i].first, positions[i].second, cardW, cardH)
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        // BG
        val bgP = Paint(); bgP.shader = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Color.rgb(10, 5, 30), Color.rgb(20, 8, 50)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgP)
        // Title
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = h * 0.045f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.WHITE }
        canvas.drawText("Выбери персонажа", w / 2f, h * 0.1f, tp)
        val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = h * 0.025f; color = Color.argb(160,200,200,200) }
        canvas.drawText("Нажми на карточку", w / 2f, h * 0.15f, sp)
        for ((i, card) in cards.withIndex()) card.draw(canvas, animTick, i == selectedCard)
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            for ((i, card) in cards.withIndex()) {
                if (card.contains(event.x, event.y)) {
                    selectedCard = i
                    invalidate()
                    postDelayed({ onSelect(card.char.id) }, 200)
                    return true
                }
            }
        }
        return true
    }

    private inner class CharCard(val char: Character, val cx: Float, val cy: Float, val cw: Float, val ch: Float) {
        fun contains(x: Float, y: Float) = RectF(cx - cw/2, cy - ch/2, cx + cw/2, cy + ch/2).contains(x, y)
        fun draw(canvas: Canvas, tick: Float, selected: Boolean) {
            val rect = RectF(cx - cw/2, cy - ch/2, cx + cw/2, cy + ch/2)
            val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                    if (selected) intArrayOf(Color.rgb(233, 69, 96), Color.rgb(100, 20, 50))
                    else intArrayOf(Color.rgb(40, 20, 70), Color.rgb(20, 10, 40)),
                    null, Shader.TileMode.CLAMP)
            }
            canvas.drawRoundRect(rect, 20f, 20f, bgP)
            if (selected) {
                val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 4f; color = Color.argb(200, 233, 69, 96) }
                canvas.drawRoundRect(rect, 20f, 20f, glow)
            }
            // Character avatar (pseudo-3D drawn shape)
            drawCharacterAvatar(canvas, char.id, cx, cy - ch * 0.1f, cw * 0.35f, ch * 0.4f)
            // Name
            val np = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER; textSize = ch * 0.14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.WHITE }
            canvas.drawText(char.name, cx, cy + ch * 0.35f, np)
            // Stats bars
            drawStatBars(canvas, rect, char)
        }

        private fun drawStatBars(canvas: Canvas, rect: RectF, c: Character) {
            val stats = listOf("Скорость" to c.speedBonus, "Убеждение" to c.persuasionBonus,
                "Конфликты" to c.conflictBonus, "Допродажи" to c.upsellBonus)
            val barW = rect.width() * 0.75f
            val barH = rect.height() * 0.035f
            val startX = rect.centerX() - barW / 2
            var y = rect.bottom - rect.height() * 0.28f
            val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = rect.height() * 0.055f; color = Color.argb(200, 220, 220, 220) }
            val barBgP = Paint().apply { color = Color.argb(80, 255, 255, 255) }
            val barFgP = Paint().apply { color = Color.argb(220, 233, 69, 96) }
            for ((name, value) in stats) {
                canvas.drawText(name, startX, y - 2f, labelP)
                canvas.drawRoundRect(RectF(startX, y, startX + barW, y + barH), 4f, 4f, barBgP)
                canvas.drawRoundRect(RectF(startX, y, startX + barW * value, y + barH), 4f, 4f, barFgP)
                y += barH + rect.height() * 0.055f
            }
        }
    }

    private fun drawCharacterAvatar(canvas: Canvas, id: CharacterId, cx: Float, cy: Float, w: Float, h: Float) {
        when (id) {
            CharacterId.KIRILL -> drawKirill(canvas, cx, cy, w, h)
            CharacterId.ZHENYA -> drawZhenya(canvas, cx, cy, w, h)
            CharacterId.TANYA  -> drawTanya(canvas, cx, cy, w, h)
            CharacterId.YULIA  -> drawYulia(canvas, cx, cy, w, h)
        }
    }

    private fun drawKirill(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        // Chubby curly-haired guy - bigger body
        val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 100, 200) }
        canvas.drawOval(RectF(cx - w*0.45f, cy, cx + w*0.45f, cy + h*0.7f), bodyP)
        val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 220, 177) }
        canvas.drawCircle(cx, cy - h*0.05f, w*0.38f, skinP)
        // Curly hair - multiple small circles
        val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 50, 20) }
        for (i in 0..7) {
            val angle = (i * 45f - 90f) * Math.PI.toFloat() / 180f
            val hx = cx + kotlin.math.cos(angle) * w * 0.3f
            val hy = (cy - h*0.05f) + kotlin.math.sin(angle) * w * 0.3f - w*0.05f
            canvas.drawCircle(hx, hy, w*0.1f, hairP)
        }
        canvas.drawCircle(cx, cy - h*0.05f - w*0.3f, w*0.12f, hairP)
        // Eyes
        val eyeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 40, 20) }
        canvas.drawCircle(cx - w*0.13f, cy - h*0.07f, w*0.07f, eyeP)
        canvas.drawCircle(cx + w*0.13f, cy - h*0.07f, w*0.07f, eyeP)
        // Smile
        val smilePath = Path()
        smilePath.moveTo(cx - w*0.15f, cy + h*0.02f)
        smilePath.quadTo(cx, cy + h*0.08f, cx + w*0.15f, cy + h*0.02f)
        canvas.drawPath(smilePath, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.rgb(180, 80, 60) })
    }

    private fun drawZhenya(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        // Small, slim, fast-looking
        val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 160, 80) }
        canvas.drawRoundRect(RectF(cx - w*0.28f, cy + h*0.05f, cx + w*0.28f, cy + h*0.7f), 12f, 12f, bodyP)
        val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 220, 177) }
        canvas.drawCircle(cx, cy, w*0.28f, skinP)
        val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(50, 40, 30) }
        canvas.drawRect(cx - w*0.28f, cy - w*0.28f, cx + w*0.28f, cy - w*0.08f, hairP)
        val eyeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 40, 20) }
        canvas.drawCircle(cx - w*0.1f, cy - w*0.04f, w*0.055f, eyeP)
        canvas.drawCircle(cx + w*0.1f, cy - w*0.04f, w*0.055f, eyeP)
    }

    private fun drawTanya(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        // Calm cashier girl
        val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(200, 50, 100) }
        canvas.drawRoundRect(RectF(cx - w*0.35f, cy + h*0.05f, cx + w*0.35f, cy + h*0.72f), 14f, 14f, bodyP)
        val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 220, 185) }
        canvas.drawCircle(cx, cy, w*0.32f, skinP)
        val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100, 50, 20) }
        val hairPath = Path()
        hairPath.addArc(RectF(cx - w*0.32f, cy - w*0.32f, cx + w*0.32f, cy), 180f, 180f)
        canvas.drawPath(hairPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.rgb(100, 50, 20) })
        // Bun
        canvas.drawCircle(cx, cy - w*0.32f, w*0.14f, hairP)
        val eyeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 50, 30) }
        canvas.drawCircle(cx - w*0.11f, cy - w*0.05f, w*0.055f, eyeP)
        canvas.drawCircle(cx + w*0.11f, cy - w*0.05f, w*0.055f, eyeP)
        // Apron
        val apronP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(cx - w*0.18f, cy + h*0.15f, cx + w*0.18f, cy + h*0.65f, apronP)
    }

    private fun drawYulia(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        // Stylish cosmetics girl
        val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(150, 50, 200) }
        canvas.drawRoundRect(RectF(cx - w*0.33f, cy + h*0.05f, cx + w*0.33f, cy + h*0.72f), 14f, 14f, bodyP)
        val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 225, 190) }
        canvas.drawCircle(cx, cy, w*0.31f, skinP)
        // Long styled hair
        val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(180, 80, 40) }
        canvas.drawRect(cx - w*0.31f, cy - w*0.31f, cx + w*0.31f, cy + w*0.1f, hairP)
        canvas.drawRect(cx - w*0.35f, cy - w*0.1f, cx - w*0.28f, cy + h*0.4f, hairP)
        canvas.drawRect(cx + w*0.28f, cy - w*0.1f, cx + w*0.35f, cy + h*0.4f, hairP)
        val eyeP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 30, 80) }
        canvas.drawCircle(cx - w*0.11f, cy - w*0.04f, w*0.06f, eyeP)
        canvas.drawCircle(cx + w*0.11f, cy - w*0.04f, w*0.06f, eyeP)
        // Lipstick
        val lipP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(200, 40, 80) }
        canvas.drawOval(RectF(cx - w*0.1f, cy + w*0.07f, cx + w*0.1f, cy + w*0.14f), lipP)
    }
}
