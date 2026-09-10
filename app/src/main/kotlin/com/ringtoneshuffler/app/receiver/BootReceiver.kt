package com.ringtoneshuffler.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ringtoneshuffler.app.RingtoneShufflerApp

/**
 * BootReceiver — wakes up once after device boot.
 *
 * Since we use a pure BroadcastReceiver architecture (no foreground service),
 * there is nothing to "start" on boot. The CallReceiver is manifest-registered
 * and will wake up automatically on the next call.
 *
 * This receiver simply ensures the app process has been initialised at least once
 * so SharedPreferences are accessible to CallReceiver on the very first call
 * after a cold boot.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.LOCKED_BOOT_COMPLETED") return

        // Touch the PrefsHelper to initialise SharedPreferences.
        // CallReceiver will handle the next real call automatically.
        val app = context.applicationContext as? RingtoneShufflerApp ?: return
        val prefs = app.prefsHelper

        // Nothing else needed — no service to start, no notification to show.
        // Log boot for debugging purposes only.
        if (prefs.isShuffleEnabled()) {
            // Ready — CallReceiver will fire on next incoming call
            com.ringtoneshuffler.app.service.ShufflerService.start(context)
        }
    }
}
