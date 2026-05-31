package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.GameViewModel
import kotlin.math.sin

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val profile = viewModel.userProfile.collectAsState().value
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    // Title retro blinking glow animation
    val blinkGlow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F13)) // carbon slate deep black
    ) {
        // Draw ambient retro matrix star field background on canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starCount = 30
            val random = java.util.Random(42)
            val elapsed = System.currentTimeMillis() / 2000f
            
            for (i in 0 until starCount) {
                val x = random.nextFloat() * size.width
                val y = ((random.nextFloat() * size.height) + elapsed * 50f) % size.height
                val radius = random.nextFloat() * 4f + 2f
                val glowAlpha = (sin(elapsed * i.toFloat() * 0.1f) + 1.0f) * 0.5f
                
                drawCircle(
                    color = Color.Yellow.copy(alpha = glowAlpha * 0.6f),
                    radius = radius,
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Stats Bar (Coins)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Yellow.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PILOT ID: DRIVER_1",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GOLD: ",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${profile?.coins ?: 0}g",
                        color = Color.Yellow,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Big Nostalgic Arcade Game Logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                Text(
                    text = "RETRO PIXEL",
                    color = Color.Cyan,
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.scale(blinkGlow)
                )
                
                Text(
                    text = "R A C E R",
                    color = Color.Magenta,
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.scale(blinkGlow)
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "-- INSERT COIN TO PLAY --",
                    color = Color.Yellow.copy(alpha = blinkGlow),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Interactive Retro Menu List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Play / Single Race
                RetroMenuButton(
                    text = "🏆 SOLO CHAMPIONSHIP",
                    color = Color(0xFF00E676), // Arcade Green
                    onClick = {
                        viewModel.soundManager.playCoinSfx()
                        viewModel.setGameState(GameViewModel.GameState.TRACK_SELECTION)
                    }
                )

                // Multiplayer Mode
                RetroMenuButton(
                    text = "⚡ MULTIPLAYER DUEL",
                    color = Color(0xFFFF1744), // Arcade Hot Red
                    onClick = {
                        viewModel.soundManager.playCoinSfx()
                        viewModel.setGameState(GameViewModel.GameState.LOBBY)
                    }
                )

                // Garage Car Upgrade
                RetroMenuButton(
                    text = "🔧 GARAGE & TUNING",
                    color = Color(0xFF2979FF), // Neon Blue
                    onClick = {
                        viewModel.soundManager.playCoinSfx()
                        viewModel.setGameState(GameViewModel.GameState.GARAGE)
                    }
                )

                // Daily Missions
                RetroMenuButton(
                    text = "📅 DAILY MISSIONS",
                    color = Color(0xFFFF9100), // Amber Gold
                    onClick = {
                        viewModel.soundManager.playCoinSfx()
                        viewModel.setGameState(GameViewModel.GameState.DAILY_MISSIONS)
                    }
                )

                // Worldwide Leaderboards
                RetroMenuButton(
                    text = "🌐 GLOBAL RECORDS",
                    color = Color(0xFFD500F9), // Purple Neon
                    onClick = {
                        viewModel.soundManager.playCoinSfx()
                        viewModel.setGameState(GameViewModel.GameState.LEADERBOARD)
                    }
                )

                // Settings
                RetroMenuButton(
                    text = "⚙️ SETTINGS",
                    color = Color(0xFF90A4AE), // Cool Slate grey
                    onClick = {
                        viewModel.soundManager.playCoinSfx()
                        viewModel.setGameState(GameViewModel.GameState.SETTINGS)
                    }
                )
            }

            // Copyright Footer
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "© 2026 RETRO ARCADE COMPATIBLE",
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun RetroMenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E1E24))
            .border(2.dp, color, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            // Retro indicator caret
            Text(
                text = "▶",
                color = color,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
