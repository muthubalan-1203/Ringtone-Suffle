package com.ringtoneshuffler.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.ringtoneshuffler.app.RingtoneShufflerApp
import com.ringtoneshuffler.app.data.PrefsHelper
import com.ringtoneshuffler.app.service.FadeInManager

/**
 * CallReceiver — manifest-registered BroadcastReceiver.
 *
 * Android wakes this automatically on every call state change,
 * even when the app is fully closed or the phone just rebooted.
 * No foreground service needed → zero persistent notification.
 *
 * Call state flows:
 *   Incoming answered  : RINGING → OFFHOOK → IDLE
 *   Incoming missed    : RINGING → IDLE
 *   Incoming rejected  : RINGING → IDLE
 *   Outgoing           : NEW_OUTGOING_CALL → OFFHOOK → IDLE  (no shuffle)
 *
 * Trigger modes (TRIGGER_IDLE is default):
 *   TRIGGER_IDLE     → rotates on IDLE after any INCOMING call
 *   TRIGGER_OUTGOING → rotates immediately when dialling out
 *   TRIGGER_RINGING  → rotates the moment an incoming call starts ringing
 */
class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app   = context.applicationContext as? RingtoneShufflerApp ?: return
        val prefs = app.prefsHelper

        // Global guard — do nothing if shuffle is disabled or pool is empty
        if (!prefs.isShuffleEnabled()) return
        if (prefs.getSavedUris().isEmpty()) return

        val action = intent.action ?: return

        when (action) {

            // ── Outgoing call ─────────────────────────────────────────────────
            Intent.ACTION_NEW_OUTGOING_CALL -> {
                prefs.setCallWasRinging(false) // outgoing — not a ring event
                prefs.setCallWasActive(true)
                if (prefs.getTriggerMoment() == PrefsHelper.TRIGGER_OUTGOING) {
                    rotateRingtone(context, app, prefs)
                }
            }

            // ── Incoming call / state change ──────────────────────────────────
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

                when (state) {

                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        // Incoming call started ringing (answered OR missed OR rejected)
                        prefs.setCallWasRinging(true)
                        prefs.setCallWasActive(true)
                        if (prefs.getTriggerMoment() == PrefsHelper.TRIGGER_RINGING) {
                            rotateRingtone(context, app, prefs)
                        }
                        // Start Fade-in if enabled
                        FadeInManager.startFadeIn(context, prefs)
                        
                        // Start Edge Lighting
                        if (android.provider.Settings.canDrawOverlays(context)) {
                            val edgeIntent = Intent(context, com.ringtoneshuffler.app.service.EdgeLightingService::class.java).apply {
                                setAction(com.ringtoneshuffler.app.service.EdgeLightingService.ACTION_START)
                            }
                            context.startService(edgeIntent)
                        }
                        
                        // Start Haptic Beat Sync (Generic pulsing for now)
                        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                        if (vibrator?.hasVibrator() == true) {
                            val pattern = longArrayOf(0, 300, 200, 300, 200)
                            vibrator.vibrate(pattern, 0)
                        }
                    }

                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        // Call was answered (or outgoing dialling)
                        prefs.setCallWasActive(true)
                        FadeInManager.stopFadeIn(context)
                        
                        // Stop Edge Lighting and Haptics
                        context.startService(Intent(context, com.ringtoneshuffler.app.service.EdgeLightingService::class.java).apply {
                            setAction(com.ringtoneshuffler.app.service.EdgeLightingService.ACTION_STOP)
                        })
                        (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.cancel()
                    }

                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        val wasRinging = prefs.getCallWasRinging()

                        // Reset flags
                        prefs.setCallWasActive(false)
                        prefs.setCallWasRinging(false)
                        FadeInManager.stopFadeIn(context)
                        
                        // Stop Edge Lighting and Haptics
                        context.startService(Intent(context, com.ringtoneshuffler.app.service.EdgeLightingService::class.java).apply {
                            setAction(com.ringtoneshuffler.app.service.EdgeLightingService.ACTION_STOP)
                        })
                        (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.cancel()

                        if (prefs.getTriggerMoment() == PrefsHelper.TRIGGER_IDLE) {
                            // Only trigger for INCOMING calls (answered / missed / rejected)
                            if (wasRinging) {
                                rotateRingtone(context, app, prefs)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Advances to the next ringtone directly — no service, no notification.
     * BroadcastReceiver has ~10 seconds to complete; ringtone change is instant.
     */
    private fun rotateRingtone(context: Context, app: RingtoneShufflerApp, prefs: PrefsHelper) {
        val nextUri = prefs.advanceToNextRingtone(context)
        if (nextUri != null) {
            app.historyManager.addHistoryItem(nextUri)
        }
        prefs.incrementCallsLogged()
    }
}
