package com.rogerprod.salonsmena

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val charId = CharacterId.valueOf(intent.getStringExtra("char_id") ?: CharacterId.KIRILL.name)
        val locId  = LocationId.valueOf(intent.getStringExtra("loc_id") ?: LocationId.MEGAFONCHIK.name)
        val levelId = intent.getIntExtra("level_id", 1)
        val level = GameData.levels.firstOrNull { it.id == levelId } ?: GameData.levels.first()
        val char  = GameData.getCharacter(charId)
        val loc   = GameData.getLocation(locId)

        gameView = GameView(this, char, loc, level) { result ->
            SaveManager.saveProgress(this, result.levelId, result.characterId, result.score, result.rank)
            val i = Intent(this, GameResultActivity::class.java).apply {
                putExtra("score", result.score)
                putExtra("rank", result.rank.name)
                putExtra("revenue", result.revenue)
                putExtra("combo", result.combo)
                putExtra("char_id", charId.name)
                putExtra("loc_id", locId.name)
                putExtra("level_id", levelId)
            }
            startActivity(i)
            finish()
        }
        setContentView(gameView)
    }

    override fun onPause()  { super.onPause();  gameView.pause() }
    override fun onResume() { super.onResume(); gameView.resume() }
}
