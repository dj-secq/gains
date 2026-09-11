package com.example.repsgrams.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.repsgrams.MainActivity

object ReminderNotifications {
    const val EXTRA_TARGET = "notification_target"
    const val TARGET_WORKOUT = "workout"
    const val TARGET_CREATINE = "creatine"
    const val TARGET_WHEY = "whey"
    private const val CHANNEL_ROUTINE = "routine_reminders"
    private const val CHANNEL_POST_WORKOUT = "post_workout_reminders"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_ROUTINE, "Routine reminders", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CHANNEL_POST_WORKOUT, "Post-workout reminders", NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun showWorkout(context: Context, dayLabel: String, minutes: Int) = show(
        context, 1001, CHANNEL_ROUTINE, "Workout $dayLabel is on for today",
        "$minutes min, whenever you're ready.", TARGET_WORKOUT,
    )

    fun showCreatine(context: Context) = show(
        context, 1002, CHANNEL_ROUTINE, "Creatine check", "5 g, anytime today.", TARGET_CREATINE,
    )

    fun showWhey(context: Context, notificationId: Int) = show(
        context, notificationId, CHANNEL_POST_WORKOUT, "Nice work", "Grab your whey when you're ready.", TARGET_WHEY,
    )

    private fun show(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        target: String,
    ) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.example.repsgrams.OPEN_${target.uppercase()}"
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET, target)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
