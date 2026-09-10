package com.ringtoneshuffler.app.ui.screens

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ringtoneshuffler.app.ui.theme.ElectricViolet
import com.ringtoneshuffler.app.ui.theme.SignalEmerald
import com.ringtoneshuffler.app.R

class ChargingAnimationActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show over lockscreen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Play default sound for now (TODO: get from preferences)
        playChargingSound()

        setContent {
            ChargingScreen()
        }

        // Auto-close after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isDestroyed) {
                finish()
            }
        }, 3000)
    }

    private fun playChargingSound() {
        try {
            // Using a default notification sound if no raw resource exists
            // Since we don't have a raw folder yet, we can use the system default notification URI
            val ringtoneUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer.create(this, ringtoneUri)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

@Composable
fun ChargingScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "charging")
    val radius by infiniteTransition.animateFloat(
        initialValue = 50f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(400.dp)) {
            drawCircle(
                color = SignalEmerald.copy(alpha = alpha),
                radius = radius,
                style = Stroke(width = 8f)
            )
            drawCircle(
                color = ElectricViolet.copy(alpha = alpha * 0.5f),
                radius = radius * 0.7f,
                style = Stroke(width = 12f)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "⚡",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SUPERCHARGING",
                color = SignalEmerald,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
        }
    }
}
