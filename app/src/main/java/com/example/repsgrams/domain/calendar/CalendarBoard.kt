package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.domain.progress.kilogramsToPounds
import com.example.repsgrams.domain.session.formatWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Paint for one calendar cell. Trained and today's empty day are different disks.
 * A personal record colors a completed workout only.
 */
enum class DayMarkKind {
    TRAINED,
    PR,
    MISSED,
    UPCOMING,
    NUMERAL,
    PENDING,
}

enum class CalendarSheetAction { START, RESUME }

private val SHEET_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.US)

fun dayMarkKind(day: CalendarDay): DayMarkKind {
    val trained = day.status == CalendarDayStatus.COMPLETE && day.template != null
    return when {
        trained && day.hasPr -> DayMarkKind.PR
        trained -> DayMarkKind.TRAINED
        day.status == CalendarDayStatus.MISSED -> DayMarkKind.MISSED
        day.status == CalendarDayStatus.UPCOMING -> DayMarkKind.UPCOMING
        day.status == CalendarDayStatus.PENDING -> DayMarkKind.PENDING
        else -> DayMarkKind.NUMERAL
    }
}

/**
 * Today only. Same order as [heroFor]: resume, finished workout, rest, no template, then start.
 * Any other date is read-only.
 */
fun calendarSheetAction(
    isToday: Boolean,
    hasActiveSession: Boolean,
    completedWorkout: Boolean,
    restDay: Boolean,
    hasTemplate: Boolean,
): CalendarSheetAction? {
    if (!isToday) return null
    if (hasActiveSession) return CalendarSheetAction.RESUME
    if (completedWorkout) return null
    if (restDay) return null
    if (!hasTemplate) return null
    return CalendarSheetAction.START
}

fun formatCalendarDate(date: LocalDate): String = date.format(SHEET_DATE)

/** Reps or seconds first, then the load. `6 × 80 kg`, `40s`, `40s × 80 kg`. */
fun formatCalendarSet(
    reps: Int?,
    durationSeconds: Int?,
    weightKg: Float?,
    unit: UnitSystem,
): String? {
    val secondsOnly = durationSeconds != null && reps == null
    val figure = when {
        secondsOnly -> "${durationSeconds}s"
        reps != null -> reps.toString()
        durationSeconds != null -> "${durationSeconds}s"
        else -> null
    }
    val plate = weightKg?.let { kilograms ->
        val shown = if (unit == UnitSystem.LB) kilogramsToPounds(kilograms) else kilograms
        val word = if (unit == UnitSystem.LB) "lb" else "kg"
        "${formatWeight(shown)} $word"
    }
    return when {
        figure != null && plate != null -> "$figure × $plate"
        figure != null -> figure
        plate != null -> plate
        else -> null
    }
}
