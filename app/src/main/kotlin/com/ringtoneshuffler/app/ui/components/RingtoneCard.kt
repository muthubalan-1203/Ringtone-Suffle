package com.ringtoneshuffler.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ringtoneshuffler.app.ui.theme.ElectricViolet
import com.ringtoneshuffler.app.ui.theme.ElectricVioletDim
import com.ringtoneshuffler.app.ui.theme.ErrorRedBright
import com.ringtoneshuffler.app.ui.theme.GlassBorder
import com.ringtoneshuffler.app.ui.theme.GlassWhite
import com.ringtoneshuffler.app.ui.theme.OnSurface
import com.ringtoneshuffler.app.ui.theme.OnSurfaceVariant
import com.ringtoneshuffler.app.ui.theme.SignalEmerald
import com.ringtoneshuffler.app.ui.theme.SurfaceContainer

/**
 * Track row card matching Stitch "Audio Item Cards & Lists" spec:
 * - Left  : index badge (rounded square, violet gradient bg)
 * - Middle: file name + "NOW" badge if active; secondary sub-label
 * - Right : drag handle + star toggle + delete
 */
@Composable
fun RingtoneCard(
    uri: Uri,
    index: Int,
    isActive: Boolean,
    isStarred: Boolean,
    isPlaying: Boolean,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = com.ringtoneshuffler.app.utils.rememberDisplayName(uri)

    val cardBg = if (isActive) {
        Brush.horizontalGradient(
            listOf(
                ElectricViolet.copy(alpha = 0.22f),
                ElectricVioletDim.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.horizontalGradient(
            listOf(SurfaceContainer, SurfaceContainer)
        )
    }

    val cardBorder = if (isActive) GlassBorder.copy(alpha = 0.3f) else GlassBorder

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Index badge ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(ElectricViolet, ElectricVioletDim)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.titleMedium,
                color = GlassWhite
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // ── Name + badges ─────────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(SignalEmerald.copy(alpha = 0.18f), RoundedCornerShape(9999.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(SignalEmerald, CircleShape)
                            )
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = SignalEmerald
                            )
                        }
                    }
                }
            }
            Text(
                text = uri.scheme ?: "audio",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceVariant,
                maxLines = 1
            )
        }

        // ── Actions ───────────────────────────────────────────────────────────
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Drag to reorder",
            tint = OnSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )

        IconButton(onClick = if (isPlaying) onStop else onPlay) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop" else "Play",
                tint = if (isPlaying) SignalEmerald else OnSurfaceVariant
            )
        }

        IconButton(onClick = onToggleStar) {
            Icon(
                imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = if (isStarred) "Unstar" else "Star",
                tint = if (isStarred) SignalEmerald else OnSurfaceVariant
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove ringtone",
                tint = ErrorRedBright.copy(alpha = 0.7f)
            )
        }
    }
}
