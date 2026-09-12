package com.example.repsgrams.data.repository

import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.CalendarSetLogRow
import com.example.repsgrams.data.db.SupplementIntakeLogEntity
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.schedule.ScheduleEngine
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class CalendarMonth(val month: YearMonth, val days: List<CalendarDay>)

data class CalendarSessionDetail(
    val sessionId: Long,
    val workoutName: String,
    val completed: Boolean,
    val durationSeconds: Int?,
    val sets: List<CalendarSetLogRow>,
)

data class CalendarDayDetail(
    val date: LocalDate,
    val template: com.example.repsgrams.data.db.WorkoutTemplateEntity?,
    val sessions: List<CalendarSessionDetail>,
    val supplements: List<Pair<com.example.repsgrams.data.db.SupplementEntity, com.example.repsgrams.data.db.SupplementIntakeLogEntity?>>,
    val unitSystem: UnitSystem,
    val projected: Boolean = false
)

class CalendarRepository(
    private val database: AppDatabase,
    private val settingsRepository: CycleSettingsRepository,
    private val clock: Clock,
    private val databaseReady: Deferred<Unit>,
) {
    fun observeMonth(month: YearMonth): Flow<CalendarMonth> {
        val dates = CalendarCalculator.datesForMonth(month)
        return combine(
            database.workoutSessionDao().observeInRange(dates.first(), dates.last()),
            database.supplementIntakeLogDao().observeInRange(dates.first(), dates.last()),
            database.workoutTemplateDao().observeAll(),
            database.workoutSessionDao().observeLastCompletedSession()
        ) { sessions, supplements, templates, lastSession ->
            val today = LocalDate.now(clock)
            val lastTemplate = lastSession?.templateId?.let { id -> templates.find { it.id == id } }
            val currentSuggestion = ScheduleEngine.computeSuggestion(today, lastSession, lastTemplate, templates)
            CalendarMonth(
                month,
                CalendarCalculator.buildMonth(
                    month = month,
                    today = today,
                    sessions = sessions,
                    templates = templates,
                    currentSuggestion = currentSuggestion
                ),
            )
        }
    }

    suspend fun dayDetail(date: LocalDate): CalendarDayDetail {
        databaseReady.await()
        val settings = settingsRepository.settings.first()
        val allSets = database.setLogDao().getForDate(date).groupBy { it.sessionId }
        val sessions = database.workoutSessionDao().getForDate(date).map { session ->
            val name = session.templateId?.let { database.workoutTemplateDao().getById(it)?.name }
                ?: "Deleted workout"
            CalendarSessionDetail(
                session.id, name, session.completed, session.durationSeconds, allSets[session.id].orEmpty(),
            )
        }
        
        // Find if there is a projected template for this date
        val templates = database.workoutTemplateDao().getAll()
        val lastSession = database.workoutSessionDao().getLastCompletedSession()
        val lastTemplate = lastSession?.templateId?.let { id -> templates.find { it.id == id } }
        val currentSuggestion = ScheduleEngine.computeSuggestion(LocalDate.now(clock), lastSession, lastTemplate, templates)
        
        // Extrapolate like in CalendarCalculator
        var projectedTemplate: com.example.repsgrams.data.db.WorkoutTemplateEntity? = null
        if (!date.isBefore(LocalDate.now(clock))) {
            var iterDate = currentSuggestion.dueDate ?: LocalDate.now(clock)
            var iterTemplate = currentSuggestion.suggestedTemplate
            val sortedTemplates = templates.sortedBy { it.orderIndex }
            
            for (i in 0..42) {
                if (iterDate == date) {
                    projectedTemplate = iterTemplate
                    break
                }
                if (iterDate.isAfter(date)) {
                    break
                }
                if (sortedTemplates.isNotEmpty() && iterTemplate != null) {
                    val currIdx = sortedTemplates.indexOfFirst { it.id == iterTemplate!!.id }
                    val nextIdx = if (currIdx == -1 || currIdx == sortedTemplates.lastIndex) 0 else currIdx + 1
                    iterDate = iterDate.plusDays(iterTemplate!!.restDaysAfter.toLong() + 1L)
                    iterTemplate = sortedTemplates[nextIdx]
                } else {
                    break
                }
            }
        } else {
            val session = sessions.find { it.completed }
            val tName = session?.workoutName
            projectedTemplate = templates.find { it.name == tName }
        }

        return CalendarDayDetail(
            date,
            projectedTemplate,
            sessions,
            database.supplementDao().observeAll().first().map { supp ->
                supp to database.supplementIntakeLogDao().getForDateAndSupplement(date, supp.id)
            },
            settings.unitSystem,
            projected = !date.isBefore(LocalDate.now(clock))
        )
    }
}
