package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.data.entity.LeaderboardEntry
import com.example.ui.viewmodel.GameViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val trackList = viewModel.trackStates.collectAsState().value
    val selectedTrackId = viewModel.selectedTrackId.collectAsState().value
    val records = viewModel.currentLeaderboard.collectAsState().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🌐 WORLDWIDE RECORDS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
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
            // Track Tab Sliders (Horizontal scrolling selectors for target track)
            ScrollableTabRow(
                selectedTabIndex = trackList.indexOfFirst { it.trackId == selectedTrackId }.coerceAtLeast(0),
                containerColor = Color(0xFF1E1E24),
                contentColor = Color.Yellow,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            ) {
                trackList.forEach { track ->
                    val isSelected = selectedTrackId == track.trackId
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectTrack(track.trackId) },
                        text = {
                            Text(
                                text = track.name.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) Color.Yellow else Color.Gray
                            )
                        }
                    )
                }
            }

            // Leaderboard Headers description
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161C))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Text("RANK", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp, modifier = Modifier.width(42.dp))
                    Text("PLAYER PILOT", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
                Text("LAP TIME", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }

            // Scrollable list of ranks
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO HIGH SCORES SAVED YET\nBE THE FIRST TO FINISH!",
                        color = Color.DarkGray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 18.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(records) { index, entry ->
                        LeaderboardItemRow(
                            index = index,
                            entry = entry
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardItemRow(
    index: Int,
    entry: LeaderboardEntry
) {
    val rank = index + 1
    val isUser = entry.isUser
    val rankColor = when (rank) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        3 -> Color(0xFFCD7F32) // Bronze
        else -> Color.DarkGray
    }
    val contentCol = if (isUser) Color.Cyan else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(if (isUser) Color(0xFF1B2A36) else Color(0xFF1E1E24))
            .border(
                width = if (isUser) 1.dp else 0.dp,
                color = if (isUser) Color.Cyan else Color.Transparent,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank circle badge
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(24.dp)
                        .background(rankColor, RoundedCornerShape(100)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        color = if (rank in 1..3) Color.Black else Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                // Name and car chassis details
                Column {
                    Text(
                        text = entry.playerName,
                        color = contentCol,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "CAR: ${entry.carName.uppercase()}",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                }
            }

            // Timestring
            Text(
                text = formatEntryTime(entry.timeMillis),
                color = contentCol,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatEntryTime(millis: Long): String {
    val sec = (millis / 1000) % 60
    val min = (millis / 60000)
    val ms = (millis % 1000) / 10
    return String.format(Locale.getDefault(), "%02d:%02d.%02d", min, sec, ms)
}
