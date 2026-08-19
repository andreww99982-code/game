package com.rogerprod.salonsmena

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = MainMenuView(this) { action ->
            when (action) {
                "play" -> startActivity(Intent(this, CharacterSelectActivity::class.java))
                "leaderboard" -> startActivity(Intent(this, LeaderboardActivity::class.java))
            }
        }
        setContentView(view)
    }
}
