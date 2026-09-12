with open("app/src/main/java/com/example/repsgrams/reminder/ReminderScheduler.kt", "w") as f:
    f.write("""package com.example.repsgrams.reminder

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
        if (settings.remindersEnabled && settings.workoutReminderEnabled) {
            enqueueWorkout(ReminderTiming.delayUntilNext(ZonedDateTime.now(clock), settings.workoutReminderTime))
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

    private fun enqueueWorkout(delay: Duration, policy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE) {
        val request = OneTimeWorkRequestBuilder<WorkoutReminderWorker>()
            .setInitialDelay(delay).addTag(TAG_WORKOUT).build()
        workManager.enqueueUniqueWork(WORKOUT_WORK, policy, request)
    }

    companion object {
        const val TAG_WORKOUT = "workout-reminders"
        private const val WORKOUT_WORK = "next-workout-reminder"
    }
}
""")
