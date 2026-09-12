package com.example.repsgrams.data.repository

import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.domain.schedule.ScheduleEngine
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
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
        val lastSessionFlow = database.workoutSessionDao().observeLastCompletedSession()
        val templatesFlow = database.workoutTemplateDao().observeAll()

        return combine(lastSessionFlow, templatesFlow) { lastSession, templates ->
            val lastTemplate = lastSession?.templateId?.let { id ->
                templates.find { it.id == id }
            }
            ScheduleEngine.computeSuggestion(today, lastSession, lastTemplate, templates)
        }.distinctUntilChanged()
    }

    override suspend fun getSuggestion(today: LocalDate): ScheduleSuggestion {
        val lastSession = database.workoutSessionDao().getLastCompletedSession()
        val templates = database.workoutTemplateDao().getAll()
        val lastTemplate = lastSession?.templateId?.let { id ->
            templates.find { it.id == id }
        }
        return ScheduleEngine.computeSuggestion(today, lastSession, lastTemplate, templates)
    }
}
