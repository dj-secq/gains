package com.example.repsgrams

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.repsgrams.ui.navigation.RepsGramsApp
import com.example.repsgrams.ui.theme.RepsGramsTheme
import com.example.repsgrams.reminder.ReminderNotifications
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build


class MainActivity : ComponentActivity() {
    private var notificationTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationTarget = intent.getStringExtra(ReminderNotifications.EXTRA_TARGET)
        val container = (application as RepsGramsApplication).container
        setContent {
            val permissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { _ -> }

            LaunchedEffect(Unit) {
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }

                if (permissions.isNotEmpty()) {
                    permissionsLauncher.launch(permissions.toTypedArray())
                }
            }

            val settings by container.cycleSettingsRepository.settings.collectAsState(initial = null)
            val darkTheme = when (settings?.themeMode) {
                com.example.repsgrams.data.datastore.ThemeMode.LIGHT -> false
                com.example.repsgrams.data.datastore.ThemeMode.DARK -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            RepsGramsTheme(darkTheme = darkTheme) {
                RepsGramsApp(container, notificationTarget) { notificationTarget = null }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTarget = intent.getStringExtra(ReminderNotifications.EXTRA_TARGET)
    }
}
