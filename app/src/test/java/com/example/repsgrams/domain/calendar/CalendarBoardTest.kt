package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.progress.kilogramsToPounds
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.SuggestionStatus
import com.example.repsgrams.domain.session.formatWeight
import com.example.repsgrams.domain.today.TodayHeroMode
import com.example.repsgrams.domain.today.heroFor
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarBoardTest {
    private val push = WorkoutTemplateEntity(
        id = 1,
        name = "Push",
        dayLabel = "A",
        maxDurationMinutes = 60,
    )
    private val friday = LocalDate.of(2026, 9, 11)

    @Test
    fun marksFollowTheEncodingTable() {
        assertEquals(DayMarkKind.TRAINED, dayMarkKind(day(CalendarDayStatus.COMPLETE, push)))
        assertEquals(DayMarkKind.PR, dayMarkKind(day(CalendarDayStatus.COMPLETE, push, hasPr = true)))
        assertEquals(DayMarkKind.NUMERAL, dayMarkKind(day(CalendarDayStatus.COMPLETE, template = null, hasPr = true)))
        assertEquals(DayMarkKind.PENDING, dayMarkKind(day(CalendarDayStatus.PENDING, push, hasPr = true)))
        assertEquals(DayMarkKind.MISSED, dayMarkKind(day(CalendarDayStatus.MISSED, push)))
        assertEquals(DayMarkKind.UPCOMING, dayMarkKind(day(CalendarDayStatus.UPCOMING, push)))
        assertEquals(DayMarkKind.NUMERAL, dayMarkKind(day(CalendarDayStatus.EMPTY)))
        assertEquals(DayMarkKind.NUMERAL, dayMarkKind(day(CalendarDayStatus.COMPLETE, template = null)))
    }

    @Test
    fun sheetPillMatchesTheHeroOrder() {
        val onTime = ScheduleSuggestion(push, SuggestionStatus.ON_TIME, friday)
        val rest = ScheduleSuggestion(push, SuggestionStatus.REST_DAY, friday.plusDays(1))
        val overdue = ScheduleSuggestion(push, SuggestionStatus.OVERDUE, friday.minusDays(1))
        val none = ScheduleSuggestion(null, SuggestionStatus.NO_HISTORY, null)

        assertEquals(TodayHeroMode.LIVE, heroFor(true, null, onTime, true).mode)
        assertEquals(
            CalendarSheetAction.RESUME,
            calendarSheetAction(
                isToday = true,
                hasActiveSession = true,
                completedWorkout = true,
                restDay = true,
                hasTemplate = true,
            ),
        )

        assertNull(heroFor(false, push, rest, true).pill)
        assertNull(
            calendarSheetAction(
                isToday = true,
                hasActiveSession = false,
                completedWorkout = true,
                restDay = true,
                hasTemplate = true,
            ),
        )

        assertNull(heroFor(false, null, rest, true).pill)
        assertNull(
            calendarSheetAction(
                isToday = true,
                hasActiveSession = false,
                completedWorkout = false,
                restDay = true,
                hasTemplate = false,
            ),
        )

        assertNull(heroFor(false, null, none, false).pill)
        assertNull(
            calendarSheetAction(
                isToday = true,
                hasActiveSession = false,
                completedWorkout = false,
                restDay = false,
                hasTemplate = false,
            ),
        )

        assertEquals("START", heroFor(false, null, onTime, true).pill)
        assertEquals("START", heroFor(false, null, overdue, true).pill)
        assertEquals(
            CalendarSheetAction.START,
            calendarSheetAction(
                isToday = true,
                hasActiveSession = false,
                completedWorkout = false,
                restDay = false,
                hasTemplate = true,
            ),
        )

        assertEquals("RESUME", heroFor(true, null, onTime, true).pill)
        assertNull(
            calendarSheetAction(
                isToday = false,
                hasActiveSession = true,
                completedWorkout = false,
                restDay = false,
                hasTemplate = true,
            ),
        )
    }

    @Test
    fun setLinesPutTheWorkBeforeTheLoad() {
        assertEquals("6 × 80 kg", formatCalendarSet(6, null, 80f, UnitSystem.KG))
        assertEquals("6", formatCalendarSet(6, null, null, UnitSystem.KG))
        assertEquals("40s", formatCalendarSet(null, 40, null, UnitSystem.KG))
        assertEquals("40s × 80 kg", formatCalendarSet(null, 40, 80f, UnitSystem.KG))
        assertEquals("6 × 80.5 kg", formatCalendarSet(6, null, 80.5f, UnitSystem.KG))
        val pounds = formatWeight(kilogramsToPounds(80f))
        assertEquals("6 × $pounds lb", formatCalendarSet(6, null, 80f, UnitSystem.LB))
        assertEquals("40s × $pounds lb", formatCalendarSet(null, 40, 80f, UnitSystem.LB))
        assertNull(formatCalendarSet(null, null, null, UnitSystem.KG))
    }

    @Test
    fun sheetDateIsTheFullWeekday() {
        assertEquals("Friday 11 September 2026", formatCalendarDate(friday))
    }

    private fun day(
        status: CalendarDayStatus,
        template: WorkoutTemplateEntity? = null,
        hasPr: Boolean = false,
    ) = CalendarDay(
        date = friday,
        inDisplayedMonth = true,
        template = template,
        status = status,
        isPast = status == CalendarDayStatus.MISSED || status == CalendarDayStatus.EMPTY,
        hasPr = hasPr,
    )
}
