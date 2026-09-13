package com.example.repsgrams.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.repsgrams.MainActivity
import com.example.repsgrams.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class RestTimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var upNextText: String = ""

    private lateinit var notificationManager: NotificationManager

    private var mediaPlayer: MediaPlayer? = null
    private var continuousVibrator: Vibrator? = null

    companion object {
        private val _restEndMillis = MutableStateFlow<Long?>(null)
        val restEndMillis: StateFlow<Long?> = _restEndMillis.asStateFlow()
        const val ACTION_START = "com.example.repsgrams.action.START_REST_TIMER"
        const val ACTION_STOP = "com.example.repsgrams.action.STOP_REST_TIMER"
        const val ACTION_ADD_TIME = "com.example.repsgrams.action.ADD_TIME"
        const val ACTION_CONTINUE = "com.example.repsgrams.action.CONTINUE"

        const val EXTRA_END_MILLIS = "end_millis"
        const val EXTRA_UP_NEXT = "up_next"

        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_ALARM = 1002

        const val CHANNEL_ID_TIMER = "rest_timer_channel"
        const val CHANNEL_ID_ALARM = "rest_alarm_channel"

        // State to tell the service if the Session screen is visible
        var isSessionForeground = false
    }

    private val stopReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP) {
                stopContinuousAlert()
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels()
        ContextCompat.registerReceiver(
            this,
            stopReceiver,
            android.content.IntentFilter(ACTION_STOP),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopContinuousAlert()
        timerJob?.cancel()
        try { unregisterReceiver(stopReceiver) } catch (e: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                stopContinuousAlert() // ensure previous alarm is stopped
                _restEndMillis.value = intent.getLongExtra(EXTRA_END_MILLIS, 0)
                upNextText = intent.getStringExtra(EXTRA_UP_NEXT) ?: ""
                startForegroundTimer()
            }
            ACTION_STOP -> {
                stopContinuousAlert()
                _restEndMillis.value = null
                stopSelf()
            }
            ACTION_ADD_TIME -> {
                stopContinuousAlert()
                _restEndMillis.value = (_restEndMillis.value ?: System.currentTimeMillis()) + 15_000L
                if (timerJob?.isActive != true) {
                    // Timer had finished, so restart it
                    notificationManager.cancel(NOTIFICATION_ID_ALARM)
                    startForegroundTimer()
                } else {
                    updateForegroundNotification()
                }
            }
            ACTION_CONTINUE -> {
                stopContinuousAlert()
                _restEndMillis.value = null
                notificationManager.cancel(NOTIFICATION_ID_ALARM)
                stopSelf()
            }
            "ACTION_DISMISS_ALARM" -> {
                stopContinuousAlert()
                _restEndMillis.value = null
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createChannels() {
        val timerChannel = NotificationChannel(
            CHANNEL_ID_TIMER,
            "Rest Timer",
            NotificationManager.IMPORTANCE_LOW
        )
        timerChannel.description = "Shows active rest timer countdown"

        val alarmChannel = NotificationChannel(
            CHANNEL_ID_ALARM,
            "Rest Over Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when your rest period is over"
            // We set default sound for the channel
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            setSound(uri, audioAttributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 200)
        }

        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(alarmChannel)
    }

    private fun startForegroundTimer() {
        val notification = buildForegroundNotification()
        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID_FOREGROUND,
            notification,
            foregroundServiceType,
        )

        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val remaining = (_restEndMillis.value ?: 0L) - System.currentTimeMillis()
                if (remaining <= 0) {
                    onTimerFinished()
                    break
                }
                updateForegroundNotification()
                delay(500)
            }
        }
    }

    private fun updateForegroundNotification() {
        notificationManager.notify(NOTIFICATION_ID_FOREGROUND, buildForegroundNotification())
    }

    private fun buildForegroundNotification(): Notification {
        val remainingSec = (((_restEndMillis.value ?: 0L) - System.currentTimeMillis()).coerceAtLeast(0) / 1000).toInt()
        val m = remainingSec / 60
        val s = remainingSec % 60
        val timeString = String.format(Locale.ROOT, "%d:%02d", m, s)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1, Intent(this, RestTimerService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_TIMER)
            .setContentTitle("Resting — $timeString")
            .setContentText(upNextText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Skip", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun onTimerFinished() {
        playAlert()
        if (!isSessionForeground) {
            postAlarmNotification()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun stopContinuousAlert() {
        timerJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {}
        mediaPlayer = null
        continuousVibrator?.cancel()
        continuousVibrator = null
    }

    private fun playAlert() {
        // Stop any existing alert first to prevent leaks
        stopContinuousAlert()

        // Vibrator
        try {
            continuousVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 500, 500) // vibrate 500ms, pause 500ms
            continuousVibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means loop
        } catch (e: Exception) { e.printStackTrace() }

        // Sound
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer.create(this, uri)?.apply {
                isLooping = true
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun postAlarmNotification() {
        val addTimeIntent = PendingIntent.getService(
            this, 2, Intent(this, RestTimerService::class.java).setAction(ACTION_ADD_TIME), PendingIntent.FLAG_IMMUTABLE
        )
        val continueIntent = PendingIntent.getService(
            this, 3, Intent(this, RestTimerService::class.java).setAction(ACTION_CONTINUE), PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getService(
            this, 4, Intent(this, RestTimerService::class.java).setAction("ACTION_DISMISS_ALARM"), PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALARM)
            .setContentTitle("Rest over")
            .setContentText(upNextText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openIntent)
            .addAction(0, "+15s", addTimeIntent)
            .addAction(0, "Continue", continueIntent)
            .setAutoCancel(true)
            .setDeleteIntent(dismissIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ALARM, notification)
    }



    override fun onBind(intent: Intent?): IBinder? = null
}
