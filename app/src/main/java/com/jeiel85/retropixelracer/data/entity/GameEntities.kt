package com.jeiel85.retropixelracer.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "singleton",
    val coins: Int = 500, // Starts with some coins to upgrade first
    val engineLevel: Int = 1,
    val handlingLevel: Int = 1,
    val speedLevel: Int = 1,
    val selectedCarIndex: Int = 0,
    val soundEnabled: Boolean = true,
    val targetFps: Int = 60,
    val screenShakeEnabled: Boolean = true,
    val particlesDensity: Float = 0.5f,
    val lastClaimedDate: String = "" // YYYY-MM-DD
)

@Entity(tableName = "daily_missions")
data class DailyMission(
    @PrimaryKey val id: String,
    val title: String,
    val type: String, // "RACE", "COIN", "UPGRADE", "SPEED"
    val progress: Int,
    val target: Int,
    val rewardCoins: Int,
    val completed: Boolean = false,
    val claimed: Boolean = false
)

@Entity(tableName = "track_states")
data class TrackState(
    @PrimaryKey val trackId: String,
    val name: String,
    val theme: String, // "FOREST", "DESERT", "NEON", "SNOW"
    val difficulty: String, // "EASY", "NORMAL", "HARD"
    val lengthMeters: Int,
    val unlocked: Boolean,
    val bestTimeMillis: Long = 0L
)

@Entity(tableName = "leaderboards")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trackId: String,
    val playerName: String,
    val carName: String,
    val timeMillis: Long,
    val isUser: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis()
)
