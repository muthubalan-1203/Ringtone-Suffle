package com.ringtoneshuffler.app.service

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.ringtoneshuffler.app.data.PrefsHelper
import kotlinx.coroutines.*

object FadeInManager {
    private const val TAG = "FadeInManager"
    private var fadeJob: Job? = null
    private var originalVolume: Int = -1
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startFadeIn(context: Context, prefs: PrefsHelper) {
        if (!prefs.isFadeInEnabled()) return

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        
        // If it's already muted or in vibrate mode, don't mess with it
        if (currentVolume == 0 || audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return
        }

        // Store original volume
        originalVolume = currentVolume
        
        // Set volume to 1 (lowest audible) to start fade-in
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 1, 0)
        
        val durationSeconds = prefs.getFadeInDuration()
        val targetVolume = originalVolume
        
        val steps = targetVolume - 1
        if (steps <= 0) return
        
        // Calculate delay per step
        val totalDurationMs = durationSeconds * 1000L
        val stepDelayMs = totalDurationMs / steps

        fadeJob?.cancel()
        fadeJob = scope.launch {
            try {
                Log.d(TAG, "Starting fade-in. Target volume: $targetVolume over ${durationSeconds}s")
                for (vol in 2..targetVolume) {
                    delay(stepDelayMs)
                    if (!isActive) break
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, vol, 0)
                    Log.d(TAG, "Faded volume to $vol")
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Fade-in cancelled")
            } finally {
                // We do NOT restore volume here if cancelled, because cancel() is called
                // on stopFadeIn() which explicitly restores volume.
            }
        }
    }

    fun stopFadeIn(context: Context) {
        fadeJob?.cancel()
        fadeJob = null
        
        if (originalVolume != -1) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            // Only restore if we are still in normal ringer mode
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, originalVolume, 0)
                Log.d(TAG, "Restored volume to $originalVolume")
            }
            originalVolume = -1
        }
    }
}
