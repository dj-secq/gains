package com.example.repsgrams.domain.schedule

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleEngineTest {
    private val workoutA = WorkoutTemplateEntity(
        id = 1,
        name = "Workout A",
        dayLabel = "A",
        maxDurationMinutes = 60,
        restDaysAfter = 1,
        orderIndex = 0,
    )
    private val workoutB = WorkoutTemplateEntity(
        id = 2,
        name = "Workout B",
        dayLabel = "B",
        maxDurationMinutes = 60,
        restDaysAfter = 2,
        orderIndex = 1,
    )
    private val templates = listOf(workoutB, workoutA)
    private val lastDate = LocalDate.of(2026, 9, 1)

    @Test
    fun `empty template list has no suggestion`() {
        val result = ScheduleEngine.computeSuggestion(lastDate, null, null, emptyList())
        assertEquals(SuggestionStatus.NO_HISTORY, result.status)
        assertNull(result.suggestedTemplate)
    }

    @Test
    fun `zero history starts with first ordered template`() {
        val result = ScheduleEngine.computeSuggestion(lastDate, null, null, templates)
        assertEquals(SuggestionStatus.NO_HISTORY, result.status)
        assertEquals(workoutA.id, result.suggestedTemplate?.id)
        assertEquals(lastDate, result.dueDate)
    }

    @Test
    fun `day before due date is rest day`() {
        val session = completedSession(workoutA, lastDate)
        val result = ScheduleEngine.computeSuggestion(lastDate.plusDays(1), session, workoutA, templates)
        assertEquals(SuggestionStatus.REST_DAY, result.status)
        assertEquals(workoutB.id, result.suggestedTemplate?.id)
        assertEquals(1, result.daysUntilDue)
    }

    @Test
    fun `due date suggests next template on time`() {
        val session = completedSession(workoutA, lastDate)
        val result = ScheduleEngine.computeSuggestion(lastDate.plusDays(2), session, workoutA, templates)
        assertEquals(SuggestionStatus.ON_TIME, result.status)
        assertEquals(workoutB.id, result.suggestedTemplate?.id)
    }

    @Test
    fun `late date reports overdue without blocking`() {
        val session = completedSession(workoutA, lastDate)
        val result = ScheduleEngine.computeSuggestion(lastDate.plusDays(4), session, workoutA, templates)
        assertEquals(SuggestionStatus.OVERDUE, result.status)
        assertEquals(workoutB.id, result.suggestedTemplate?.id)
        assertEquals(2, result.overdueByDays)
    }

    @Test
    fun `manual deviation naturally continues after actual last template`() {
        val session = completedSession(workoutB, lastDate)
        val result = ScheduleEngine.computeSuggestion(lastDate.plusDays(3), session, workoutB, templates)
        assertEquals(SuggestionStatus.ON_TIME, result.status)
        assertEquals(workoutA.id, result.suggestedTemplate?.id)
    }

    @Test
    fun `replay on the due morning matches the engine and ignores that day's log`() {
        val earlier = completedSession(workoutA, lastDate)
        val due = lastDate.plusDays(2)
        val loggedOnDueMorning = completedSession(workoutB, due)
        val later = completedSession(workoutB, due.plusDays(10))
        val morning = ScheduleEngine.replay(due, listOf(earlier, loggedOnDueMorning, later), templates)
        val direct = ScheduleEngine.computeSuggestion(due, earlier, workoutA, templates)

        assertEquals(direct, morning)
        assertEquals(SuggestionStatus.ON_TIME, morning.status)
        assertEquals(due, morning.dueDate)
        assertEquals(workoutB.id, morning.suggestedTemplate?.id)
    }

    @Test
    fun `replay after the due date stays overdue on that date`() {
        val due = lastDate.plusDays(2)
        val later = due.plusDays(4)
        val result = ScheduleEngine.replay(later, listOf(completedSession(workoutA, lastDate)), templates)

        assertEquals(SuggestionStatus.OVERDUE, result.status)
        assertEquals(due, result.dueDate)
        assertEquals(4, result.overdueByDays)
        assertEquals(workoutB.id, result.suggestedTemplate?.id)
    }

    @Test
    fun `replay does not treat a rest marker as the last workout`() {
        val rest = WorkoutSessionEntity(
            templateId = null,
            date = lastDate.plusDays(2),
            completed = true,
            notes = "Rest day",
        )
        val result = ScheduleEngine.replay(
            lastDate.plusDays(4),
            listOf(rest, completedSession(workoutA, lastDate)),
            templates,
        )

        assertEquals(SuggestionStatus.OVERDUE, result.status)
        assertEquals(lastDate.plusDays(2), result.dueDate)
    }

    @Test
    fun `replay skips a workout whose template is missing`() {
        val orphan = WorkoutSessionEntity(templateId = 99, date = lastDate.plusDays(10), completed = true)
        val result = ScheduleEngine.replay(
            lastDate.plusDays(12),
            listOf(completedSession(workoutA, lastDate), orphan),
            templates,
        )

        assertEquals(SuggestionStatus.OVERDUE, result.status)
        assertEquals(workoutB.id, result.suggestedTemplate?.id)
        assertEquals(lastDate.plusDays(2), result.dueDate)
    }

    @Test
    fun `including the day lets that workout move the due date`() {
        val today = lastDate.plusDays(2)
        val result = ScheduleEngine.replay(
            today,
            listOf(completedSession(workoutA, lastDate), completedSession(workoutB, today)),
            templates,
            includeDay = true,
        )

        assertEquals(SuggestionStatus.REST_DAY, result.status)
        assertEquals(workoutA.id, result.suggestedTemplate?.id)
        assertEquals(today.plusDays(3), result.dueDate)
    }

    @Test
    fun `open due day projects the following template after today's gap`() {
        val today = lastDate.plusDays(2)
        val live = ScheduleEngine.replay(today, listOf(completedSession(workoutA, lastDate)), templates)
        val projected = project(today, live)

        assertEquals(SuggestionStatus.ON_TIME, live.status)
        assertFalse(projected.containsKey(today))
        assertNull(projected[today.plusDays(1)])
        assertNull(projected[today.plusDays(2)])
        assertEquals(workoutA.id, projected[today.plusDays(3)]?.id)
    }

    @Test
    fun `overdue projection anchors on today rather than the missed due date`() {
        val due = lastDate.plusDays(2)
        val today = due.plusDays(5)
        val live = ScheduleEngine.replay(today, listOf(completedSession(workoutA, lastDate)), templates)
        val projected = project(today, live)

        assertEquals(SuggestionStatus.OVERDUE, live.status)
        assertFalse(projected.containsKey(today))
        assertNull(projected[due])
        assertNull(projected[today.plusDays(1)])
        assertEquals(workoutA.id, projected[today.plusDays(3)]?.id)
    }

    @Test
    fun `rest day projects the due template and not the days before it`() {
        val today = lastDate.plusDays(1)
        val live = ScheduleEngine.replay(today, listOf(completedSession(workoutA, lastDate)), templates)
        val projected = project(today, live)

        assertEquals(SuggestionStatus.REST_DAY, live.status)
        assertFalse(projected.containsKey(today))
        assertEquals(workoutB.id, projected[lastDate.plusDays(2)]?.id)
        assertEquals(workoutA.id, projected[lastDate.plusDays(5)]?.id)
    }

    @Test
    fun `a workout today projects the engine due date`() {
        val today = lastDate.plusDays(2)
        val live = ScheduleEngine.replay(
            today,
            listOf(completedSession(workoutA, lastDate), completedSession(workoutB, today)),
            templates,
            includeDay = true,
        )
        val projected = project(today, live, loggedWorkoutToday = true)

        assertEquals(workoutA.id, projected[today.plusDays(3)]?.id)
        assertFalse(projected.containsKey(today))
    }

    @Test
    fun `rest logged on an open due day does not project another workout`() {
        val today = lastDate.plusDays(2)
        val live = ScheduleEngine.replay(today, listOf(completedSession(workoutA, lastDate)), templates)
        val projected = project(today, live, loggedRestToday = true)

        assertEquals(SuggestionStatus.ON_TIME, live.status)
        assertTrue(projected.isEmpty())
    }

    @Test
    fun `no history uses the first template once then projects the next`() {
        val today = lastDate
        val live = ScheduleEngine.replay(today, emptyList(), templates)
        val projected = project(today, live)

        assertEquals(SuggestionStatus.NO_HISTORY, live.status)
        assertEquals(workoutA.id, live.suggestedTemplate?.id)
        assertFalse(projected.containsKey(today))
        assertEquals(workoutB.id, projected[today.plusDays(2)]?.id)
    }

    private fun project(
        today: LocalDate,
        live: ScheduleSuggestion,
        loggedWorkoutToday: Boolean = false,
        loggedRestToday: Boolean = false,
    ) = ScheduleEngine.projectAfterToday(
        today = today,
        live = live,
        templates = templates,
        loggedWorkoutToday = loggedWorkoutToday,
        loggedRestToday = loggedRestToday,
        horizon = today.plusDays(21),
    )

    private fun completedSession(template: WorkoutTemplateEntity, date: LocalDate) =
        WorkoutSessionEntity(templateId = template.id, date = date, completed = true)
}
