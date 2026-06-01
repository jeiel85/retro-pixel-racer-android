package com.jeiel85.retropixelracer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.jeiel85.retropixelracer.data.entity.DailyMission
import com.jeiel85.retropixelracer.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyMissionsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val missions = viewModel.dailyMissions.collectAsState().value
    val profile = viewModel.userProfile.collectAsState().value ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "📅 DAILY RECYCLE MISSIONS",
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
            // Gold cache info
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

            Text(
                text = "Missions recycle automatically every day. Complete tasks during races to unlock massive gold awards!",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )

            // Scrollable list of missions
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(missions) { mission ->
                    DailyMissionCard(
                        mission = mission,
                        onClaim = {
                            viewModel.claimMissionReward(mission.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyMissionCard(
    mission: DailyMission,
    onClaim: () -> Unit
) {
    val progressPercent = (mission.progress.toFloat() / mission.target.toFloat()).coerceIn(0f, 1f)
    val isReadyToClaim = mission.completed && !mission.claimed
    val borderCol = if (mission.claimed) Color.DarkGray else if (mission.completed) Color.Green else Color.DarkGray

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF1E1E24))
            .border(1.dp, borderCol, RoundedCornerShape(4.dp))
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title and reward coins
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mission.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = "+${mission.rewardCoins}g",
                    color = Color.Yellow,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }

            // Progress bar and claim button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Progress Bar
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.DarkGray, RoundedCornerShape(100))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .background(if (mission.completed) Color.Green else Color.Cyan, RoundedCornerShape(100))
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${mission.progress} / ${mission.target}",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action Claim buttons
                if (mission.claimed) {
                    Box(
                        modifier = Modifier
                            .background(Color.DarkGray, RoundedCornerShape(3.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "CLAIMED",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onClaim,
                        enabled = isReadyToClaim,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green,
                            contentColor = Color.Black,
                            disabledContainerColor = Color.DarkGray
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = "CLAIM",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
