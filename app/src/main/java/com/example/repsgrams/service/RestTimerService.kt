package com.example.repsgrams.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import com.example.repsgrams.data.datastore.PreferencesCycleSettingsRepository
import com.example.repsgrams.data.datastore.SessionProgressStore
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RestTimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)
    private val gate = Mutex()
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var upNextText: String = ""
    private var sessionId: Long = -1L
    private var activeStartId: Int = 0

    /** When true, onDestroy leaves the timeout lock so a one-shot alert can finish with the screen off. */
    private var retainWakeLock: Boolean = false

    @Volatile
    private var stopped = false

    private val progressStore by lazy { SessionProgressStore(applicationContext) }

    override fun onCreate() {
        super.onCreate()
        stopped = false
        RestNotifications.ensureChannels(this)
    }

    override fun onDestroy() {
        stopped = true
        timerJob?.cancel()
        if (!retainWakeLock) releaseWakeLock()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        activeStartId = startId
        val commandStartId = startId
        when (intent?.action) {
            ACTION_STOP -> {
                // Notification Skip uses getForegroundService, so this still has to enter the foreground.
                if (!enterForeground()) {
                    scope.launch { gate.withLock { handleStop(intent, commandStartId) } }
                    return START_STICKY
                }
                scope.launch { gate.withLock { handleStop(intent, commandStartId) } }
            }
            ACTION_CONTINUE, ACTION_DISMISS_ALARM -> {
                scope.launch { gate.withLock { handleDismiss(commandStartId) } }
            }
            else -> {
                captureExtras(intent)
                if (!enterForeground()) {
                    val commandAction = intent?.action
                    scope.launch {
                        val storedEnd = progressStore.progress.first()?.restEndEpochMillis
                        val extraEnd = intent?.getLongExtra(EXTRA_END_MILLIS, 0L)?.takeIf { it != 0L }
                        val end = storedEnd ?: extraEnd
                        val due = end != null && end <= System.currentTimeMillis()
                        // A failed start of a future rest must not ring or claim the alert.
                        if (commandAction == ACTION_DEADLINE || due) {
                            playDeadlineFallback(applicationContext)
                        } else if (end != null) {
                            RestAlarmScheduler.schedule(applicationContext, end)
                        }
                    }
                    stopSelf(commandStartId)
                    return START_STICKY
                }
                scope.launch {
                    gate.withLock {
                        if (stopped) return@withLock
                        when (intent?.action) {
                            ACTION_ADJUST -> handleAdjust(intent, commandStartId)
                            ACTION_START -> handleStart(intent, commandStartId)
                            else -> handleReconcile(commandStartId)
                        }
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun captureExtras(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra(EXTRA_SESSION_ID)) {
            val id = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
            if (id > 0L) sessionId = id
        }
        intent.getStringExtra(EXTRA_UP_NEXT)?.let { upNextText = it }
        if (intent.action == ACTION_START && intent.hasExtra(EXTRA_END_MILLIS)) {
            val end = intent.getLongExtra(EXTRA_END_MILLIS, 0L)
            if (end != 0L) _restEndMillis.value = end
        }
    }

    private fun enterForeground(): Boolean {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        return try {
            ServiceCompat.startForeground(
                this,
                RestNotifications.ID_ONGOING,
                RestNotifications.ongoing(this, _restEndMillis.value, upNextText, sessionId),
                type,
            )
            true
        } catch (error: Exception) {
            Log.w(TAG, "startForeground failed", error)
            false
        }
    }

    private suspend fun handleStart(intent: Intent, commandStartId: Int) {
        stopped = false
        val saved = progressStore.progress.first()
        val end = saved?.restEndEpochMillis
            ?: intent.getLongExtra(EXTRA_END_MILLIS, 0L).takeIf { it != 0L }
        if (end == null) {
            releaseAndStop(commandStartId)
            return
        }
        sessionId = intent.getLongExtra(EXTRA_SESSION_ID, saved?.sessionId ?: sessionId)
        upNextText = saved?.restCaption?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra(EXTRA_UP_NEXT)
            ?: upNextText
        _restEndMillis.value = end
        applyTiming(end, commandStartId)
    }

    private suspend fun handleAdjust(intent: Intent, commandStartId: Int) {
        stopped = false
        val delta = intent.getLongExtra(EXTRA_DELTA_MS, 0L)
        val updated = progressStore.adjustRestDeadline(delta, System.currentTimeMillis())
        if (updated == null) {
            releaseAndStop(commandStartId)
            return
        }
        val saved = progressStore.progress.first()
        if (saved != null) {
            sessionId = saved.sessionId
            if (saved.restCaption.isNotBlank()) upNextText = saved.restCaption
        }
        _restEndMillis.value = updated
        applyTiming(updated, commandStartId)
    }

    private suspend fun handleReconcile(commandStartId: Int) {
        val saved = progressStore.progress.first()
        val end = saved?.restEndEpochMillis
        if (saved == null || end == null) {
            releaseAndStop(commandStartId)
            return
        }
        sessionId = saved.sessionId
        upNextText = saved.restCaption
        _restEndMillis.value = end
        applyTiming(end, commandStartId)
    }

    private suspend fun handleStop(intent: Intent?, commandStartId: Int) {
        // The notification carries the deadline it was built for. A newer rest must survive Skip.
        val expected = intent?.takeIf { it.hasExtra(EXTRA_END_MILLIS) }
            ?.getLongExtra(EXTRA_END_MILLIS, 0L)
        val current = progressStore.progress.first()?.restEndEpochMillis
        if (expected != null && current != expected) {
            if (timerJob?.isActive != true && commandStartId == activeStartId) {
                dropForeground()
                stopSelf(commandStartId)
            }
            return
        }
        if (current != null) {
            progressStore.clearRestDeadlineIfMatch(expected ?: current)
        }
        if (progressStore.progress.first()?.restEndEpochMillis != null) {
            if (timerJob?.isActive != true && commandStartId == activeStartId) {
                dropForeground()
                stopSelf(commandStartId)
            }
            return
        }
        stopped = true
        timerJob?.cancel()
        RestAlertPlayback.stop()
        releaseWakeLock()
        RestAlarmScheduler.cancel(this)
        _restEndMillis.value = null
        RestNotifications.cancelRestOver(this)
        dropForeground()
        if (commandStartId == activeStartId) stopSelf(commandStartId)
    }

    private fun handleDismiss(commandStartId: Int) {
        // Swipe / legacy Continue must not clear the stored deadline.
        RestAlertPlayback.stop()
        RestNotifications.cancelRestOver(this)
        val end = _restEndMillis.value
        if (end != null && end > System.currentTimeMillis()) return
        releaseWakeLock()
        _restEndMillis.value = null
        dropForeground()
        if (commandStartId == activeStartId) stopSelf(commandStartId)
    }

    private suspend fun applyTiming(end: Long, commandStartId: Int) {
        val remaining = end - System.currentTimeMillis()
        if (remaining <= 0L) {
            timerJob?.cancel()
            acquireWakeLock(ALERT_GRACE_MS)
            onTimerFinished(commandStartId)
            return
        }
        acquireWakeLock(remaining + ALERT_GRACE_MS)
        RestAlarmScheduler.schedule(this, end)
        RestNotifications.cancelRestOver(this)
        refreshOngoing()
        startLoop(commandStartId)
    }

    private fun startLoop(commandStartId: Int) {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive && !stopped) {
                val end = _restEndMillis.value ?: break
                if (end - System.currentTimeMillis() <= 0L) {
                    gate.withLock {
                        val current = _restEndMillis.value
                        // An adjust may have moved the deadline while this loop waited for the gate.
                        if (!stopped && current != null && current <= System.currentTimeMillis()) {
                            onTimerFinished(commandStartId)
                        }
                    }
                    break
                }
                refreshOngoing()
                delay(1_000)
            }
        }
    }

    private suspend fun onTimerFinished(commandStartId: Int) {
        if (stopped) return
        val won = progressStore.claimRestAlert()
        if (!won || stopped) {
            if (!stopped) {
                _restEndMillis.value = null
                // The winner already holds the timeout lock for the one-shot. Don't drop it.
                if (!retainWakeLock) releaseWakeLock()
                dropForeground()
                if (commandStartId == activeStartId) stopSelf(commandStartId)
            }
            return
        }
        RestAlarmScheduler.cancel(this)
        val alertEnd = _restEndMillis.value
        val settings = PreferencesCycleSettingsRepository(applicationContext, Clock.systemUTC())
            .settings
            .first()
        if (stopped) return
        RestAlertPlayback.play(
            applicationContext,
            sound = settings.restTimerSound,
            vibrationEnabled = settings.restTimerVibrationEnabled,
            sessionVisible = isSessionForeground,
        )
        if (!isSessionForeground) {
            RestNotifications.postRestOver(this, upNextText, sessionId, alertEnd)
        }
        // Mirror only. The stored deadline stays until Skip, finish, or discard.
        _restEndMillis.value = null
        // The lock was acquired for a few seconds. Leave that timeout in place so the
        // one-shot can finish after this service stops; skip/finish still release immediately.
        retainWakeLock = true
        retainedWakeLock = wakeLock
        dropForeground()
        if (commandStartId == activeStartId) stopSelf(commandStartId)
    }

    private fun refreshOngoing() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(
            RestNotifications.ID_ONGOING,
            RestNotifications.ongoing(this, _restEndMillis.value, upNextText, sessionId),
        )
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        retainWakeLock = false
        val lock = wakeLock ?: retainedWakeLock ?: (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RepsGrams:RestTimer")
            .also { it.setReferenceCounted(false) }
        wakeLock = lock
        retainedWakeLock = lock
        if (lock.isHeld) lock.release()
        lock.acquire(timeoutMs.coerceAtLeast(1L))
    }

    private fun releaseWakeLock() {
        retainWakeLock = false
        retainedWakeLock?.let { retained ->
            if (retained.isHeld) retained.release()
        }
        retainedWakeLock = null
        val lock = wakeLock
        wakeLock = null
        if (lock != null && lock.isHeld) lock.release()
    }

    private fun dropForeground() {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
    }

    private fun releaseAndStop(commandStartId: Int) {
        timerJob?.cancel()
        releaseWakeLock()
        RestAlarmScheduler.cancel(this)
        _restEndMillis.value = null
        dropForeground()
        if (commandStartId == activeStartId) stopSelf(commandStartId)
    }

    companion object {
        private val _restEndMillis = MutableStateFlow<Long?>(null)
        val restEndMillis: StateFlow<Long?> = _restEndMillis.asStateFlow()

        const val ACTION_START = "com.example.repsgrams.action.START_REST_TIMER"
        const val ACTION_STOP = "com.example.repsgrams.action.STOP_REST_TIMER"
        const val ACTION_ADJUST = "com.example.repsgrams.action.ADJUST_REST_TIMER"
        const val ACTION_DEADLINE = "com.example.repsgrams.action.REST_DEADLINE"
        const val ACTION_CONTINUE = "com.example.repsgrams.action.CONTINUE"
        const val ACTION_DISMISS_ALARM = "ACTION_DISMISS_ALARM"

        const val EXTRA_END_MILLIS = "end_millis"
        const val EXTRA_UP_NEXT = "up_next"
        const val EXTRA_DELTA_MS = "delta_ms"
        const val EXTRA_SESSION_ID = "session_id"

        var isSessionForeground = false

        private const val ALERT_GRACE_MS = 5_000L
        private const val TAG = "RestTimer"
        private var retainedWakeLock: PowerManager.WakeLock? = null

        suspend fun playDeadlineFallback(context: Context) {
            val appContext = context.applicationContext
            val store = SessionProgressStore(appContext)
            if (!store.claimRestAlert()) return
            val saved = store.progress.first()
            RestNotifications.ensureChannels(appContext)
            RestNotifications.postRestOver(
                appContext,
                saved?.restCaption.orEmpty(),
                saved?.sessionId ?: -1L,
                saved?.restEndEpochMillis,
            )
            val settings = PreferencesCycleSettingsRepository(appContext, Clock.systemUTC()).settings.first()
            RestAlertPlayback.play(
                appContext,
                sound = settings.restTimerSound,
                vibrationEnabled = settings.restTimerVibrationEnabled,
                sessionVisible = false,
            )
        }

        fun releaseRetainedWakeLock() {
            val lock = retainedWakeLock
            retainedWakeLock = null
            if (lock != null && lock.isHeld) lock.release()
        }
    }
}
