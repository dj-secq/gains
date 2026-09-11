package com.example.repsgrams.data.repository

import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.CalendarSetLogRow
import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.schedule.CycleSlot
import com.example.repsgrams.domain.schedule.RotationCalculator
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.example.repsgrams.data.datastore.UnitSystem

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
    val slot: CycleSlot,
    val sessions: List<CalendarSessionDetail>,
    val supplements: SupplementLogEntity?,
    val unitSystem: UnitSystem,
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
            settingsRepository.settings,
            database.workoutSessionDao().observeInRange(dates.first(), dates.last()),
            database.supplementLogDao().observeInRange(dates.first(), dates.last()),
        ) { settings, sessions, supplements ->
            CalendarMonth(
                month,
                CalendarCalculator.buildMonth(
                    month, LocalDate.now(clock), settings.cycleStartDate, sessions, supplements,
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
        return CalendarDayDetail(
            date,
            RotationCalculator.slotFor(settings.cycleStartDate, date),
            sessions,
            database.supplementLogDao().getForDate(date),
            settings.unitSystem,
        )
    }
}
