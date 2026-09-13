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

class SupplementReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val container = (applicationContext as RepsGramsApplication).container
        container.databaseInitialization.await()
        val settings = container.cycleSettingsRepository.settings.first()
        val today = LocalDate.now(container.clock)
        val supplements = container.supplementRepository.observeAllSupplements().first()
        val logs = container.supplementRepository.observeIntakesForDate(today).first()
        val workoutCompleted = container.database.workoutSessionDao().getForDate(today)
            .any { it.completed && it.templateId != null }
        val dueUntaken = supplements.filter { supplement ->
            supplement.isActive && logs.none { it.supplementId == supplement.id && it.taken } && when (supplement.scheduleType) {
                "workoutDayOnly" -> workoutCompleted
                "customDays" -> supplement.customDays?.contains(today.dayOfWeek.name.take(3), ignoreCase = true) == true
                else -> true
            }
        }
        if (settings.remindersEnabled && settings.creatineReminderEnabled && dueUntaken.isNotEmpty()) {
            ReminderNotifications.showSupplements(applicationContext, dueUntaken.map { it.name })
        }
        container.reminderScheduler.scheduleNextSupplementReminder()
        Result.success()
    }.getOrElse { Result.retry() }
}



