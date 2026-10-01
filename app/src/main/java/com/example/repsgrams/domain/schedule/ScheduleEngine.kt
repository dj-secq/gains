package com.example.repsgrams.domain.schedule

import com.example.repsgrams.data.db.SessionKind
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
        restsSinceWorkout: List<WorkoutSessionEntity> = emptyList(),
    ): ScheduleSuggestion {
        if (allTemplates.isEmpty()) {
            return ScheduleSuggestion(null, SuggestionStatus.NO_HISTORY, null)
        }

        // Tied orderIndex must not follow whichever list the DAO happened to return.
        val sortedTemplates = rotationOrder(allTemplates)
        if (lastSession == null || lastTemplate == null) {
            return ScheduleSuggestion(
                suggestedTemplate = sortedTemplates.first(),
                status = SuggestionStatus.NO_HISTORY,
                dueDate = today
            )
        }

        val nextTemplate = templateAfter(sortedTemplates, lastTemplate)

        // due = W.date + T.restDaysAfter + 1. A rest strictly before due stays a calendar
        // fact. A rest on or after due restarts that same gap and does not advance rotation.
        // A second rest inside the new gap does not push again.
        val lastDate = lastSession.date
        val gapDays = lastTemplate.restDaysAfter.toLong() + 1L
        var dueDate = lastDate.plusDays(gapDays)
        restsSinceWorkout
            .asSequence()
            .filter { it.completed && it.sessionKind == SessionKind.REST && !it.date.isBefore(lastDate) }
            .map { it.date }
            .sorted()
            .forEach { restDate ->
                if (!restDate.isBefore(dueDate)) {
                    dueDate = restDate.plusDays(gapDays)
                }
            }

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
     * erase older history into [SuggestionStatus.NO_HISTORY]. A completed [SessionKind.REST]
     * row is not a workout and does not advance the rotation. Rests on or after the workout
     * date, and visible on [day], restart the gap when they land on or after the due date.
     */
    fun replay(
        day: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
        includeDay: Boolean = false,
    ): ScheduleSuggestion {
        val templatesById = templates.associateBy { it.id }
        val visible: (WorkoutSessionEntity) -> Boolean = { session ->
            if (includeDay) !session.date.isAfter(day) else session.date.isBefore(day)
        }
        val latest = sessions
            .asSequence()
            .filter { it.completed && it.templateId != null && it.sessionKind != SessionKind.REST }
            .filter(visible)
            .sortedWith(workoutOrder)
            .firstNotNullOfOrNull { session ->
                templatesById[session.templateId]?.let { template -> session to template }
            }
        return if (latest == null) {
            computeSuggestion(day, null, null, templates)
        } else {
            val (workout, template) = latest
            val rests = sessions.filter { session ->
                session.completed &&
                    session.sessionKind == SessionKind.REST &&
                    !session.date.isBefore(workout.date) &&
                    visible(session)
            }
            computeSuggestion(day, workout, template, templates, rests)
        }
    }

    /**
     * Workout dates strictly after [today], through [horizon]. Today is not given a mark here.
     * When [live] is already a rest gap, or a logged rest has restarted that gap, the next cell
     * is [ScheduleSuggestion.dueDate] with the same next template. An empty result is only the
     * stale case where [loggedRestToday] is set and the live due date did not move.
     */
    fun projectAfterToday(
        today: LocalDate,
        live: ScheduleSuggestion,
        templates: List<WorkoutTemplateEntity>,
        loggedWorkoutToday: Boolean,
        loggedRestToday: Boolean,
        horizon: LocalDate,
    ): Map<LocalDate, WorkoutTemplateEntity> {
        val sorted = rotationOrder(templates)
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
        if (!loggedWorkoutToday && !loggedRestToday && live.status != SuggestionStatus.REST_DAY) {
            val nextDate = today.plusDays(suggested.restDaysAfter.toLong() + 1L)
            if (!nextDate.isAfter(today)) return null
            return nextDate to templateAfter(sorted, suggested)
        }
        if (loggedWorkoutToday || live.status == SuggestionStatus.REST_DAY) {
            val due = live.dueDate ?: return null
            if (!due.isAfter(today)) return null
            return due to suggested
        }
        // loggedRestToday was set, but the live suggestion is still the open due day.
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

    private fun rotationOrder(templates: List<WorkoutTemplateEntity>): List<WorkoutTemplateEntity> =
        templates.sortedWith(compareBy({ it.orderIndex }, { it.dayLabel }, { it.id }))

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
