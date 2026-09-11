package com.example.repsgrams.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.domain.reminder.ReminderTiming
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.first

interface ReminderScheduler {
    suspend fun syncDailyReminders()
    suspend fun scheduleNextWorkoutReminder()
    suspend fun scheduleNextCreatineReminder()
    suspend fun schedulePostWorkoutWhey(session: WorkoutSessionEntity)
    fun cancelPostWorkoutWhey(dateEpochDay: Long)
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
        workManager.cancelAllWorkByTag(TAG_CREATINE)
        if (settings.remindersEnabled && settings.workoutReminderEnabled) {
            enqueueWorkout(ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.workoutReminderTime))
        }
        if (settings.remindersEnabled && settings.creatineReminderEnabled) {
            enqueueCreatine(ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.creatineReminderTime))
        }
        if (!settings.remindersEnabled || !settings.postWorkoutWheyReminderEnabled) {
            workManager.cancelAllWorkByTag(TAG_WHEY)
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

    override suspend fun scheduleNextCreatineReminder() {
        val settings = settingsRepository.settings.first()
        if (settings.remindersEnabled && settings.creatineReminderEnabled) {
            enqueueCreatine(
                ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.creatineReminderTime),
                ExistingWorkPolicy.APPEND_OR_REPLACE,
            )
        }
    }

    override suspend fun schedulePostWorkoutWhey(session: WorkoutSessionEntity) {
        val settings = settingsRepository.settings.first()
        if (!settings.remindersEnabled || !settings.postWorkoutWheyReminderEnabled) return
        val input = Data.Builder()
            .putLong(PostWorkoutWheyWorker.KEY_DATE_EPOCH_DAY, session.date.toEpochDay())
            .putLong(PostWorkoutWheyWorker.KEY_SESSION_ID, session.id)
            .build()
        val request = OneTimeWorkRequestBuilder<PostWorkoutWheyWorker>()
            .setInitialDelay(Duration.ofMinutes(settings.postWorkoutWheyDelayMinutes.toLong()))
            .setInputData(input)
            .addTag(TAG_WHEY)
            .addTag(wheyDateTag(session.date.toEpochDay()))
            .build()
        workManager.enqueueUniqueWork("post-workout-whey-${session.id}", ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancelPostWorkoutWhey(dateEpochDay: Long) {
        workManager.cancelAllWorkByTag(wheyDateTag(dateEpochDay))
    }

    private fun enqueueWorkout(delay: Duration, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val request = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
            .setInitialDelay(delay).addTag(TAG_WORKOUT).build()
        workManager.enqueueUniqueWork(WORKOUT_WORK, policy, request)
    }

    private fun enqueueCreatine(delay: Duration, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val request = OneTimeWorkRequestBuilder<CreatineReminderWorker>()
            .setInitialDelay(delay).addTag(TAG_CREATINE).build()
        workManager.enqueueUniqueWork(CREATINE_WORK, policy, request)
    }

    companion object {
        const val TAG_WORKOUT = "workout-reminders"
        const val TAG_CREATINE = "creatine-reminders"
        const val TAG_WHEY = "whey-reminders"
        private const val WORKOUT_WORK = "next-workout-reminder"
        private const val CREATINE_WORK = "next-creatine-reminder"
        fun wheyDateTag(epochDay: Long) = "whey-date-$epochDay"
    }
}
