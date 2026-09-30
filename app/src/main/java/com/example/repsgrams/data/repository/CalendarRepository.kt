package com.example.repsgrams.data.repository

import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.CalendarSetLogRow
import com.example.repsgrams.data.db.SupplementIntakeLogEntity
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.domain.calendar.CalendarDay
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
            database.workoutSessionDao().observeOnOrBefore(dates.last()),
            database.workoutTemplateDao().observeAll(),
            database.personalRecordDao().observeNonEstimateInRange(dates.first(), dates.last()),
        ) { sessions, templates, records ->
            val today = LocalDate.now(clock)
            CalendarMonth(
                month,
                CalendarCalculator.buildMonth(
                    month = month,
                    today = today,
                    sessions = sessions,
                    templates = templates,
                    prDates = records.map { it.achievedDate }.toSet(),
                ),
            )
        }
    }

    suspend fun dayDetail(date: LocalDate): CalendarDayDetail {
        databaseReady.await()
        val today = LocalDate.now(clock)
        val settings = settingsRepository.settings.first()
        val sessionsForDate = database.workoutSessionDao().getForDate(date)
        val allSets = database.setLogDao().getForDate(date).groupBy { it.sessionId }
        val sessions = sessionsForDate.map { session ->
            val name = session.templateId?.let { database.workoutTemplateDao().getById(it)?.name }
                ?: if (session.notes == "Rest day") "Rest day" else "Deleted workout"
            CalendarSessionDetail(
                session.id, name, session.completed, session.durationSeconds, allSets[session.id].orEmpty(),
            )
        }

        val templates = database.workoutTemplateDao().getAll()
        val historyEnd = if (date.isAfter(today)) date else today
        val history = database.workoutSessionDao().getOnOrBefore(historyEnd)
        val day = CalendarCalculator.cellFor(date, today, history, templates)
        val hasLog = sessionsForDate.any { it.completed && (it.templateId != null || it.notes == "Rest day") }

        return CalendarDayDetail(
            date,
            day.template,
            sessions,
            database.supplementDao().observeAll().first().map { supp ->
                supp to database.supplementIntakeLogDao().getForDateAndSupplement(date, supp.id)
            },
            settings.unitSystem,
            projected = day.template != null && !hasLog && !date.isBefore(today),
        )
    }
}
