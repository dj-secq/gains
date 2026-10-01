package com.example.repsgrams.domain.today

import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.db.SessionKind
import com.example.repsgrams.data.db.SupplementEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.domain.calendar.CalendarDayStatus
import com.example.repsgrams.domain.schedule.ScheduleEngine
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.SuggestionStatus
import com.example.repsgrams.domain.session.WorkoutExercise
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayBoardTest {
    private val workoutA = template(1, "A", "Workout A", restDaysAfter = 1, orderIndex = 0)
    private val workoutB = template(2, "B", "Workout B", restDaysAfter = 2, orderIndex = 1)
    private val templates = listOf(workoutB, workoutA)
    private val monday = LocalDate.of(2026, 9, 7)

    @Test
    fun finishedAOnMondayIsDoneEvenThoughTheEngineSaysRest() {
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        val session = workout(workoutA, monday)
        val suggestion = ScheduleEngine.computeSuggestion(monday, session, workoutA, templates)
        assertEquals(SuggestionStatus.REST_DAY, suggestion.status)
        assertEquals(workoutB.id, suggestion.suggestedTemplate?.id)
        val hero = heroFor(false, workoutA, suggestion, true)
        assertEquals(TodayHeroMode.DONE, hero.mode)
        assertEquals("DONE", hero.label)
        assertEquals(TodayMark.Token("A"), hero.mark)
        assertEquals("B \u00B7 due Wednesday", hero.caption)
        assertNull(hero.pill)
        assertEquals(TodayAlternate.SHEET, hero.alternate)
    }

    @Test
    fun tuesdayInsideTheGapIsRecovery() {
        val tuesday = monday.plusDays(1)
        val suggestion = ScheduleEngine.computeSuggestion(tuesday, workout(workoutA, monday), workoutA, templates)
        val hero = heroFor(false, null, suggestion, true)
        assertEquals(TodayHeroMode.REST, hero.mode)
        assertEquals("RECOVERY", hero.label)
        assertEquals(TodayMark.Rest, hero.mark)
        assertEquals("1 day until Workout B", hero.caption)
        assertNull(hero.pill)
    }

    @Test
    fun wednesdayOnTimeStartsB() {
        val wednesday = LocalDate.of(2026, 9, 9)
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.dayOfWeek)
        val suggestion = ScheduleEngine.computeSuggestion(wednesday, workout(workoutA, monday), workoutA, templates)
        assertEquals(SuggestionStatus.ON_TIME, suggestion.status)
        val hero = heroFor(false, null, suggestion, true)
        assertEquals(TodayHeroMode.START, hero.mode)
        assertEquals("SUGGESTED", hero.label)
        assertEquals(TodayMark.Token("B"), hero.mark)
        assertEquals("Workout B", hero.caption)
        assertEquals("START", hero.pill)
    }

    @Test
    fun finishedBNamesBAndTheFollowingDueDay() {
        val wednesday = LocalDate.of(2026, 9, 9)
        val suggestion = ScheduleEngine.computeSuggestion(wednesday, workout(workoutB, wednesday), workoutB, templates)
        val hero = heroFor(false, workoutB, suggestion, true)
        assertEquals(TodayHeroMode.DONE, hero.mode)
        assertEquals(TodayMark.Token("B"), hero.mark)
        assertEquals("A \u00B7 due Saturday", hero.caption)
    }

    @Test
    fun oneTemplateDoneCaptionsItsOwnNextDue() {
        val due = LocalDate.of(2026, 9, 9)
        val suggestion = ScheduleSuggestion(workoutA, SuggestionStatus.REST_DAY, due, daysUntilDue = 2)
        val hero = heroFor(false, workoutA, suggestion, true)
        assertEquals("A \u00B7 due Wednesday", hero.caption)
        assertEquals(TodayMark.Token("A"), hero.mark)
    }

    @Test
    fun liveSessionBeatsAFinishedWorkout() {
        val suggestion = ScheduleSuggestion(workoutB, SuggestionStatus.REST_DAY, monday.plusDays(2))
        val hero = heroFor(true, workoutA, suggestion, true, activeTemplateName = "Workout A")
        assertEquals(TodayHeroMode.LIVE, hero.mode)
        assertEquals(TodayMark.Elapsed, hero.mark)
        assertEquals("RESUME", hero.pill)
        assertEquals("Workout A", hero.caption)
        assertEquals(TodayAlternate.NONE, hero.alternate)
        assertEquals(workoutA.id, upNextTemplateId(true, workoutA.id, workoutA, suggestion, true))
    }

    @Test
    fun restAndDoneDoNotPreviewTomorrow() {
        val rest = ScheduleSuggestion(workoutB, SuggestionStatus.REST_DAY, monday.plusDays(2), daysUntilDue = 2)
        assertNull(upNextTemplateId(false, null, null, rest, true))
        assertNull(upNextTemplateId(false, null, workoutA, rest, true))
    }

    @Test
    fun emptyProgramDoesNotStart() {
        val suggestion = ScheduleSuggestion(null, SuggestionStatus.NO_HISTORY, null)
        val hero = heroFor(false, null, suggestion, false)
        assertEquals(TodayHeroMode.NO_PROGRAM, hero.mode)
        assertEquals(TodayMark.None, hero.mark)
        assertNull(hero.pill)
        assertEquals(TodayAlternate.TEMPLATES, hero.alternate)
    }

    @Test
    fun firstTemplateWithNoHistoryIsSuggested() {
        val suggestion = ScheduleSuggestion(workoutA, SuggestionStatus.NO_HISTORY, monday)
        val hero = heroFor(false, null, suggestion, true)
        assertEquals(TodayHeroMode.START, hero.mode)
        assertEquals("SUGGESTED", hero.label)
        assertEquals(TodayMark.Token("A"), hero.mark)
        assertEquals("Workout A", hero.caption)
    }

    @Test
    fun overdueIsASentenceAndALongLabelUsesTheName() {
        val push = workoutA.copy(dayLabel = "Push", name = "Push Day")
        val overdue = ScheduleSuggestion(push, SuggestionStatus.OVERDUE, monday, overdueByDays = 2)
        val hero = heroFor(false, null, overdue, true)
        assertEquals("OVERDUE", hero.label)
        assertEquals(TodayMark.Words("Push Day"), hero.mark)
        assertEquals("Due 2 days ago", hero.caption)
        assertEquals("START", hero.pill)
        val letter = heroFor(false, null, overdue.copy(suggestedTemplate = workoutA), true)
        assertEquals(TodayMark.Token("A"), letter.mark)
        assertEquals("Due 2 days ago", letter.caption)
    }

    @Test
    fun restRowReadsLogRestBeforeTheDueDate() {
        assertEquals("Log rest", restRowLabel(SuggestionStatus.REST_DAY))
        assertEquals("Rest today", restRowLabel(SuggestionStatus.ON_TIME))
        assertEquals("Rest today", restRowLabel(SuggestionStatus.OVERDUE))
        assertEquals("1 rest day", restDaysCaption(1))
        assertEquals("2 rest days", restDaysCaption(2))
    }

    @Test
    fun weekStripUsesTheCalendarWeek() {
        val wednesday = LocalDate.of(2026, 9, 9)
        val days = CalendarCalculator.buildMonth(YearMonth.of(2026, 9), wednesday, emptyList(), templates)
        val week = weekContaining(days, wednesday)
        assertEquals(7, week.size)
        assertEquals(DayOfWeek.MONDAY, week.first().date.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, week.last().date.dayOfWeek)
        assertEquals(CalendarDayStatus.PENDING, week.first { it.date == wednesday }.status)
    }

    @Test
    fun restLogIsNotTheDoneWorkout() {
        val rest = WorkoutSessionEntity(
            templateId = null,
            date = monday,
            completed = true,
            sessionKind = SessionKind.REST,
        )
        assertNull(CalendarCalculator.completedWorkoutOn(listOf(rest), templates))
        val done = WorkoutSessionEntity(id = 3, templateId = workoutA.id, date = monday, completed = true)
        assertEquals(3L, CalendarCalculator.completedWorkoutOn(listOf(done), templates)?.id)
    }

    @Test
    fun previewKeepsThreeExercisesAndASupersetHairline() {
        val lines = previewLines(
            listOf(
                BlockKind.SUPERSET to listOf(exercise("Pull-Ups", 4, 6), exercise("DB Floor Press", 8, 12, perSide = true)),
                BlockKind.STANDARD to listOf(exercise("Squat", 5, 5), exercise("Curl", 10, 12)),
            ),
        )
        assertEquals(3, lines.size)
        assertEquals("Pull-Ups", lines[0].name)
        assertEquals("4\u20136 REPS", lines[0].figures())
        assertFalse(lines[0].supersetWithPrevious)
        assertTrue(lines[1].supersetWithPrevious)
        assertTrue(lines[1].perSide)
        assertEquals("8\u201312 REPS", lines[1].figures())
        assertFalse(lines[2].supersetWithPrevious)
        assertEquals("5 REPS", lines[2].figures())
    }

    @Test
    fun liveCursorStartsAtTheCurrentExercise() {
        val lines = previewLines(
            listOf(BlockKind.SUPERSET to listOf(exercise("Pull-Ups", 4, 6), exercise("Row", 8, 8))),
            startBlock = 0,
            startExercise = 1,
        )
        assertEquals(listOf("Row"), lines.map { it.name })
        assertFalse(lines.single().supersetWithPrevious)
        val seconds = previewLines(listOf(BlockKind.STANDARD to listOf(exercise("Plank", 30, 30, seconds = true))))
        assertEquals("30 SEC", seconds.single().figures())
    }

    @Test
    fun badCursorFallsBackToTheStart() {
        val lines = previewLines(
            listOf(BlockKind.STANDARD to listOf(exercise("Squat", 5, 5))),
            startBlock = 4,
            startExercise = 2,
        )
        assertEquals("Squat", lines.single().name)
    }

    @Test
    fun doseKeysAllowOneDecimal() {
        assertEquals("1.5", applyDoseKey(applyDoseKey(applyDoseKey("", "1"), "."), "5"))
        assertEquals("1.5", applyDoseKey("1.5", "."))
        assertEquals("1.5", applyDoseKey("1.5", "a"))
        assertEquals("1.", applyDoseKey("1.5", DOSE_DELETE))
        assertEquals("0.", applyDoseKey("", "."))
        assertEquals(1.5f, parseDose("1.5"))
        assertEquals(5f, parseDose("5."))
        assertNull(parseDose(""))
        assertNull(parseDose("."))
        assertEquals("5", formatDose(5f))
        assertEquals("5.5", formatDose(5.5f))
    }

    @Test
    fun lowSupplyPicksTheSmallestTruncatedServing() {
        val today = monday
        val creatine = supplement(2, "Creatine", threshold = 5)
        val whey = supplement(1, "Whey", threshold = 5)
        val idle = supplement(3, "Idle", threshold = 5, active = false)
        val tile = lowSupplyTile(
            inventory = listOf(
                supply(2, 4.9f, today),
                supply(1, 4.9f, today),
                supply(3, 1f, today),
            ),
            supplements = listOf(creatine, whey, idle),
            today = today,
        )
        assertEquals(1L, tile?.supplementId)
        assertEquals(4, tile?.servings)
        assertNull(
            lowSupplyTile(
                inventory = listOf(supply(2, 9f, today)),
                supplements = listOf(creatine),
                today = today,
            ),
        )
    }

    @Test
    fun elapsedUsesMinutesUntilAnHour() {
        val start = Instant.parse("2026-09-07T12:00:00Z")
        assertEquals("0:00", formatElapsed(start, start))
        assertEquals("12:04", formatElapsed(start, start.plusSeconds(12 * 60 + 4L)))
        assertEquals("59:59", formatElapsed(start, start.plusSeconds(3599)))
        assertEquals("1:00:00", formatElapsed(start, start.plusSeconds(3600)))
        assertEquals("0:00", formatElapsed(start.plusSeconds(5), start))
    }

    @Test
    fun supplementScrollSkipsTheWarningIndex() {
        assertEquals(2, supplementItemIndex(showUpNext = false))
        assertEquals(3, supplementItemIndex(showUpNext = true))
        assertTrue(isSupplementScrollTarget("supplements"))
        assertTrue(isSupplementScrollTarget("creatine"))
        assertTrue(isSupplementScrollTarget("whey"))
        assertFalse(isSupplementScrollTarget("workout"))
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

    private fun exercise(
        name: String,
        low: Int,
        high: Int,
        perSide: Boolean = false,
        seconds: Boolean = false,
    ) = WorkoutExercise(
        id = name.hashCode().toLong(),
        name = name,
        imageAssetName = null,
        notes = null,
        tracksWeight = !seconds,
        targetValueLow = low,
        targetValueHigh = high,
        repType = if (seconds) RepType.SECONDS else RepType.REPS,
        perSide = perSide,
    )

    private fun supplement(id: Long, name: String, threshold: Int, active: Boolean = true) = SupplementEntity(
        id = id,
        name = name,
        doseAmount = 5f,
        unit = "g",
        scheduleType = "daily",
        containerSize = 30,
        lowSupplyThreshold = threshold,
        colorToken = "",
        iconName = "",
        isActive = active,
    )

    private fun supply(id: Long, remaining: Float, today: LocalDate) = SupplyInventoryEntity(
        supplementId = id,
        totalServings = 30,
        servingsRemaining = remaining,
        startDate = today,
    )
}
