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
    private var restToken: Long = 0L
    private var activeStartId: Int = 0
    private var latestStartId: Int = 0

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
        serviceJob.cancel()
        RestAlarmScheduler.cancel(this)
        // Skip, finish, and discard clear retainWakeLock before stopping. A deadline that just
        // fired keeps the short lock and the one-shot; destroying the service must not cut them.
        if (!retainWakeLock) {
            RestAlertPlayback.stop()
            releaseWakeLock()
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        when (intent?.action) {
            ACTION_STOP -> {
                // Notification Skip uses getForegroundService, so this still has to enter the foreground.
                if (!enterForeground()) {
                    val appContext = applicationContext
                    val expectedEnd = intent.getLongExtra(EXTRA_END_MILLIS, 0L).takeIf { it != 0L }
                    val expectedToken = intent.getLongExtra(EXTRA_REST_TOKEN, 0L)
                    holdRecoveryLock()
                    recoveryScope.launch { stopStoredRest(appContext, expectedEnd, expectedToken) }
                    stopSelf(startId)
                    return START_STICKY
                }
                scope.launch { gate.withLock { if (!stopped) handleStop(intent, startId) } }
            }
            ACTION_CONTINUE, ACTION_DISMISS_ALARM -> {
                scope.launch { gate.withLock { if (!stopped) handleDismiss(startId) } }
            }
            else -> {
                captureExtras(intent)
                if (!enterForeground()) {
                    val commandAction = intent?.action
                    val appContext = applicationContext
                    holdRecoveryLock()
                    // Independent of serviceJob: stopSelf below destroys this instance and cancels that job.
                    recoveryScope.launch {
                        val end = SessionProgressStore(appContext).progress.first()?.restEndEpochMillis
                        if (end == null) return@launch
                        if (commandAction == ACTION_DEADLINE || end <= System.currentTimeMillis()) {
                            playDeadlineFallback(appContext)
                        } else {
                            RestAlarmScheduler.schedule(appContext, end)
                        }
                    }
                    stopSelf(startId)
                    return START_STICKY
                }
                scope.launch {
                    gate.withLock {
                        if (stopped) return@withLock
                        when (intent?.action) {
                            ACTION_ADJUST -> handleAdjust(intent, startId)
                            ACTION_START -> handleStart(intent, startId)
                            else -> handleReconcile(startId)
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
        if (intent.hasExtra(EXTRA_REST_TOKEN)) {
            val token = intent.getLongExtra(EXTRA_REST_TOKEN, 0L)
            if (token != 0L) restToken = token
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
                RestNotifications.ongoing(this, _restEndMillis.value, upNextText, sessionId, restToken),
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
        // A late start must not rebuild a countdown from the intent after Skip or discard cleared the store.
        val end = saved?.restEndEpochMillis
        if (end == null) {
            releaseAndStop(commandStartId)
            return
        }
        val progress = saved ?: run {
            releaseAndStop(commandStartId)
            return
        }
        activeStartId = commandStartId
        sessionId = progress.sessionId.takeIf { it > 0L }
            ?: intent.getLongExtra(EXTRA_SESSION_ID, sessionId)
        restToken = progress.restToken.takeIf { it != 0L } ?: restToken
        upNextText = progress.restCaption.takeIf { it.isNotBlank() }
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
        activeStartId = commandStartId
        val saved = progressStore.progress.first()
        if (saved != null) {
            sessionId = saved.sessionId
            if (saved.restToken != 0L) restToken = saved.restToken
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
        activeStartId = commandStartId
        sessionId = saved.sessionId
        if (saved.restToken != 0L) restToken = saved.restToken
        upNextText = saved.restCaption
        _restEndMillis.value = end
        applyTiming(end, commandStartId)
    }

    private suspend fun handleStop(intent: Intent?, commandStartId: Int) {
        val expectedEnd = intent?.takeIf { it.hasExtra(EXTRA_END_MILLIS) }
            ?.getLongExtra(EXTRA_END_MILLIS, 0L)
            ?.takeIf { it != 0L }
        val expectedToken = intent?.getLongExtra(EXTRA_REST_TOKEN, 0L) ?: 0L
        val saved = progressStore.progress.first()
        if (isDifferentRest(expectedEnd, expectedToken, saved?.restEndEpochMillis, saved?.restToken ?: 0L)) {
            // A newer rest owns the service. This Skip must not become the id that blocks stopSelf.
            return
        }
        if (expectedToken != 0L) {
            progressStore.clearRestForToken(expectedToken)
        } else if (expectedEnd != null) {
            progressStore.clearRestDeadlineIfMatch(expectedEnd)
        }
        val after = progressStore.progress.first()
        if (isDifferentRest(expectedEnd, expectedToken, after?.restEndEpochMillis, after?.restToken ?: 0L)) {
            return
        }
        activeStartId = commandStartId
        stopped = true
        timerJob?.cancel()
        RestAlertPlayback.stop()
        retainWakeLock = false
        releaseWakeLock()
        RestAlarmScheduler.cancel(this)
        _restEndMillis.value = null
        restToken = 0L
        RestNotifications.cancelRestOver(this)
        dropForeground()
        stopSelf(latestStartId)
    }

    private fun handleDismiss(commandStartId: Int) {
        // Swipe / legacy Continue must not clear the stored deadline.
        RestAlertPlayback.stop()
        RestNotifications.cancelRestOver(this)
        val end = _restEndMillis.value
        if (end != null && end > System.currentTimeMillis()) return
        activeStartId = commandStartId
        retainWakeLock = false
        releaseWakeLock()
        _restEndMillis.value = null
        dropForeground()
        stopSelf(latestStartId)
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
        if (stopped) return
        RestAlarmScheduler.schedule(this, end)
        if (stopped) {
            RestAlarmScheduler.cancel(this)
            return
        }
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
        val settings = PreferencesCycleSettingsRepository(applicationContext, Clock.systemUTC())
            .settings
            .first()
        if (stopped) return
        val sessionVisible = isSessionForeground
        val willSound = !sessionVisible && settings.restTimerSound != "off"
        val willVibrate = settings.restTimerVibrationEnabled
        val willNotify = !sessionVisible
        if (!willSound && !willVibrate && !willNotify) {
            _restEndMillis.value = null
            if (!retainWakeLock) releaseWakeLock()
            dropForeground()
            stopSelf(latestStartId)
            return
        }
        val won = progressStore.claimRestAlert()
        if (!won || stopped) {
            if (!stopped) {
                _restEndMillis.value = null
                if (!retainWakeLock) releaseWakeLock()
                dropForeground()
                stopSelf(latestStartId)
            }
            return
        }
        RestAlarmScheduler.cancel(this)
        val alertEnd = _restEndMillis.value
        RestAlertPlayback.play(
            applicationContext,
            sound = settings.restTimerSound,
            vibrationEnabled = settings.restTimerVibrationEnabled,
            sessionVisible = sessionVisible,
        )
        if (willNotify) {
            RestNotifications.postRestOver(this, upNextText, sessionId, alertEnd, restToken)
        }
        // Mirror only. The stored deadline stays until Skip, finish, or discard.
        _restEndMillis.value = null
        // The lock was acquired for a few seconds. Leave that timeout in place so the
        // one-shot can finish after this service stops; skip/finish still release immediately.
        retainWakeLock = true
        retainedWakeLock = wakeLock
        dropForeground()
        stopSelf(latestStartId)
    }

    private fun refreshOngoing() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(
            RestNotifications.ID_ONGOING,
            RestNotifications.ongoing(this, _restEndMillis.value, upNextText, sessionId, restToken),
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
        activeStartId = commandStartId
        stopped = true
        timerJob?.cancel()
        RestAlertPlayback.stop()
        retainWakeLock = false
        releaseWakeLock()
        RestAlarmScheduler.cancel(this)
        _restEndMillis.value = null
        restToken = 0L
        dropForeground()
        stopSelf(latestStartId)
    }

    private fun holdRecoveryLock() {
        acquireWakeLock(ALERT_GRACE_MS)
        retainWakeLock = true
        retainedWakeLock = wakeLock
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
        const val EXTRA_REST_TOKEN = "rest_token"

        var isSessionForeground = false

        private const val ALERT_GRACE_MS = 5_000L
        private const val TAG = "RestTimer"
        private var retainedWakeLock: PowerManager.WakeLock? = null
        private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        suspend fun playDeadlineFallback(context: Context) {
            val appContext = context.applicationContext
            val store = SessionProgressStore(appContext)
            val saved = store.progress.first()
            if (saved?.restEndEpochMillis == null || saved.restAlertFired) return
            val settings = PreferencesCycleSettingsRepository(appContext, Clock.systemUTC()).settings.first()
            val sessionVisible = isSessionForeground
            val willSound = !sessionVisible && settings.restTimerSound != "off"
            val willVibrate = settings.restTimerVibrationEnabled
            val willNotify = !sessionVisible
            if (!willSound && !willVibrate && !willNotify) return
            if (!store.claimRestAlert()) return
            acquireFallbackWakeLock(appContext)
            if (willNotify) {
                RestNotifications.ensureChannels(appContext)
                RestNotifications.postRestOver(
                    appContext,
                    saved.restCaption,
                    saved.sessionId,
                    saved.restEndEpochMillis,
                    saved.restToken,
                )
            }
            RestAlertPlayback.play(
                appContext,
                sound = settings.restTimerSound,
                vibrationEnabled = settings.restTimerVibrationEnabled,
                sessionVisible = sessionVisible,
            )
        }

        suspend fun stopStoredRest(context: Context, expectedEnd: Long?, expectedToken: Long) {
            val appContext = context.applicationContext
            val store = SessionProgressStore(appContext)
            val saved = store.progress.first()
            if (isDifferentRest(expectedEnd, expectedToken, saved?.restEndEpochMillis, saved?.restToken ?: 0L)) {
                return
            }
            val cleared = when {
                expectedToken != 0L -> store.clearRestForToken(expectedToken)
                expectedEnd != null -> store.clearRestDeadlineIfMatch(expectedEnd)
                else -> false
            }
            val after = store.progress.first()
            if (!cleared && after?.restEndEpochMillis != null) return
            if (isDifferentRest(expectedEnd, expectedToken, after?.restEndEpochMillis, after?.restToken ?: 0L)) {
                return
            }
            RestAlarmScheduler.cancel(appContext)
            RestAlertPlayback.stop()
            releaseRetainedWakeLock()
        }

        private fun isDifferentRest(
            expectedEnd: Long?,
            expectedToken: Long,
            currentEnd: Long?,
            currentToken: Long,
        ): Boolean {
            if (expectedToken != 0L && currentToken != 0L) return expectedToken != currentToken
            if (expectedEnd != null && currentEnd != null) return expectedEnd != currentEnd
            return false
        }

        private fun acquireFallbackWakeLock(context: Context) {
            val lock = (context.getSystemService(POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RepsGrams:RestTimer")
                .also { it.setReferenceCounted(false) }
            retainedWakeLock = lock
            lock.acquire(ALERT_GRACE_MS)
        }

        fun releaseRetainedWakeLock() {
            val lock = retainedWakeLock
            retainedWakeLock = null
            if (lock != null && lock.isHeld) lock.release()
        }
    }
}
