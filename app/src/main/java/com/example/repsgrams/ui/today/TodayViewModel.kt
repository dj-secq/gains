package com.example.repsgrams.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.db.SupplementEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.data.repository.ScheduleRepository
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.domain.progress.ProgressStatsCalculator
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import com.example.repsgrams.domain.schedule.SuggestionStatus
import java.time.temporal.ChronoUnit

data class TodaySupplement(val supplement: SupplementEntity, val taken: Boolean, val actualAmount: Float)

private data class TodayPlanData(
    val suggestion: ScheduleSuggestion,
    val supplements: List<TodaySupplement>,
    val activeSession: com.example.repsgrams.data.db.WorkoutSessionEntity?,
    val templates: List<com.example.repsgrams.data.db.WorkoutTemplateEntity>,
    val workoutCompletedToday: Boolean,
)

sealed interface TodayUiState {
    data object Loading : TodayUiState


    data class Content(
        val suggestion: ScheduleSuggestion,
        val supplements: List<TodaySupplement>,
        val activeSessionId: Long?,
        val templates: List<com.example.repsgrams.data.db.WorkoutTemplateEntity> = emptyList(),
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
        combine(
            scheduleRepository.observeSuggestion(today),
            combine(
                supplementRepository.observeAllSupplements(),
                supplementRepository.observeIntakesForDate(today)
            ) { allSupps, logs ->
                allSupps.filter { it.isActive }.map { supp ->
                    val log = logs.firstOrNull { it.supplementId == supp.id }
                    TodaySupplement(supp, log?.taken == true, log?.actualAmount?.takeIf { it > 0 } ?: supp.doseAmount)
                }
            },
            workoutRepository.observeActiveSession(),
            workoutRepository.observeAllTemplates(),
            workoutRepository.observeSessionsForDate(today),
        ) { suggestion, todaySupps, activeSession, templates, sessionsToday ->
            TodayPlanData(
                suggestion,
                todaySupps,
                activeSession,
                templates.sortedBy { it.orderIndex },
                sessionsToday.any { it.completed && it.templateId != null },
            )
        },
        combine(
            cycleSettingsRepository.settings.flatMapLatest { settings ->
                progressRepository.observeStreakInfo(today, settings.adherenceGraceDays)
            },
            progressRepository.observeSupplyInventory(),
            supplementRepository.observeAllSupplements()
        ) { streakInfo, supplies, allSupps ->
            Triple(streakInfo, supplies, allSupps)
        }
    ) { plan, (streakInfo, supplies, allSupps) ->
        // Only show supplements that are due today according to scheduleType
        val filteredSupps = plan.supplements.filter {
            val supp = it.supplement
            when (supp.scheduleType) {
                "daily" -> true
                "workoutDayOnly" -> plan.workoutCompletedToday
                "customDays" -> {
                    val currentDay = today.dayOfWeek.name.take(3).replaceFirstChar { it.uppercase() }
                    supp.customDays?.contains(currentDay, ignoreCase = true) == true
                }
                else -> true
            }
        }
        val warnings = buildWarnings(supplies, allSupps)
        TodayUiState.Content(
            suggestion = plan.suggestion,
            supplements = filteredSupps,
            activeSessionId = plan.activeSession?.id,
            templates = plan.templates,
            currentStreak = streakInfo.currentStreak,
            lowSupplyWarnings = warnings
        ) as TodayUiState
    }.flowOn(Dispatchers.Default).catch {
        emit(TodayUiState.Error("Couldn't load today's plan."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TodayUiState.Loading,
    )

    private fun buildWarnings(supplies: List<com.example.repsgrams.data.db.SupplyInventoryEntity>, allSupps: List<com.example.repsgrams.data.db.SupplementEntity>): List<String> {
        val warnings = mutableListOf<String>()
        for (inv in supplies) {
            val supp = allSupps.find { it.id == inv.supplementId } ?: continue
            if (!supp.isActive) continue
            val status = progressStatsCalculator.calculateSupplyStatus(inv, today, lowThreshold = supp.lowSupplyThreshold.toFloat())
            if (status.isLow) {
                val days = status.estimatedRunOutDate?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) } ?: 0
                val timeStr = if (days > 0) "about $days days left" else "soon"
                warnings.add("${supp.name} running low — $timeStr.")
            }
        }
        return warnings
    }

    fun setSupplementTaken(supplement: com.example.repsgrams.data.db.SupplementEntity, taken: Boolean, amount: Float? = null) {
        viewModelScope.launch { supplementRepository.setSupplementTaken(today, supplement, taken, amount) }
    }

    fun startWorkout(dayLabel: String) {
        viewModelScope.launch { _openSession.emit(workoutRepository.startSession(dayLabel)) }
    }

    fun resumeWorkout(sessionId: Long) {
        viewModelScope.launch { _openSession.emit(sessionId) }
    }

    fun logRestDay() {
        viewModelScope.launch { workoutRepository.logRestDay(today) }
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
