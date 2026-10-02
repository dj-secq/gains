package com.example.repsgrams.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import com.example.repsgrams.MainActivity
import com.example.repsgrams.R
import androidx.core.app.NotificationCompat
import java.util.Locale

internal object RestNotifications {
    const val ID_ONGOING = 1001
    const val ID_ALARM = 1002

    private const val CHANNEL_TIMER = "rest_timer_channel"
    private const val CHANNEL_ALARM = "rest_alarm_channel"
    private const val RC_OPEN = 501
    private const val RC_SKIP = 502
    private const val RC_MINUS = 503
    private const val RC_PLUS = 504

    private var alarmChannelReady = false

    fun ensureChannels(context: Context) {
        val manager = notificationManager(context)
        if (manager.getNotificationChannel(CHANNEL_TIMER) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_TIMER,
                    "Rest Timer",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows active rest timer countdown"
                    setSound(null, null)
                    enableVibration(false)
                },
            )
        }
        if (!alarmChannelReady) {
            // The previous channel carried its own sound. Playback is one-shot in RestAlertPlayback.
            manager.deleteNotificationChannel(CHANNEL_ALARM)
            val silent = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALARM,
                    "Rest Over Alarms",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Alerts when your rest period is over"
                    setSound(null, silent)
                    enableVibration(false)
                },
            )
            alarmChannelReady = true
        }
    }

    fun ongoing(
        context: Context,
        endEpochMillis: Long?,
        caption: String,
        sessionId: Long,
        restToken: Long = 0L,
    ): Notification {
        ensureChannels(context)
        val title = if (endEpochMillis == null) {
            "Rest"
        } else {
            val seconds = restSecondsUntil(endEpochMillis, System.currentTimeMillis()).remaining
            "Resting — ${formatSeconds(seconds)}"
        }
        return base(context, CHANNEL_TIMER, sessionId, endEpochMillis, restToken)
            .setContentTitle(title)
            .setContentText(caption)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun postRestOver(
        context: Context,
        caption: String,
        sessionId: Long,
        endEpochMillis: Long?,
        restToken: Long = 0L,
    ) {
        ensureChannels(context)
        val notification = base(context, CHANNEL_ALARM, sessionId, endEpochMillis, restToken)
            .setContentTitle("Rest over")
            .setContentText(caption)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
        notificationManager(context).notify(ID_ALARM, notification)
    }

    fun cancelRestOver(context: Context) {
        notificationManager(context).cancel(ID_ALARM)
    }

    private fun base(
        context: Context,
        channelId: String,
        sessionId: Long,
        endEpochMillis: Long?,
        restToken: Long,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_rest)
            .setContentIntent(openSession(context, sessionId))
            .addAction(0, "−15", adjust(context, -15_000L, RC_MINUS))
            .addAction(0, "+15", adjust(context, 15_000L, RC_PLUS))
            .addAction(0, "Skip", skip(context, endEpochMillis, restToken))
    }

    private fun openSession(context: Context, sessionId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (sessionId > 0L) putExtra(RestTimerService.EXTRA_SESSION_ID, sessionId)
        }
        return PendingIntent.getActivity(
            context,
            RC_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun adjust(context: Context, deltaMs: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_ADJUST
            putExtra(RestTimerService.EXTRA_DELTA_MS, deltaMs)
        }
        return PendingIntent.getForegroundService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun skip(context: Context, endEpochMillis: Long?, restToken: Long): PendingIntent {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_STOP
            if (endEpochMillis != null) putExtra(RestTimerService.EXTRA_END_MILLIS, endEpochMillis)
            if (restToken != 0L) putExtra(RestTimerService.EXTRA_REST_TOKEN, restToken)
        }
        return PendingIntent.getForegroundService(
            context,
            RC_SKIP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
            ?: error("NotificationManager missing")

    private fun formatSeconds(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        return String.format(Locale.ROOT, "%d:%02d", safe / 60, safe % 60)
    }
}
