package com.jeiel85.retropixelracer.data.repository

import com.jeiel85.retropixelracer.data.dao.GameDao
import com.jeiel85.retropixelracer.data.entity.UserProfile
import com.jeiel85.retropixelracer.data.entity.DailyMission
import com.jeiel85.retropixelracer.data.entity.TrackState
import com.jeiel85.retropixelracer.data.entity.LeaderboardEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class GameRepository(private val gameDao: GameDao) {

    val userProfile: Flow<UserProfile?> = gameDao.getUserProfileFlow()
    val trackStates: Flow<List<TrackState>> = gameDao.getTrackStatesFlow()
    val dailyMissions: Flow<List<DailyMission>> = gameDao.getDailyMissionsFlow()

    fun getLeaderboardEntries(trackId: String): Flow<List<LeaderboardEntry>> =
        gameDao.getLeaderboardEntriesFlow(trackId)

    suspend fun getActiveUserProfile(): UserProfile? = gameDao.getUserProfile()

    suspend fun initializeDefaultData() {
        // 1. Initialize empty UserProfile if null
        val profile = gameDao.getUserProfile()
        if (profile == null) {
            gameDao.insertUserProfile(UserProfile())
        }

        // 2. Initialize Track States
        val existingTracks = gameDao.getTrackStatesFlow().firstOrNull()
        if (existingTracks.isNullOrEmpty()) {
            val defaultTracks = listOf(
                TrackState("track_forest", "Green Valley", "FOREST", "EASY", 1200, unlocked = true),
                TrackState("track_desert", "Dusty Ruins", "DESERT", "NORMAL", 1800, unlocked = false),
                TrackState("track_neon", "Pixel Neon Grid", "NEON", "HARD", 2400, unlocked = false),
                TrackState("track_snow", "Chilling Glacier", "SNOW", "HARD", 3000, unlocked = false)
            )
            gameDao.insertTrackStates(defaultTracks)

            // Populate some initial leaderboard records for a real worldwide feel!
            val botRacerNames = listOf("RetroGamer", "NeonShifter", "ApexViper", "ChronoRider", "SpeedyGons", "PixelDon", "TurboTaro")
            val cars = listOf("Classic RX", "Interceptor", "Neon GT", "Formula One")
            
            for (track in defaultTracks) {
                var baseTime = when (track.trackId) {
                    "track_forest" -> 55000L // 55 seconds
                    "track_desert" -> 85000L // 1:25
                    "track_neon" -> 115000L // 1:55
                    else -> 145000L // 2:25
                }
                
                // Add 5 bot run records for each track
                for (i in 0 until 5) {
                    val variation = i * 4000L + Random.nextLong(1000, 3000)
                    val botTime = baseTime + variation
                    val botName = botRacerNames[i % botRacerNames.size]
                    val botCar = cars[Random.nextInt(cars.size)]
                    gameDao.insertLeaderboardEntry(
                        LeaderboardEntry(
                            trackId = track.trackId,
                            playerName = botName,
                            carName = botCar,
                            timeMillis = botTime,
                            isUser = false
                        )
                    )
                }
            }
        }

        // 3. Initialize Daily Missions
        val existingMissions = gameDao.getDailyMissionsFlow().firstOrNull()
        if (existingMissions.isNullOrEmpty()) {
            val defaultMissions = listOf(
                DailyMission("mission_race", "Complete 2 Races", "RACE", 0, 2, 150),
                DailyMission("mission_coin", "Collect 300 Coins Total", "COIN", 0, 300, 200),
                DailyMission("mission_upgrade", "Perform 1 Car Upgrade", "UPGRADE", 0, 1, 250),
                DailyMission("mission_speed", "Reach Max Speed 180 km/h", "SPEED", 0, 180, 200)
            )
            gameDao.insertDailyMissions(defaultMissions)
        }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        gameDao.insertUserProfile(profile)
    }

    suspend fun addCoins(amount: Int) {
        val profile = gameDao.getUserProfile() ?: UserProfile()
        val updated = profile.copy(coins = profile.coins + amount)
        gameDao.insertUserProfile(updated)
        incrementMissionProgress("mission_coin", amount)
    }

    suspend fun buyUpgrade(type: String): Boolean {
        val profile = gameDao.getUserProfile() ?: return false
        
        val level = when (type) {
            "ENGINE" -> profile.engineLevel
            "HANDLING" -> profile.handlingLevel
            "SPEED" -> profile.speedLevel
            else -> return false
        }
        
        if (level >= 5) return false // Max level 5
        
        val cost = level * 200 // Level 1->2 costs 200, 2->3 costs 400, etc.
        if (profile.coins >= cost) {
            val updatedProfile = when (type) {
                "ENGINE" -> profile.copy(coins = profile.coins - cost, engineLevel = level + 1)
                "HANDLING" -> profile.copy(coins = profile.coins - cost, handlingLevel = level + 1)
                "SPEED" -> profile.copy(coins = profile.coins - cost, speedLevel = level + 1)
                else -> profile
            }
            gameDao.insertUserProfile(updatedProfile)
            incrementMissionProgress("mission_upgrade", 1)
            return true
        }
        return false
    }

    suspend fun unlockTrack(trackId: String, cost: Int): Boolean {
        val profile = gameDao.getUserProfile() ?: return false
        if (profile.coins >= cost) {
            val track = gameDao.getTrackState(trackId)
            if (track != null && !track.unlocked) {
                // Deduct coins
                gameDao.insertUserProfile(profile.copy(coins = profile.coins - cost))
                // Unlock track
                gameDao.updateTrackState(track.copy(unlocked = true))
                return true
            }
        }
        return false
    }

    suspend fun saveRaceTime(trackId: String, timeMillis: Long, carName: String) {
        // Save to leaderboard as user
        gameDao.insertLeaderboardEntry(
            LeaderboardEntry(
                trackId = trackId,
                playerName = "You (Driver)",
                carName = carName,
                timeMillis = timeMillis,
                isUser = true
            )
        )

        // Check if this is the best time for the track and update TrackState
        val track = gameDao.getTrackState(trackId)
        if (track != null) {
            if (track.bestTimeMillis == 0L || timeMillis < track.bestTimeMillis) {
                gameDao.updateTrackState(track.copy(bestTimeMillis = timeMillis))
            }
        }

        // Increment race missions
        incrementMissionProgress("mission_race", 1)
    }

    suspend fun checkMaxSpeedReached(speedKmph: Int) {
        val mission = gameDao.getDailyMission("mission_speed") ?: return
        if (!mission.completed && speedKmph >= mission.target) {
            val updated = mission.copy(
                progress = speedKmph.coerceAtMost(mission.target),
                completed = true
            )
            gameDao.updateDailyMission(updated)
        }
    }

    private suspend fun incrementMissionProgress(missionId: String, amount: Int) {
        val mission = gameDao.getDailyMission(missionId) ?: return
        if (mission.completed) return

        val newProgress = (mission.progress + amount).coerceAtMost(mission.target)
        val completed = newProgress >= mission.target
        val updated = mission.copy(
            progress = newProgress,
            completed = completed
        )
        gameDao.updateDailyMission(updated)
    }

    suspend fun claimMissionReward(missionId: String): Int {
        val mission = gameDao.getDailyMission(missionId) ?: return 0
        if (mission.completed && !mission.claimed) {
            // Give reward coins
            addCoins(mission.rewardCoins)
            // Mark claimed
            gameDao.updateDailyMission(mission.copy(claimed = true))
            return mission.rewardCoins
        }
        return 0
    }

    suspend fun resetDailyMissionsIfNewDay(currentDateStr: String) {
        val profile = gameDao.getUserProfile() ?: return
        if (profile.lastClaimedDate != currentDateStr) {
            // It's a new day! Reset daily missions
            val defaultMissions = listOf(
                DailyMission("mission_race", "Complete 2 Races", "RACE", 0, 2, 150),
                DailyMission("mission_coin", "Collect 300 Coins Total", "COIN", 0, 300, 200),
                DailyMission("mission_upgrade", "Perform 1 Car Upgrade", "UPGRADE", 0, 1, 250),
                DailyMission("mission_speed", "Reach Max Speed 180 km/h", "SPEED", 0, 180, 200)
            )
            gameDao.insertDailyMissions(defaultMissions)
            
            // Save state date
            gameDao.insertUserProfile(profile.copy(lastClaimedDate = currentDateStr))
        }
    }
}
