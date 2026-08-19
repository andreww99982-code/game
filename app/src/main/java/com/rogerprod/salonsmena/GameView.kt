package com.rogerprod.salonsmena

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────
//  Main Game View — pseudo-3D with perspective, physics, queue
// ─────────────────────────────────────────────────────────────
class GameView(
    context: Context,
    private val char: Character,
    private val loc: Location,
    private val level: LevelDef,
    private val onLevelEnd: (LevelResult) -> Unit
) : View(context) {

    // ── State ────────────────────────────────────────────────
    private var score = 0
    private var revenue = 0
    private var combo = 0
    private var maxCombo = 0
    private var timeLeft = level.timeLimitSec.toFloat()
    private var running = true
    private var paused = false
    private var lastTime = System.currentTimeMillis()

    private var playerX = 0f
    private var playerY = 0f
    private var playerVX = 0f
    private var playerVY = 0f
    private val playerSpeed = 280f * (0.5f + char.speedBonus * 0.5f)

    private val customers = mutableListOf<GameCustomer>()
    private val obstacles  = mutableListOf<Obstacle>()
    private var activeCustomer: GameCustomer? = null
    private var actionPanel: ActionPanel? = null
    private var resultMessage: String? = null
    private var resultAlpha = 0f

    private val homeBonus = if (loc.id == GameData.getHomeLocation(char.id)) 1.2f else 1.0f
    private var spawnedCount = 0
    private var servedCount  = 0

    // ── Paint pool ──────────────────────────────────────────
    private val bgP   = Paint()
    private val floorP = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wallP  = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textP  = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; color = Color.WHITE }
    private val hudP   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val redP   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(233, 69, 96) }
    private val greenP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 200, 80) }
    private val yellowP= Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 220, 60) }

    private var dirX = 0f; private var dirY = 0f  // joystick direction
    private var joyCx = 0f; private var joyCy = 0f
    private var joyActive = false; private var joyPointer = -1

    // ── Scene geometry ───────────────────────────────────────
    private var sceneTop = 0f; private var sceneH = 0f
    private var vp = Vanishing(0f, 0f) // vanishing point for pseudo-3D

    // ── Init ────────────────────────────────────────────────
    init { post(::gameLoop) }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        sceneTop = h * 0.10f
        sceneH = h * 0.55f
        vp = Vanishing(w / 2f, sceneTop + sceneH * 0.3f)
        playerX = w * 0.5f; playerY = sceneTop + sceneH * 0.75f
        joyCx = w * 0.2f; joyCy = h * 0.82f
        setupObstacles(w)
        spawnInitialCustomers(w)
    }

    private fun setupObstacles(w: Int) {
        obstacles.clear()
        val sceneW = w.toFloat()
        when (loc.id) {
            LocationId.MEGAFONCHIK, LocationId.INTERSVYAZ, LocationId.BILAN_PLUS -> {
                // Counter/showcase
                obstacles += Obstacle(sceneW*0.15f, sceneTop+sceneH*0.2f, sceneW*0.35f, sceneH*0.08f, Color.rgb(60,60,120), "Витрина")
                obstacles += Obstacle(sceneW*0.55f, sceneTop+sceneH*0.2f, sceneW*0.3f,  sceneH*0.08f, Color.rgb(60,60,120), "Стенд")
                obstacles += Obstacle(sceneW*0.25f, sceneTop+sceneH*0.6f, sceneW*0.5f,  sceneH*0.07f, Color.rgb(80,40,100), "Касса")
            }
            LocationId.MONETKA -> {
                obstacles += Obstacle(sceneW*0.05f, sceneTop+sceneH*0.1f, sceneW*0.2f, sceneH*0.7f, Color.rgb(40,80,40), "Полка")
                obstacles += Obstacle(sceneW*0.3f,  sceneTop+sceneH*0.1f, sceneW*0.2f, sceneH*0.5f, Color.rgb(40,80,40), "Полка")
                obstacles += Obstacle(sceneW*0.6f,  sceneTop+sceneH*0.55f, sceneW*0.35f, sceneH*0.1f, Color.rgb(100,60,40), "Касса")
            }
            LocationId.MAGNET_KOSMETIK -> {
                obstacles += Obstacle(sceneW*0.05f, sceneTop+sceneH*0.1f, sceneW*0.18f, sceneH*0.6f, Color.rgb(80,40,100), "Стеллаж")
                obstacles += Obstacle(sceneW*0.3f,  sceneTop+sceneH*0.1f, sceneW*0.18f, sceneH*0.45f, Color.rgb(80,40,100), "Витрина")
                obstacles += Obstacle(sceneW*0.55f, sceneTop+sceneH*0.1f, sceneW*0.18f, sceneH*0.45f, Color.rgb(80,40,100), "Стеллаж")
                obstacles += Obstacle(sceneW*0.2f,  sceneTop+sceneH*0.6f, sceneW*0.6f,  sceneH*0.1f,  Color.rgb(120,50,130), "Касса")
            }
        }
    }

    private fun spawnInitialCustomers(w: Int) {
        val queueX = w * 0.85f
        val queueStartY = sceneTop + sceneH * 0.25f
        val toSpawn = minOf(3, level.clientCount)
        repeat(toSpawn) { i ->
            customers += GameCustomer(spawnedCount++, queueX, queueStartY + i * sceneH * 0.18f, level)
        }
    }

    // ── Game Loop ────────────────────────────────────────────
    private fun gameLoop() {
        if (!running) return
        val now = System.currentTimeMillis()
        val dt = (now - lastTime) / 1000f
        lastTime = now
        if (!paused) update(dt)
        invalidate()
        postDelayed(::gameLoop, 16)
    }

    private fun update(dt: Float) {
        // Timer
        timeLeft -= dt
        if (timeLeft <= 0f) { timeLeft = 0f; endLevel() }

        // Player physics
        val ax = dirX * playerSpeed; val ay = dirY * playerSpeed
        playerVX = playerVX * 0.7f + ax * 0.3f
        playerVY = playerVY * 0.7f + ay * 0.3f
        var nx = playerX + playerVX * dt
        var ny = playerY + playerVY * dt
        nx = nx.coerceIn(40f, width - 40f)
        ny = ny.coerceIn(sceneTop + 10f, sceneTop + sceneH - 30f)
        // Obstacle collision
        for (obs in obstacles) {
            val ob = RectF(obs.x, obs.y, obs.x + obs.w, obs.y + obs.h)
            if (ob.contains(nx, ny)) {
                if (!ob.contains(playerX, ny)) ny = playerY
                if (!ob.contains(nx, playerY)) nx = playerX
            }
        }
        playerX = nx; playerY = ny

        // Customer logic
        val queueX = width * 0.85f
        val queueBaseY = sceneTop + sceneH * 0.25f
        customers.forEachIndexed { idx, c ->
            c.update(dt, queueX, queueBaseY + idx * sceneH * 0.18f)
        }
        customers.removeAll { c -> c.patience <= 0f && c != activeCustomer.also { if (c.patience <= 0f && c == activeCustomer) dismissActiveCustomer() } }

        // Spawn new customers
        if (spawnedCount < level.clientCount && customers.size < 5) {
            customers += GameCustomer(spawnedCount++, queueX, queueBaseY + customers.size * sceneH * 0.18f, level)
        }

        // Check proximity to first customer
        if (activeCustomer == null && customers.isNotEmpty()) {
            val fc = customers.first()
            val dist = hypot(playerX - fc.x, playerY - fc.y)
            if (dist < sceneH * 0.22f) showActionPanel(fc)
        }

        // Result message fade
        if (resultAlpha > 0f) resultAlpha = (resultAlpha - dt * 1.5f).coerceAtLeast(0f)
    }

    private fun showActionPanel(c: GameCustomer) {
        activeCustomer = c
        actionPanel = ActionPanel(c.need, char, loc, width, height)
    }

    private fun dismissActiveCustomer() {
        combo = 0
        activeCustomer?.let { customers.remove(it) }
        activeCustomer = null; actionPanel = null
    }

    fun pause()  { paused = true }
    fun resume() { paused = false; lastTime = System.currentTimeMillis() }

    // ── Draw ────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        drawBackground(canvas, w, h)
        drawScene(canvas, w, h)
        drawCustomers(canvas)
        drawPlayer(canvas)
        actionPanel?.draw(canvas, w, h)
        drawHUD(canvas, w, h)
        drawJoystick(canvas)
        resultMessage?.let { drawResultMessage(canvas, it, w, h) }
        if (timeLeft <= 0f) drawOverlay(canvas, w, h)
    }

    private fun drawBackground(canvas: Canvas, w: Float, h: Float) {
        bgP.shader = LinearGradient(0f, 0f, 0f, h,
            intArrayOf(Color.rgb(8, 4, 20), Color.rgb(15, 8, 35)), null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, bgP)
    }

    private fun drawScene(canvas: Canvas, w: Float, h: Float) {
        val bottom = sceneTop + sceneH
        // Floor pseudo-3D
        val floorPath = Path()
        floorPath.moveTo(0f, bottom)
        floorPath.lineTo(w, bottom)
        floorPath.lineTo(vp.x + w*0.3f, vp.y)
        floorPath.lineTo(vp.x - w*0.3f, vp.y)
        floorPath.close()
        floorP.shader = when (loc.id) {
            LocationId.MONETKA         -> LinearGradient(0f, vp.y, 0f, bottom, intArrayOf(Color.rgb(50,80,50), Color.rgb(80,120,60)), null, Shader.TileMode.CLAMP)
            LocationId.MAGNET_KOSMETIK -> LinearGradient(0f, vp.y, 0f, bottom, intArrayOf(Color.rgb(60,30,80), Color.rgb(100,50,120)), null, Shader.TileMode.CLAMP)
            else                       -> LinearGradient(0f, vp.y, 0f, bottom, intArrayOf(Color.rgb(40,40,80), Color.rgb(60,60,100)), null, Shader.TileMode.CLAMP)
        }
        canvas.drawPath(floorPath, floorP)

        // Floor grid lines for depth
        val gridP = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.argb(40, 255, 255, 255) }
        for (i in 1..5) {
            val t = i / 6f
            val lx1 = lerp(0f, vp.x - w*0.3f, t); val ly1 = lerp(bottom, vp.y, t)
            val lx2 = lerp(w, vp.x + w*0.3f, t);   val ly2 = lerp(bottom, vp.y, t)
            canvas.drawLine(lx1, ly1, lx2, ly2, gridP)
        }

        // Back wall
        wallP.shader = when (loc.id) {
            LocationId.MONETKA         -> LinearGradient(0f, sceneTop, 0f, vp.y, intArrayOf(Color.rgb(200,220,200), Color.rgb(150,180,150)), null, Shader.TileMode.CLAMP)
            LocationId.MAGNET_KOSMETIK -> LinearGradient(0f, sceneTop, 0f, vp.y, intArrayOf(Color.rgb(220,180,230), Color.rgb(170,130,190)), null, Shader.TileMode.CLAMP)
            else                       -> LinearGradient(0f, sceneTop, 0f, vp.y, intArrayOf(Color.rgb(180,190,220), Color.rgb(140,150,190)), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, sceneTop, w, vp.y, wallP)

        // Store name sign
        val signP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(200, 233, 69, 96) }
        canvas.drawRoundRect(RectF(w*0.3f, sceneTop+4f, w*0.7f, sceneTop+28f), 8f, 8f, signP)
        textP.textSize = 18f; textP.color = Color.WHITE
        canvas.drawText(loc.name, w/2f, sceneTop+20f, textP)

        // Obstacles (shelves, counter, etc.)
        for (obs in obstacles) {
            val obP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = obs.color }
            canvas.drawRoundRect(RectF(obs.x, obs.y, obs.x+obs.w, obs.y+obs.h), 8f, 8f, obP)
            // 3D top face
            val topP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lighten(obs.color) }
            val topPath = Path()
            topPath.moveTo(obs.x, obs.y); topPath.lineTo(obs.x+obs.w, obs.y)
            topPath.lineTo(lerp(obs.x+obs.w, vp.x, 0.15f), lerp(obs.y, vp.y, 0.15f))
            topPath.lineTo(lerp(obs.x, vp.x, 0.15f), lerp(obs.y, vp.y, 0.15f))
            topPath.close()
            canvas.drawPath(topPath, topP)
            // Label
            val lbP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; color = Color.WHITE; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText(obs.label, obs.x + obs.w/2f, obs.y + obs.h/2f + 5f, lbP)
        }
    }

    private fun drawCustomers(canvas: Canvas) {
        for (c in customers) {
            val depthScale = depthScale(c.y)
            val r = 24f * depthScale
            // Shadow
            val shadowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 0,0,0) }
            canvas.drawOval(RectF(c.x-r*1.2f, c.y+r*0.8f, c.x+r*1.2f, c.y+r*1.2f), shadowP)
            // Body
            val bodyC = customerColor(c.mood)
            val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bodyC }
            canvas.drawCircle(c.x, c.y, r, bodyP)
            // Patience bar
            val barW = r * 2f; val barH = 5f * depthScale
            val barX = c.x - r; val barY = c.y - r - 12f * depthScale
            canvas.drawRect(barX, barY, barX+barW, barY+barH, Paint().apply { color = Color.argb(100,255,0,0) })
            canvas.drawRect(barX, barY, barX+barW*c.patience, barY+barH, Paint().apply { color = Color.rgb(60,200,60) })
            // Need icon
            val iconP = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f * depthScale; textAlign = Paint.Align.CENTER; color = Color.WHITE }
            canvas.drawText(needEmoji(c.need), c.x, c.y + 6f * depthScale, iconP)
            // Secret buyer mark
            if (c.isSecret) {
                val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; textAlign = Paint.Align.CENTER; color = Color.rgb(255,220,0) }
                canvas.drawText("👁", c.x, c.y - r - 15f*depthScale, sp)
            }
        }
    }

    private fun drawPlayer(canvas: Canvas) {
        val depthScale = depthScale(playerY)
        val r = 26f * depthScale
        val shadowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(100, 0,0,0) }
        canvas.drawOval(RectF(playerX-r*1.3f, playerY+r*0.9f, playerX+r*1.3f, playerY+r*1.3f), shadowP)

        // Draw character-specific look
        when (char.id) {
            CharacterId.KIRILL -> {
                // Chubby body
                val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 100, 200) }
                canvas.drawOval(RectF(playerX-r*0.9f, playerY-r*0.4f, playerX+r*0.9f, playerY+r*1.2f), bodyP)
                val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 220, 177) }
                canvas.drawCircle(playerX, playerY-r*0.3f, r*0.65f, skinP)
                // Curly hair dots
                val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 50, 20) }
                for (i in 0..5) { val a = (i*60f)*PI.toFloat()/180f
                    canvas.drawCircle(playerX + cos(a)*r*0.55f, playerY-r*0.3f+sin(a)*r*0.45f-r*0.1f, r*0.18f, hairP) }
            }
            CharacterId.ZHENYA -> {
                val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(40, 160, 80) }
                canvas.drawRoundRect(RectF(playerX-r*0.55f, playerY-r*0.2f, playerX+r*0.55f, playerY+r), 8f, 8f, bodyP)
                val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 220, 177) }
                canvas.drawCircle(playerX, playerY-r*0.5f, r*0.5f, skinP)
                val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(50, 40, 30) }
                canvas.drawRect(playerX-r*0.5f, playerY-r, playerX+r*0.5f, playerY-r*0.65f, hairP)
            }
            CharacterId.TANYA -> {
                val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(200, 50, 100) }
                canvas.drawRoundRect(RectF(playerX-r*0.6f, playerY-r*0.2f, playerX+r*0.6f, playerY+r), 10f, 10f, bodyP)
                val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 220, 185) }
                canvas.drawCircle(playerX, playerY-r*0.45f, r*0.52f, skinP)
                val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(100, 50, 20) }
                val hp = Path(); hp.addArc(RectF(playerX-r*0.52f, playerY-r, playerX+r*0.52f, playerY-r*0.45f), 180f, 180f)
                canvas.drawPath(hp, Paint(Paint.ANTI_ALIAS_FLAG).apply { style=Paint.Style.FILL; color=Color.rgb(100,50,20) })
            }
            CharacterId.YULIA -> {
                val bodyP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(150, 50, 200) }
                canvas.drawRoundRect(RectF(playerX-r*0.58f, playerY-r*0.2f, playerX+r*0.58f, playerY+r), 10f, 10f, bodyP)
                val skinP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 225, 190) }
                canvas.drawCircle(playerX, playerY-r*0.45f, r*0.5f, skinP)
                val hairP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(180, 80, 40) }
                canvas.drawRect(playerX-r*0.5f, playerY-r, playerX+r*0.5f, playerY-r*0.35f, hairP)
                canvas.drawRect(playerX-r*0.62f, playerY-r*0.6f, playerX-r*0.45f, playerY+r*0.5f, hairP)
                canvas.drawRect(playerX+r*0.45f, playerY-r*0.6f, playerX+r*0.62f, playerY+r*0.5f, hairP)
            }
        }
    }

    private fun drawHUD(canvas: Canvas, w: Float, h: Float) {
        // HUD bar
        val hudBar = Paint().apply { color = Color.argb(180, 10, 5, 30) }
        canvas.drawRect(0f, 0f, w, sceneTop, hudBar)

        // Timer
        val timerColor = if (timeLeft < 30f) Color.rgb(233, 69, 96) else Color.WHITE
        textP.textSize = 22f; textP.color = timerColor
        canvas.drawText("⏱ ${timeLeft.toInt()}с", w*0.15f, sceneTop*0.7f, textP)

        // Score
        textP.color = Color.rgb(255,220,60)
        canvas.drawText("${score}очк", w*0.5f, sceneTop*0.7f, textP)

        // Revenue
        textP.color = Color.rgb(80,200,80)
        canvas.drawText("${revenue}₽", w*0.82f, sceneTop*0.7f, textP)

        // Combo
        if (combo > 1) {
            val cp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f; textAlign = Paint.Align.CENTER
                color = Color.rgb(255,180,0)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("COMBO ×${combo}!", w*0.5f, sceneTop + 32f, cp)
        }

        // Level info
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; textAlign = Paint.Align.LEFT; color = Color.argb(160, 220,220,220) }
        canvas.drawText("${level.title} • ${char.name} • ${loc.shortName}", 12f, h*0.97f, lp)

        // Queue warning
        if (customers.size >= 4) {
            val wp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; textAlign = Paint.Align.CENTER; color = Color.rgb(255, 100, 60) }
            canvas.drawText("⚠ Очередь! Спеши!", w*0.5f, sceneTop + sceneH + 18f, wp)
        }
    }

    private fun drawJoystick(canvas: Canvas) {
        val baseP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255,255,255); style = Paint.Style.FILL }
        val stickP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 233, 69, 96) }
        canvas.drawCircle(joyCx, joyCy, 55f, baseP)
        val stickX = joyCx + dirX * 30f
        val stickY = joyCy + dirY * 30f
        canvas.drawCircle(stickX, stickY, 28f, stickP)
    }

    private fun drawResultMessage(canvas: Canvas, msg: String, w: Float, h: Float) {
        if (resultAlpha <= 0f) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 30f; textAlign = Paint.Align.CENTER
            color = Color.argb((resultAlpha * 255).toInt(), 255, 220, 60)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(msg, w/2f, h*0.45f, p)
    }

    private fun drawOverlay(canvas: Canvas, w: Float, h: Float) {
        canvas.drawRect(0f, 0f, w, h, Paint().apply { color = Color.argb(180, 0,0,0) })
        textP.textSize = 36f; textP.color = Color.WHITE
        canvas.drawText("СМЕНА ОКОНЧЕНА", w/2f, h*0.4f, textP)
        textP.textSize = 24f; textP.color = Color.rgb(255,220,60)
        canvas.drawText("Итого: ${score} очков", w/2f, h*0.5f, textP)
        textP.color = Color.rgb(80,200,80)
        canvas.drawText("Выручка: ${revenue}₽", w/2f, h*0.58f, textP)
        textP.color = Color.WHITE; textP.textSize = 18f
        canvas.drawText("Нажми для продолжения", w/2f, h*0.72f, textP)
    }

    // ── Touch ────────────────────────────────────────────────
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val idx = event.actionIndex
                val x = event.getX(idx); val y = event.getY(idx)
                // Check action panel first
                actionPanel?.let { ap ->
                    val result = ap.onTouch(x, y)
                    if (result != null) { handleAction(result); return true }
                }
                // Joystick
                if (hypot(x - joyCx, y - joyCy) < 80f) {
                    joyActive = true; joyPointer = event.getPointerId(idx)
                    updateJoy(x, y)
                }
                // End screen tap
                if (timeLeft <= 0f) { endLevel(); return true }
            }
            MotionEvent.ACTION_MOVE -> {
                if (joyActive) {
                    val pIdx = event.findPointerIndex(joyPointer)
                    if (pIdx >= 0) updateJoy(event.getX(pIdx), event.getY(pIdx))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pId = event.getPointerId(event.actionIndex)
                if (pId == joyPointer) { joyActive = false; dirX = 0f; dirY = 0f }
            }
        }
        return true
    }

    private fun updateJoy(x: Float, y: Float) {
        val dx = x - joyCx; val dy = y - joyCy
        val dist = hypot(dx, dy).coerceAtLeast(1f)
        val norm = dist.coerceAtMost(55f)
        dirX = dx / dist; dirY = dy / dist
    }

    // ── Actions ───────────────────────────────────────────────
    private fun handleAction(action: GameAction) {
        val c = activeCustomer ?: return
        val baseScore = action.baseScore
        val bonusMultiplier = when {
            action.matchesNeed(c.need) && char.id == action.bestChar -> 1.5f
            action.matchesNeed(c.need) -> 1.2f
            else -> 0.5f
        } * homeBonus

        val isCorrect = action.matchesNeed(c.need)
        if (isCorrect) {
            combo++
            maxCombo = maxOf(maxCombo, combo)
            val comboMult = 1f + (combo - 1) * 0.15f
            val earned = (baseScore * bonusMultiplier * comboMult).toInt()
            score += earned
            revenue += (action.revenue * bonusMultiplier).toInt()
            resultMessage = when {
                combo >= 5 -> "🔥 PERFECT ×${combo}! +${earned}"
                combo >= 3 -> "⚡ COMBO ×${combo}! +${earned}"
                else -> "✓ +${earned}"
            }
        } else {
            combo = 0
            score = (score - 50).coerceAtLeast(0)
            resultMessage = "✗ Не то! -50"
        }
        resultAlpha = 1f
        customers.remove(c)
        activeCustomer = null; actionPanel = null
        servedCount++
    }

    private fun endLevel() {
        if (!running) return
        running = false
        val rank = GameData.getRank(score, level.clientCount * 400)
        onLevelEnd(LevelResult(level.id, char.id, loc.id, score, revenue, 0f, rank, maxCombo, level.timeLimitSec - timeLeft.toInt()))
    }

    // ── Helpers ──────────────────────────────────────────────
    private fun depthScale(y: Float): Float {
        val t = ((y - sceneTop) / sceneH).coerceIn(0f, 1f)
        return 0.5f + t * 0.6f
    }
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun lighten(color: Int): Int {
        val r = ((Color.red(color) + 60).coerceAtMost(255))
        val g = ((Color.green(color) + 60).coerceAtMost(255))
        val b = ((Color.blue(color) + 60).coerceAtMost(255))
        return Color.rgb(r, g, b)
    }
    private fun customerColor(mood: CustomerMood) = when (mood) {
        CustomerMood.HAPPY   -> Color.rgb(80, 200, 100)
        CustomerMood.NEUTRAL -> Color.rgb(100, 140, 200)
        CustomerMood.ANNOYED -> Color.rgb(220, 150, 50)
        CustomerMood.ANGRY   -> Color.rgb(220, 60, 60)
        CustomerMood.CONFUSED-> Color.rgb(160, 100, 200)
    }
    private fun needEmoji(need: CustomerNeed) = when (need) {
        CustomerNeed.BUY_PHONE     -> "📱"
        CustomerNeed.BUY_TARIFF    -> "📶"
        CustomerNeed.BUY_ACCESSORY -> "🎧"
        CustomerNeed.INSTALLMENT   -> "💳"
        CustomerNeed.DATA_TRANSFER -> "💾"
        CustomerNeed.BUY_PRODUCT   -> "🛒"
        CustomerNeed.LOYALTY_CARD  -> "🎁"
        CustomerNeed.RETURN        -> "↩"
        CustomerNeed.COSMETICS     -> "💄"
        CustomerNeed.GIFT_SET      -> "🎀"
        CustomerNeed.COMPLAINT     -> "😤"
        CustomerNeed.PRICE_CHECK   -> "🏷"
    }
}

