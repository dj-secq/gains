package com.example.repsgrams.data.repository

import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.domain.schedule.CycleSlot
import com.example.repsgrams.domain.schedule.RotationCalculator
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ScheduleRepository {
    fun observeToday(): Flow<CycleSlot>
    fun slotFor(date: LocalDate): Flow<CycleSlot>
}

class DefaultScheduleRepository(
    private val settingsRepository: CycleSettingsRepository,
    private val clock: Clock,
) : ScheduleRepository {
    override fun observeToday(): Flow<CycleSlot> = slotFor(LocalDate.now(clock))

    override fun slotFor(date: LocalDate): Flow<CycleSlot> = settingsRepository.settings.map { settings ->
        RotationCalculator.slotFor(settings.cycleStartDate, date)
    }
}
