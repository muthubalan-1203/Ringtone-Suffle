package com.ringtoneshuffler.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ringtoneshuffler.app.ui.theme.ElectricViolet
import com.ringtoneshuffler.app.ui.theme.GlassWhite
import com.ringtoneshuffler.app.ui.theme.SignalEmerald
import kotlin.math.sin

/**
 * Animated audio waveform indicator.
 * Shows 28 vertical bars that oscillate when [isActive] is true.
 */
@Composable
fun WaveformIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 28,
    totalHeight: Dp = 40.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveformPhase"
    )

    val activeGradient = Brush.verticalGradient(
        colors = listOf(SignalEmerald, ElectricViolet)
    )
    val inactiveColor = GlassWhite.copy(alpha = 0.15f)

    Row(
        modifier = modifier.height(totalHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val fraction = if (isActive) {
                val wave = sin(phase + index * 0.45f).toFloat()
                // Normalize to 0.15..1.0
                (wave + 1f) / 2f * 0.85f + 0.15f
            } else {
                0.15f  // minimal static bars when inactive
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) activeGradient else Brush.verticalGradient(listOf(inactiveColor, inactiveColor)))
            )
        }
    }
}
