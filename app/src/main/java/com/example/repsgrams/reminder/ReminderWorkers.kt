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
        val suggestion = container.scheduleRepository.observeSuggestion(today).first()
        val started = container.database.workoutSessionDao().getForDate(today).isNotEmpty()
        val dayLabel = if (suggestion.status != com.example.repsgrams.domain.schedule.SuggestionStatus.REST_DAY) suggestion.suggestedTemplate?.dayLabel else null
        
        if (ReminderPolicy.shouldNotifyWorkout(
                settings.remindersEnabled, settings.workoutReminderEnabled, dayLabel, started,
            )
        ) {
            val template = suggestion.suggestedTemplate
            ReminderNotifications.showWorkout(
                applicationContext, requireNotNull(dayLabel), template?.maxDurationMinutes ?: 40,
            )
        }
        container.reminderScheduler.scheduleNextWorkoutReminder()
        Result.success()
    }.getOrElse { Result.retry() }
}




