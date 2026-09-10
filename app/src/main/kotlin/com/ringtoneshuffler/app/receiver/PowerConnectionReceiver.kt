package com.ringtoneshuffler.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ringtoneshuffler.app.ui.screens.ChargingAnimationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        if (action == Intent.ACTION_POWER_CONNECTED) {
            // Launch the charging animation activity
            val animationIntent = Intent(context, ChargingAnimationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(animationIntent)
        }
    }
}
