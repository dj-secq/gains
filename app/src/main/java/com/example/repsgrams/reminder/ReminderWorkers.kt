package com.example.repsgrams.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.repsgrams.RepsGramsApplication
import com.example.repsgrams.domain.reminder.ReminderPolicy
import java.time.LocalDate
import kotlinx.coroutines.flow.first

class WorkoutReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val container = (applicationContext as RepsGramsApplication).container
        container.databaseInitialization.await()
        val settings = container.cycleSettingsRepository.settings.first()
        val today = LocalDate.now(container.clock)
        val slot = container.scheduleRepository.slotFor(today).first()
        val started = container.database.workoutSessionDao().getForDate(today).isNotEmpty()
        if (ReminderPolicy.shouldNotifyWorkout(
                settings.remindersEnabled, settings.workoutReminderEnabled, slot.workoutDayLabel, started,
            )
        ) {
            val template = container.database.workoutTemplateDao().getByDayLabel(requireNotNull(slot.workoutDayLabel))
            ReminderNotifications.showWorkout(
                applicationContext, slot.workoutDayLabel, template?.maxDurationMinutes ?: 40,
            )
        }
        container.reminderScheduler.scheduleNextWorkoutReminder()
        Result.success()
    }.getOrElse { Result.retry() }
}

class CreatineReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val container = (applicationContext as RepsGramsApplication).container
        container.databaseInitialization.await()
        val settings = container.cycleSettingsRepository.settings.first()
        val today = LocalDate.now(container.clock)
        val taken = container.database.supplementLogDao().getForDate(today)?.creatineTaken == true
        if (ReminderPolicy.shouldNotifyCreatine(
                settings.remindersEnabled, settings.creatineReminderEnabled, taken,
            )
        ) ReminderNotifications.showCreatine(applicationContext)
        container.reminderScheduler.scheduleNextCreatineReminder()
        Result.success()
    }.getOrElse { Result.retry() }
}

class PostWorkoutWheyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val container = (applicationContext as RepsGramsApplication).container
        container.databaseInitialization.await()
        val settings = container.cycleSettingsRepository.settings.first()
        val epochDay = inputData.getLong(KEY_DATE_EPOCH_DAY, Long.MIN_VALUE)
        require(epochDay != Long.MIN_VALUE)
        val taken = container.database.supplementLogDao().getForDate(LocalDate.ofEpochDay(epochDay))?.wheyTaken == true
        if (ReminderPolicy.shouldNotifyWhey(
                settings.remindersEnabled, settings.postWorkoutWheyReminderEnabled, taken,
            )
        ) {
            val sessionId = inputData.getLong(KEY_SESSION_ID, 0)
            ReminderNotifications.showWhey(applicationContext, 2000 + (sessionId % 1_000_000).toInt())
        }
        Result.success()
    }.getOrElse { Result.retry() }

    companion object {
        const val KEY_DATE_EPOCH_DAY = "date_epoch_day"
        const val KEY_SESSION_ID = "session_id"
    }
}
