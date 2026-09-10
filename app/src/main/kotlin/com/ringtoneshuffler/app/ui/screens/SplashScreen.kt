package com.ringtoneshuffler.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ringtoneshuffler.app.ui.components.WaveformIndicator
import com.ringtoneshuffler.app.ui.theme.CyanFrequencyLight
import com.ringtoneshuffler.app.ui.theme.ElectricVioletDark
import com.ringtoneshuffler.app.ui.theme.ElectricVioletLight
import com.ringtoneshuffler.app.ui.theme.OnSurface
import com.ringtoneshuffler.app.ui.theme.OnSurfaceVariant
import com.ringtoneshuffler.app.ui.theme.PlusJakartaSans
import com.ringtoneshuffler.app.ui.theme.SurfaceObsidian
import kotlinx.coroutines.delay

/**
 * SplashScreen — shown once on app launch.
 *
 * Sequence:
 *   0ms   → fade-in + scale-up app name & waveform
 *   800ms → "Made by Muthubalan" fades in
 *   2500ms → navigate to HomeScreen
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    // ── Animatables ────────────────────────────────────────────────────────────
    val logoAlpha  = remember { Animatable(0f) }
    val logoScale  = remember { Animatable(0.7f) }
    val bylineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1 — logo animates in
        logoAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = EaseOutCubic)
        )
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = EaseOutCubic)
        )

        // Step 2 — byline fades in after a beat
        delay(200)
        bylineAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )

        // Step 3 — hold, then navigate
        delay(1400)
        onSplashComplete()
    }

    // ── UI ─────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ElectricVioletDark.copy(alpha = 0.35f),
                        SurfaceObsidian
                    ),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(logoAlpha.value)
                .scale(logoScale.value)
        ) {
            // Animated waveform as logo
            WaveformIndicator(
                isActive = true,
                modifier = Modifier.size(width = 120.dp, height = 56.dp)
            )

            Spacer(Modifier.height(24.dp))

            // App name
            Text(
                text = "Ringtone Shuffler",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = OnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            // Tagline
            Text(
                text = "Your ringtone. Always fresh.",
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // ── Made by ───────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(bylineAlpha.value)
            ) {
                Text(
                    text = "Made by",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Light,
                    fontSize = 13.sp,
                    color = OnSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                // Name with gradient
                Text(
                    text = "Muthubalan",
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = ElectricVioletLight,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
