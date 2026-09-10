package com.ringtoneshuffler.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ringtoneshuffler.app.ui.theme.CyanFrequency
import com.ringtoneshuffler.app.ui.theme.ErrorRedBright
import com.ringtoneshuffler.app.ui.theme.SignalEmerald

enum class PermissionStatus { GRANTED, DENIED, INFO }

/**
 * Pill-shaped permission status badge with pulsing dot indicator.
 * Matches Stitch "Permission Status Badges" component spec.
 */
@Composable
fun PermissionBadge(
    label: String,
    status: PermissionStatus,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgColor, textColor) = when (status) {
        PermissionStatus.GRANTED -> Triple(
            SignalEmerald,
            SignalEmerald.copy(alpha = 0.12f),
            SignalEmerald
        )
        PermissionStatus.DENIED  -> Triple(
            ErrorRedBright,
            ErrorRedBright.copy(alpha = 0.12f),
            ErrorRedBright
        )
        PermissionStatus.INFO    -> Triple(
            CyanFrequency,
            CyanFrequency.copy(alpha = 0.12f),
            CyanFrequency
        )
    }

    // Pulsing animation for the dot
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == PermissionStatus.GRANTED) 1.4f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(9999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .scale(scale)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium
        )
    }
}
