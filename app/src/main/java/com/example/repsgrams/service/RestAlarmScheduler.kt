package com.example.repsgrams.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Process-death and Doze path. Below API 31 the exact API needs no permission
 * and [AlarmManager.canScheduleExactAlarms] must not be called. No inexact fallback.
 */
internal object RestAlarmScheduler {
    private const val REQUEST_CODE = 7101

    fun schedule(context: Context, deadlineEpochMillis: Long) {
        val alarmManager = alarmManager(context) ?: return
        if (deadlineEpochMillis <= System.currentTimeMillis()) {
            cancel(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.i(TAG, "Exact alarm not scheduled; SCHEDULE_EXACT_ALARM is off")
            return
        }
        val pendingIntent = pendingIntent(context)
        try {
            alarmManager.cancel(pendingIntent)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                deadlineEpochMillis,
                pendingIntent,
            )
        } catch (error: SecurityException) {
            Log.w(TAG, "setExactAndAllowWhileIdle denied", error)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = alarmManager(context) ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, RestDeadlineReceiver::class.java).apply {
            action = RestTimerService.ACTION_DEADLINE
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmManager(context: Context): AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    private const val TAG = "RestTimer"
}
