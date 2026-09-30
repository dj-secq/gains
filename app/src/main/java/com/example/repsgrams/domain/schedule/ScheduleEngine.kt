package com.example.repsgrams.domain.schedule

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class SuggestionStatus {
    ON_TIME,
    OVERDUE,
    REST_DAY,
    NO_HISTORY
}

data class ScheduleSuggestion(
    val suggestedTemplate: WorkoutTemplateEntity?,
    val status: SuggestionStatus,
    val dueDate: LocalDate?,
    val daysUntilDue: Int = 0,
    val overdueByDays: Int = 0,
)

object ScheduleEngine {
    fun computeSuggestion(
        today: LocalDate,
        lastSession: WorkoutSessionEntity?,
        lastTemplate: WorkoutTemplateEntity?,
        allTemplates: List<WorkoutTemplateEntity>,
    ): ScheduleSuggestion {
        if (allTemplates.isEmpty()) {
            return ScheduleSuggestion(null, SuggestionStatus.NO_HISTORY, null)
        }

        if (lastSession == null || lastTemplate == null) {
            // "If there is no prior completed session at all: suggestion = whichever template the user picked as 'start with'"
            // For now, we return the first template by orderIndex
            val first = allTemplates.minByOrNull { it.orderIndex }
            return ScheduleSuggestion(
                suggestedTemplate = first,
                status = SuggestionStatus.NO_HISTORY,
                dueDate = today
            )
        }

        // Find next template in order
        val sortedTemplates = allTemplates.sortedBy { it.orderIndex }
        val currentIndex = sortedTemplates.indexOfFirst { it.id == lastTemplate.id }
        val nextTemplate = if (currentIndex != -1 && currentIndex < sortedTemplates.lastIndex) {
            sortedTemplates[currentIndex + 1]
        } else {
            sortedTemplates.first()
        }

        // Compute dueDate = D + T.restDaysAfter + 1
        val lastDate = lastSession.date
        val dueDate = lastDate.plusDays(lastTemplate.restDaysAfter.toLong() + 1L)

        return when {
            today.isBefore(dueDate) -> {
                val daysUntil = ChronoUnit.DAYS.between(today, dueDate).toInt()
                ScheduleSuggestion(
                    suggestedTemplate = nextTemplate,
                    status = SuggestionStatus.REST_DAY,
                    dueDate = dueDate,
                    daysUntilDue = daysUntil
                )
            }
            today.isEqual(dueDate) -> {
                ScheduleSuggestion(
                    suggestedTemplate = nextTemplate,
                    status = SuggestionStatus.ON_TIME,
                    dueDate = dueDate
                )
            }
            else -> {
                val overdue = ChronoUnit.DAYS.between(dueDate, today).toInt()
                ScheduleSuggestion(
                    suggestedTemplate = nextTemplate,
                    status = SuggestionStatus.OVERDUE,
                    dueDate = dueDate,
                    overdueByDays = overdue
                )
            }
        }
    }

    /**
     * Suggestion as of [day]. Sessions on [day] are excluded unless [includeDay] is set,
     * so a later overdue morning keeps the original due date instead of becoming a new one.
     * A row whose template was deleted is skipped; treating it as the last workout would
     * erase older history into [SuggestionStatus.NO_HISTORY]. Rest rows are not workouts.
     */
    fun replay(
        day: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
        includeDay: Boolean = false,
    ): ScheduleSuggestion {
        val templatesById = templates.associateBy { it.id }
        val latest = sessions
            .asSequence()
            .filter { it.completed && it.templateId != null }
            .filter { session -> if (includeDay) !session.date.isAfter(day) else session.date.isBefore(day) }
            .sortedWith(workoutOrder)
            .firstNotNullOfOrNull { session ->
                templatesById[session.templateId]?.let { template -> session to template }
            }
        return if (latest == null) {
            computeSuggestion(day, null, null, templates)
        } else {
            computeSuggestion(day, latest.first, latest.second, templates)
        }
    }

    /**
     * Workout dates strictly after [today], through [horizon]. Today is not given a mark here:
     * an open due day already shows its template on the sheet, and a logged workout has moved
     * the engine's due date. Rest logged on an open due day does not invent a further cell;
     * rest does not move the due date until the session-kind anchor exists.
     */
    fun projectAfterToday(
        today: LocalDate,
        live: ScheduleSuggestion,
        templates: List<WorkoutTemplateEntity>,
        loggedWorkoutToday: Boolean,
        loggedRestToday: Boolean,
        horizon: LocalDate,
    ): Map<LocalDate, WorkoutTemplateEntity> {
        val sorted = templates.sortedBy { it.orderIndex }
        if (sorted.isEmpty()) return emptyMap()
        val anchor = projectionAnchor(today, live, sorted, loggedWorkoutToday, loggedRestToday) ?: return emptyMap()
        return walkProjection(anchor, sorted, today, horizon)
    }

    private fun projectionAnchor(
        today: LocalDate,
        live: ScheduleSuggestion,
        sorted: List<WorkoutTemplateEntity>,
        loggedWorkoutToday: Boolean,
        loggedRestToday: Boolean,
    ): Pair<LocalDate, WorkoutTemplateEntity>? {
        val suggested = live.suggestedTemplate ?: return null
        if (loggedWorkoutToday) {
            val due = live.dueDate ?: return null
            if (!due.isAfter(today)) return null
            return due to suggested
        }
        if (!loggedRestToday && live.status != SuggestionStatus.REST_DAY) {
            val nextDate = today.plusDays(suggested.restDaysAfter.toLong() + 1L)
            if (!nextDate.isAfter(today)) return null
            return nextDate to templateAfter(sorted, suggested)
        }
        if (live.status == SuggestionStatus.REST_DAY) {
            val due = live.dueDate ?: return null
            if (!due.isAfter(today)) return null
            return due to suggested
        }
        return null
    }

    private fun walkProjection(
        start: Pair<LocalDate, WorkoutTemplateEntity>,
        sorted: List<WorkoutTemplateEntity>,
        today: LocalDate,
        horizon: LocalDate,
    ): Map<LocalDate, WorkoutTemplateEntity> {
        val projected = mutableMapOf<LocalDate, WorkoutTemplateEntity>()
        var date = start.first
        var template = start.second
        repeat(400) {
            if (date.isAfter(horizon)) return projected
            if (date.isAfter(today)) projected[date] = template
            val step = template.restDaysAfter.toLong() + 1L
            if (step <= 0L) return projected
            val next = templateAfter(sorted, template)
            date = date.plusDays(step)
            template = next
        }
        return projected
    }

    private fun templateAfter(
        sorted: List<WorkoutTemplateEntity>,
        current: WorkoutTemplateEntity,
    ): WorkoutTemplateEntity {
        val index = sorted.indexOfFirst { it.id == current.id }
        val nextIndex = if (index == -1 || index == sorted.lastIndex) 0 else index + 1
        return sorted[nextIndex]
    }

    private val workoutOrder =
        compareByDescending<WorkoutSessionEntity> { it.date }
            .thenByDescending { it.endTime?.toEpochMilli() ?: Long.MIN_VALUE }
            .thenByDescending { it.id }
}
