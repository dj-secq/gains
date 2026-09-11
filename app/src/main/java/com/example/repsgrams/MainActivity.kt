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

class MainActivity : ComponentActivity() {
    private var notificationTarget by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        notificationTarget = intent.getStringExtra(ReminderNotifications.EXTRA_TARGET)
        val container = (application as RepsGramsApplication).container
        setContent {
            RepsGramsTheme {
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
