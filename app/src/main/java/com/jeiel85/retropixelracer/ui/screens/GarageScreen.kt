package com.jeiel85.retropixelracer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeiel85.retropixelracer.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val profile = viewModel.userProfile.collectAsState().value ?: return
    val selectedCar = viewModel.carIndex.collectAsState().value

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val carScalePulse by infiniteTransition.animateFloat(
        initialValue = 2.8f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "🔧 GARAGE TUNING",
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
            // Player stats header with glowing border
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1E24), RoundedCornerShape(4.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WALLET CASH:",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Text(
                    text = "${profile.coins}g",
                    color = Color.Yellow,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
            }

            // Interactive Live Car Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF16161C))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Grid background lines for blueprint feel
                    val numLinesX = 15
                    for (i in 0..numLinesX) {
                        val lx = i * (size.width / numLinesX)
                        drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(lx, 0f), Offset(lx, size.height), 1f)
                    }
                    val numLinesY = 8
                    for (i in 0..numLinesY) {
                        val ly = i * (size.height / numLinesY)
                        drawLine(Color.DarkGray.copy(alpha = 0.15f), Offset(0f, ly), Offset(size.width, ly), 1f)
                    }

                    // Draw rotating chassis in selected color
                    val bodyColor = when(selectedCar) {
                        0 -> Color(0xFFE53935) // Red
                        1 -> Color(0xFF1E88E5) // Blue
                        2 -> Color(0xFFFFB300) // Yellow
                        else -> Color(0xFF43A047) // Green
                    }

                    // Render central chassis preview custom scaled
                    drawGarageCarPreview(bodyColor, size.width / 2, size.height / 2, carScalePulse)
                }

                Text(
                    text = "MODEL: CHASSIS " + when(selectedCar) {
                        0 -> "ALPHA CLASSIC"
                        1 -> "CYBER STRYKER"
                        2 -> "GOLDEN HORNET"
                        else -> "FOREST TRIS"
                    },
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                )
            }

            // Chassis Color Selector Row
            Column {
                Text(
                    text = "SELECT CHASSIS PAINT:",
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf(
                        Color(0xFFE53935) to "RED",
                        Color(0xFF1E88E5) to "BLUE",
                        Color(0xFFFFB300) to "GOLD",
                        Color(0xFF43A047) to "GREEN"
                    )

                    colors.forEachIndexed { index, (color, name) ->
                        val isSelected = selectedCar == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) color else Color(0xFF1E1E24))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.selectCar(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Divider(color = Color.DarkGray)

            // Upgrades Options
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ENGINE ACCEL UPGRADE
                UpgradeItemRow(
                    title = "🚀 ACCELERATION (ENGINE)",
                    level = profile.engineLevel,
                    coins = profile.coins,
                    onUpgrade = { viewModel.upgradeEngine() }
                )

                // HANDLING STEERING UPGRADE
                UpgradeItemRow(
                    title = "🕹️ HANDLING (STEERING)",
                    level = profile.handlingLevel,
                    coins = profile.coins,
                    onUpgrade = { viewModel.upgradeHandling() }
                )

                // MAX SPEED UPGRADE
                UpgradeItemRow(
                    title = "🏎️ MAXIMUM ENGINE SPEED",
                    level = profile.speedLevel,
                    coins = profile.coins,
                    onUpgrade = { viewModel.upgradeSpeed() }
                )
            }
        }
    }
}

@Composable
fun UpgradeItemRow(
    title: String,
    level: Int,
    coins: Int,
    onUpgrade: () -> Unit
) {
    val isMax = level >= 5
    val cost = level * 200
    val canAfford = coins >= cost && !isMax

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E24), RoundedCornerShape(4.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isMax) "MAX" else "${cost}g",
                color = if (isMax) Color.Magenta else if (canAfford) Color.Yellow else Color.Red,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Stats Visual bar indicator (Level blocks [▮▮▮▯▯])
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (i in 1..5) {
                    val active = i <= level
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(100))
                            .background(if (active) Color.Green else Color.DarkGray)
                    )
                }
            }

            // Upgrade action trigger button
            Button(
                onClick = onUpgrade,
                enabled = canAfford,
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Yellow,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.DarkGray
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "UPGRADE",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Low-level canvas vector car draw preview
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGarageCarPreview(
    bodyColor: Color,
    cx: Float,
    cy: Float,
    scale: Float
) {
    val bodyW = 20f * scale
    val bodyH = 32f * scale

    // Draw outline
    drawRect(
        color = Color.Black,
        topLeft = Offset(cx - bodyW / 2, cy - bodyH / 2),
        size = Size(bodyW, bodyH)
    )

    // Inner Body fill
    drawRect(
        color = bodyColor,
        topLeft = Offset(cx - bodyW / 2 + 1f * scale, cy - bodyH / 2 + 1f * scale),
        size = Size(bodyW - 2f * scale, bodyH - 2f * scale)
    )

    // Wheels
    val wheelW = 4f * scale
    val wheelH = 8f * scale
    drawRect(Color.Black, Offset(cx - bodyW / 2 - wheelW + 1f, cy - bodyH / 2 + 3f * scale), Size(wheelW, wheelH))
    drawRect(Color.Black, Offset(cx + bodyW / 2 - 1f, cy - bodyH / 2 + 3f * scale), Size(wheelW, wheelH))
    drawRect(Color.Black, Offset(cx - bodyW / 2 - wheelW + 1f, cy + bodyH / 2 - wheelH - 3f * scale), Size(wheelW, wheelH))
    drawRect(Color.Black, Offset(cx + bodyW / 2 - 1f, cy + bodyH / 2 - wheelH - 3f * scale), Size(wheelW, wheelH))

    // Windshield
    drawRect(Color.Black, Offset(cx - bodyW/4, cy - 2f * scale), Size(bodyW/2, 8f * scale))
    drawRect(Color(0xFF80DEEA), Offset(cx - bodyW/4 + 1f, cy - 1f * scale), Size(bodyW/2 - 2f, 6f * scale))

    // Spoiler
    drawRect(Color.Black, Offset(cx - bodyW/2 - 2f, cy + bodyH/2 - 3f * scale), Size(bodyW + 4f, 3f * scale))
}
