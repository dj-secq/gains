package com.example.repsgrams.domain.calendar

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.schedule.ScheduleEngine
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.SuggestionStatus
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class CalendarDayStatus { COMPLETE, MISSED, PENDING, UPCOMING }

data class CalendarDay(
    val date: LocalDate,
    val inDisplayedMonth: Boolean,
    val template: WorkoutTemplateEntity?,
    val status: CalendarDayStatus,
    val isPast: Boolean,
)

object CalendarCalculator {
    fun datesForMonth(month: YearMonth): List<LocalDate> {
        val first = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return List(42) { first.plusDays(it.toLong()) }
    }

    fun buildMonth(
        month: YearMonth,
        today: LocalDate,
        sessions: List<WorkoutSessionEntity>,
        templates: List<WorkoutTemplateEntity>,
        currentSuggestion: ScheduleSuggestion
    ): List<CalendarDay> {
        val sessionsByDate = sessions.groupBy { it.date }
        
        // We need to project future days
        // We know the current suggestion's due date and template
        
        return datesForMonth(month).map { date ->
            val isPast = date.isBefore(today)
            
            val template: WorkoutTemplateEntity?
            val status: CalendarDayStatus
            
            if (isPast) {
                // Past days: show what was actually logged
                val session = sessionsByDate[date]?.find { it.completed }
                template = session?.templateId?.let { tid -> templates.find { it.id == tid } }
                
                // For past days, if a workout was logged, it's COMPLETE. 
                // Wait, what if they didn't log anything? Then it's MISSED if it was a workout day?
                // Actually, "Past days: show what was actually logged (workout type completed, or explicitly logged rest, or nothing logged)"
                status = if (template != null) CalendarDayStatus.COMPLETE else CalendarDayStatus.MISSED
            } else {
                // Today or Future
                // For simplicity, we just project based on the current suggestion
                // Let's extrapolate the schedule:
                // If the user does the suggested workout exactly on its dueDate:
                // We can generate a sequence of future due dates.
                
                // Let's find out if this date is a projected workout date
                var iterDate = currentSuggestion.dueDate ?: today
                var iterTemplate = currentSuggestion.suggestedTemplate
                var foundTemplate: WorkoutTemplateEntity? = null
                
                // A quick way to project forward (capped to 42 days for safety)
                val sortedTemplates = templates.sortedBy { it.orderIndex }
                
                for (i in 0..42) {
                    if (iterDate == date) {
                        foundTemplate = iterTemplate
                        break
                    }
                    if (iterDate.isAfter(date)) {
                        break
                    }
                    
                    // advance to next
                    if (sortedTemplates.isNotEmpty() && iterTemplate != null) {
                        val currIdx = sortedTemplates.indexOfFirst { it.id == iterTemplate!!.id }
                        val nextIdx = if (currIdx == -1 || currIdx == sortedTemplates.lastIndex) 0 else currIdx + 1
                        val nextTmpl = sortedTemplates[nextIdx]
                        iterDate = iterDate.plusDays(iterTemplate!!.restDaysAfter.toLong() + 1L)
                        iterTemplate = nextTmpl
                    } else {
                        break
                    }
                }
                
                template = foundTemplate
                status = if (date == today) {
                    if (template != null) CalendarDayStatus.PENDING else CalendarDayStatus.COMPLETE // no workout today means rest is pending/complete
                } else {
                    CalendarDayStatus.UPCOMING
                }
            }
            
            CalendarDay(
                date = date,
                inDisplayedMonth = YearMonth.from(date) == month,
                template = template,
                status = status,
                isPast = isPast
            )
        }
    }
}
