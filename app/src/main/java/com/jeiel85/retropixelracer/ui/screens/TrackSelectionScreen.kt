package com.jeiel85.retropixelracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeiel85.retropixelracer.data.entity.UserProfile
import com.jeiel85.retropixelracer.data.entity.TrackState
import com.jeiel85.retropixelracer.ui.viewmodel.GameViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectionScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onTrackSelected: (String) -> Unit
) {
    val profile = viewModel.userProfile.collectAsState().value ?: return
    val tracks = viewModel.trackStates.collectAsState().value
    val selectedTrackId = viewModel.selectedTrackId.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🏆 SELECT CIRCUIT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Yellow
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats indicator (wallet gold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("COINS AVAILABLE:", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("${profile.coins}g", color = Color.Yellow, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            }

            // Scrollable list of tracks
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tracks) { track ->
                    val isSelected = selectedTrackId == track.trackId
                    val unlockCost = when (track.trackId) {
                        "track_desert" -> 300
                        "track_neon" -> 800
                        "track_snow" -> 1200
                        "track_volcano" -> 1800
                        "track_space" -> 2500
                        else -> 0
                    }

                    TrackItemCard(
                        track = track,
                        isSelected = isSelected,
                        unlockCost = unlockCost,
                        userCoins = profile.coins,
                        onSelect = {
                            if (track.unlocked) {
                                viewModel.selectTrack(track.trackId)
                            }
                        },
                        onUnlock = {
                            viewModel.unlockTrack(track.trackId, unlockCost)
                        }
                    )
                }
            }

            // Big Start Trigger Button at high contrast
            val chosenTrack = tracks.firstOrNull { it.trackId == selectedTrackId }
            val startEnabled = chosenTrack != null && chosenTrack.unlocked

            Button(
                onClick = {
                    if (chosenTrack != null && startEnabled) {
                        onTrackSelected(chosenTrack.trackId)
                    }
                },
                enabled = startEnabled,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Green,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.DarkGray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "START SOLO CHAMPIONSHIP 🏁",
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun TrackItemCard(
    track: TrackState,
    isSelected: Boolean,
    unlockCost: Int,
    userCoins: Int,
    onSelect: () -> Unit,
    onUnlock: () -> Unit
) {
    val canUnlock = userCoins >= unlockCost && !track.unlocked
    val borderCol = if (isSelected) Color.Green else if (!track.unlocked) Color.Red.copy(alpha = 0.5f) else Color.DarkGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFF1B2E1E) else Color(0xFF1E1E24))
            .border(2.dp, borderCol, RoundedCornerShape(4.dp))
            .clickable { if (track.unlocked) onSelect() }
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = track.name.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "THEME: ${track.theme}",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "DIFF: ${track.difficulty}",
                            color = when (track.difficulty) {
                                "EASY" -> Color.Green
                                "NORMAL" -> Color.Yellow
                                else -> Color.Red
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Lock/Unlock badge & best times
                if (!track.unlocked) {
                    Button(
                        onClick = onUnlock,
                        enabled = canUnlock,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            disabledContainerColor = Color.DarkGray
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "UNLOCK ${unlockCost}g",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color.DarkGray, RoundedCornerShape(3.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "UNLOCKED",
                            color = Color.Green,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Length and best lap display
            if (track.unlocked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL LENGTH: ${track.lengthMeters}m",
                        color = Color.LightGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )

                    Text(
                        text = "BEST LAP: " + if (track.bestTimeMillis > 0) formatTrackTime(track.bestTimeMillis) else "--:--.--",
                        color = Color.Cyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTrackTime(millis: Long): String {
    val sec = (millis / 1000) % 60
    val min = (millis / 60000)
    val ms = (millis % 1000) / 10
    return String.format(Locale.getDefault(), "%02d:%02d.%02d", min, sec, ms)
}
