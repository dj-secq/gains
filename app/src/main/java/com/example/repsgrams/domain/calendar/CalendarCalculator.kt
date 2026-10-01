package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.db.SessionKind
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.schedule.ScheduleEngine
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.SuggestionStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarDayStatus {
    COMPLETE,
    MISSED,
    /** Today, and nothing was logged. Not trained and not missed. */
    PENDING,
    UPCOMING,
    /** Day numeral only. An unlogged day that was not the due date. */
    EMPTY,
}

data class CalendarDay(
    val date: LocalDate,
    val inDisplayedMonth: Boolean,
    val template: WorkoutTemplateEntity?,
    val status: CalendarDayStatus,
    val isPast: Boolean,
    /** This date has a personal record other than estimated1RM. */
    val hasPr: Boolean = false,
    /** Engine status as of today, so the sheet can tell a rest gap from an empty day. */
    val liveStatus: SuggestionStatus = SuggestionStatus.NO_HISTORY,
    val liveDueDate: LocalDate? = null,
)

object CalendarCalculator {
    fun datesForMonth(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return List(42) { first.plusDays(it.toLong()) }
    }

    fun buildMonth(
        month: YearMonth,
        today: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
        prDates: Set<LocalDate> = emptySet(),
    ): List<CalendarDay> {
        val dates = datesForMonth(month)
        val sessionsByDate = sessions.groupBy { it.date }
        val live = ScheduleEngine.replay(today, sessions, templates, includeDay = true)
        val todaySessions = sessionsByDate[today].orEmpty()
        val projected = ScheduleEngine.projectAfterToday(
            today = today,
            live = live,
            templates = templates,
            loggedWorkoutToday = completedWorkout(todaySessions, templates) != null,
            loggedRestToday = todaySessions.any(::isRestMarker),
            horizon = dates.last(),
        )
        return dates.map { date ->
            dayFor(
                date = date,
                month = month,
                today = today,
                daySessions = sessionsByDate[date].orEmpty(),
                sessions = sessions,
                templates = templates,
                live = live,
                projected = projected,
                prDates = prDates,
            )
        }
    }

    fun cellFor(
        date: LocalDate,
        today: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
        prDates: Set<LocalDate> = emptySet(),
    ): CalendarDay = buildMonth(YearMonth.from(date), today, sessions, templates, prDates).first { it.date == date }

    private fun dayFor(
        date: LocalDate,
        month: YearMonth,
        today: LocalDate,
        daySessions: List<WorkoutSessionEntity>,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
        live: ScheduleSuggestion,
        projected: Map<LocalDate, WorkoutTemplateEntity>,
        prDates: Set<LocalDate>,
    ): CalendarDay {
        val workout = completedWorkout(daySessions, templates)
        val (status, template) = when {
            workout != null -> CalendarDayStatus.COMPLETE to templates.find { it.id == workout.templateId }
            daySessions.any(::isRestMarker) -> CalendarDayStatus.COMPLETE to null
            date.isBefore(today) -> missedOrEmpty(date, sessions, templates)
            date == today -> CalendarDayStatus.PENDING to emptyTodayTemplate(live)
            else -> projected[date]?.let { CalendarDayStatus.UPCOMING to it }
                ?: (CalendarDayStatus.EMPTY to null)
        }
        return CalendarDay(
            date = date,
            inDisplayedMonth = YearMonth.from(date) == month,
            template = template,
            status = status,
            isPast = date.isBefore(today),
            hasPr = date in prDates,
            liveStatus = live.status,
            liveDueDate = live.dueDate,
        )
    }

    private fun missedOrEmpty(
        date: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
    ): Pair<CalendarDayStatus, WorkoutTemplateEntity?> {
        val morning = ScheduleEngine.replay(date, sessions, templates)
        // Only the morning that would have called ON_TIME is missed. A later overdue
        // morning still points at that earlier due date, so it stays empty.
        return if (morning.status == SuggestionStatus.ON_TIME && morning.dueDate == date) {
            CalendarDayStatus.MISSED to morning.suggestedTemplate
        } else {
            CalendarDayStatus.EMPTY to null
        }
    }

    /** The sheet shows the live template once. A rest gap's workout stays on its due date. */
    private fun emptyTodayTemplate(live: ScheduleSuggestion): WorkoutTemplateEntity? =
        when (live.status) {
            SuggestionStatus.ON_TIME,
            SuggestionStatus.OVERDUE,
            SuggestionStatus.NO_HISTORY -> live.suggestedTemplate
            SuggestionStatus.REST_DAY -> null
        }

    private fun completedWorkout(
        daySessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
    ): WorkoutSessionEntity? {
        val templateIds = templates.map { it.id }.toSet()
        return daySessions
            .filter { it.completed && it.templateId != null && it.templateId in templateIds }
            .maxWithOrNull(
                compareBy<WorkoutSessionEntity> { it.endTime?.toEpochMilli() ?: Long.MIN_VALUE }
                    .thenBy { it.id },
            )
    }

    private fun isRestMarker(session: WorkoutSessionEntity): Boolean =
        session.completed && session.sessionKind == SessionKind.REST
}
