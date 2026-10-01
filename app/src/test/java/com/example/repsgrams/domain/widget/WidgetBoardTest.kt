package com.example.repsgrams.domain.widget

import com.example.repsgrams.data.db.SessionKind
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.domain.calendar.DayMarkKind
import com.example.repsgrams.domain.calendar.dayMarkKind
import com.example.repsgrams.domain.schedule.ScheduleEngine
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.SuggestionStatus
import com.example.repsgrams.domain.today.weekContaining
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBoardTest {
    private val workoutA = template(1, "A", "Workout A", restDaysAfter = 1, orderIndex = 0)
    private val workoutB = template(2, "B", "Workout B", restDaysAfter = 2, orderIndex = 1)
    private val templates = listOf(workoutB, workoutA)
    private val monday = LocalDate.of(2026, 9, 7)

    @Test
    fun liveSessionSaysResumeEvenAfterAFinishedWorkout() {
        val session = workout(workoutA, monday)
        val suggestion = ScheduleEngine.computeSuggestion(monday, session, workoutA, templates)
        assertEquals(SuggestionStatus.REST_DAY, suggestion.status)
        val face = widgetFace(true, workoutA, suggestion, true, "Squat", emptyList())
        assertEquals(WIDGET_LIVE, face.status)
        assertEquals(WIDGET_RESUME, face.headline)
        assertEquals(WIDGET_IN_PROGRESS, face.detail)
        assertTrue(face.live)
    }

    @Test
    fun finishedWorkoutTodayIsDoneNotRest() {
        val session = workout(workoutA, monday)
        val suggestion = ScheduleEngine.computeSuggestion(monday, session, workoutA, templates)
        assertEquals(SuggestionStatus.REST_DAY, suggestion.status)
        val marks = marksFor(monday, listOf(session))
        assertEquals(DayMarkKind.TRAINED, marks.first())
        assertNotEquals(DayMarkKind.PENDING, marks.first())
        val face = widgetFace(false, workoutA, suggestion, true, "Squat", marks)
        assertEquals(WIDGET_DONE, face.status)
        assertEquals("A", face.headline)
        assertEquals("Squat", face.detail)
        assertFalse(face.live)
        assertEquals(DayMarkKind.TRAINED, face.marks.first())
    }

    @Test
    fun restGapSaysRestAndALoggedRestIsNotPending() {
        val tuesday = monday.plusDays(1)
        val trained = workout(workoutA, monday)
        val suggestion = ScheduleEngine.computeSuggestion(tuesday, trained, workoutA, templates)
        assertEquals(SuggestionStatus.REST_DAY, suggestion.status)
        val marks = marksFor(tuesday, listOf(trained, rest(tuesday)))
        assertEquals(DayMarkKind.NUMERAL, marks[1])
        assertNotEquals(DayMarkKind.PENDING, marks[1])
        val face = widgetFace(false, null, suggestion, true, "Row", marks)
        assertEquals(WIDGET_REST, face.status)
        assertEquals(WIDGET_REST, face.headline)
        assertEquals("Row", face.detail)
        assertFalse(face.live)
    }

    @Test
    fun suggestedWorkoutIsDue() {
        val wednesday = LocalDate.of(2026, 9, 9)
        val suggestion = ScheduleEngine.computeSuggestion(wednesday, workout(workoutA, monday), workoutA, templates)
        assertEquals(SuggestionStatus.ON_TIME, suggestion.status)
        val marks = marksFor(wednesday, listOf(workout(workoutA, monday)))
        assertEquals(DayMarkKind.PENDING, marks[2])
        val face = widgetFace(false, null, suggestion, true, "Bench Press", marks)
        assertEquals(WIDGET_DUE, face.status)
        assertEquals("B", face.headline)
        assertEquals("Bench Press", face.detail)
        assertFalse(face.live)
    }

    @Test
    fun overdueSuggestionIsStillDue() {
        val suggestion = ScheduleSuggestion(workoutB, SuggestionStatus.OVERDUE, monday, overdueByDays = 3)
        val face = widgetFace(false, null, suggestion, true, "Row", emptyList())
        assertEquals(WIDGET_DUE, face.status)
        assertEquals("B", face.headline)
        assertEquals("Row", face.detail)
        assertFalse(face.live)
    }

    @Test
    fun noProgramLeavesTheWordsBlank() {
        val suggestion = ScheduleSuggestion(null, SuggestionStatus.NO_HISTORY, null)
        val face = widgetFace(false, null, suggestion, false, "Squat", emptyList())
        assertEquals("", face.status)
        assertEquals("", face.headline)
        assertEquals("", face.detail)
        assertFalse(face.live)
    }

    @Test
    fun blankDayLabelUsesTheWorkoutName() {
        val named = workoutA.copy(dayLabel = "  ")
        val suggestion = ScheduleSuggestion(named, SuggestionStatus.ON_TIME, monday)
        val face = widgetFace(false, null, suggestion, true, null, emptyList())
        assertEquals("Workout A", face.headline)
        assertEquals("", face.detail)
    }

    @Test
    fun aPersonalRecordKeepsTheSharedMark() {
        val session = workout(workoutA, monday)
        val days = CalendarCalculator.buildMonth(
            YearMonth.from(monday),
            monday,
            listOf(session),
            templates,
            prDates = setOf(monday),
        )
        val marks = weekContaining(days, monday).map(::dayMarkKind)
        assertEquals(DayMarkKind.PR, marks.first())
        val suggestion = ScheduleEngine.computeSuggestion(monday, session, workoutA, templates)
        val face = widgetFace(false, workoutA, suggestion, true, "Squat", marks)
        assertEquals(DayMarkKind.PR, face.marks.first())
        assertEquals(WIDGET_DONE, face.status)
    }

    private fun marksFor(today: LocalDate, sessions: List<WorkoutSessionEntity>): List<DayMarkKind> {
        val days = CalendarCalculator.buildMonth(YearMonth.from(today), today, sessions, templates)
        val week = weekContaining(days, today)
        assertEquals(7, week.size)
        return week.map(::dayMarkKind)
    }

    private fun template(id: Long, label: String, name: String, restDaysAfter: Int, orderIndex: Int) =
        WorkoutTemplateEntity(
            id = id,
            name = name,
            dayLabel = label,
            maxDurationMinutes = 60,
            restDaysAfter = restDaysAfter,
            orderIndex = orderIndex,
        )

    private fun workout(template: WorkoutTemplateEntity, date: LocalDate) = WorkoutSessionEntity(
        id = template.id,
        templateId = template.id,
        date = date,
        completed = true,
    )

    private fun rest(date: LocalDate) = WorkoutSessionEntity(
        templateId = null,
        date = date,
        completed = true,
        sessionKind = SessionKind.REST,
    )
}
