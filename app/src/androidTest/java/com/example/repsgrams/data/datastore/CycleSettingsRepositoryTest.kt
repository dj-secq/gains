package com.example.repsgrams.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class CycleSettingsRepositoryTest {
    private lateinit var context: Context
    private val clock = Clock.fixed(Instant.parse("2026-09-10T04:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        context.cycleSettingsDataStore.edit { it.clear() }
    }

    @After
    fun tearDown() = runTest {
        context.cycleSettingsDataStore.edit { it.clear() }
    }

    @Test
    fun returnsFirstRunDefaults() = runTest {
        val repository = PreferencesCycleSettingsRepository(context, clock)
        repository.ensureInitialized()
        val settings = repository.settings.first()

        assertEquals(LocalDate.of(2026, 9, 10), settings.cycleStartDate)
        assertEquals(25f, settings.wheyServingGrams)
        assertEquals(1.6f, settings.proteinGoalMultiplierLow)
        assertEquals(2f, settings.proteinGoalMultiplierHigh)
        assertEquals(UnitSystem.KG, settings.unitSystem)
        assertFalse(settings.remindersEnabled)
        assertFalse(settings.workoutReminderEnabled)
        assertFalse(settings.creatineReminderEnabled)
        assertFalse(settings.postWorkoutWheyReminderEnabled)
        assertEquals(LocalTime.of(18, 0), settings.workoutReminderTime)
        assertEquals(LocalTime.of(20, 0), settings.creatineReminderTime)
        assertEquals(25, settings.postWorkoutWheyDelayMinutes)
    }

    @Test
    fun persistsFocusedUpdates() = runTest {
        val repository = PreferencesCycleSettingsRepository(context, clock)
        repository.ensureInitialized()
        repository.setCycleStartDate(LocalDate.of(2026, 8, 1))
        repository.setWheyServingGrams(30f)
        repository.setProteinGoalMultipliers(1.7f, 2.2f)
        repository.setUnitSystem(UnitSystem.LB)
        repository.setRemindersEnabled(true)
        repository.setWorkoutReminderEnabled(true)
        repository.setCreatineReminderEnabled(true)
        repository.setPostWorkoutWheyReminderEnabled(true)
        repository.setWorkoutReminderTime(LocalTime.of(17, 30))
        repository.setCreatineReminderTime(LocalTime.of(21, 15))
        repository.setPostWorkoutWheyDelayMinutes(30)

        val restored = PreferencesCycleSettingsRepository(context, clock).settings.first()
        assertEquals(LocalDate.of(2026, 8, 1), restored.cycleStartDate)
        assertEquals(30f, restored.wheyServingGrams)
        assertEquals(1.7f, restored.proteinGoalMultiplierLow)
        assertEquals(2.2f, restored.proteinGoalMultiplierHigh)
        assertEquals(UnitSystem.LB, restored.unitSystem)
        assertEquals(true, restored.remindersEnabled)
        assertEquals(true, restored.workoutReminderEnabled)
        assertEquals(true, restored.creatineReminderEnabled)
        assertEquals(true, restored.postWorkoutWheyReminderEnabled)
        assertEquals(LocalTime.of(17, 30), restored.workoutReminderTime)
        assertEquals(LocalTime.of(21, 15), restored.creatineReminderTime)
        assertEquals(30, restored.postWorkoutWheyDelayMinutes)
    }
}
