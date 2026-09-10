package com.ringtoneshuffler.app.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.ringtoneshuffler.app.MainActivity
import com.ringtoneshuffler.app.RingtoneShufflerApp

/**
 * ShufflerService — persistent ForegroundService that handles ringtone rotation.
 *
 * Survival strategy (survives recent-apps swipe & OEM battery killers):
 *  1. stopWithTask="false"  in Manifest  → not killed on task removal
 *  2. START_STICKY          in onStartCommand → OS restarts after kill
 *  3. onTaskRemoved()       → schedules AlarmManager restart (3 sec)
 *  4. onDestroy()           → schedules AlarmManager restart (3 sec)
 *  5. BootReceiver          → restarts after reboot
 *  6. CallReceiver          → startForegroundService() restarts if dead
 */
class ShufflerService : Service() {

    private lateinit var prefsHelper: com.ringtoneshuffler.app.data.PrefsHelper

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        prefsHelper = (applicationContext as RingtoneShufflerApp).prefsHelper
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(currentRingtoneName()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHUFFLE -> {
                val nextUri = prefsHelper.advanceToNextRingtone(this)
                prefsHelper.incrementCallsLogged()
                updateNotification(nextUri)
            }
            ACTION_STOP -> {
                // User explicitly stopped — cancel any pending restarts
                cancelAlarmRestart()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY   // Don't restart after explicit stop
            }
        }
        // START_STICKY: OS will restart this service after killing it,
        // delivering a null intent. We handle that gracefully above.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Called when user swipes the app from Recent Apps.
     * stopWithTask="false" already prevents immediate kill,
     * but we schedule an AlarmManager restart as a belt-and-suspenders safety net
     * for aggressive OEMs (Xiaomi MIUI, Samsung OneUI, etc.)
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleAlarmRestart(delayMs = 3_000L)
    }

    /**
     * Called when Android kills the service (e.g. extreme memory pressure, OEM killer).
     * Schedule AlarmManager to bring it back.
     */
    override fun onDestroy() {
        super.onDestroy()
        // Only auto-restart if shuffle is still enabled (not explicitly stopped by user)
        if (prefsHelper.isShuffleEnabled()) {
            scheduleAlarmRestart(delayMs = 3_000L)
        }
    }

    // ── AlarmManager restart ───────────────────────────────────────────────────

    /**
     * Schedules a one-shot alarm to restart the service after [delayMs] milliseconds.
     * Works even after the app process is killed, because AlarmManager is a system service.
     */
    private fun scheduleAlarmRestart(delayMs: Long) {
        val restartIntent = Intent(this, ShufflerService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            RESTART_REQUEST_CODE,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = SystemClock.elapsedRealtime() + delayMs
        // RTC_WAKEUP wakes the device if sleeping; ELAPSED_REALTIME_WAKEUP is battery-friendlier
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAt,
            pendingIntent
        )
    }

    private fun cancelAlarmRestart() {
        val restartIntent = Intent(this, ShufflerService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            RESTART_REQUEST_CODE,
            restartIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pendingIntent)
    }

    // ── Notification ───────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ringtone Shuffler",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors calls to rotate your ringtone pool"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(ringtoneName: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // "Stop service" action in notification
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ShufflerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ringtone Shuffler Active")
            .setContentText("Now: $ringtoneName")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(nextUri: Uri?) {
        val name = nextUri?.let { com.ringtoneshuffler.app.utils.getDisplayNameFromUri(this, it) } ?: "Unknown"
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(name))
    }

    private fun currentRingtoneName(): String {
        val uri = prefsHelper.getCurrentRingtoneUri()
        return uri?.let { com.ringtoneshuffler.app.utils.getDisplayNameFromUri(this, it) } ?: "None"
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        const val CHANNEL_ID          = "shuffler_channel"
        const val NOTIFICATION_ID     = 1001
        const val RESTART_REQUEST_CODE = 9001

        const val ACTION_SHUFFLE = "com.ringtoneshuffler.app.ACTION_SHUFFLE"
        const val ACTION_STOP    = "com.ringtoneshuffler.app.ACTION_STOP"

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ShufflerService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ShufflerService::class.java).apply { action = ACTION_STOP }
            )
        }

        fun shuffle(context: Context) {
            // startForegroundService() also RESTARTS the service if it was killed —
            // so CallReceiver calling this is itself a resurrection mechanism.
            context.startForegroundService(
                Intent(context, ShufflerService::class.java).apply { action = ACTION_SHUFFLE }
            )
        }
    }
}



