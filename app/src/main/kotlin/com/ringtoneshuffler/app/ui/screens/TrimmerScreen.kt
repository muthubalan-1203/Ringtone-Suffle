package com.ringtoneshuffler.app.ui.screens

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ringtoneshuffler.app.ui.theme.ElectricViolet
import com.ringtoneshuffler.app.ui.theme.ElectricVioletLight
import com.ringtoneshuffler.app.ui.theme.OnSurface
import com.ringtoneshuffler.app.ui.theme.SurfaceContainer
import com.ringtoneshuffler.app.ui.theme.SurfaceObsidian
import com.ringtoneshuffler.app.utils.AudioTrimmerUtil
import com.ringtoneshuffler.app.utils.rememberDisplayName
import com.ringtoneshuffler.app.viewmodel.ShufflerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimmerScreen(
    viewModel: ShufflerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var durationMs by remember { mutableStateOf(0L) }
    var startRangeMs by remember { mutableStateOf(0L) }
    var endRangeMs by remember { mutableStateOf(0L) }
    
    var isPlaying by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, uri)
                val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                durationMs = durStr?.toLongOrNull() ?: 30000L
                startRangeMs = 0L
                endRangeMs = minOf(durationMs, 15000L) // Default 15 sec window
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    Scaffold(
        containerColor = SurfaceObsidian,
        topBar = {
            TopAppBar(
                title = { Text("Trim Ringtone", color = ElectricVioletLight) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceObsidian)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (selectedUri == null) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = ElectricViolet)
                Text("Select an audio file to trim", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                Button(
                    onClick = { filePicker.launch(arrayOf("audio/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Text("Browse Files")
                }
                Spacer(Modifier.weight(1f))
            } else {
                Text(
                    text = rememberDisplayName(selectedUri!!),
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface,
                    maxLines = 2
                )

                // Range Slider
                Column(modifier = Modifier.fillMaxWidth().background(SurfaceContainer).padding(16.dp)) {
                    Text("Select Time Range", color = OnSurface)
                    Spacer(Modifier.height(8.dp))
                    RangeSlider(
                        value = (startRangeMs.toFloat() / durationMs.coerceAtLeast(1L))..(endRangeMs.toFloat() / durationMs.coerceAtLeast(1L)),
                        onValueChange = { range ->
                            startRangeMs = (range.start * durationMs).toLong()
                            endRangeMs = (range.endInclusive * durationMs).toLong()
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricVioletLight,
                            activeTrackColor = ElectricViolet
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(startRangeMs), color = OnSurface)
                        Text(formatTime(endRangeMs), color = OnSurface)
                    }
                }

                // Playback Controls
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                mediaPlayer?.stop()
                                mediaPlayer?.release()
                                mediaPlayer = null
                                isPlaying = false
                            } else {
                                mediaPlayer = MediaPlayer.create(context, selectedUri)?.apply {
                                    seekTo(startRangeMs.toInt())
                                    start()
                                }
                                isPlaying = true
                            }
                        },
                        modifier = Modifier.background(SurfaceContainer, shape = MaterialTheme.shapes.medium)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Play/Stop",
                            tint = ElectricVioletLight
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Action Button
                if (isProcessing) {
                    CircularProgressIndicator(color = ElectricViolet)
                    Text("Trimming audio...", color = OnSurface)
                } else {
                    Button(
                        onClick = {
                            mediaPlayer?.stop()
                            isPlaying = false
                            isProcessing = true
                            coroutineScope.launch {
                                val trimmedUri = AudioTrimmerUtil.trimAudio(context, selectedUri!!, startRangeMs, endRangeMs)
                                isProcessing = false
                                if (trimmedUri != null) {
                                    viewModel.addUri(trimmedUri)
                                    Toast.makeText(context, "Trimmed ringtone added to pool!", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                } else {
                                    Toast.makeText(context, "Failed to trim audio", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Trim & Add to Pool", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
