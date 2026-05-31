package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.GameDatabase
import com.example.data.entity.UserProfile
import com.example.data.entity.DailyMission
import com.example.data.entity.TrackState
import com.example.data.entity.LeaderboardEntry
import com.example.data.repository.GameRepository
import com.example.sound.RetroSoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    val soundManager = RetroSoundManager()

    // Database Observables
    val userProfile: StateFlow<UserProfile?>
    val trackStates: StateFlow<List<TrackState>>
    val dailyMissions: StateFlow<List<DailyMission>>

    // Selected track for leaderboard or racing
    private val _selectedTrackId = MutableStateFlow<String?>("track_forest")
    val selectedTrackId = _selectedTrackId.asStateFlow()

    // Dynamic Leaderboard list for selected track
    val currentLeaderboard: StateFlow<List<LeaderboardEntry>>

    // Active Screen Route/State
    private val _gameState = MutableStateFlow(GameState.MAIN_MENU)
    val gameState = _gameState.asStateFlow()

    // Selected car color/style index (0: Red, 1: Blue, 2: Yellow, 3: Green)
    private val _carIndex = MutableStateFlow(0)
    val carIndex = _carIndex.asStateFlow()

    // Multiplayer Matchmaking State
    private val _lobbyState = MutableStateFlow<LobbyState>(LobbyState.Idle)
    val lobbyState = _lobbyState.asStateFlow()

    enum class GameState {
        MAIN_MENU,
        GARAGE,
        TRACK_SELECTION,
        DAILY_MISSIONS,
        LEADERBOARD,
        LOBBY,
        RACING,
        SETTINGS
    }

    sealed interface LobbyState {
        object Idle : LobbyState
        object Searching : LobbyState
        data class Found(val opponents: List<String>) : LobbyState
        object Ready : LobbyState
    }

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())

        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        trackStates = repository.trackStates.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        dailyMissions = repository.dailyMissions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        @Suppress("OPT_IN_USAGE")
        currentLeaderboard = _selectedTrackId.flatMapLatest { trackId ->
            if (trackId != null) {
                repository.getLeaderboardEntries(trackId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize and clean up profile/settings on startup
        viewModelScope.launch {
            repository.initializeDefaultData()
            refreshDailyMissions()
            
            // Sync initial sound preferences
            userProfile.collect { profile ->
                if (profile != null) {
                    soundManager.setMute(!profile.soundEnabled)
                    _carIndex.value = profile.selectedCarIndex
                }
            }
        }
    }

    fun selectTrack(trackId: String) {
        _selectedTrackId.value = trackId
    }

    fun setGameState(state: GameState) {
        _gameState.value = state
        if (state == GameState.MAIN_MENU) {
            // Restart catchy menu theme
            soundManager.startBgm()
        } else if (state == GameState.RACING) {
            soundManager.stopBgm() // Stops menu BGM when racing (racing uses active audio engine sound effects!)
        }
    }

    private suspend fun refreshDailyMissions() {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = format.format(Date())
        repository.resetDailyMissionsIfNewDay(todayStr)
    }

    // --- Garage / Upgrades Actions ---
    fun upgradeEngine() {
        viewModelScope.launch {
            val success = repository.buyUpgrade("ENGINE")
            if (success) {
                soundManager.playUpgradeSfx()
            }
        }
    }

    fun upgradeHandling() {
        viewModelScope.launch {
            val success = repository.buyUpgrade("HANDLING")
            if (success) {
                soundManager.playUpgradeSfx()
            }
        }
    }

    fun upgradeSpeed() {
        viewModelScope.launch {
            val success = repository.buyUpgrade("SPEED")
            if (success) {
                soundManager.playUpgradeSfx()
            }
        }
    }

    fun selectCar(index: Int) {
        _carIndex.value = index
        viewModelScope.launch {
            val profile = repository.getActiveUserProfile()
            if (profile != null) {
                repository.saveUserProfile(profile.copy(selectedCarIndex = index))
            }
        }
    }

    // --- Track Selection ---
    fun unlockTrack(trackId: String, cost: Int) {
        viewModelScope.launch {
            val success = repository.unlockTrack(trackId, cost)
            if (success) {
                soundManager.playUpgradeSfx()
            }
        }
    }

    // --- Daily Missions Actions ---
    fun claimMissionReward(missionId: String) {
        viewModelScope.launch {
            val reward = repository.claimMissionReward(missionId)
            if (reward > 0) {
                soundManager.playCoinSfx()
            }
        }
    }

    // --- Settings Actions ---
    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch {
            val profile = repository.getActiveUserProfile()
            if (profile != null) {
                val updated = profile.copy(soundEnabled = enabled)
                repository.saveUserProfile(updated)
                soundManager.setMute(!enabled)
                if (enabled) {
                    soundManager.startBgm()
                } else {
                    soundManager.stopBgm()
                }
            }
        }
    }

    fun updateGraphicsSettings(fps: Int, screenShake: Boolean, particles: Float) {
        viewModelScope.launch {
            val profile = repository.getActiveUserProfile()
            if (profile != null) {
                val updated = profile.copy(
                    targetFps = fps,
                    screenShakeEnabled = screenShake,
                    particlesDensity = particles
                )
                repository.saveUserProfile(updated)
            }
        }
    }

    // --- Simulated Multiplayer Matchmaking Lobby ---
    fun startMultiplayerMatchmaking() {
        _lobbyState.value = LobbyState.Searching
        viewModelScope.launch {
            soundManager.playCountDownBeep(false)
            // Simulated delay for network matching
            kotlinx.coroutines.delay(2000)
            
            val potentialOpponents = listOf("NeonRacer_X", "RetroSpeedy", "ViperApex", "GamerDrift", "ChronoTurbo")
            val selectedOpponentRandom = potentialOpponents.shuffled().take(2)
            
            _lobbyState.value = LobbyState.Found(selectedOpponentRandom)
            soundManager.playCountDownBeep(false)
            
            kotlinx.coroutines.delay(1500)
            _lobbyState.value = LobbyState.Ready
            soundManager.playCountDownBeep(true)
        }
    }

    fun cancelMatchmaking() {
        _lobbyState.value = LobbyState.Idle
    }

    // --- Race Event Reporting ---
    fun finalizeRace(trackId: String, finalTimeMillis: Long, earnedCoins: Int, maxSpeedKmph: Int, isMultiplayerMode: Boolean, hasWonDuel: Boolean = false) {
        viewModelScope.launch {
            // Save race score & time
            val profile = userProfile.value ?: UserProfile()
            val carPrefix = when (profile.selectedCarIndex) {
                0 -> "Classic Red"
                1 -> "Vortex Blue"
                2 -> "Bumble Yellow"
                else -> "Cyber Green"
            }
            repository.saveRaceTime(trackId, finalTimeMillis, carPrefix)

            // Grant earned coins + bonus if won matchmaking
            var rewardCoins = earnedCoins
            if (isMultiplayerMode && hasWonDuel) {
                rewardCoins += 150 // 150 coins winner bonus
            }
            repository.addCoins(rewardCoins)

            // Submit max speed checks to missions
            repository.checkMaxSpeedReached(maxSpeedKmph)
        }
    }
}
