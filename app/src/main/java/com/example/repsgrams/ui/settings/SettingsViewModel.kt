package com.example.repsgrams.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettings
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.reminder.ReminderScheduler
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: CycleSettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {
    val settings: StateFlow<CycleSettings?> = repository.settings.stateIn(
        viewModelScope, SharingStarted.Eagerly, null,
    )

    // Reminders
    fun setMaster(enabled: Boolean) = update { repository.setRemindersEnabled(enabled) }
    fun setWorkoutEnabled(enabled: Boolean) = update { repository.setWorkoutReminderEnabled(enabled) }
    fun setCreatineEnabled(enabled: Boolean) = update { repository.setCreatineReminderEnabled(enabled) }
    fun setWheyEnabled(enabled: Boolean) = update { repository.setPostWorkoutWheyReminderEnabled(enabled) }
    fun setWorkoutTime(time: LocalTime) = update { repository.setWorkoutReminderTime(time) }
    fun setCreatineTime(time: LocalTime) = update { repository.setCreatineReminderTime(time) }
    fun setWheyDelay(minutes: Int) = update { repository.setPostWorkoutWheyDelayMinutes(minutes) }

    // Cycle & Units
    fun setCycleStartDate(date: LocalDate) {
        viewModelScope.launch { repository.setCycleStartDate(date) }
    }
    fun setAdherenceGraceDays(days: Int) {
        viewModelScope.launch { repository.setAdherenceGraceDays(days) }
    }
    fun setUnitSystem(unit: UnitSystem) {
        viewModelScope.launch { repository.setUnitSystem(unit) }
    }

    // Supplements
    fun setWheyServingGrams(grams: Float) {
        viewModelScope.launch { repository.setWheyServingGrams(grams) }
    }
    fun setProteinGoalMultiplier(low: Float, high: Float) {
        viewModelScope.launch { repository.setProteinGoalMultipliers(low, high) }
    }

    // Timer Settings
    fun setRestTimerSound(sound: String) {
        viewModelScope.launch { repository.setRestTimerSound(sound) }
    }
    fun setRestTimerVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setRestTimerVibrationEnabled(enabled) }
    }
    fun setThemeMode(mode: com.example.repsgrams.data.datastore.ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }
    fun updateDefaultRestSeconds(seconds: Int) {
        viewModelScope.launch { repository.setDefaultRestSeconds(seconds) }
    }

    private fun update(change: suspend () -> Unit) {
        viewModelScope.launch {
            change()
            scheduler.syncDailyReminders()
        }
    }

    companion object {
        fun factory(repository: CycleSettingsRepository, scheduler: ReminderScheduler) =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, scheduler) as T
                }
            }
    }
    fun setTrackedMeasurements(measurements: Set<String>) {
        viewModelScope.launch {
            repository.setTrackedMeasurements(measurements)
        }
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHealthConnectEnabled(enabled)
        }
    }

    fun setVoiceCuesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVoiceCuesEnabled(enabled)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) = write { repository.setKeepScreenOn(enabled) }
    fun setProgressionEnabled(enabled: Boolean) = write { repository.setProgressionEnabled(enabled) }
    fun setProgressionIncrementKg(kilograms: Float) = write { repository.setProgressionIncrementKg(kilograms) }
    fun setProgressionIncrementLb(pounds: Float) = write { repository.setProgressionIncrementLb(pounds) }
    fun setRpeEnabled(enabled: Boolean) = write { repository.setRpeEnabled(enabled) }
    fun setHapticsEnabled(enabled: Boolean) = write { repository.setHapticsEnabled(enabled) }
    fun setBarbellKg(kilograms: Float) = write { repository.setBarbellKg(kilograms) }
    fun setBarbellLb(pounds: Float) = write { repository.setBarbellLb(pounds) }
    fun setPlatesKg(stored: String) = write { repository.setPlatesKg(stored) }
    fun setPlatesLb(stored: String) = write { repository.setPlatesLb(stored) }
    fun setWarmupRest(seconds: Int) = write { repository.setDefaultWarmupRestSeconds(seconds) }
    fun setWorkingRest(seconds: Int) = write { repository.setDefaultWorkingRestSeconds(seconds) }
    fun setSupersetRest(seconds: Int) = write { repository.setDefaultSupersetIntraRestSeconds(seconds) }

    private fun write(change: suspend () -> Unit) {
        viewModelScope.launch { change() }
    }

}
