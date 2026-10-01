package com.example.repsgrams.data.repository

import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.domain.schedule.ScheduleEngine
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.scopeToActiveProgram
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

interface ScheduleRepository {
    fun observeSuggestion(today: LocalDate): Flow<ScheduleSuggestion>
    suspend fun getSuggestion(today: LocalDate): ScheduleSuggestion
}

class DefaultScheduleRepository(
    private val database: AppDatabase,
    private val settingsRepository: CycleSettingsRepository,
    private val clock: Clock,
) : ScheduleRepository {
    override fun observeSuggestion(today: LocalDate): Flow<ScheduleSuggestion> {
        // observeAll, not the last workout: a rest insert has to recompute the due date.
        return combine(
            database.workoutSessionDao().observeAll(),
            database.workoutTemplateDao().observeAll(),
            database.programDao().observeAll(),
        ) { sessions, templates, programs ->
            val scope = scopeToActiveProgram(programs, templates)
            ScheduleEngine.replay(
                today,
                sessions,
                scope.templates,
                includeDay = true,
                scheduleMode = scope.mode,
            )
        }.distinctUntilChanged()
    }

    override suspend fun getSuggestion(today: LocalDate): ScheduleSuggestion {
        val sessions = database.workoutSessionDao().getOnOrBefore(today)
        val templates = database.workoutTemplateDao().getAll()
        val scope = scopeToActiveProgram(database.programDao().getAll(), templates)
        return ScheduleEngine.replay(
            today,
            sessions,
            scope.templates,
            includeDay = true,
            scheduleMode = scope.mode,
        )
    }
}
