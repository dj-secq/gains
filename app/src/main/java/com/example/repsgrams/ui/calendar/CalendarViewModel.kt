package com.example.repsgrams.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.repository.CalendarDayDetail
import com.example.repsgrams.data.repository.CalendarMonth
import com.example.repsgrams.data.repository.CalendarRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.reminder.ReminderScheduler
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: CalendarRepository,
    private val settingsRepository: CycleSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val workoutRepository: WorkoutRepository,
    private val clock: Clock,
) : ViewModel() {
    val today: LocalDate = LocalDate.now(clock)
    private val displayedMonth = MutableStateFlow(YearMonth.from(today))
    val month: StateFlow<CalendarMonth?> = displayedMonth
        .flatMapLatest(repository::observeMonth)
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _selectedDay = MutableStateFlow<CalendarDayDetail?>(null)
    val selectedDay: StateFlow<CalendarDayDetail?> = _selectedDay
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate
    private val _loadingDay = MutableStateFlow(false)
    val loadingDay: StateFlow<Boolean> = _loadingDay
    private var request = 0

    private val _openSession = MutableSharedFlow<Long>()
    val openSession: SharedFlow<Long> = _openSession.asSharedFlow()

    val activeSessionId: StateFlow<Long?> = workoutRepository.observeActiveSession()
        .map { session -> session?.id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun previousMonth() { displayedMonth.value = displayedMonth.value.minusMonths(1) }
    fun nextMonth() { displayedMonth.value = displayedMonth.value.plusMonths(1) }
    fun showToday() { displayedMonth.value = YearMonth.from(today) }

    fun selectDate(date: LocalDate) {
        val token = ++request
        _selectedDate.value = date
        _loadingDay.value = true
        viewModelScope.launch {
            val detail = runCatching { repository.dayDetail(date) }.getOrNull()
            if (token != request) return@launch
            _selectedDay.value = detail
            if (detail == null) _selectedDate.value = null
            _loadingDay.value = false
        }
    }

    fun closeDay() {
        request++
        _selectedDate.value = null
        _selectedDay.value = null
        _loadingDay.value = false
    }

    fun startSuggested(templateId: Long) {
        viewModelScope.launch { _openSession.emit(workoutRepository.startSession(templateId)) }
    }

    fun resume(sessionId: Long) {
        viewModelScope.launch { _openSession.emit(sessionId) }
    }

    companion object {
        fun factory(
            repository: CalendarRepository,
            settingsRepository: CycleSettingsRepository,
            reminderScheduler: ReminderScheduler,
            workoutRepository: WorkoutRepository,
            clock: Clock,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CalendarViewModel::class.java))
                return CalendarViewModel(
                    repository,
                    settingsRepository,
                    reminderScheduler,
                    workoutRepository,
                    clock,
                ) as T
            }
        }
    }
}
