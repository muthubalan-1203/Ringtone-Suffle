package com.ringtoneshuffler.app.ui.screens

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ringtoneshuffler.app.ui.components.RingtoneCard
import com.ringtoneshuffler.app.ui.components.WaveformIndicator
import com.ringtoneshuffler.app.ui.theme.ElectricViolet
import com.ringtoneshuffler.app.ui.theme.ElectricVioletDark
import com.ringtoneshuffler.app.ui.theme.ElectricVioletLight
import com.ringtoneshuffler.app.ui.theme.GlassBorder
import com.ringtoneshuffler.app.ui.theme.GlassWhite
import com.ringtoneshuffler.app.ui.theme.OnSurface
import com.ringtoneshuffler.app.ui.theme.OnSurfaceVariant
import com.ringtoneshuffler.app.ui.theme.SignalEmerald
import com.ringtoneshuffler.app.ui.theme.SurfaceContainer
import com.ringtoneshuffler.app.ui.theme.SurfaceContainerHigh
import com.ringtoneshuffler.app.ui.theme.SurfaceObsidian
import com.ringtoneshuffler.app.viewmodel.ShufflerViewModel

/**
 * HomeScreen — Dashboard matching Stitch "Ringtone Shuffler - Dashboard" screen.
 *
 * Sections:
 * 1. TopBar with app title + shuffle enable toggle
 * 2. "Now Playing" hero card with waveform + current ringtone name
 * 3. Stats row (calls logged)
 * 4. Ringtone pool list (LazyColumn of RingtoneCards)
 * 5. FABs: Shuffle Now + Add Track
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ShufflerViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToVip: () -> Unit,
    onNavigateToTrimmer: () -> Unit
) {
    val context = LocalContext.current
    val uriList by viewModel.uriList.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val starredIndices by viewModel.starredIndices.collectAsState()
    val callsLogged by viewModel.callsLogged.collectAsState()
    val playingUri by viewModel.playingUri.collectAsState()

    // File picker for adding audio
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addUrisFromPicker(context, uris)
        }
    }

    // Toggle color animation
    val toggleColor by animateColorAsState(
        targetValue = if (shuffleEnabled) SignalEmerald else OnSurfaceVariant,
        animationSpec = tween(300),
        label = "toggleColor"
    )

    Scaffold(
        containerColor = SurfaceObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Ringtone Shuffler",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ElectricVioletLight
                    )
                },
                actions = {
                    // Shuffle master toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (shuffleEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelMedium,
                            color = toggleColor
                        )
                        Switch(
                            checked = shuffleEnabled,
                            onCheckedChange = { viewModel.setShuffleEnabled(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GlassWhite,
                                checkedTrackColor = SignalEmerald,
                                uncheckedThumbColor = GlassWhite,
                                uncheckedTrackColor = SurfaceContainerHigh
                            )
                        )
                    }
                    IconButton(onClick = onNavigateToVip) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "VIP Contacts",
                            tint = OnSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History",
                            tint = OnSurfaceVariant
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = OnSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceObsidian
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Trim and Add track FAB
                FloatingActionButton(
                    onClick = onNavigateToTrimmer,
                    shape = CircleShape,
                    containerColor = SurfaceContainerHigh,
                    contentColor = ElectricVioletLight,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Trim ringtone")
                }

                // Add track FAB
                FloatingActionButton(
                    onClick = { filePicker.launch(arrayOf("audio/*")) },
                    shape = CircleShape,
                    containerColor = SurfaceContainerHigh,
                    contentColor = ElectricVioletLight,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add ringtone")
                }

                // Shuffle Now FAB — primary action, Electric Violet with glow
                FloatingActionButton(
                    onClick = { viewModel.manualShuffle(context) },
                    shape = RoundedCornerShape(9999.dp),
                    containerColor = ElectricViolet,
                    contentColor = GlassWhite,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 8.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null)
                        Text(
                            "Shuffle Now",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Hero "Now Playing" Card ────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                
                val currentName = if (uriList.isNotEmpty() && currentIndex < uriList.size) {
                    com.ringtoneshuffler.app.utils.rememberDisplayName(uriList[currentIndex])
                } else {
                    "No ringtone selected"
                }

                NowPlayingCard(
                    ringtoneName = currentName,
                    isActive = shuffleEnabled && uriList.isNotEmpty()
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Stats row ─────────────────────────────────────────────────────
            item {
                StatsRow(callsLogged = callsLogged, poolSize = uriList.size)
                Spacer(Modifier.height(20.dp))
            }

            // ── Pool header ───────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ringtone Pool",
                        style = MaterialTheme.typography.headlineSmall,
                        color = OnSurface
                    )
                    Text(
                        "${uriList.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            // ── Ringtone cards ────────────────────────────────────────────────
            if (uriList.isEmpty()) {
                item { EmptyPoolHint() }
            } else {
                itemsIndexed(uriList, key = { _, uri -> uri.toString() }) { index, uri ->
                    RingtoneCard(
                        uri = uri,
                        index = index,
                        isActive = index == currentIndex,
                        isStarred = starredIndices.contains(index),
                        isPlaying = uri == playingUri,
                        onToggleStar = { viewModel.toggleStar(index) },
                        onDelete = { viewModel.removeUri(uri) },
                        onPlay = { viewModel.playPreview(context, uri) },
                        onStop = { viewModel.stopPreview() }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Bottom padding for FAB clearance
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

// ── Now Playing Hero Card ─────────────────────────────────────────────────────

@Composable
private fun NowPlayingCard(ringtoneName: String, isActive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        ElectricViolet.copy(alpha = 0.25f),
                        SurfaceContainer
                    )
                )
            )
            .shadow(
                elevation = 0.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ElectricViolet.copy(alpha = 0.4f),
                spotColor = ElectricViolet.copy(alpha = 0.4f)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "NOW PLAYING",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricVioletLight.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))

            WaveformIndicator(
                isActive = isActive,
                totalHeight = 48.dp,
                barCount = 32
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = ringtoneName,
                style = MaterialTheme.typography.headlineMedium,
                color = OnSurface,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            if (!isActive) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Shuffle is OFF",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

// ── Stats Row ─────────────────────────────────────────────────────────────────

@Composable
private fun StatsRow(callsLogged: Int, poolSize: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatChip(label = "Calls Rotated", value = "$callsLogged", modifier = Modifier.weight(1f))
        StatChip(label = "Pool Size", value = "$poolSize tracks", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(16.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = ElectricVioletLight
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariant
        )
    }
}

// ── Empty Pool Hint ───────────────────────────────────────────────────────────

@Composable
private fun EmptyPoolHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceContainer)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🎵", style = MaterialTheme.typography.displayLarge)
        Text(
            "No ringtones added",
            style = MaterialTheme.typography.headlineSmall,
            color = OnSurface,
            textAlign = TextAlign.Center
        )
        Text(
            "Tap the + button to add audio files from your device",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}


