package com.example.repsgrams.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.domain.reminder.ReminderTiming
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

interface ReminderScheduler {
    suspend fun syncDailyReminders()
    suspend fun scheduleNextWorkoutReminder()
    suspend fun scheduleNextSupplementReminder()
}

class WorkManagerReminderScheduler(
    context: Context,
    private val settingsRepository: CycleSettingsRepository,
    private val clock: Clock,
) : ReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    override suspend fun syncDailyReminders() {
        val settings = settingsRepository.settings.first()
        workManager.cancelAllWorkByTag(TAG_WORKOUT)
        workManager.cancelAllWorkByTag(TAG_SUPPLEMENTS)
        if (settings.remindersEnabled && settings.workoutReminderEnabled) {
            enqueueWorkout(ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.workoutReminderTime))
        }
        if (settings.remindersEnabled && settings.creatineReminderEnabled) {
            enqueueSupplements(ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.creatineReminderTime))
        }
    }

    override suspend fun scheduleNextWorkoutReminder() {
        val settings = settingsRepository.settings.first()
        if (settings.remindersEnabled && settings.workoutReminderEnabled) {
            enqueueWorkout(
                ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.workoutReminderTime),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
            )
        }
    }

    override suspend fun scheduleNextSupplementReminder() {
        val settings = settingsRepository.settings.first()
        if (settings.remindersEnabled && settings.creatineReminderEnabled) {
            enqueueSupplements(
                ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.creatineReminderTime),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
            )
        }
    }

    private fun enqueueWorkout(delay: Duration, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val request = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
            .setInitialDelay(delay).addTag(TAG_WORKOUT).build()
        workManager.enqueueUniqueWork(WORKOUT_WORK, policy, request)
    }

    private fun enqueueSupplements(delay: Duration, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val request = OneTimeWorkRequestBuilder<SupplementReminderWorker>()
            .setInitialDelay(delay).addTag(TAG_SUPPLEMENTS).build()
        workManager.enqueueUniqueWork(SUPPLEMENT_WORK, policy, request)
    }

    companion object {
        const val TAG_WORKOUT = "workout-reminders"
        const val TAG_SUPPLEMENTS = "supplement-reminders"
        private const val WORKOUT_WORK = "next-workout-reminder"
        private const val SUPPLEMENT_WORK = "next-supplement-reminder"
    }
}
