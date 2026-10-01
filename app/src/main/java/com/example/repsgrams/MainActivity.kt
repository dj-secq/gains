package com.example.repsgrams

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import android.graphics.Color as AndroidColor
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.repsgrams.ui.navigation.RepsGramsApp
import com.example.repsgrams.ui.theme.RepsGramsTheme
import com.example.repsgrams.reminder.ReminderNotifications
import com.example.repsgrams.service.RestTimerService
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.Manifest
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import com.example.repsgrams.ui.gate.LocalGainsLaunchers
import com.example.repsgrams.ui.gate.rememberGainsLaunchers
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.repsgrams.ui.gate.DatabaseGateScreen


class MainActivity : ComponentActivity() {
    private var notificationTarget by mutableStateOf<String?>(null)
    private var openSessionId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        notificationTarget = intent.getStringExtra(ReminderNotifications.EXTRA_TARGET)
        openSessionId = sessionIdExtra(intent)
        val container = (application as RepsGramsApplication).container
        setContent {
            var pastGate by remember { mutableStateOf(container.databaseInitialization != null) }
            val scope = rememberCoroutineScope()
            val launchers = rememberGainsLaunchers()
            val importNotice = remember { SnackbarHostState() }
            LaunchedEffect(Unit) {
                container.backupManager.consumeImportNotice()?.let { importNotice.showSnackbar(it) }
            }
            LaunchedEffect(pastGate) {
                if (!pastGate) return@LaunchedEffect
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (permissions.isNotEmpty()) {
                    launchers.notifications.launch(permissions.toTypedArray())
                }
            }
            CompositionLocalProvider(LocalGainsLaunchers provides launchers) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!pastGate) {
                    RepsGramsTheme(themeMode = com.example.repsgrams.data.datastore.ThemeMode.SYSTEM) {
                        DatabaseGateScreen(
                            gate = container.databaseGate,
                            unreadable = container.databaseUnreadable,
                            onExport = { uri -> container.backupManager.exportDatabaseToZip(uri) },
                            onImport = { uri -> container.backupManager.importDatabaseFromZip(uri) },
                            onContinue = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        container.startDatabaseInitialization()
                                    }
                                    if (container.databaseInitialization != null) {
                                        pastGate = true
                                    }
                                }
                            },
                        )
                    }
                } else {
                    val settings by container.cycleSettingsRepository.settings.collectAsState(initial = null)
                    RepsGramsTheme(
                        themeMode = settings?.themeMode ?: com.example.repsgrams.data.datastore.ThemeMode.SYSTEM,
                    ) {
                        RepsGramsApp(
                            container = container,
                            notificationTarget = notificationTarget,
                            onNotificationHandled = { notificationTarget = null },
                            openSessionId = openSessionId,
                            onOpenSessionHandled = {
                                openSessionId = null
                                intent.removeExtra(RestTimerService.EXTRA_SESSION_ID)
                            },
                        )
                    }
                }
                SnackbarHost(
                    hostState = importNotice,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 72.dp),
                )
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTarget = intent.getStringExtra(ReminderNotifications.EXTRA_TARGET)
        openSessionId = sessionIdExtra(intent)
    }

    private fun sessionIdExtra(intent: Intent?): Long? {
        val id = intent?.getLongExtra(RestTimerService.EXTRA_SESSION_ID, -1L) ?: -1L
        return id.takeIf { it > 0L }
    }
}
