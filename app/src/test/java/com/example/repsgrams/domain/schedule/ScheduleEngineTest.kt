package com.example.repsgrams.domain.schedule

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    private fun completedSession(template: WorkoutTemplateEntity, date: LocalDate) =
        WorkoutSessionEntity(templateId = template.id, date = date, completed = true)
}
