package com.example.repsgrams.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.db.SupplyType
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.data.repository.ScheduleRepository
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.domain.progress.ProgressStatsCalculator
import com.example.repsgrams.domain.schedule.CycleSlot
import com.example.repsgrams.domain.streak.StreakInfo
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class Content(
        val slot: CycleSlot,
        val creatineTaken: Boolean,
        val wheyTaken: Boolean,
        val activeSessionId: Long?,
        val currentStreak: Int = 0,
        val lowSupplyWarnings: List<String> = emptyList(),
    ) : TodayUiState
    data class Error(val message: String) : TodayUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val supplementRepository: SupplementRepository,
    private val workoutRepository: WorkoutRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val progressRepository: ProgressRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val _openSession = MutableSharedFlow<Long>()
    val openSession: SharedFlow<Long> = _openSession.asSharedFlow()

    private val progressStatsCalculator = ProgressStatsCalculator()

    val uiState: StateFlow<TodayUiState> = combine(
        scheduleRepository.slotFor(today),
        supplementRepository.observeForDate(today),
        workoutRepository.observeActiveSession(),
        cycleSettingsRepository.settings.flatMapLatest { settings ->
            progressRepository.observeStreakInfo(settings.cycleStartDate, today)
        },
        progressRepository.observeSupplyInventory()
    ) { slot, supplements, activeSession, streakInfo, supplies ->
        val warnings = buildWarnings(supplies)
        TodayUiState.Content(
            slot = slot,
            creatineTaken = supplements?.creatineTaken == true,
            wheyTaken = supplements?.wheyTaken == true,
            activeSessionId = activeSession?.id,
            currentStreak = streakInfo.currentStreak,
            lowSupplyWarnings = warnings
        ) as TodayUiState
    }.catch {
        emit(TodayUiState.Error("Couldn't load today's plan."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState.Loading,
    )

    private fun buildWarnings(supplies: List<SupplyInventoryEntity>): List<String> {
        val warnings = mutableListOf<String>()
        val whey = supplies.find { it.type == SupplyType.WHEY }
        if (whey != null) {
            val status = progressStatsCalculator.calculateSupplyStatus(whey, today, lowThreshold = 7f)
            if (status.isLow) {
                val days = status.estimatedRunOutDate?.let { ChronoUnit.DAYS.between(today, it) } ?: 0
                val timeStr = if (days > 0) "about $days days left" else "soon"
                warnings.add("Whey running low — $timeStr.")
            }
        }
        val creatine = supplies.find { it.type == SupplyType.CREATINE }
        if (creatine != null) {
            val status = progressStatsCalculator.calculateSupplyStatus(creatine, today, lowThreshold = 5f)
            if (status.isLow) {
                val days = status.estimatedRunOutDate?.let { ChronoUnit.DAYS.between(today, it) } ?: 0
                val timeStr = if (days > 0) "about $days days left" else "soon"
                warnings.add("Creatine running low — $timeStr.")
            }
        }
        return warnings
    }

    fun setCreatineTaken(taken: Boolean) {
        viewModelScope.launch { supplementRepository.setCreatineTaken(today, taken) }
    }

    fun setWheyTaken(taken: Boolean) {
        viewModelScope.launch { supplementRepository.setWheyTaken(today, taken) }
    }

    fun startWorkout(dayLabel: String) {
        viewModelScope.launch { _openSession.emit(workoutRepository.startSession(dayLabel)) }
    }

    fun resumeWorkout(sessionId: Long) {
        viewModelScope.launch { _openSession.emit(sessionId) }
    }

    companion object {
        fun factory(
            scheduleRepository: ScheduleRepository,
            supplementRepository: SupplementRepository,
            workoutRepository: WorkoutRepository,
            cycleSettingsRepository: CycleSettingsRepository,
            progressRepository: ProgressRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TodayViewModel::class.java))
                return TodayViewModel(
                    scheduleRepository,
                    supplementRepository,
                    workoutRepository,
                    cycleSettingsRepository,
                    progressRepository
                ) as T
            }
        }
    }
}