// ── Supporting classes ───────────────────────────────────────

data class Vanishing(val x: Float, val y: Float)

data class Obstacle(val x: Float, val y: Float, val w: Float, val h: Float, val color: Int, val label: String)

class GameCustomer(val id: Int, var x: Float, var y: Float, level: LevelDef) {
    val need: CustomerNeed
    var patience = 1.0f
    val mood: CustomerMood
    val isSecret: Boolean

    init {
        val needs = needsForLevel(level)
        need = needs[id % needs.size]
        mood = CustomerMood.values().random()
        isSecret = level.specialCondition.contains("тайн", ignoreCase = true) && id == level.clientCount / 2
        patience = if (mood == CustomerMood.ANGRY) 0.6f else 1.0f
    }

    var targetX = x; var targetY = y

    fun update(dt: Float, queueX: Float, queueTargetY: Float) {
        targetX = queueX; targetY = queueTargetY
        val dx = targetX - x; val dy = targetY - y
        val dist = hypot(dx, dy)
        if (dist > 2f) { x += dx / dist * 60f * dt; y += dy / dist * 60f * dt }
        // Patience drain
        patience -= dt * (0.02f + if (mood == CustomerMood.ANGRY) 0.03f else 0f)
        patience = patience.coerceAtLeast(0f)
    }

