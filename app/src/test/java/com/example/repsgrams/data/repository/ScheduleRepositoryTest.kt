package com.example.repsgrams.data.repository

import com.example.repsgrams.data.datastore.CycleSettings
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.domain.schedule.CycleSlot
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleRepositoryTest {
    private val today = LocalDate.of(2026, 9, 10)
    private val clock = Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `observeToday uses the injected clock`() = runTest {
        val settings = FakeSettingsRepository(today.minusDays(2))
        val repository = DefaultScheduleRepository(settings, clock)

        assertEquals(CycleSlot(3, "B"), repository.observeToday().first())
    }

    @Test
    fun `slot flow reacts to cycle start updates`() = runTest {
        val settings = FakeSettingsRepository(today)
        val repository = DefaultScheduleRepository(settings, clock)

        assertEquals(CycleSlot(1, "A"), repository.slotFor(today).first())
        settings.setCycleStartDate(today.minusDays(1))
        assertEquals(CycleSlot(2, null), repository.slotFor(today).first())
    }

    private class FakeSettingsRepository(startDate: LocalDate) : CycleSettingsRepository {
        private val state = MutableStateFlow(defaultSettings(startDate))
        override val settings: Flow<CycleSettings> = state

        override suspend fun ensureInitialized() = Unit

        override suspend fun setCycleStartDate(date: LocalDate) {
            state.value = state.value.copy(cycleStartDate = date)
        }

        override suspend fun setWheyServingGrams(grams: Float) {
            state.value = state.value.copy(wheyServingGrams = grams)
        }

        override suspend fun setProteinGoalMultipliers(low: Float, high: Float) {
            state.value = state.value.copy(proteinGoalMultiplierLow = low, proteinGoalMultiplierHigh = high)
        }

        override suspend fun setUnitSystem(unitSystem: UnitSystem) {
            state.value = state.value.copy(unitSystem = unitSystem)
        }

        override suspend fun setRemindersEnabled(enabled: Boolean) {
            state.value = state.value.copy(remindersEnabled = enabled)
        }
        override suspend fun setWorkoutReminderEnabled(enabled: Boolean) = Unit
        override suspend fun setCreatineReminderEnabled(enabled: Boolean) = Unit
        override suspend fun setPostWorkoutWheyReminderEnabled(enabled: Boolean) = Unit
        override suspend fun setWorkoutReminderTime(time: LocalTime) = Unit
        override suspend fun setCreatineReminderTime(time: LocalTime) = Unit
        override suspend fun setPostWorkoutWheyDelayMinutes(minutes: Int) = Unit
    }

    private companion object {
        fun defaultSettings(startDate: LocalDate) = CycleSettings(
            cycleStartDate = startDate,
            wheyServingGrams = 25f,
            proteinGoalMultiplierLow = 1.6f,
            proteinGoalMultiplierHigh = 2f,
            unitSystem = UnitSystem.KG,
            remindersEnabled = false,
            workoutReminderEnabled = false,
            creatineReminderEnabled = false,
            postWorkoutWheyReminderEnabled = false,
            workoutReminderTime = LocalTime.of(18, 0),
            creatineReminderTime = LocalTime.of(20, 0),
            postWorkoutWheyDelayMinutes = 25,
        )
    }
}
