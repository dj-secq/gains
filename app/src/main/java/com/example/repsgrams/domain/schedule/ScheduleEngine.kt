package com.example.repsgrams.domain.schedule

import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class SuggestionStatus {
    ON_TIME,
    OVERDUE,
    REST_DAY,
    NO_HISTORY
}

data class ScheduleSuggestion(
    val suggestedTemplate: WorkoutTemplateEntity?,
    val status: SuggestionStatus,
    val dueDate: LocalDate?,
    val daysUntilDue: Int = 0,
    val overdueByDays: Int = 0,
)

object ScheduleEngine {
    fun computeSuggestion(
        today: LocalDate,
        lastSession: WorkoutSessionEntity?,
        lastTemplate: WorkoutTemplateEntity?,
        allTemplates: List<WorkoutTemplateEntity>,
    ): ScheduleSuggestion {
        if (allTemplates.isEmpty()) {
            return ScheduleSuggestion(null, SuggestionStatus.NO_HISTORY, null)
        }

        if (lastSession == null || lastTemplate == null) {
            // "If there is no prior completed session at all: suggestion = whichever template the user picked as 'start with'"
            // For now, we return the first template by orderIndex
            val first = allTemplates.minByOrNull { it.orderIndex }
            return ScheduleSuggestion(
                suggestedTemplate = first,
                status = SuggestionStatus.NO_HISTORY,
                dueDate = today
            )
        }

        // Find next template in order
        val sortedTemplates = allTemplates.sortedBy { it.orderIndex }
        val currentIndex = sortedTemplates.indexOfFirst { it.id == lastTemplate.id }
        val nextTemplate = if (currentIndex != -1 && currentIndex < sortedTemplates.lastIndex) {
            sortedTemplates[currentIndex + 1]
        } else {
            sortedTemplates.first()
        }

        // Compute dueDate = D + T.restDaysAfter + 1
        val lastDate = lastSession.date
        val dueDate = lastDate.plusDays(lastTemplate.restDaysAfter.toLong() + 1L)

        return when {
            today.isBefore(dueDate) -> {
                val daysUntil = ChronoUnit.DAYS.between(today, dueDate).toInt()
                ScheduleSuggestion(
                    suggestedTemplate = nextTemplate,
                    status = SuggestionStatus.REST_DAY,
                    dueDate = dueDate,
                    daysUntilDue = daysUntil
                )
            }
            today.isEqual(dueDate) -> {
                ScheduleSuggestion(
                    suggestedTemplate = nextTemplate,
                    status = SuggestionStatus.ON_TIME,
                    dueDate = dueDate
                )
            }
            else -> {
                val overdue = ChronoUnit.DAYS.between(dueDate, today).toInt()
                ScheduleSuggestion(
                    suggestedTemplate = nextTemplate,
                    status = SuggestionStatus.OVERDUE,
                    dueDate = dueDate,
                    overdueByDays = overdue
                )
            }
        }
    }
}
