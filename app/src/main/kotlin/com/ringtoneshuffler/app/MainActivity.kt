package com.ringtoneshuffler.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ringtoneshuffler.app.ui.screens.HomeScreen
import com.ringtoneshuffler.app.ui.screens.SettingsScreen
import com.ringtoneshuffler.app.ui.screens.SplashScreen
import com.ringtoneshuffler.app.ui.screens.HistoryScreen
import com.ringtoneshuffler.app.ui.screens.VIPContactsScreen
import com.ringtoneshuffler.app.ui.theme.RingtoneShufflerTheme
import com.ringtoneshuffler.app.viewmodel.ShufflerViewModel

class MainActivity : ComponentActivity() {

    // ── ViewModel ──────────────────────────────────────────────────────────────

    private val viewModel: ShufflerViewModel by viewModels {
        val app = applicationContext as RingtoneShufflerApp
        ShufflerViewModel.Factory(
            app.prefsHelper,
            app.historyManager,
            app.contactManager
        )
    }

    // ── Permission launcher ────────────────────────────────────────────────────

    private var pendingPermission: String = ""

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // Results are handled automatically; UI badges will refresh on recompose
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request essential permissions on first launch
        requestEssentialPermissions()

        // WRITE_SETTINGS is a special permission — must be granted via system settings screen.
        // Without this, RingtoneManager.setActualDefaultRingtoneUri() silently does nothing.
        requestWriteSettingsIfNeeded()

        setContent {
            RingtoneShufflerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    RingtoneShufflerNavHost(
                        viewModel = viewModel,
                        permissionGranted = { permission ->
                            ContextCompat.checkSelfPermission(this, permission) ==
                                    PackageManager.PERMISSION_GRANTED
                        },
                        requestPermission = { permission ->
                            permissionLauncher.launch(arrayOf(permission))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // No service to start — CallReceiver handles everything automatically.
        // Just refresh the call count display.
        viewModel.refreshCallsLogged()
    }

    // ── Permissions ────────────────────────────────────────────────────────────

    private fun requestEssentialPermissions() {
        val permissions = buildList {
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.PROCESS_OUTGOING_CALLS)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.WRITE_CONTACTS)
            if (Build.VERSION.SDK_INT >= 33) {
                add(Manifest.permission.READ_MEDIA_AUDIO)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        val missing = permissions.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    /**
     * WRITE_SETTINGS is a "special" permission — cannot be requested via the normal
     * permission dialog. Instead, we must redirect the user to the system settings page.
     * Called on every onCreate() so it re-checks after the user returns from settings.
     */
    private fun requestWriteSettingsIfNeeded() {
        if (!Settings.System.canWrite(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

// ── Navigation ─────────────────────────────────────────────────────────────────

private const val ROUTE_SPLASH   = "splash"
private const val ROUTE_HOME     = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_HISTORY  = "history"
private const val ROUTE_VIP      = "vip_contacts"
private const val ROUTE_TRIMMER  = "trimmer"

@Composable
private fun RingtoneShufflerNavHost(
    viewModel: ShufflerViewModel,
    permissionGranted: (String) -> Boolean,
    requestPermission: (String) -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = ROUTE_SPLASH) {

        composable(ROUTE_SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(ROUTE_HOME) {
                        // Remove splash from back stack — pressing Back from Home exits app
                        popUpTo(ROUTE_SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(ROUTE_HOME) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToSettings = { navController.navigate(ROUTE_SETTINGS) },
                onNavigateToHistory = { navController.navigate(ROUTE_HISTORY) },
                onNavigateToVip = { navController.navigate(ROUTE_VIP) },
                onNavigateToTrimmer = { navController.navigate(ROUTE_TRIMMER) }
            )
        }

        composable(ROUTE_SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                permissionGranted = permissionGranted,
                requestPermission = requestPermission
            )
        }

        composable(ROUTE_HISTORY) {
            HistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_VIP) {
            VIPContactsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(ROUTE_TRIMMER) {
            com.ringtoneshuffler.app.ui.screens.TrimmerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
