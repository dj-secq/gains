package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarCalculatorTest {
    private val month = YearMonth.of(2026, 9)
    private val today = LocalDate.of(2026, 9, 11)
    private val cycleStart = LocalDate.of(2026, 9, 1)

    @Test fun monthGridAlwaysContainsSixMondayFirstWeeks() {
        val dates = CalendarCalculator.datesForMonth(month)
        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dates.last().dayOfWeek)
        assertEquals(LocalDate.of(2026, 8, 31), dates.first())
        assertEquals(LocalDate.of(2026, 10, 11), dates.last())
    }

    @Test fun completionAndMissedRulesFollowWorkoutAndRestRequirements() {
        val completedWorkout = WorkoutSessionEntity(templateId = null, date = LocalDate.of(2026, 9, 1), completed = true)
        val incompleteWorkout = WorkoutSessionEntity(templateId = null, date = LocalDate.of(2026, 9, 6), completed = false)
        val creatine = SupplementLogEntity(date = LocalDate.of(2026, 9, 2), creatineTaken = true)
        val days = CalendarCalculator.buildMonth(
            month, today, cycleStart, listOf(completedWorkout, incompleteWorkout), listOf(creatine),
        ).associateBy { it.date }

        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(LocalDate.of(2026, 9, 1)).status)
        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(LocalDate.of(2026, 9, 2)).status)
        assertEquals(CalendarDayStatus.MISSED, days.getValue(LocalDate.of(2026, 9, 6)).status)
        assertEquals(CalendarDayStatus.PENDING, days.getValue(today).status)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(today.plusDays(1)).status)
    }

    @Test fun reschedulingPlacesRequestedWorkoutOnSelectedDate() {
        val selected = LocalDate.of(2026, 9, 20)
        val startForA = CalendarCalculator.cycleStartFor(selected, "A")
        val startForB = CalendarCalculator.cycleStartFor(selected, "B")
        assertEquals("A", com.example.repsgrams.domain.schedule.RotationCalculator.slotFor(startForA, selected).workoutDayLabel)
        assertEquals("B", com.example.repsgrams.domain.schedule.RotationCalculator.slotFor(startForB, selected).workoutDayLabel)
    }
}