    private fun needsForLevel(level: LevelDef): List<CustomerNeed> = when (level.locationId) {
        LocationId.MEGAFONCHIK, LocationId.INTERSVYAZ, LocationId.BILAN_PLUS ->
            listOf(CustomerNeed.BUY_PHONE, CustomerNeed.BUY_TARIFF, CustomerNeed.BUY_ACCESSORY,
                CustomerNeed.INSTALLMENT, CustomerNeed.DATA_TRANSFER, CustomerNeed.COMPLAINT)
        LocationId.MONETKA ->
            listOf(CustomerNeed.BUY_PRODUCT, CustomerNeed.LOYALTY_CARD, CustomerNeed.RETURN,
                CustomerNeed.COMPLAINT, CustomerNeed.PRICE_CHECK)
        LocationId.MAGNET_KOSMETIK ->
            listOf(CustomerNeed.COSMETICS, CustomerNeed.GIFT_SET, CustomerNeed.LOYALTY_CARD,
                CustomerNeed.RETURN, CustomerNeed.COMPLAINT)
    }
}

data class GameAction(
    val label: String,
    val baseScore: Int,
    val revenue: Int,
    val matchingNeeds: List<CustomerNeed>,
    val bestChar: CharacterId
) {
    fun matchesNeed(need: CustomerNeed) = need in matchingNeeds
}

