package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.db.SessionKind
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarCalculatorTest {
    private val month = YearMonth.of(2026, 9)
    private val workoutA = template(id = 1, label = "A", restDaysAfter = 2, orderIndex = 0)
    private val workoutB = template(id = 2, label = "B", restDaysAfter = 1, orderIndex = 1)
    private val templates = listOf(workoutB, workoutA)

    @Test
    fun `month grid contains six Monday-first weeks`() {
        val dates = CalendarCalculator.datesForMonth(month)
        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.MONDAY, dates.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, dates.last().dayOfWeek)
    }

    @Test
    fun `only the due date is missed across a multi-day gap`() {
        val today = LocalDate.of(2026, 9, 14)
        val workoutDate = LocalDate.of(2026, 9, 8)
        val sessions = listOf(
            workout(workoutA, workoutDate),
            rest(LocalDate.of(2026, 9, 9)),
        )
        val days = days(today, sessions, prDates = setOf(workoutDate))

        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(workoutDate).status)
        assertEquals(workoutA.id, days.getValue(workoutDate).template?.id)
        assertTrue(days.getValue(workoutDate).hasPr)
        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(LocalDate.of(2026, 9, 9)).status)
        assertNull(days.getValue(LocalDate.of(2026, 9, 9)).template)

        // restDaysAfter 2 → due Sept 11. Sept 10 is inside the gap, not a missed cell.
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 10)).status)
        assertNull(days.getValue(LocalDate.of(2026, 9, 10)).template)
        assertEquals(CalendarDayStatus.MISSED, days.getValue(LocalDate.of(2026, 9, 11)).status)
        assertEquals(workoutB.id, days.getValue(LocalDate.of(2026, 9, 11)).template?.id)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 12)).status)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 13)).status)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 11)),
            (8..13).map { LocalDate.of(2026, 9, it) }.filter { days.getValue(it).status == CalendarDayStatus.MISSED },
        )
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 1)).status)
        assertFalse(days.getValue(LocalDate.of(2026, 9, 1)).hasPr)

        assertEquals(CalendarDayStatus.PENDING, days.getValue(today).status)
        assertEquals(workoutB.id, days.getValue(today).template?.id)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 15)).status)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(LocalDate.of(2026, 9, 16)).status)
        assertEquals(workoutA.id, days.getValue(LocalDate.of(2026, 9, 16)).template?.id)
    }

    @Test
    fun `a workout logged today wins over the empty today mark`() {
        val today = LocalDate.of(2026, 9, 11)
        val sessions = listOf(
            workout(workoutA, LocalDate.of(2026, 9, 1)),
            workout(workoutB, today),
        )
        val days = days(today, sessions)

        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(today).status)
        assertEquals(workoutB.id, days.getValue(today).template?.id)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 12)).status)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(LocalDate.of(2026, 9, 13)).status)
        assertEquals(workoutA.id, days.getValue(LocalDate.of(2026, 9, 13)).template?.id)
        assertEquals(CalendarDayStatus.MISSED, days.getValue(LocalDate.of(2026, 9, 4)).status)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 5)).status)
    }

    @Test
    fun `open due day uses today's template once and projects the following one`() {
        val today = LocalDate.of(2026, 9, 11)
        val days = days(today, listOf(workout(workoutA, LocalDate.of(2026, 9, 8))))

        assertEquals(CalendarDayStatus.PENDING, days.getValue(today).status)
        assertEquals(workoutB.id, days.getValue(today).template?.id)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 12)).status)
        assertNull(days.getValue(LocalDate.of(2026, 9, 12)).template)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(LocalDate.of(2026, 9, 13)).status)
        assertEquals(workoutA.id, days.getValue(LocalDate.of(2026, 9, 13)).template?.id)
    }

    @Test
    fun `overdue today does not paint the suggested template on the next day`() {
        val today = LocalDate.of(2026, 9, 11)
        val days = days(today, listOf(workout(workoutA, LocalDate.of(2026, 9, 1))))

        assertEquals(CalendarDayStatus.PENDING, days.getValue(today).status)
        assertEquals(workoutB.id, days.getValue(today).template?.id)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 12)).status)
        assertNull(days.getValue(LocalDate.of(2026, 9, 12)).template)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(LocalDate.of(2026, 9, 13)).status)
        assertEquals(workoutA.id, days.getValue(LocalDate.of(2026, 9, 13)).template?.id)
        assertEquals(CalendarDayStatus.MISSED, days.getValue(LocalDate.of(2026, 9, 4)).status)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 10)).status)
    }

    @Test
    fun `rest gap leaves today empty and projects the due template`() {
        val today = LocalDate.of(2026, 9, 9)
        val days = days(today, listOf(workout(workoutA, LocalDate.of(2026, 9, 8))))

        assertEquals(CalendarDayStatus.PENDING, days.getValue(today).status)
        assertNull(days.getValue(today).template)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(LocalDate.of(2026, 9, 11)).status)
        assertEquals(workoutB.id, days.getValue(LocalDate.of(2026, 9, 11)).template?.id)
        assertEquals(CalendarDayStatus.EMPTY, days.getValue(LocalDate.of(2026, 9, 10)).status)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(LocalDate.of(2026, 9, 13)).status)
        assertEquals(workoutA.id, days.getValue(LocalDate.of(2026, 9, 13)).template?.id)
    }

    @Test
    fun `logged rest today is not the empty today status`() {
        val today = LocalDate.of(2026, 9, 11)
        val duringGap = days(
            LocalDate.of(2026, 9, 9),
            listOf(workout(workoutA, LocalDate.of(2026, 9, 8)), rest(LocalDate.of(2026, 9, 9))),
        )
        assertEquals(CalendarDayStatus.COMPLETE, duringGap.getValue(LocalDate.of(2026, 9, 9)).status)
        assertNull(duringGap.getValue(LocalDate.of(2026, 9, 9)).template)
        assertEquals(CalendarDayStatus.UPCOMING, duringGap.getValue(LocalDate.of(2026, 9, 11)).status)

        val onDueDate = days(today, listOf(workout(workoutA, LocalDate.of(2026, 9, 8)), rest(today)))
        assertEquals(CalendarDayStatus.COMPLETE, onDueDate.getValue(today).status)
        assertNull(onDueDate.getValue(today).template)
        // restDaysAfter 2 restarts from Sept 11, so B is due Sept 14 and A follows on Sept 16.
        assertEquals(CalendarDayStatus.EMPTY, onDueDate.getValue(LocalDate.of(2026, 9, 12)).status)
        assertEquals(CalendarDayStatus.EMPTY, onDueDate.getValue(LocalDate.of(2026, 9, 13)).status)
        assertEquals(CalendarDayStatus.UPCOMING, onDueDate.getValue(LocalDate.of(2026, 9, 14)).status)
        assertEquals(workoutB.id, onDueDate.getValue(LocalDate.of(2026, 9, 14)).template?.id)
        assertEquals(CalendarDayStatus.EMPTY, onDueDate.getValue(LocalDate.of(2026, 9, 15)).status)
        assertEquals(CalendarDayStatus.UPCOMING, onDueDate.getValue(LocalDate.of(2026, 9, 16)).status)
        assertEquals(workoutA.id, onDueDate.getValue(LocalDate.of(2026, 9, 16)).template?.id)
    }

    @Test
    fun `a missing template is not a logged workout and does not drop the projection`() {
        val today = LocalDate.of(2026, 9, 12)
        val due = LocalDate.of(2026, 9, 11)
        val orphanToday = WorkoutSessionEntity(templateId = 99, date = today, completed = true)
        val orphanDue = WorkoutSessionEntity(templateId = 99, date = due, completed = true)
        val days = days(today, listOf(workout(workoutA, LocalDate.of(2026, 9, 8)), orphanToday, orphanDue))

        assertEquals(CalendarDayStatus.MISSED, days.getValue(due).status)
        assertEquals(workoutB.id, days.getValue(due).template?.id)
        assertEquals(CalendarDayStatus.PENDING, days.getValue(today).status)
        assertEquals(workoutB.id, days.getValue(today).template?.id)
        assertEquals(CalendarDayStatus.UPCOMING, days.getValue(LocalDate.of(2026, 9, 14)).status)
        assertEquals(workoutA.id, days.getValue(LocalDate.of(2026, 9, 14)).template?.id)
    }

    @Test
    fun `a workout on the same day wins over a rest marker`() {
        val today = LocalDate.of(2026, 9, 12)
        val date = LocalDate.of(2026, 9, 11)
        val days = days(today, listOf(rest(date), workout(workoutB, date)))
        assertEquals(CalendarDayStatus.COMPLETE, days.getValue(date).status)
        assertEquals(workoutB.id, days.getValue(date).template?.id)
    }

    private fun days(
        today: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        prDates: Set<LocalDate> = emptySet(),
    ) = CalendarCalculator.buildMonth(month, today, sessions, templates, prDates).associateBy { it.date }

    private fun template(id: Long, label: String, restDaysAfter: Int, orderIndex: Int) = WorkoutTemplateEntity(
        id = id,
        name = "Workout $label",
        dayLabel = label,
        maxDurationMinutes = 60,
        restDaysAfter = restDaysAfter,
        orderIndex = orderIndex,
    )

    private fun workout(template: WorkoutTemplateEntity, date: LocalDate) =
        WorkoutSessionEntity(templateId = template.id, date = date, completed = true)

    private fun rest(date: LocalDate) = WorkoutSessionEntity(
        templateId = null,
        date = date,
        completed = true,
        sessionKind = SessionKind.REST,
    )
}
