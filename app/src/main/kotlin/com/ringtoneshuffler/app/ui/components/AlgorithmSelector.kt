package com.ringtoneshuffler.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ringtoneshuffler.app.data.PrefsHelper
import com.ringtoneshuffler.app.ui.theme.ElectricViolet
import com.ringtoneshuffler.app.ui.theme.GlassBorder
import com.ringtoneshuffler.app.ui.theme.GlassWhite
import com.ringtoneshuffler.app.ui.theme.OnSurface
import com.ringtoneshuffler.app.ui.theme.SurfaceContainerHigh

/**
 * 3-chip algorithm selector: Random / Sequential / Starred.
 * Matches Stitch "System Tabs" component — active chip has violet gradient underline/fill.
 */
@Composable
fun AlgorithmSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        PrefsHelper.ALGO_RANDOM     to "Random",
        PrefsHelper.ALGO_SEQUENTIAL to "Sequential",
        PrefsHelper.ALGO_STARRED    to "Starred ★"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceContainerHigh, RoundedCornerShape(9999.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(
                        if (isSelected) Brush.horizontalGradient(
                            listOf(ElectricViolet, ElectricViolet.copy(alpha = 0.7f))
                        )
                        else Brush.horizontalGradient(
                            listOf(GlassBorder, GlassBorder)
                        )
                    )
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) GlassWhite else OnSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
