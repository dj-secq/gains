package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.SuggestionStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarCalculatorTest {
    private val month = YearMonth.of(2026, 9)
    private val today = LocalDate.of(2026, 9, 11)
    private val template = WorkoutTemplateEntity(
        id = 1,
        name = "Workout A",
        dayLabel = "A",
        maxDurationMinutes = 60,
        restDaysAfter = 1,
        orderIndex = 0,
    )

    @Test
    fun `month grid contains six Monday-first weeks`() {
        val dates = CalendarCalculator.datesForMonth(month)
        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dates.last().dayOfWeek)
    }

    @Test
    fun `past workout and explicit rest are historical facts`() {
        val sessions = listOf(
            WorkoutSessionEntity(templateId = 1, date = LocalDate.of(2026, 9, 8), completed = true),
            WorkoutSessionEntity(templateId = null, date = LocalDate.of(2026, 9, 9), completed = true, notes = "Rest day"),
        )
        val suggestion = ScheduleSuggestion(template, SuggestionStatus.ON_TIME, today)
        val days = CalendarCalculator.buildMonth(month, today, sessions, listOf(template), suggestion).associateBy { it.date }

        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(LocalDate.of(2026, 9, 8)).status)
        assertEquals(template.id, days.getValue(LocalDate.of(2026, 9, 8)).template?.id)
        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(LocalDate.of(2026, 9, 9)).status)
        assertEquals(null, days.getValue(LocalDate.of(2026, 9, 9)).template)
        assertEquals(CalendarDayStatus.MISSED, days.getValue(LocalDate.of(2026, 9, 10)).status)
    }
}
