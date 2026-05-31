package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ui.screens.*
import com.example.ui.game.GameEngineScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.GameViewModel
import com.example.ui.viewmodel.GameViewModel.GameState

class MainActivity : ComponentActivity() {
    private lateinit var gameViewModel: GameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        gameViewModel = ViewModelProvider(this)[GameViewModel::class.java]

        setContent {
            MyApplicationTheme {
                val activeState by gameViewModel.gameState.collectAsState()
                val profile = gameViewModel.userProfile.collectAsState().value

                // Active Session Settings
                var isMultiplayerSession by remember { mutableStateOf(false) }
                var matchedOpponents by remember { mutableStateOf(listOf<String>()) }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Animated Crossfade creates high-fidelity premium screen transitions (< 300ms)
                    Crossfade(
                        targetState = activeState,
                        modifier = Modifier.fillMaxSize()
                    ) { targetState ->
                        when (targetState) {
                            GameState.MAIN_MENU -> {
                                MainMenuScreen(
                                    viewModel = gameViewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            GameState.GARAGE -> {
                                GarageScreen(
                                    viewModel = gameViewModel,
                                    onBack = { gameViewModel.setGameState(GameState.MAIN_MENU) }
                                )
                            }
                            GameState.TRACK_SELECTION -> {
                                isMultiplayerSession = false
                                TrackSelectionScreen(
                                    viewModel = gameViewModel,
                                    onBack = { gameViewModel.setGameState(GameState.MAIN_MENU) },
                                    onTrackSelected = {
                                        gameViewModel.setGameState(GameState.RACING)
                                    }
                                )
                            }
                            GameState.DAILY_MISSIONS -> {
                                DailyMissionsScreen(
                                    viewModel = gameViewModel,
                                    onBack = { gameViewModel.setGameState(GameState.MAIN_MENU) }
                                )
                            }
                            GameState.LEADERBOARD -> {
                                LeaderboardScreen(
                                    viewModel = gameViewModel,
                                    onBack = { gameViewModel.setGameState(GameState.MAIN_MENU) }
                                )
                            }
                            GameState.LOBBY -> {
                                isMultiplayerSession = true
                                if (profile != null) {
                                    MultiplayerLobbyScreen(
                                        viewModel = gameViewModel,
                                        profile = profile,
                                        onBack = { gameViewModel.setGameState(GameState.MAIN_MENU) },
                                        onMatchReady = { opponents ->
                                            matchedOpponents = opponents
                                            gameViewModel.setGameState(GameState.RACING)
                                        }
                                    )
                                }
                            }
                            GameState.SETTINGS -> {
                                SettingsScreen(
                                    viewModel = gameViewModel,
                                    onBack = { gameViewModel.setGameState(GameState.MAIN_MENU) }
                                )
                            }
                            GameState.RACING -> {
                                if (profile != null) {
                                    val selectedTrack = gameViewModel.selectedTrackId.collectAsState().value ?: "track_forest"
                                    GameEngineScreen(
                                        viewModel = gameViewModel,
                                        profile = profile,
                                        trackId = selectedTrack,
                                        isMultiplayer = isMultiplayerSession,
                                        opponents = matchedOpponents,
                                        onBack = { gameViewModel.setGameState(GameState.MAIN_MENU) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Start or resume background loop
        try {
            if (::gameViewModel.isInitialized && gameViewModel.gameState.value == GameState.MAIN_MENU) {
                gameViewModel.soundManager.startBgm()
            }
        } catch (e: Exception) {}
    }

    override fun onPause() {
        super.onPause()
        // Cut audio threads to avoid battery drain
        try {
            if (::gameViewModel.isInitialized) {
                gameViewModel.soundManager.stopBgm()
            }
        } catch (e: Exception) {}
    }
}