class ActionPanel(need: CustomerNeed, char: Character, loc: Location, private val sw: Int, private val sh: Int) {
    private val actions: List<GameAction> = buildActions(loc.id)
    private val buttons = mutableListOf<ActionButton>()

    init {
        val panelH = sh * 0.28f
        val panelY = sh * 0.70f
        val btnW = sw * 0.42f; val btnH = panelH * 0.22f
        actions.forEachIndexed { i, act ->
            val col = i % 2; val row = i / 2
            val bx = sw * 0.05f + col * (btnW + sw * 0.06f)
            val by = panelY + sh * 0.02f + row * (btnH + sh * 0.01f)
            buttons += ActionButton(act, bx + btnW/2f, by + btnH/2f, btnW, btnH)
        }
    }

    fun onTouch(x: Float, y: Float): GameAction? {
        for (btn in buttons) if (btn.contains(x, y)) return btn.action
        return null
    }

    fun draw(canvas: Canvas, w: Float, h: Float) {
        val panelR = RectF(0f, h*0.68f, w, h)
        val bgP = Paint().apply { color = Color.argb(210, 10, 5, 30) }
        canvas.drawRect(panelR, bgP)
        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER; textSize = h*0.028f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.WHITE }
        canvas.drawText("Что делаем?", w/2f, h*0.705f, titleP)
        for (btn in buttons) btn.draw(canvas)
    }

    private fun buildActions(locId: LocationId): List<GameAction> = when (locId) {
        LocationId.MEGAFONCHIK, LocationId.INTERSVYAZ, LocationId.BILAN_PLUS -> listOf(
            GameAction("📱 Продать телефон", 200, 15000, listOf(CustomerNeed.BUY_PHONE), CharacterId.KIRILL),
            GameAction("📶 Предложить тариф", 150, 800, listOf(CustomerNeed.BUY_TARIFF), CharacterId.KIRILL),
            GameAction("🎧 Аксессуар", 120, 1200, listOf(CustomerNeed.BUY_ACCESSORY), CharacterId.KIRILL),
            GameAction("💳 Оформить рассрочку", 180, 20000, listOf(CustomerNeed.INSTALLMENT), CharacterId.ZHENYA),
            GameAction("💾 Перенести данные", 100, 500, listOf(CustomerNeed.DATA_TRANSFER), CharacterId.ZHENYA),
            GameAction("😌 Разрешить конфликт", 160, 0, listOf(CustomerNeed.COMPLAINT), CharacterId.KIRILL)
        )
        LocationId.MONETKA -> listOf(
            GameAction("🛒 Пробить товар", 120, 600, listOf(CustomerNeed.BUY_PRODUCT), CharacterId.TANYA),
            GameAction("🎁 Карта лояльности", 100, 0, listOf(CustomerNeed.LOYALTY_CARD), CharacterId.TANYA),
            GameAction("↩ Оформить возврат", 140, -300, listOf(CustomerNeed.RETURN), CharacterId.TANYA),
            GameAction("😌 Успокоить клиента", 160, 0, listOf(CustomerNeed.COMPLAINT), CharacterId.TANYA),
            GameAction("🏷 Проверить ценник", 90, 0, listOf(CustomerNeed.PRICE_CHECK), CharacterId.TANYA),
            GameAction("💳 Скидка по карте", 80, -100, listOf(CustomerNeed.LOYALTY_CARD, CustomerNeed.BUY_PRODUCT), CharacterId.TANYA)
        )
        LocationId.MAGNET_KOSMETIK -> listOf(
            GameAction("💄 Подобрать косметику", 160, 1200, listOf(CustomerNeed.COSMETICS), CharacterId.YULIA),
            GameAction("🎀 Подарочный набор", 200, 2500, listOf(CustomerNeed.GIFT_SET), CharacterId.YULIA),
            GameAction("🎁 Карта лояльности", 100, 0, listOf(CustomerNeed.LOYALTY_CARD), CharacterId.YULIA),
            GameAction("↩ Оформить возврат", 130, -400, listOf(CustomerNeed.RETURN), CharacterId.YULIA),
            GameAction("😌 Разрешить конфликт", 160, 0, listOf(CustomerNeed.COMPLAINT), CharacterId.YULIA),
            GameAction("🏷 Акция 2+1", 180, 1800, listOf(CustomerNeed.COSMETICS, CustomerNeed.GIFT_SET), CharacterId.YULIA)
        )
    }

    private inner class ActionButton(val action: GameAction, val cx: Float, val cy: Float, val w: Float, val h: Float) {
        fun contains(x: Float, y: Float) = RectF(cx-w/2, cy-h/2, cx+w/2, cy+h/2).contains(x, y)
        fun draw(canvas: Canvas) {
            val r = RectF(cx-w/2, cy-h/2, cx+w/2, cy+h/2)
            val bgP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(r.left, r.top, r.right, r.bottom,
                    intArrayOf(Color.rgb(60, 30, 100), Color.rgb(30, 10, 60)), null, Shader.TileMode.CLAMP) }
            canvas.drawRoundRect(r, 12f, 12f, bgP)
            val borderP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style=Paint.Style.STROKE; strokeWidth=1.5f; color=Color.argb(120,233,69,96) }
            canvas.drawRoundRect(r, 12f, 12f, borderP)
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = h*0.38f; textAlign = Paint.Align.CENTER; color = Color.WHITE }
            canvas.drawText(action.label, cx, cy+h*0.13f, tp)
        }
    }
}
