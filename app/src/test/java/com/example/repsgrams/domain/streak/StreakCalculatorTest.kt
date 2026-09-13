package com.example.repsgrams.domain.streak

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {
    private val template = WorkoutTemplateEntity(
        id = 1,
        name = "Workout",
        dayLabel = "A",
        maxDurationMinutes = 60,
        restDaysAfter = 1,
        orderIndex = 0,
    )

    @Test
    fun `sessions inside grace window extend streak`() {
        val sessions = listOf(1L, 4L, 6L).map { day ->
            WorkoutSessionEntity(templateId = 1, date = LocalDate.of(2026, 9, day.toInt()), completed = true)
        }
        val result = StreakCalculator().calculate(LocalDate.of(2026, 9, 7), sessions, listOf(template), graceDays = 1)
        assertEquals(3, result.currentWorkoutStreak)
        assertEquals(3, result.bestWorkoutStreak)
    }

    @Test
    fun `inactivity beyond grace resets current streak`() {
        val sessions = listOf(
            WorkoutSessionEntity(templateId = 1, date = LocalDate.of(2026, 9, 1), completed = true),
            WorkoutSessionEntity(templateId = 1, date = LocalDate.of(2026, 9, 3), completed = true),
        )
        val result = StreakCalculator().calculate(LocalDate.of(2026, 9, 7), sessions, listOf(template), graceDays = 1)
        assertEquals(0, result.currentWorkoutStreak)
        assertEquals(2, result.bestWorkoutStreak)
    }
}
