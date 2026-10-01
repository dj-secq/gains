package com.example.repsgrams.domain.widget

import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.calendar.DayMarkKind
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.today.TodayHeroMode
import com.example.repsgrams.domain.today.heroFor

/**
 * Home-screen tile. Status follows [heroFor]: a live session, then a workout finished today,
 * then a rest gap, then no program, then the suggested workout. Overdue stays [DUE].
 */
data class WidgetFace(
    val status: String = "",
    val headline: String = "",
    val detail: String = "",
    val live: Boolean = false,
    val marks: List<DayMarkKind> = emptyList(),
)

const val WIDGET_LIVE = "LIVE"
const val WIDGET_DONE = "DONE"
const val WIDGET_REST = "REST"
const val WIDGET_DUE = "DUE"
const val WIDGET_RESUME = "RESUME"
const val WIDGET_IN_PROGRESS = "In progress"

fun widgetFace(
    hasActiveSession: Boolean,
    completedToday: WorkoutTemplateEntity?,
    suggestion: ScheduleSuggestion,
    hasTemplates: Boolean,
    nextExercise: String?,
    marks: List<DayMarkKind>,
): WidgetFace {
    val exercise = nextExercise?.trim().orEmpty()
    return when (heroFor(hasActiveSession, completedToday, suggestion, hasTemplates).mode) {
        TodayHeroMode.LIVE -> WidgetFace(
            status = WIDGET_LIVE,
            headline = WIDGET_RESUME,
            detail = WIDGET_IN_PROGRESS,
            live = true,
            marks = marks,
        )
        TodayHeroMode.DONE -> WidgetFace(
            status = WIDGET_DONE,
            headline = dayLabelOf(completedToday),
            detail = exercise,
            marks = marks,
        )
        TodayHeroMode.REST -> WidgetFace(
            status = WIDGET_REST,
            headline = WIDGET_REST,
            detail = exercise,
            marks = marks,
        )
        TodayHeroMode.NO_PROGRAM -> WidgetFace(marks = marks)
        TodayHeroMode.START -> WidgetFace(
            status = WIDGET_DUE,
            headline = dayLabelOf(suggestion.suggestedTemplate),
            detail = exercise,
            marks = marks,
        )
    }
}

private fun dayLabelOf(template: WorkoutTemplateEntity?): String {
    if (template == null) return ""
    val label = template.dayLabel.trim()
    return if (label.isEmpty()) template.name else label
}
