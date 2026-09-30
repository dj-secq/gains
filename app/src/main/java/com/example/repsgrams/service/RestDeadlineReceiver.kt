package com.example.repsgrams.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Exact-alarm delivery. Starting a foreground service from an exact alarm is allowed;
 * if that start is rejected, post the alert here so the deadline is not silence.
 */
class RestDeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val start = Intent(appContext, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_DEADLINE
        }
        try {
            ContextCompat.startForegroundService(appContext, start)
        } catch (_: Exception) {
            val pending = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    RestTimerService.playDeadlineFallback(appContext)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
