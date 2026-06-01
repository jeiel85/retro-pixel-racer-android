package com.jeiel85.retropixelracer.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Custom 8-bit synthesizer engine playing authentic retro music and SFX using AudioTrack.
 * No external file assets are needed - all audio is dynamically synthesized!
 */
class RetroSoundManager {

    private val sampleRate = 22050
    private var isMuted = false
    private var bgmJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val activeTrackCount = java.util.concurrent.atomic.AtomicInteger(0)
    private val MAX_ACTIVE_TRACKS = 6

    // Notes mapped to frequencies for retroactive melodies (8-bit square wave)
    private val NoteC4 = 261.63f
    private val NoteD4 = 293.66f
    private val NoteE4 = 329.63f
    private val NoteF4 = 349.23f
    private val NoteG4 = 392.00f
    private val NoteA4 = 440.00f
    private val NoteB4 = 493.88f
    private val NoteC5 = 523.25f
    private val NoteD5 = 587.33f
    private val NoteE5 = 659.25f
    private val NoteF5 = 698.46f
    private val NoteG5 = 783.99f
    private val NoteA5 = 880.00f
    private val NoteB5 = 987.77f

    fun setMute(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopBgm()
        }
    }

    /**
     * Synthesizes and plays a simple square wave sound effect.
     */
    fun playCoinSfx() {
        if (isMuted) return
        coroutineScope.launch {
            // Coin sound is typically a fast chirp: Note B5 then Note E6
            val note1 = NoteB5
            val note2 = note1 * 1.5f // pitch shift for chime
            
            val duration1 = 0.08f
            val duration2 = 0.15f
            
            val pcm1 = generateSquareWave(note1, duration1, volume = 0.2f)
            val pcm2 = generateSquareWave(note2, duration2, volume = 0.2f)
            
            playRawPcm(pcm1 + pcm2)
        }
    }

    fun playCrashSfx() {
        if (isMuted) return
        coroutineScope.launch {
            // Explosion sound is randomized white/pink noise decaying fast
            val duration = 0.5f
            val numSamples = (sampleRate * duration).toInt()
            val pcm = ShortArray(numSamples)
            var currentVolume = 0.35f
            
            for (i in 0 until numSamples) {
                // Decay the volume for a fade out crunch
                currentVolume *= 0.9998f
                // White noise random sample
                val noise = (Math.random() * 2.0 - 1.0) * Short.MAX_VALUE
                pcm[i] = (noise * currentVolume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            playRawPcm(pcm)
        }
    }

    fun playUpgradeSfx() {
        if (isMuted) return
        coroutineScope.launch {
            // Slide frequency upward (pitch sweep)
            val duration = 0.3f
            val numSamples = (sampleRate * duration).toInt()
            val pcm = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val progress = i.toFloat() / numSamples
                // Sweep frequency from 300Hz to 900Hz
                val frequency = 300f + progress * 700f
                val t = i.toFloat() / sampleRate
                
                // Square wave
                val sine = sin(2 * Math.PI * frequency * t)
                val squareValue = if (sine >= 0) 1 else -1
                
                pcm[i] = (squareValue * Short.MAX_VALUE * 0.15f).toInt().toShort()
            }
            playRawPcm(pcm)
        }
    }

    fun playCountDownBeep(highPitch: Boolean) {
        if (isMuted) return
        coroutineScope.launch {
            val frequency = if (highPitch) NoteC5 * 2 else NoteC5
            val duration = 0.25f
            val pcm = generateSquareWave(frequency, duration, volume = 0.2f)
            playRawPcm(pcm)
        }
    }

    fun playEngineRevSfx(speedPercent: Float) {
        if (isMuted) return
        // We can synthesize a short engine buzz depending on the RPM/Speed
        coroutineScope.launch {
            val baseFrequency = 80f + speedPercent * 180f
            val duration = 0.1f
            // Engine uses dirty saw/square wave
            val numSamples = (sampleRate * duration).toInt()
            val pcm = ShortArray(numSamples)
            
            for (i in 0 until numSamples) {
                val t = i.toFloat() / sampleRate
                val phase = (baseFrequency * t) % 1.0
                val sawValue = (phase * 2.0 - 1.0) // sawtooth
                val squareValue = if (sin(2 * Math.PI * baseFrequency * t) >= 0) 0.5f else -0.5f
                
                pcm[i] = ((sawValue * 0.7f + squareValue * 0.3f) * Short.MAX_VALUE * 0.08f).toInt().toShort()
            }
            playRawPcm(pcm)
        }
    }

    /**
     * Background retro music (8-bit loop) running continuously in a worker job.
     */
    fun startBgm() {
        if (isMuted) return
        if (bgmJob != null && bgmJob?.isActive == true) return

        bgmJob = coroutineScope.launch {
            val engine = createAudioTrack(sampleRate) ?: return@launch
            activeTrackCount.incrementAndGet()
            try {
                engine.play()
                
                // A classic upbeat retro bassline and synth arpeggio:
                // Chord progression: Am, F, C, G
                val chAm = listOf(NoteA4, NoteC5, NoteE5)
                val chF = listOf(NoteF4, NoteA4, NoteC5)
                val chC = listOf(NoteC4, NoteE4, NoteG4)
                val chG = listOf(NoteG4, NoteB4, NoteD5)
                
                val progression = listOf(chAm, chF, chC, chG)
                var progressionIndex = 0
                
                while (bgmJob?.isActive == true) {
                    val currentChord = progression[progressionIndex % progression.size]
                    
                    // Generate an energetic 8-bit racing pattern (8 steps of 150ms each)
                    for (step in 0..7) {
                        if (isMuted || bgmJob?.isActive != true) break
                        
                        // Select note based on arpeggio step
                        val note = when(step) {
                            0 -> currentChord[0] // root
                            1 -> currentChord[1] // third
                            2 -> currentChord[2] // fifth
                            3 -> currentChord[1]
                            4 -> currentChord[0] * 2 // octave up root
                            5 -> currentChord[2]
                            6 -> currentChord[1]
                            7 -> currentChord[2] * 1.5f // high tension note
                            else -> currentChord[0]
                        }
                        
                        // 150 milliseconds step
                        val noteDuration = 0.14f
                        val restDuration = 0.01f
                        
                        // Synthesize the step
                        val pcm = generateSquareWave(note, noteDuration, volume = 0.06f)
                        engine.write(pcm, 0, pcm.size)
                        
                        // Rest delay between notes
                        delay((restDuration * 1000).toLong())
                    }
                    progressionIndex++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    engine.stop()
                    engine.release()
                } catch (e: Exception) {}
                activeTrackCount.decrementAndGet()
            }
        }
    }

    fun stopBgm() {
        bgmJob?.cancel()
        bgmJob = null
    }

    // Helper functions
    private fun generateSquareWave(frequency: Float, duration: Float, volume: Float = 0.15f): ShortArray {
        val numSamples = (sampleRate * duration).toInt()
        val pcm = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val t = i.toFloat() / sampleRate
            val sine = sin(2 * Math.PI * frequency * t)
            val squareValue = if (sine >= 0) 1 else -1
            pcm[i] = (squareValue * Short.MAX_VALUE * volume).toInt().toShort()
        }
        return pcm
    }

    private fun playRawPcm(pcm: ShortArray) {
        val track = createAudioTrack(sampleRate) ?: return
        activeTrackCount.incrementAndGet()
        try {
            track.play()
            track.write(pcm, 0, pcm.size)
            track.stop()
            track.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            activeTrackCount.decrementAndGet()
        }
    }

    private fun createAudioTrack(rate: Int): AudioTrack? {
        if (activeTrackCount.get() >= MAX_ACTIVE_TRACKS) return null
        val minBufferSize = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    rate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
