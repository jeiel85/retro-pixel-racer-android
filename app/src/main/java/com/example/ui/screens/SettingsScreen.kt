package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile = viewModel.userProfile.collectAsState().value ?: return

    // Screen local editable states synced with ViewModel
    var soundOn by remember { mutableStateOf(profile.soundEnabled) }
    var fpsSelected by remember { mutableStateOf(profile.targetFps) }
    var screenShake by remember { mutableStateOf(profile.screenShakeEnabled) }
    var particlesDensity by remember { mutableStateOf(profile.particlesDensity) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "⚙️ AUDIO & SYSTEM CONFIG",
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "TUNE ENGINE SYSTEM PARAMETERS TO RUN CORRESPONDING RETRO EMULATION:",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )

            // 1. Audio sound toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("8-BIT SOUNDTRACK", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("Dynamic audio synthesis", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                Switch(
                    checked = soundOn,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Green,
                        checkedTrackColor = Color.Green.copy(alpha = 0.5f)
                    ),
                    onCheckedChange = { checked ->
                        soundOn = checked
                        viewModel.toggleSound(checked)
                    }
                )
            }

            // 2. Target Framerate options
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(4.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text("PHYSICS SIMULATOR DELTA (FPS)", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("Target graphics frames updates", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fpsOptions = listOf(30, 60, 144)
                    fpsOptions.forEach { fps ->
                        val isSelected = fpsSelected == fps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color.Green else Color(0xFF121216))
                                .border(1.dp, if (isSelected) Color.White else Color.DarkGray, RoundedCornerShape(4.dp))
                                .clickable {
                                    fpsSelected = fps
                                    viewModel.updateGraphicsSettings(fps, screenShake, particlesDensity)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (fps == 144) "UNCAPPED" else "${fps} FPS",
                                color = if (isSelected) Color.Black else Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 3. Collision Screen Shake
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("COLLISION SHAKE", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("vibrate viewport on crashes", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }

                Switch(
                    checked = screenShake,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Green,
                        checkedTrackColor = Color.Green.copy(alpha = 0.5f)
                    ),
                    onCheckedChange = { checked ->
                        screenShake = checked
                        viewModel.updateGraphicsSettings(fpsSelected, checked, particlesDensity)
                    }
                )
            }

            // 4. Exhaust Particles Density
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(4.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("EXHAUST EMISSION INDEX", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text("Chunky pixel-smoke quantity", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                    Text(
                        text = if (particlesDensity == 0f) "OFF" else if (particlesDensity <= 0.4f) "LOW" else "HIGH",
                        color = Color.Green,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Slider(
                    value = particlesDensity,
                    valueRange = 0f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Green,
                        activeTrackColor = Color.Green.copy(alpha = 0.5f)
                    ),
                    onValueChange = { density ->
                        particlesDensity = density
                        viewModel.updateGraphicsSettings(fpsSelected, screenShake, density)
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Back button
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "SAVE & CLOSE",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
