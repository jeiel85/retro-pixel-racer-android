package com.jeiel85.retropixelracer.ui.game

import android.view.KeyEvent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jeiel85.retropixelracer.data.entity.UserProfile
import com.jeiel85.retropixelracer.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin
import kotlin.random.Random

// Represents a road object (coin, obstacle, rival)
data class RoadElement(
    val id: Int,
    var trackDistance: Float, // distance along track (0 to trackLength)
    val roadX: Float,         // horizontal offset (-1.0 to 1.0)
    val type: ElementType,
    var collected: Boolean = false,
    var crashed: Boolean = false,
    val speedKmph: Float = 0f, // only for CPU cars
    var currentRoadX: Float = roadX, // mutable for CPU steering
    var spinAngle: Float = 0f
)

enum class ElementType {
    COIN,
    BARRIER,
    CPU_CAR_RED,
    CPU_CAR_BLUE,
    CPU_CAR_YELLOW,
    RIVAL_CAR
}

data class GameParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var life: Float, // 1.0 to 0.0
    val size: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEngineScreen(
    viewModel: GameViewModel,
    profile: UserProfile,
    trackId: String,
    isMultiplayer: Boolean,
    opponents: List<String>,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val trackList = viewModel.trackStates.collectAsState().value
    val activeTrack = trackList.firstOrNull { it.trackId == trackId } ?: return

    // Screen adjustments
    val screenShakeOn = profile.screenShakeEnabled
    val particleIntensity = profile.particlesDensity
    val fpsTarget = profile.targetFps

    // Game track stats
    val trackLength = activeTrack.lengthMeters.toFloat() // meters
    val theme = activeTrack.theme // FOREST, DESERT, NEON, SNOW

    // Physics parameters based on Upgrades
    // Level 1-5 Engine (Speed)
    val baseMaxSpeed = 160f // km/h
    val maxSpeed = baseMaxSpeed + (profile.speedLevel - 1) * 20f
    // Level 1-5 Accelerator
    val kAcceleration = 35f + (profile.engineLevel - 1) * 10f // km/h per second
    val kDeacceleration = 50f
    // Level 1-5 Handling (Steering)
    val steeringPower = 1.2f + (profile.handlingLevel - 1) * 0.2f // width of track steering per sec

    // Active screen focus for PC Keyboard Controls
    val focusRequester = remember { FocusRequester() }

    // Game Running States
    var isPlaying by remember { mutableStateOf(false) }
    var gameTimeMillis by remember { mutableStateOf(0L) }
    var speedKmph by remember { mutableStateOf(0f) }
    var progressDistance by remember { mutableStateOf(0f) }
    var playerX by remember { mutableStateOf(0f) } // Road position -1.0 to 1.0

    // Opponent Progress (Rival in Multiplayer mode)
    var rivalFinished by remember { mutableStateOf(false) }
    var rivalDistance by remember { mutableStateOf(0f) }
    val rivalName = if (isMultiplayer && opponents.isNotEmpty()) opponents.first() else "AI Racer"
    val rivalSpeedKmph = maxSpeed * 0.93f // Competitive! Close to player speed

    var countDown by remember { mutableStateOf(4) } // 3, 2, 1, GO, RUN
    var gameCompleted by remember { mutableStateOf(false) }
    var isCrashed by remember { mutableStateOf(false) }
    var crashTime by remember { mutableStateOf(0f) } // crash cooldown Timer
    var totalCoinsCollected by remember { mutableStateOf(0) }
    var screenShakeAmount by remember { mutableStateOf(0f) }

    // Input States (Supports simultaneous touch/keys)
    var keyLeftPressed by remember { mutableStateOf(false) }
    var keyRightPressed by remember { mutableStateOf(false) }
    var keyGasPressed by remember { mutableStateOf(false) }
    var keyBrakePressed by remember { mutableStateOf(false) }

    // Particle pool
    val particles = remember { mutableStateListOf<GameParticle>() }
    // Game Elements Pool
    val roadElements = remember { mutableStateListOf<RoadElement>() }

    // Initialize elements once
    LaunchedEffect(trackId) {
        roadElements.clear()
        particles.clear()
        
        // Spawn elements along the track
        val random = Random(42) // Deterministic track layout
        var dist = 150f
        var elementId = 1
        
        while (dist < trackLength - 100f) {
            val elementX = random.nextFloat() * 1.4f - 0.7f // centered on road
            val isCoin = random.nextFloat() < 0.45f
            
            if (isCoin) {
                // Spawn a line of 3 coins
                roadElements.add(RoadElement(elementId++, dist, elementX, ElementType.COIN))
                roadElements.add(RoadElement(elementId++, dist + 15f, elementX, ElementType.COIN))
                roadElements.add(RoadElement(elementId++, dist + 30f, elementX, ElementType.COIN))
                dist += 80f
            } else {
                // Spawn a barrier/hazard or other car
                val type = when (random.nextInt(4)) {
                    0 -> ElementType.BARRIER
                    1 -> ElementType.CPU_CAR_RED
                    2 -> ElementType.CPU_CAR_BLUE
                    else -> ElementType.CPU_CAR_YELLOW
                }
                val speed = if (type == ElementType.BARRIER) 0f else random.nextFloat() * 40f + 60f
                roadElements.add(
                    RoadElement(
                        id = elementId++,
                        trackDistance = dist,
                        roadX = elementX,
                        type = type,
                        speedKmph = speed
                    )
                )
                dist += 120f
            }
        }
        
        // Spawn actual Rival in Multiplayer mode
        if (isMultiplayer) {
            roadElements.add(
                RoadElement(
                    id = 9999,
                    trackDistance = 100f,
                    roadX = 0.4f,
                    type = ElementType.RIVAL_CAR,
                    speedKmph = rivalSpeedKmph
                )
            )
        }
    }

    // Countdown effect on start
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (countDown > 1) {
            delay(1000)
            countDown--
            viewModel.soundManager.playCountDownBeep(highPitch = (countDown == 1))
        }
        isPlaying = true
        // Keep engine rev hum
        viewModel.soundManager.playEngineRevSfx(0f)
    }

    // Engine Humming Audio sound triggers
    LaunchedEffect(isPlaying, gameCompleted) {
        if (isPlaying && !gameCompleted) {
            while (isPlaying && !gameCompleted) {
                val soundDensityPercent = speedKmph / maxSpeed
                viewModel.soundManager.playEngineRevSfx(soundDensityPercent)
                delay(120)
            }
        }
    }

    // Core Game Update Loop (Calculates delta time and frames dynamically based on settings)
    LaunchedEffect(isPlaying, gameCompleted, countDown) {
        if (!isPlaying || gameCompleted) return@LaunchedEffect
        
        var lastTime = System.nanoTime()
        val delayTime = if (fpsTarget == 60) 16L else if (fpsTarget == 30) 33L else 5L
        
        while (isPlaying && !gameCompleted) {
            val now = System.nanoTime()
            val elapsedSec = (now - lastTime) / 1_000_000_000f
            lastTime = now

            gameTimeMillis += (elapsedSec * 1000).toLong()

            // 1. Handle Screen Shake Decay
            if (screenShakeAmount > 0f) {
                screenShakeAmount = (screenShakeAmount - elapsedSec * 15f).coerceAtLeast(0f)
            }

            // 2. Handle Crash cooldown timer
            if (isCrashed) {
                crashTime -= elapsedSec
                if (crashTime <= 0) {
                    isCrashed = false
                }
            }

            // 3. User Input Speed / Steering physics
            // Accelerate / Brake / Friction
            var targetSpeed = 0f
            if (keyGasPressed && !isCrashed) {
                targetSpeed = maxSpeed
            } else if (keyBrakePressed) {
                targetSpeed = 0f
            } else {
                targetSpeed = 0f // friction takes down
            }

            if (speedKmph < targetSpeed) {
                speedKmph = (speedKmph + kAcceleration * elapsedSec).coerceAtMost(targetSpeed)
            } else if (speedKmph > targetSpeed) {
                val decayRate = if (keyBrakePressed) kDeacceleration * 1.5f else kDeacceleration * 0.5f
                speedKmph = (speedKmph - decayRate * elapsedSec).coerceAtLeast(0f)
            }

            // Steering calculations
            var steerDir = 0f
            if (keyLeftPressed) steerDir = -1f
            if (keyRightPressed) steerDir = 1f
            
            if (speedKmph > 10f) {
                playerX = (playerX + steerDir * steeringPower * (speedKmph / maxSpeed) * elapsedSec).coerceIn(-1.3f, 1.3f)
            }

            // Off-track friction (on grass player slows down)
            if (playerX < -1.0f || playerX > 1.0f) {
                if (speedKmph > 50f) {
                    speedKmph -= 120f * elapsedSec // speed reduction on grass
                }
                // Generate dust particles
                if (particleIntensity > 0 && Math.random() < 0.3 * particleIntensity) {
                    particles.add(
                        GameParticle(
                            x = playerX * 0.4f + 0.5f + (Math.random().toFloat() - 0.5f) * 0.1f,
                            y = 0.85f,
                            vx = -steerDir * 0.2f + (Math.random().toFloat() - 0.5f) * 0.1f,
                            vy = -0.1f,
                            color = when(theme) {
                                "DESERT" -> Color(0xFFD2B48C)
                                "SNOW" -> Color.White
                                else -> Color(0xFF4A5D23)
                            },
                            life = 1.0f,
                            size = 12f
                        )
                    )
                }
            }

            // Translate speed kmph to distance progress (m/s)
            val speedMps = (speedKmph * 1000f) / 3600f
            progressDistance += speedMps * elapsedSec

            // 4. Update Opponent progress in Multiplayer mode
            if (isMultiplayer) {
                val opponentSpeedMps = (rivalSpeedKmph * 1000f) / 3600f
                rivalDistance += opponentSpeedMps * elapsedSec
                if (rivalDistance >= trackLength) {
                    rivalFinished = true
                }
            }

            // 5. Update spawned obstacle items & Check Collisions
            val playerDistance = progressDistance
            
            // Generate Exhaust Smoke
            if (isPlaying && speedKmph > 0 && particleIntensity > 0 && Math.random() < 0.2 * particleIntensity) {
                particles.add(
                    GameParticle(
                        x = playerX * 0.2f + 0.5f + (Math.random().toFloat() - 0.5f) * 0.02f,
                        y = 0.82f,
                        vx = (Math.random().toFloat() - 0.5f) * 0.05f,
                        vy = -0.05f,
                        color = Color.LightGray.copy(alpha = 0.6f),
                        life = 1.0f,
                        size = 8f
                    )
                )
            }

            // Update items
            roadElements.forEach { element ->
                // Make CPU cars drive along & update physics/spins
                if (element.type != ElementType.COIN && element.type != ElementType.BARRIER) {
                    if (element.crashed) {
                        element.spinAngle += 720f * elapsedSec
                        element.currentRoadX += (if (element.roadX >= 0) 2.2f else -2.2f) * elapsedSec
                    } else {
                        // Constant slow CPU driving progress
                        if (element.type == ElementType.RIVAL_CAR) {
                            element.trackDistance = rivalDistance
                        } else {
                            val cpuSpeedMps = (element.speedKmph * 1000f) / 3600f
                            element.trackDistance += cpuSpeedMps * elapsedSec
                        }
                        
                        // Steer back and forth randomly
                        if (Math.random() < 0.01) {
                            element.currentRoadX = (element.roadX + (Math.random().toFloat() - 0.5f) * 0.4f).coerceIn(-0.8f, 0.8f)
                        }
                    }
                }

                // Check collisions when player passes the element's actual distance
                val relativeDist = element.trackDistance - playerDistance
                
                // If it is matching player block (within +/- 3 meters)
                if (relativeDist in -3.0f..3.0f) {
                    if (element.type == ElementType.COIN && !element.collected) {
                        element.collected = true
                        totalCoinsCollected += 15
                        viewModel.soundManager.playCoinSfx()
                    } else if (element.type != ElementType.COIN && !element.crashed) {
                        // Crash checks! Check if horizontally close
                        val isHorizonHit = Math.abs(playerX - element.currentRoadX) < 0.45f
                        if (isHorizonHit) {
                            element.crashed = true
                            isCrashed = true
                            crashTime = 1.5f // seconds slow penalty
                            speedKmph = 20f   // slows down instantly
                            if (screenShakeOn) screenShakeAmount = 10f // stronger shake
                            viewModel.soundManager.playCrashSfx()
                            
                            // Explosion shockwave and 40 metallic sparks flying downwards/backwards
                            for (p in 0..40) {
                                val sparkColor = if (p % 3 == 0) Color(0xFFFF5722) else if (p % 3 == 1) Color(0xFFFFD54F) else Color.White
                                particles.add(
                                    GameParticle(
                                        x = playerX * 0.3f + 0.5f + (Math.random().toFloat() - 0.5f) * 0.05f,
                                        y = 0.8f,
                                        vx = (Math.random().toFloat() - 0.5f) * 0.5f,
                                        vy = 0.15f + Math.random().toFloat() * 0.35f, // downwards
                                        color = sparkColor,
                                        life = 1.0f,
                                        size = 6f + Math.random().toFloat() * 8f
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 6. Update Particles Life
            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx
                p.y += p.vy
                p.life -= elapsedSec * 2f // decays
                if (p.life <= 0f) {
                    iterator.remove()
                }
            }

            // Check race finished state
            if (progressDistance >= trackLength) {
                gameCompleted = true
                isPlaying = false
                
                // Finalize report to DB
                val isWinner = !isMultiplayer || !rivalFinished
                viewModel.finalizeRace(
                    trackId = trackId,
                    finalTimeMillis = gameTimeMillis,
                    earnedCoins = totalCoinsCollected,
                    maxSpeedKmph = maxSpeed.toInt(),
                    isMultiplayerMode = isMultiplayer,
                    hasWonDuel = isWinner
                )
                
                viewModel.soundManager.playUpgradeSfx()
            }

            delay(delayTime)
        }
    }

    // Safe drawing variables
    val screenShakeOffset = if (screenShakeAmount > 0f) {
        Offset(
            (Math.random().toFloat() - 0.5f) * screenShakeAmount * 5,
            (Math.random().toFloat() - 0.5f) * screenShakeAmount * 5
        )
    } else {
        Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                // Support keys for Keyboard PC Compatibility (Up, Left, Right, Brake etc.)
                var handled = false
                if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (event.key) {
                        androidx.compose.ui.input.key.Key.DirectionLeft -> {
                            keyLeftPressed = true; handled = true
                        }
                        androidx.compose.ui.input.key.Key.DirectionRight -> {
                            keyRightPressed = true; handled = true
                        }
                        androidx.compose.ui.input.key.Key.DirectionUp -> {
                            keyGasPressed = true; handled = true
                        }
                        androidx.compose.ui.input.key.Key.Spacebar -> {
                            keyGasPressed = true; handled = true
                        }
                        androidx.compose.ui.input.key.Key.DirectionDown -> {
                            keyBrakePressed = true; handled = true
                        }
                    }
                } else if (event.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                    when (event.key) {
                        androidx.compose.ui.input.key.Key.DirectionLeft -> {
                            keyLeftPressed = false; handled = true
                        }
                        androidx.compose.ui.input.key.Key.DirectionRight -> {
                            keyRightPressed = false; handled = true
                        }
                        androidx.compose.ui.input.key.Key.DirectionUp -> {
                            keyGasPressed = false; handled = true
                        }
                        androidx.compose.ui.input.key.Key.Spacebar -> {
                            keyGasPressed = false; handled = true
                        }
                        androidx.compose.ui.input.key.Key.DirectionDown -> {
                            keyBrakePressed = false; handled = true
                        }
                    }
                }
                handled
            }
    ) {
        // High Performance Canvas Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // Touch requestFocus
                            focusRequester.requestFocus()
                        }
                    )
                }
        ) {
            // Apply screen shake vibration translate
            drawContext.canvas.save()
            drawContext.canvas.translate(screenShakeOffset.x, screenShakeOffset.y)

            // Draw Pseudo-3D environment
            drawGameRoad(
                theme = theme,
                progressDistance = progressDistance,
                playerX = playerX,
                trackLength = trackLength,
                roadElements = roadElements,
                particles = particles,
                isMultiplayer = isMultiplayer,
                rivalDistance = rivalDistance,
                rivalName = rivalName,
                isCrashed = isCrashed,
                carIndex = profile.selectedCarIndex
            )

            drawContext.canvas.restore()
        }

        // Draw HUD overlay in retro pixel font style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left HUD: Stats
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "SPEED: ${speedKmph.toInt()} KM/H",
                        color = if (isCrashed) Color.Red else Color.Cyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "COINS: $totalCoinsCollected",
                        color = Color.Yellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "TIME: ${formatTime(gameTimeMillis)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Right HUD: Track Distance
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = activeTrack.name.uppercase(),
                        color = Color.Green,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DIST: ${progressDistance.toInt()}m / ${trackLength.toInt()}m",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    // Linear mini progress bar
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .background(Color.DarkGray, RoundedCornerShape(2.dp))
                    ) {
                        val progressPercent = (progressDistance / trackLength).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressPercent)
                                .background(Color.Yellow, RoundedCornerShape(2.dp))
                        )
                        
                        // Multiplayer rival dot mark on map
                        if (isMultiplayer) {
                            val rivalPercent = (rivalDistance / trackLength).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .offset(x = (rivalPercent * 100).dp - 3.dp)
                                    .background(Color.Red, RoundedCornerShape(100))
                            )
                        }
                    }
                }
            }

            // Multiplayer Live Leaderboard Standings
            if (isMultiplayer) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val userAhead = progressDistance >= rivalDistance
                    Text(
                        text = "1ST: " + (if (userAhead) "You (Driver)" else rivalName),
                        color = if (userAhead) Color.Cyan else Color.Red,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "2ND: " + (if (userAhead) rivalName else "You (Driver)"),
                        color = if (userAhead) Color.Red else Color.Cyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // On-Screen Retro Button Overlays for phone Touch controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            // Steering Left / Right Buttons on Left
            Row(
                modifier = Modifier.align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // LEFT
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.85f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(68.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    keyLeftPressed = true
                                    tryAwaitRelease()
                                    keyLeftPressed = false
                                }
                            )
                        }
                ) {
                    Text("◀", fontSize = 24.sp)
                }

                // RIGHT
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.85f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(68.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    keyRightPressed = true
                                    tryAwaitRelease()
                                    keyRightPressed = false
                                }
                            )
                        }
                ) {
                    Text("▶", fontSize = 24.sp)
                }
            }

            // Gas / Brake Pedals on Right
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // BRAKE
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828).copy(alpha = 0.85f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(64.dp, 68.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    keyBrakePressed = true
                                    tryAwaitRelease()
                                    keyBrakePressed = false
                                }
                            )
                        }
                ) {
                    Text("STOP\n(S)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // ACCEL GAS
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32).copy(alpha = 0.85f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .size(76.dp, 68.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    keyGasPressed = true
                                    tryAwaitRelease()
                                    keyGasPressed = false
                                }
                            )
                        }
                ) {
                    Text("GO!\n(W)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3... 2... 1... GO! Overlay
        if (countDown > 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                val label = if (countDown == 2) "1" else if (countDown == 3) "2" else "3"
                Text(
                    text = label,
                    color = Color.Yellow,
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else if (countDown == 1) {
            // GO popup displaying for 1 second
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "START!",
                    color = Color.Green,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(16.dp)
                )
            }
            LaunchedEffect(Unit) {
                delay(1000)
                countDown = 0
            }
        }

        // Collision crash RED screen flash
        if (isCrashed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.25f))
            )
        }

        // Bouncing/Flashing Neon warning crash banner overlay
        if (isCrashed) {
            val infiniteTransition = rememberInfiniteTransition(label = "crash_flash")
            val warningAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(150, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "crash_flash"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp), // slightly above controls
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.85f * warningAlpha), RoundedCornerShape(8.dp))
                        .border(2.dp, Color.Red, RoundedCornerShape(8.dp))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "💥 COLLISION CRASH! 💥",
                        color = Color.Yellow,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "SPEED DECAY PENALTY ACTIVE",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Final Completed Score Summary Dialog Box
        if (gameCompleted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🏁 RACE FINISHED!",
                            color = Color.Yellow,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Divider(color = Color.DarkGray)

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("COMPLETED TIME:", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                Text(formatTime(gameTimeMillis), color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("COINS COLLECTED:", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                Text("+$totalCoinsCollected", color = Color.Green, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            }

                            if (isMultiplayer) {
                                val userWinner = progressDistance >= rivalDistance
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("DUEL VS $rivalName:", color = Color.Gray, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                                    Text(
                                        text = if (userWinner) "VICTORY! (+150g)" else "DEFEAT",
                                        color = if (userWinner) Color.Cyan else Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Divider(color = Color.DarkGray)

                        Button(
                            onClick = {
                                isPlaying = false
                                viewModel.setGameState(GameViewModel.GameState.MAIN_MENU)
                                onBack()
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                        ) {
                            Text("BACK TO MENU", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// Low-level Vector graphics pseudo-3D game layout engine logic drawn on Canvas
private fun DrawScope.drawGameRoad(
    theme: String,
    progressDistance: Float,
    playerX: Float,
    trackLength: Float,
    roadElements: List<RoadElement>,
    particles: List<GameParticle>,
    isMultiplayer: Boolean,
    rivalDistance: Float,
    rivalName: String,
    isCrashed: Boolean,
    carIndex: Int
) {
    val canvasWidth = size.width
    val canvasHeight = size.height

    // Horizon coordinates (y-axis center point of pseudo-3D)
    val horizonY = canvasHeight * 0.45f
    val roadWidthHorizon = canvasWidth * 0.08f
    val roadWidthBottom = canvasWidth * 0.85f

    // 1. Draw sky/horizon backgrounds (Desert, Neo Cyber, forest green, Snow white)
    val skyColor = when (theme) {
        "DESERT" -> Color(0xFFFDD835) // warm yellow sky
        "NEON" -> Color(0xFF0D0213)  // deep cyberpunk navy
        "SNOW" -> Color(0xFFB0BEC5)  // cold blue gray
        else -> Color(0xFF64B5F6)     // standard blue sky
    }
    drawRect(color = skyColor, size = Size(canvasWidth, horizonY))

    // Draw horizon sun or mountains
    val ambientSunColor = when (theme) {
        "DESERT" -> Color(0xFFE65100)
        "NEON" -> Color(0xFFEC407A)
        "SNOW" -> Color(0xFFE0F7FA)
        else -> Color(0xFFFFF9C4)
    }
    drawCircle(color = ambientSunColor, radius = 55f, center = Offset(canvasWidth * 0.5f, horizonY - 10f))

    // 2. Draw ground background (grass / sand / dark neon / snow base)
    val groundColor = when (theme) {
        "DESERT" -> Color(0xFFE5C158) // sand yellow
        "NEON" -> Color(0xFF16192E)  // dark grid violet
        "SNOW" -> Color(0xFFECEFF1)  // white snow
        else -> Color(0xFF3B7A23)     // forest green
    }
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, horizonY),
        size = Size(canvasWidth, canvasHeight - horizonY)
    )

    // Segment calculation for roadside grass stripes/scenery curves
    val segmentCount = 20
    for (i in segmentCount downTo 0) {
        val y1Percent = i.toFloat() / segmentCount
        val y2Percent = (i + 1).toFloat() / segmentCount

        // Projection conversion
        val y1 = horizonY + y1Percent * y1Percent * (canvasHeight - horizonY)
        val y2 = horizonY + y2Percent * y2Percent * (canvasHeight - horizonY)

        val w1 = roadWidthHorizon + y1Percent * (roadWidthBottom - roadWidthHorizon)
        val w2 = roadWidthHorizon + y2Percent * (roadWidthBottom - roadWidthHorizon)

        // Road curve bending offset depends on segment
        // Simple trigonometric offset creates gorgeous retro road curves!
        val curveOffset1 = sin((progressDistance * 0.02f) + y1Percent * 3.5f) * 60f * y1Percent
        val curveOffset2 = sin((progressDistance * 0.02f) + y2Percent * 3.5f) * 60f * y2Percent

        // Center projection adjustments based on player steering offset
        val cx1 = canvasWidth * 0.5f - playerX * w1 * 0.35f + curveOffset1
        val cx2 = canvasWidth * 0.5f - playerX * w2 * 0.35f + curveOffset2

        // Alternating color stripes for retro movement feel
        val oddSegment = ((progressDistance / 5f).toInt() + i) % 2 == 0
        
        // Road surface colors
        val roadCol = if (theme == "NEON") {
            if (oddSegment) Color(0xFF0F1122) else Color(0xFF141628)
        } else {
            if (oddSegment) Color(0xFF424242) else Color(0xFF4A4A4A)
        }

        // Kerbs/Margins colors (Red/White on normal tracks, Pink/Neon Cyan on Neon theme)
        val kerbCol = if (theme == "NEON") {
            if (oddSegment) Color(0xFFFF007F) else Color(0xFF00F0FF)
        } else {
            if (oddSegment) Color(0xFFD32F2F) else Color.White
        }

        // Outer secondary margins (e.g. grass accents)
        val outerCol = if (theme == "DESERT") {
            if (oddSegment) Color(0xFFD4B04C) else Color(0xFFE5C158)
        } else if (theme == "SNOW") {
            if (oddSegment) Color(0xFFCFD8DC) else Color(0xFFECEFF1)
        } else {
            if (oddSegment) Color(0xFF2E691E) else Color(0xFF3B7A23)
        }

        // Outer side strips
        drawRect(color = outerCol, topLeft = Offset(0f, y1), size = Size(canvasWidth, y2 - y1))

        // Center Asphalt Road Projection
        val roadPath = Path().apply {
            moveTo(cx1 - w1 * 0.5f, y1)
            lineTo(cx1 + w1 * 0.5f, y1)
            lineTo(cx2 + w2 * 0.5f, y2)
            lineTo(cx2 - w2 * 0.5f, y2)
            close()
        }
        drawPath(path = roadPath, color = roadCol)

        // Left Kerb
        val lKerbPath = Path().apply {
            moveTo(cx1 - w1 * 0.53f, y1)
            lineTo(cx1 - w1 * 0.5f, y1)
            lineTo(cx2 - w2 * 0.5f, y2)
            lineTo(cx2 - w2 * 0.53f, y2)
            close()
        }
        drawPath(path = lKerbPath, color = kerbCol)

        // Right Kerb
        val rKerbPath = Path().apply {
            moveTo(cx1 + w1 * 0.5f, y1)
            lineTo(cx1 + w1 * 0.53f, y1)
            lineTo(cx2 + w2 * 0.53f, y2)
            lineTo(cx2 + w2 * 0.5f, y2)
            close()
        }
        drawPath(path = rKerbPath, color = kerbCol)

        // Middle dash stripes
        if (oddSegment) {
            val dashWidth1 = w1 * 0.02f
            val dashWidth2 = w2 * 0.02f
            val dashPath = Path().apply {
                moveTo(cx1 - dashWidth1 * 0.5f, y1)
                moveTo(cx1 + dashWidth1 * 0.5f, y1)
                lineTo(cx2 + dashWidth2 * 0.5f, y2)
                lineTo(cx2 - dashWidth2 * 0.5f, y2)
                close()
            }
            drawPath(path = dashPath, color = Color.White)
        }
    }

    // 3. Draw Roadside Assets (Retro Pixel Trees / Cacti / Cyber Pillars)
    val randomScenery = Random(2026)
    for (i in 1..25) {
        val itemDist = i * 150f
        val relDist = itemDist - progressDistance
        
        if (relDist in 0f..400f) {
            val progressYPercent = (400f - relDist) / 400f // 0 to 1
            val py = horizonY + progressYPercent * progressYPercent * (canvasHeight - horizonY)
            val pw = roadWidthHorizon + progressYPercent * (roadWidthBottom - roadWidthHorizon)
            val curveOffset = sin((progressDistance * 0.02f) + progressYPercent * 3.5f) * 60f * progressYPercent
            val pcx = canvasWidth * 0.5f - playerX * pw * 0.35f + curveOffset

            // Scale factor based on perspective
            val scale = progressYPercent * progressYPercent * 1.5f
            
            // Alternating side items (Left = -0.7f, Right = 0.7f relative to road edges)
            val sideXSign = if (i % 2 == 0) -1f else 1f
            val sx = pcx + sideXSign * pw * 0.7f

            // Draw nostalgic 8-bit retro trees
            drawRetroPixelTree(theme, sx, py, scale)
        }
    }

    // 4. Draw Spawned Items on Road (Coins, Barriers, CPU/Opponent cars)
    roadElements.forEach { element ->
        val relDist = element.trackDistance - progressDistance
        // Draw details within view distance (0 to 350 meters ahead)
        if (relDist in 0f..350f) {
            val normPercent = (350f - relDist) / 350f // 0 to 1 scaling
            val scaleY = normPercent * normPercent
            
            val py = horizonY + scaleY * (canvasHeight - horizonY)
            val pw = roadWidthHorizon + normPercent * (roadWidthBottom - roadWidthHorizon)
            val curveOffset = sin((progressDistance * 0.02f) + normPercent * 3.5f) * 60f * normPercent
            val pcx = canvasWidth * 0.5f - playerX * pw * 0.35f + curveOffset

            // Calculate exact projected x location on screens
            val px = pcx + element.currentRoadX * pw * 0.5f
            val scale = scaleY * 2.2f // perspective scaling (gets larger as it approaches)

            if (element.type == ElementType.COIN && !element.collected) {
                drawPixelCoin(px, py, scale)
            } else if (element.type == ElementType.BARRIER && !element.crashed) {
                drawPixelBarrier(px, py, scale)
            } else if (element.type == ElementType.CPU_CAR_RED || element.type == ElementType.CPU_CAR_BLUE || element.type == ElementType.CPU_CAR_YELLOW || element.type == ElementType.RIVAL_CAR) {
                // Determine Car style
                val fillCol = when (element.type) {
                    ElementType.RIVAL_CAR -> Color.Red // rival is glowing danger red
                    ElementType.CPU_CAR_BLUE -> Color(0xFF1565C0)
                    ElementType.CPU_CAR_YELLOW -> Color(0xFFFF8F00)
                    else -> Color(0xFF2E7D32)
                }
                
                // Draw AI Car - rotated if spinning/crashed
                if (element.spinAngle != 0f) {
                    rotate(element.spinAngle, Offset(px, py)) {
                        drawPixelCarSprite(fillCol, px, py, scale, headlightsOn = false)
                    }
                } else {
                    drawPixelCarSprite(fillCol, px, py, scale, headlightsOn = true)
                }

                // Label tag above Rival car
                if (element.type == ElementType.RIVAL_CAR) {
                    val labelY = py - 40 * scale
                    drawContext.canvas.nativeCanvas.save()
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.RED
                        textSize = 10f * scale
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        rivalName.uppercase(),
                        px,
                        labelY,
                        textPaint
                    )
                    drawContext.canvas.nativeCanvas.restore()
                }
            }
        }
    }

    // 5. Render Particle FX
    particles.forEach { p ->
        val screenX = p.x * canvasWidth
        val screenY = p.y * canvasHeight
        drawRect(
            color = p.color.copy(alpha = p.life),
            topLeft = Offset(screenX - p.size/2, screenY - p.size/2),
            size = Size(p.size * p.life, p.size * p.life)
        )
    }

    // 6. Draw Player's Main Car (Fixed at the bottom center of the viewport)
    val userCarX = canvasWidth * 0.5f
    val userCarY = canvasHeight * 0.82f
    val playerCarScale = 3.6f

    val playerCarCol = when (carIndex) {
        0 -> Color(0xFFE53935) // Red
        1 -> Color(0xFF1E88E5) // Blue
        2 -> Color(0xFFFFB300) // Yellow
        else -> Color(0xFF43A047) // Green
    }

    // Flash car slightly white if crashed penalty is active
    val displayCarColor = if (isCrashed && (System.currentTimeMillis() / 150) % 2 == 0L) {
        Color.White
    } else {
        playerCarCol
    }

    drawPixelCarSprite(
        bodyColor = displayCarColor,
        centerX = userCarX,
        centerY = userCarY,
        scale = playerCarScale,
        headlightsOn = false
    )
}

// Draw classic retro pixel blocks tree with solid canvas coordinates
private fun DrawScope.drawRetroPixelTree(theme: String, cx: Float, cy: Float, scale: Float) {
    if (scale < 0.1f) return
    val trunkWidth = 10f * scale
    val trunkHeight = 25f * scale

    // Trunk
    drawRect(
        color = Color(0xFF5D4037),
        topLeft = Offset(cx - trunkWidth / 2, cy - trunkHeight),
        size = Size(trunkWidth, trunkHeight)
    )

    // Foliage (Blocks of pixel leaves)
    val leafColor = when (theme) {
        "DESERT" -> Color(0xFF8D6E63) // dead dry shrub
        "SNOW" -> Color.White       // snow-capped fir tree
        else -> Color(0xFF1B5E20)     // lush green pine-tree
    }

    val path = Path().apply {
        moveTo(cx, cy - trunkHeight - 40 * scale)
        lineTo(cx - 25 * scale, cy - trunkHeight)
        lineTo(cx + 25 * scale, cy - trunkHeight)
        close()
    }
    drawPath(path = path, color = leafColor)

    // Shadow details for retro feel
    drawCircle(
        color = Color.Black.copy(alpha = 0.15f),
        radius = 12f * scale,
        center = Offset(cx, cy)
    )
}

// 8-bit Pixel golden Coin design
private fun DrawScope.drawPixelCoin(cx: Float, cy: Float, scale: Float) {
    if (scale < 0.1f) return
    val r = 10f * scale
    // Thick outline retro coin
    drawCircle(color = Color(0xFFE65100), radius = r + 2f * scale, center = Offset(cx, cy))
    drawCircle(color = Color(0xFFFFEB3B), radius = r, center = Offset(cx, cy))
    // Center star details
    drawRect(
        color = Color(0xFFFF9800),
        topLeft = Offset(cx - 3 * scale, cy - 3 * scale),
        size = Size(6 * scale, 6 * scale)
    )
}

// Classic concrete striped hazard road block barricade
private fun DrawScope.drawPixelBarrier(cx: Float, cy: Float, scale: Float) {
    if (scale < 0.1f) return
    val w = 32f * scale
    val h = 12f * scale

    // Feet
    drawRect(color = Color.DarkGray, topLeft = Offset(cx - w/2 - 4, cy - 5), size = Size(8f, 6f))
    drawRect(color = Color.DarkGray, topLeft = Offset(cx + w/2 - 4, cy - 5), size = Size(8f, 6f))

    // Body Block
    drawRect(color = Color.Black, topLeft = Offset(cx - w/2, cy - h - 5), size = Size(w, h))
    // Diagonal yellow visual indicators
    for (i in 0..4) {
        val dx = cx - w/2 + i * (w/4)
        val stripePath = Path().apply {
            moveTo(dx, cy - h - 5)
            lineTo(dx + 4 * scale, cy - h - 5)
            lineTo(dx + 12 * scale, cy - 5)
            lineTo(dx + 8 * scale, cy - 5)
            close()
        }
        drawPath(path = stripePath, color = Color.Yellow)
    }
}

// Top-down retro arcade styled pixel race car! Done on high-speed vectors
private fun DrawScope.drawPixelCarSprite(
    bodyColor: Color,
    centerX: Float,
    centerY: Float,
    scale: Float,
    headlightsOn: Boolean
) {
    if (scale < 0.1f) return
    
    val bodyW = 22f * scale
    val bodyH = 34f * scale

    // 1. Draw 4 Pixel Wheels (Chunky Black Blocks)
    val wheelW = 5f * scale
    val wheelH = 9f * scale
    val offsetW = bodyW / 2 + 1f * scale
    
    val wheelsCoordonates = listOf(
        Offset(centerX - offsetW, centerY - bodyH / 2 + 2 * scale),     // TL
        Offset(centerX + offsetW - wheelW, centerY - bodyH / 2 + 2 * scale), // TR
        Offset(centerX - offsetW, centerY + bodyH / 2 - wheelH - 2 * scale), // BL
        Offset(centerX + offsetW - wheelW, centerY + bodyH / 2 - wheelH - 2 * scale) // BR
    )
    wheelsCoordonates.forEach { offset ->
        drawRect(color = Color.Black, topLeft = offset, size = Size(wheelW, wheelH))
    }

    // 2. Draw Main Outer Chassis
    drawRect(
        color = Color.Black, // dark stroke outline
        topLeft = Offset(centerX - bodyW / 2, centerY - bodyH / 2),
        size = Size(bodyW, bodyH)
    )
    drawRect(
        color = bodyColor,
        topLeft = Offset(centerX - bodyW / 2 + 1f * scale, centerY - bodyH / 2 + 1f * scale),
        size = Size(bodyW - 2f * scale, bodyH - 2f * scale)
    )

    // 3. Cockpit Window Glass (Black or Cyan)
    val glassW = bodyW - 6f * scale
    val glassH = 10f * scale
    drawRect(
        color = Color.Black,
        topLeft = Offset(centerX - glassW / 2, centerY - 2f * scale),
        size = Size(glassW, glassH)
    )
    drawRect(
        color = Color(0xFF80DEEA), // cyan reflection
        topLeft = Offset(centerX - glassW / 2 + 1f * scale, centerY - 1f * scale),
        size = Size(glassW - 2f * scale, glassH - 2f * scale)
    )

    // 4. Rear Spoiler Wing
    val wingW = bodyW + 4f * scale
    val wingH = 4f * scale
    drawRect(
        color = Color.Black,
        topLeft = Offset(centerX - wingW/2, centerY + bodyH/2 - wingH),
        size = Size(wingW, wingH)
    )
    drawRect(
        color = bodyColor,
        topLeft = Offset(centerX - wingW/2 + 1f * scale, centerY + bodyH/2 - wingH + 1f * scale),
        size = Size(wingW - 2f * scale, wingH - 2f * scale)
    )

    // 5. Headlights details
    if (headlightsOn) {
        drawRect(color = Color.Yellow, topLeft = Offset(centerX - bodyW/2 + 2 * scale, centerY - bodyH/2), size = Size(3 * scale, 1.5f * scale))
        drawRect(color = Color.Yellow, topLeft = Offset(centerX + bodyW/2 - 5 * scale, centerY - bodyH/2), size = Size(3 * scale, 1.5f * scale))
    } else {
        // Red taillights details for back orientation
        drawRect(color = Color.Red, topLeft = Offset(centerX - bodyW/2 + 2 * scale, centerY + bodyH/2 - 2 * scale), size = Size(3 * scale, 1.5f * scale))
        drawRect(color = Color.Red, topLeft = Offset(centerX + bodyW/2 - 5 * scale, centerY + bodyH/2 - 2 * scale), size = Size(3 * scale, 1.5f * scale))
    }
}

private fun formatTime(millis: Long): String {
    val sec = (millis / 1000) % 60
    val min = (millis / 60000)
    val ms = (millis % 1000) / 10
    return String.format(Locale.getDefault(), "%02d:%02d.%02d", min, sec, ms)
}
