package com.example.repsgrams.service

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.repsgrams.data.datastore.SessionProgressStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manifest receiver so a grant while the session is not open can still arm a future deadline.
 * The broadcast does not exist below API 31, and [AlarmManager.canScheduleExactAlarms] is not called there.
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (intent?.action != AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        if (!alarmManager.canScheduleExactAlarms()) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val end = SessionProgressStore(appContext).progress.first()?.restEndEpochMillis ?: return@launch
                if (end > System.currentTimeMillis()) {
                    RestAlarmScheduler.schedule(appContext, end)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
