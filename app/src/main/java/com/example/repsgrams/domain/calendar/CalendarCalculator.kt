package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.domain.schedule.CycleSlot
import com.example.repsgrams.domain.schedule.RotationCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarDayStatus { COMPLETE, MISSED, PENDING, UPCOMING }

data class CalendarDay(
    val date: LocalDate,
    val inDisplayedMonth: Boolean,
    val slot: CycleSlot,
    val status: CalendarDayStatus,
)

object CalendarCalculator {
    fun datesForMonth(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return List(42) { first.plusDays(it.toLong()) }
    }

    fun buildMonth(
        month: YearMonth,
        today: LocalDate,
        cycleStartDate: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        supplements: List<SupplementLogEntity>,
    ): List<CalendarDay> {
        val sessionsByDate = sessions.groupBy { it.date }
        val supplementsByDate = supplements.associateBy { it.date }
        return datesForMonth(month).map { date ->
            val plannedSlot = RotationCalculator.slotFor(cycleStartDate, date)
            val requirementMet = if (plannedSlot.isWorkoutDay) {
                sessionsByDate[date].orEmpty().any { it.completed }
            } else {
                supplementsByDate[date]?.creatineTaken == true
            }
            CalendarDay(
                date = date,
                inDisplayedMonth = YearMonth.from(date) == month,
                slot = plannedSlot,
                status = when {
                    requirementMet -> CalendarDayStatus.COMPLETE
                    date.isBefore(today) -> CalendarDayStatus.MISSED
                    date == today -> CalendarDayStatus.PENDING
                    else -> CalendarDayStatus.UPCOMING
                },
            )
        }
    }

    fun cycleStartFor(date: LocalDate, workoutDayLabel: String): LocalDate = when (workoutDayLabel) {
        "A" -> date
        "B" -> date.minusDays(2)
        else -> error("Workout day label must be A or B")
    }
}
