package com.ringtoneshuffler.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ringtoneshuffler.app.data.PrefsHelper
import com.ringtoneshuffler.app.ui.components.AlgorithmSelector
import com.ringtoneshuffler.app.ui.components.PermissionBadge
import com.ringtoneshuffler.app.ui.components.PermissionStatus
import com.ringtoneshuffler.app.ui.theme.ElectricViolet
import com.ringtoneshuffler.app.ui.theme.ElectricVioletDark
import com.ringtoneshuffler.app.ui.theme.ElectricVioletLight
import com.ringtoneshuffler.app.ui.theme.ErrorRedBright
import com.ringtoneshuffler.app.ui.theme.GlassWhite
import com.ringtoneshuffler.app.ui.theme.OnSurface
import com.ringtoneshuffler.app.ui.theme.OnSurfaceVariant
import com.ringtoneshuffler.app.ui.theme.SignalEmerald
import com.ringtoneshuffler.app.ui.theme.SurfaceContainer
import com.ringtoneshuffler.app.ui.theme.SurfaceContainerHigh
import com.ringtoneshuffler.app.ui.theme.SurfaceObsidian
import com.ringtoneshuffler.app.viewmodel.ShufflerViewModel

/**
 * SettingsScreen — matches Stitch "Ringtone Shuffler - Settings & Smali Config" screen.
 *
 * Sections:
 * 1. Algorithm selector (Random / Sequential / Starred)
 * 2. Trigger Moment (After Call / Outgoing / On Ring)
 * 3. Options (Skip Repeated, Require Call Active)
 * 4. Permissions status panel
 * 5. Battery optimization
 * 6. Danger Zone (Clear pool)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ShufflerViewModel,
    onNavigateBack: () -> Unit,
    permissionGranted: (String) -> Boolean,
    requestPermission: (String) -> Unit
) {
    val context = LocalContext.current
    val algorithm by viewModel.algorithm.collectAsState()
    val triggerMoment by viewModel.triggerMoment.collectAsState()
    val skipRepeated by viewModel.skipRepeated.collectAsState()
    val requireCallActive by viewModel.requireCallActive.collectAsState()

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SurfaceObsidian,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnSurface
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Algorithm ─────────────────────────────────────────────────────
            SettingsSection(title = "Shuffle Algorithm") {
                AlgorithmSelector(
                    selected = algorithm,
                    onSelect = { viewModel.setAlgorithm(it) }
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when (algorithm) {
                        PrefsHelper.ALGO_SEQUENTIAL -> "Plays ringtones in order, one per call."
                        PrefsHelper.ALGO_STARRED    -> "Only rotates through starred tracks."
                        else                        -> "Picks a random ringtone each call."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }

            // ── Trigger Moment ────────────────────────────────────────────────
            SettingsSection(title = "When to Rotate") {
                TriggerMomentSelector(
                    selected = triggerMoment,
                    onSelect = { viewModel.setTriggerMoment(it) }
                )
            }

            // ── Options ───────────────────────────────────────────────────────
            SettingsSection(title = "Options") {
                val fadeInEnabled by viewModel.fadeInEnabled.collectAsState()
                val fadeInDuration by viewModel.fadeInDuration.collectAsState()
                
                ToggleRow(
                    label = "Fade-in Ringtone",
                    subLabel = "Gradually increase volume when phone rings",
                    checked = fadeInEnabled,
                    onCheckedChange = { viewModel.setFadeInEnabled(it) }
                )
                
                if (fadeInEnabled) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Fade Duration: $fadeInDuration seconds",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceVariant
                    )
                    androidx.compose.material3.Slider(
                        value = fadeInDuration.toFloat(),
                        onValueChange = { viewModel.setFadeInDuration(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = ElectricVioletLight,
                            activeTrackColor = ElectricViolet,
                            inactiveTrackColor = SurfaceContainerHigh
                        )
                    )
                }
                SectionDivider()
                ToggleRow(
                    label = "Skip repeated ringtones",
                    subLabel = "Avoid playing the same track twice in a row (random mode)",
                    checked = skipRepeated,
                    onCheckedChange = { viewModel.setSkipRepeated(it) }
                )
                SectionDivider()
                ToggleRow(
                    label = "Require call was active",
                    subLabel = "Only rotate on IDLE if a real call happened (blocks phantom events)",
                    checked = requireCallActive,
                    onCheckedChange = { viewModel.setRequireCallActive(it) }
                )
            }

            // ── Permissions ───────────────────────────────────────────────────
            SettingsSection(title = "Permissions") {
                val perms = buildList {
                    add(Triple(Manifest.permission.READ_PHONE_STATE, "Phone State", "Read call state"))
                    add(Triple(Manifest.permission.PROCESS_OUTGOING_CALLS, "Outgoing Calls", "Detect outgoing calls"))
                    add(Triple(Manifest.permission.READ_CONTACTS, "Read Contacts", "Needed for VIP ringtones"))
                    add(Triple(Manifest.permission.WRITE_CONTACTS, "Write Contacts", "Needed to set VIP ringtones"))
                    if (Build.VERSION.SDK_INT >= 33) {
                        add(Triple(Manifest.permission.READ_MEDIA_AUDIO, "Media Audio", "Access audio files"))
                    } else {
                        add(Triple(Manifest.permission.READ_EXTERNAL_STORAGE, "Storage", "Access audio files"))
                    }
                }
                perms.forEach { (perm, label, desc) ->
                    val granted = permissionGranted(perm)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !granted) { requestPermission(perm) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.titleMedium, color = OnSurface)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                        }
                        PermissionBadge(
                            label = if (granted) "Granted" else "Tap to grant",
                            status = if (granted) PermissionStatus.GRANTED else PermissionStatus.DENIED
                        )
                    }
                }

                // WRITE_SETTINGS — special system permission
                SectionDivider()
                val canWriteSettings = Settings.System.canWrite(context)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !canWriteSettings) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Write System Settings", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        Text("Required to change the default ringtone", style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
                    }
                    PermissionBadge(
                        label = if (canWriteSettings) "Granted" else "Tap to grant",
                        status = if (canWriteSettings) PermissionStatus.GRANTED else PermissionStatus.DENIED
                    )
                }
            }

            // ── Battery Optimization ──────────────────────────────────────────
            SettingsSection(title = "Battery") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery Optimization Exemption", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        Text(
                            "Prevents Android from killing the shuffle service in the background",
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurfaceVariant
                        )
                    }
                    PermissionBadge(
                        label = "Request",
                        status = PermissionStatus.INFO
                    )
                }
            }

            // ── Danger Zone ───────────────────────────────────────────────────
            SettingsSection(
                title = "Danger Zone",
                titleColor = ErrorRedBright
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ErrorRedBright.copy(alpha = 0.08f))
                        .clickable { showClearDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = ErrorRedBright
                    )
                    Column {
                        Text("Clear Ringtone Pool", style = MaterialTheme.typography.titleMedium, color = ErrorRedBright)
                        Text("Remove all tracks from the shuffle pool", style = MaterialTheme.typography.bodySmall, color = ErrorRedBright.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    // Clear pool confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Pool?") },
            text = { Text("This will remove all ringtones from the shuffle pool. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearPool()
                    showClearDialog = false
                }) {
                    Text("Clear", color = ErrorRedBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = SurfaceContainerHigh
        )
    }
}

// ── Trigger Moment Selector ───────────────────────────────────────────────────

@Composable
private fun TriggerMomentSelector(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        Triple(PrefsHelper.TRIGGER_IDLE,     "After Incoming Call",   "Rotates after any incoming call (answered, missed, or rejected)"),
        Triple(PrefsHelper.TRIGGER_OUTGOING, "After Outgoing Call",   "Rotates only when you make an outgoing call"),
        Triple(PrefsHelper.TRIGGER_RINGING,  "On Ring",               "Rotates the moment an incoming call starts ringing")
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (key, label, desc) ->
            val isSelected = selected == key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) ElectricVioletDark.copy(alpha = 0.25f)
                        else SurfaceContainer
                    )
                    .clickable { onSelect(key) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSelected) ElectricVioletLight else OnSurface
                    )
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariant
                    )
                }
                if (isSelected) {
                    PermissionBadge(label = "Active", status = PermissionStatus.GRANTED)
                }
            }
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color = ElectricVioletLight,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = titleColor.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subLabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = OnSurface)
            Text(subLabel, style = MaterialTheme.typography.bodySmall, color = OnSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GlassWhite,
                checkedTrackColor = SignalEmerald,
                uncheckedThumbColor = GlassWhite,
                uncheckedTrackColor = SurfaceContainerHigh
            )
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = OnSurfaceVariant.copy(alpha = 0.1f),
        thickness = 0.5.dp
    )
}
