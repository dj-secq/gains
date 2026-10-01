package com.example.repsgrams.widget

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.example.repsgrams.RepsGramsApplication
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.domain.calendar.DayMarkKind
import com.example.repsgrams.domain.calendar.dayMarkKind
import com.example.repsgrams.domain.today.TodayHeroMode
import com.example.repsgrams.domain.today.heroFor
import com.example.repsgrams.domain.today.weekContaining
import com.example.repsgrams.domain.widget.WidgetFace
import com.example.repsgrams.domain.widget.widgetFace
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

private const val WIDGET_LOG = "Widget"

private val presentKey = booleanPreferencesKey("widget_present")
private val statusKey = stringPreferencesKey("widget_status")
private val headlineKey = stringPreferencesKey("widget_headline")
private val detailKey = stringPreferencesKey("widget_detail")
private val liveKey = booleanPreferencesKey("widget_live")
private val marksKey = stringPreferencesKey("widget_marks")

/** Redraws placed tiles. Does not open Room when database initialization never started. */
suspend fun refreshHomeWidget(context: Context) {
    runCatching {
        val face = loadWidgetFace(context)
        val manager = GlanceAppWidgetManager(context)
        manager.getGlanceIds(RepsGramsWidget::class.java).forEach { id ->
            writeWidgetFace(context, id, face)
        }
        RepsGramsWidget().updateAll(context)
    }.onFailure { error ->
        Log.e(WIDGET_LOG, "${error.javaClass.simpleName}: ${error.message}")
    }
}

internal suspend fun loadWidgetFace(context: Context): WidgetFace {
    val app = context.applicationContext as? RepsGramsApplication ?: return WidgetFace()
    val ready = app.container.databaseInitialization ?: return WidgetFace()
    ready.await()
    val container = app.container
    val today = LocalDate.now(container.clock)
    val templates = container.database.workoutTemplateDao().getAll()
    val suggestion = container.scheduleRepository.getSuggestion(today)
    val active = container.database.workoutSessionDao().getActive()
    val sessionsToday = container.database.workoutSessionDao().getForDate(today)
    val completed = templates.find { template ->
        template.id == CalendarCalculator.completedWorkoutOn(sessionsToday, templates)?.templateId
    }
    val hasTemplates = templates.isNotEmpty()
    val month = container.calendarRepository.observeMonth(YearMonth.from(today)).first()
    val marks = weekContaining(month.days, today).map(::dayMarkKind)
    val mode = heroFor(active != null, completed, suggestion, hasTemplates).mode
    val exerciseTemplateId = when (mode) {
        TodayHeroMode.LIVE, TodayHeroMode.NO_PROGRAM -> null
        else -> suggestion.suggestedTemplate?.id
    }
    val nextExercise = exerciseTemplateId?.let { firstExerciseName(container.database, it) }
    return widgetFace(
        hasActiveSession = active != null,
        completedToday = completed,
        suggestion = suggestion,
        hasTemplates = hasTemplates,
        nextExercise = nextExercise,
        marks = marks,
    )
}

internal suspend fun writeWidgetFace(context: Context, id: GlanceId, face: WidgetFace) {
    updateAppWidgetState(context, id) { prefs ->
        prefs[presentKey] = true
        prefs[statusKey] = face.status
        prefs[headlineKey] = face.headline
        prefs[detailKey] = face.detail
        prefs[liveKey] = face.live
        prefs[marksKey] = face.marks.joinToString(",") { it.name }
    }
}

internal fun Preferences.widgetFaceOrNull(): WidgetFace? {
    if (this[presentKey] != true) return null
    val marks = this[marksKey].orEmpty().split(',').mapNotNull { token ->
        token.takeIf { it.isNotEmpty() }?.let { runCatching { DayMarkKind.valueOf(it) }.getOrNull() }
    }
    return WidgetFace(
        status = this[statusKey].orEmpty(),
        headline = this[headlineKey].orEmpty(),
        detail = this[detailKey].orEmpty(),
        live = this[liveKey] == true,
        marks = marks,
    )
}

private suspend fun firstExerciseName(database: AppDatabase, templateId: Long): String? {
    for (block in database.templateBlockDao().getForTemplate(templateId)) {
        val link = database.templateBlockExerciseDao().getForBlock(block.id).firstOrNull() ?: continue
        val name = database.exerciseDao().getById(link.exerciseId)?.name?.trim().orEmpty()
        if (name.isNotEmpty()) return name
    }
    return null
}
