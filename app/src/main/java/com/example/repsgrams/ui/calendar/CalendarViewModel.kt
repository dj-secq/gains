package com.example.repsgrams.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.repository.CalendarDayDetail
import com.example.repsgrams.data.repository.CalendarMonth
import com.example.repsgrams.data.repository.CalendarRepository
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.reminder.ReminderScheduler
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CalendarViewModel(
    private val repository: CalendarRepository,
    private val settingsRepository: CycleSettingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock,
) : ViewModel() {
    val today: LocalDate = LocalDate.now(clock)
    private val displayedMonth = MutableStateFlow(YearMonth.from(today))
    val month: StateFlow<CalendarMonth?> = displayedMonth
        .flatMapLatest(repository::observeMonth)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selectedDay = MutableStateFlow<CalendarDayDetail?>(null)
    val selectedDay: StateFlow<CalendarDayDetail?> = _selectedDay
    private val _loadingDay = MutableStateFlow(false)
    val loadingDay: StateFlow<Boolean> = _loadingDay

    fun previousMonth() { displayedMonth.value = displayedMonth.value.minusMonths(1) }
    fun nextMonth() { displayedMonth.value = displayedMonth.value.plusMonths(1) }
    fun showToday() { displayedMonth.value = YearMonth.from(today) }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            _loadingDay.value = true
            _selectedDay.value = runCatching { repository.dayDetail(date) }.getOrNull()
            _loadingDay.value = false
        }
    }

    fun closeDay() { _selectedDay.value = null }



    companion object {
        fun factory(
            repository: CalendarRepository,
            settingsRepository: CycleSettingsRepository,
            reminderScheduler: ReminderScheduler,
            clock: Clock,
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CalendarViewModel::class.java))
                return CalendarViewModel(repository, settingsRepository, reminderScheduler, clock) as T
            }
        }
    }
}
