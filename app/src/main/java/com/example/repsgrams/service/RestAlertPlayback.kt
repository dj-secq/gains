package com.example.repsgrams.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * One-shot alert. The player is not looping, and vibration uses a finite waveform
 * (repeat index -1). Sound plays while the workout is on screen. The system
 * notification is the only part skipped in that case.
 */
internal object RestAlertPlayback {
    private val lock = Any()
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun stop() {
        synchronized(lock) {
            val current = player
            player = null
            current?.setOnCompletionListener(null)
            try {
                current?.release()
            } catch (_: Exception) {
            }
            try {
                vibrator?.cancel()
            } catch (_: Exception) {
            }
            vibrator = null
        }
    }

    fun play(context: Context, sound: String, vibrationEnabled: Boolean) {
        stop()
        val appContext = context.applicationContext
        if (vibrationEnabled) {
            vibrate(appContext)
        }
        if (sound == "off") return
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        synchronized(lock) {
            try {
                val created = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                    setDataSource(appContext, uri)
                    isLooping = false
                    setOnCompletionListener { finished ->
                        synchronized(lock) {
                            if (player == finished) player = null
                        }
                        finished.release()
                    }
                    prepare()
                    start()
                }
                player = created
            } catch (_: Exception) {
                player?.release()
                player = null
            }
        }
    }

    private fun vibrate(context: Context) {
        val resolved = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(VibratorManager::class.java)
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        } ?: return
        vibrator = resolved
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1)
        try {
            resolved.vibrate(effect)
        } catch (_: Exception) {
        }
    }
}
