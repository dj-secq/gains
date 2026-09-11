package com.example.repsgrams.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.CycleSettings
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.ExerciseSetHistoryRow
import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.db.BodyweightLogEntity
import com.example.repsgrams.data.db.SupplyType
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.domain.progress.ProgressStatsCalculator
import com.example.repsgrams.domain.progress.ProteinEstimate
import com.example.repsgrams.domain.progress.SupplyStatus
import com.example.repsgrams.domain.streak.StreakInfo
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val selectedExerciseId: Long? = null,
    val exerciseHistory: List<ExerciseSetHistoryRow> = emptyList(),
    val isWeightView: Boolean = true,
    
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    
    val bodyweightHistory: List<Pair<LocalDate, Float>> = emptyList(),
    
    val creatineAdherence30d: Float = 0f,
    val creatineAdherence90d: Float = 0f,
    
    val proteinEstimate: ProteinEstimate? = null,
    
    val wheyStatus: SupplyStatus? = null,
    val creatineStatus: SupplyStatus? = null,
    val wheyInventory: SupplyInventoryEntity? = null,
    val creatineInventory: SupplyInventoryEntity? = null,
)

private data class ExerciseData(
    val exercises: List<ExerciseEntity>,
    val selectedExerciseId: Long?,
    val isWeightView: Boolean,
    val exerciseHistory: List<ExerciseSetHistoryRow>
)

private data class UserStats(
    val streakInfo: StreakInfo,
    val bwHistory: List<BodyweightLogEntity>,
    val suppLogs: List<SupplementLogEntity>,
    val supplies: List<SupplyInventoryEntity>,
    val settings: CycleSettings
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    private val _isWeightView = MutableStateFlow(true)
    private val statsCalculator = ProgressStatsCalculator()

    private val exerciseDataFlow = combine(
        progressRepository.observeAllExercises(),
        _selectedExerciseId,
        _isWeightView,
        _selectedExerciseId.flatMapLatest { id -> 
            if (id != null) progressRepository.observeExerciseHistory(id) else flowOf(emptyList()) 
        }
    ) { exercises, selectedId, isWeight, history ->
        ExerciseData(exercises, selectedId, isWeight, history)
    }

    private val userStatsFlow = combine(
        cycleSettingsRepository.settings.flatMapLatest { settings ->
            progressRepository.observeStreakInfo(settings.cycleStartDate, today)
        },
        progressRepository.observeBodyweightHistory(today.minusDays(90), today),
        progressRepository.observeSupplementLogs(today.minusDays(90), today),
        progressRepository.observeSupplyInventory(),
        cycleSettingsRepository.settings
    ) { streakInfo, bwHistory, suppLogs, supplies, settings ->
        UserStats(streakInfo, bwHistory, suppLogs, supplies, settings)
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        exerciseDataFlow,
        userStatsFlow
    ) { exData, stats ->
        val selectedId = exData.selectedExerciseId ?: exData.exercises.firstOrNull()?.id
        val whey = stats.supplies.find { it.type == SupplyType.WHEY }
        val creatine = stats.supplies.find { it.type == SupplyType.CREATINE }
        val latestBw = stats.bwHistory.lastOrNull()?.weightKg
        val proteinEstimate = if (latestBw != null) {
            val weekLogs = stats.suppLogs.filter { it.date.isAfter(today.minusDays(7)) && !it.date.isAfter(today) }
            statsCalculator.calculateProteinEstimate(latestBw, stats.settings.proteinGoalMultiplierLow, stats.settings.proteinGoalMultiplierHigh, weekLogs, stats.settings.wheyServingGrams)
        } else null
        
        ProgressUiState(
            exercises = exData.exercises,
            selectedExerciseId = selectedId,
            exerciseHistory = exData.exerciseHistory,
            isWeightView = exData.isWeightView,
            currentStreak = stats.streakInfo.currentStreak,
            bestStreak = stats.streakInfo.bestStreak,
            bodyweightHistory = stats.bwHistory.map { it.date to it.weightKg },
            creatineAdherence30d = statsCalculator.calculateCreatineAdherence(stats.suppLogs, today, 30),
            creatineAdherence90d = statsCalculator.calculateCreatineAdherence(stats.suppLogs, today, 90),
            proteinEstimate = proteinEstimate,
            wheyStatus = whey?.let { statsCalculator.calculateSupplyStatus(it, today, 7f) },
            creatineStatus = creatine?.let { statsCalculator.calculateSupplyStatus(it, today, 5f) },
            wheyInventory = whey,
            creatineInventory = creatine
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProgressUiState())

    fun selectExercise(id: Long) {
        _selectedExerciseId.value = id
    }

    fun setWeightView(isWeight: Boolean) {
        _isWeightView.value = isWeight
    }

    fun logBodyweight(weightKg: Float) {
        viewModelScope.launch { progressRepository.logBodyweight(today, weightKg) }
    }

    fun restockSupply(type: SupplyType, totalServings: Int) {
        viewModelScope.launch { progressRepository.restockSupply(type, totalServings, today) }
    }

    companion object {
        fun factory(
            progressRepository: ProgressRepository,
            cycleSettingsRepository: CycleSettingsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel(progressRepository, cycleSettingsRepository) as T
            }
        }
    }
}
