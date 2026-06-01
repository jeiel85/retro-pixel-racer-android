package com.jeiel85.retropixelracer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeiel85.retropixelracer.data.entity.UserProfile
import com.jeiel85.retropixelracer.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(
    viewModel: GameViewModel,
    profile: UserProfile,
    onBack: () -> Unit,
    onMatchReady: (List<String>) -> Unit
) {
    val lobbyState = viewModel.lobbyState.collectAsState().value
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scaleBlink by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    // Trigger searching automatically when opening screen
    LaunchedEffect(Unit) {
        viewModel.startMultiplayerMatchmaking()
    }

    // Trigger navigation when match is Ready
    LaunchedEffect(lobbyState) {
        if (lobbyState is GameViewModel.LobbyState.Ready) {
            delay(1200)
            val matchedNames = when (val state = viewModel.lobbyState.value) {
                is GameViewModel.LobbyState.Found -> state.opponents
                else -> listOf("AI Racer")
            }
            onMatchReady(matchedNames)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⚡ MULTIPLAYER DUEL",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.Yellow
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelMatchmaking()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E24)
                )
            )
        },
        containerColor = Color(0xFF0F0F13)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (lobbyState) {
                is GameViewModel.LobbyState.Searching -> {
                    // Local matchmaking log console simulation
                    val logs = remember { mutableStateListOf<String>() }
                    LaunchedEffect(Unit) {
                        logs.clear()
                        val rawLogs = listOf(
                            "📡 INITIATING MULTIPLAYER TRANSCEIVER CORE...",
                            "🌐 CONNECTING TO TOKYO/PACIFIC EDGE NODE...",
                            "🛡️ VERIFYING ANTIGRAVITY ENCRYPTED PROTOCOL (OK)",
                            "📡 SCANNING SECURE ACTIVE PILOT CELLS (1,492 online)",
                            "🔎 FILTERING CLUSTERS BY COMPARABLE ENGINE LEVEL...",
                            "⚡ STABLE CONNECTION ESTABLISHED WITH REMOTE CELL!"
                        )
                        rawLogs.forEachIndexed { index, log ->
                            delay(300L * (index + 1))
                            logs.add(log)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = Color.Red,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )

                        Text(
                            text = "ESTABLISHING SIGNAL...",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.scale(scaleBlink)
                        )

                        // Scrolling Matchmaking Log Console Card
                        Card(
                            shape = RoundedCornerShape(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                logs.forEach { log ->
                                    Text(
                                        text = log,
                                        color = if (log.contains("✔") || log.contains("ESTABLISHED")) Color.Green else if (log.contains("ESTABLISHING") || log.contains("INITIATING")) Color.Yellow else Color.Cyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Button(
                            onClick = {
                                viewModel.cancelMatchmaking()
                                onBack()
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("CANCEL SIGNAL", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }

                is GameViewModel.LobbyState.Found -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "MATCH ESTABLISHED!",
                            color = Color.Green,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Match card showing components
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                            modifier = Modifier.fillMaxWidth().border(1.dp, Color.Green.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // User
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("YOU (PILOT)", color = Color.Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                        Text("CAR LVL: ${profile.engineLevel + profile.handlingLevel + profile.speedLevel}", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    }
                                    
                                    Text("READY ✔", color = Color.Green, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }

                                Divider(color = Color.DarkGray)

                                // Match list
                                lobbyState.opponents.forEach { opponent ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(opponent, color = Color.Red, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                            Text("CAR LVL: " + (profile.engineLevel + profile.handlingLevel + profile.speedLevel - 1).coerceAtLeast(3), color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                        }

                                        Text("CONNECTED", color = Color.Yellow, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Syncing physics simulation clock...",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                is GameViewModel.LobbyState.Ready -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "GO!",
                            color = Color.Green,
                            fontSize = 64.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.scale(scaleBlink)
                        )
                        Text(
                            text = "LAUCHING SIMULATION CORE...",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    // Idle default screen
                    Button(
                        onClick = { viewModel.startMultiplayerMatchmaking() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("CONNECT MULTIPLAYER SIGNAL", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
