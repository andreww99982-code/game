package com.rogerprod.salonsmena

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SaveManager {
    private const val PREFS = "salon_smena_save"

    fun saveProgress(ctx: Context, levelId: Int, charId: CharacterId,
                     score: Int, rank: Rank) {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "lvl_${levelId}_${charId.name}"
        val old = prefs.getInt(key + "_score", 0)
        if (score > old) {
            prefs.edit()
                .putInt(key + "_score", score)
                .putString(key + "_rank", rank.name)
                .apply()
        }
        // unlock next level
        val unlocked = prefs.getInt("unlocked", 1)
        if (levelId >= unlocked) prefs.edit().putInt("unlocked", levelId + 1).apply()
    }

    fun getBestScore(ctx: Context, levelId: Int, charId: CharacterId): Int {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getInt("lvl_${levelId}_${charId.name}_score", 0)
    }

    fun getBestRank(ctx: Context, levelId: Int, charId: CharacterId): Rank? {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val s = prefs.getString("lvl_${levelId}_${charId.name}_rank", null) ?: return null
        return Rank.valueOf(s)
    }

    fun getUnlockedLevel(ctx: Context): Int {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("unlocked", 1)
    }

    fun getLeaderboard(ctx: Context): List<Pair<String, Int>> {
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return CharacterId.values().map { charId ->
            val total = GameData.levels.sumOf { lvl ->
                prefs.getInt("lvl_${lvl.id}_${charId.name}_score", 0)
            }
            charId.name to total
        }.sortedByDescending { it.second }
    }
}
