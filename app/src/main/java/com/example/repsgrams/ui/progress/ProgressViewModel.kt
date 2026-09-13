package com.example.repsgrams.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.CycleSettings
import com.example.repsgrams.data.db.BodyweightLogEntity
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.ExerciseSetHistoryRow
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.domain.streak.StreakInfo
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers

import com.example.repsgrams.data.repository.SupplementRepository

data class ProgressUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val selectedExerciseId: Long? = null,
    val exerciseHistory: List<ExerciseSetHistoryRow> = emptyList(),
    val isWeightView: Boolean = true,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val bodyweightHistory: List<Pair<LocalDate, Float>> = emptyList(),
    val supplyInventory: List<com.example.repsgrams.data.db.SupplyInventoryEntity> = emptyList(),
    val supplements: List<com.example.repsgrams.data.db.SupplementEntity> = emptyList(),
    val selectedSupplementId: Long? = null,
    val supplementAdherence: List<Pair<LocalDate, Boolean>> = emptyList(),
)

private data class ExerciseData(
    val exercises: List<ExerciseEntity>,
    val selectedExerciseId: Long?,
    val exerciseHistory: List<ExerciseSetHistoryRow>,
    val isWeightView: Boolean
)

private data class UserStats(
    val bwHistory: List<BodyweightLogEntity>,
    val streakInfo: StreakInfo,
    val supply: List<com.example.repsgrams.data.db.SupplyInventoryEntity>,
    val supplements: List<com.example.repsgrams.data.db.SupplementEntity>,
    val selectedSupplementId: Long?,
    val supplementAdherence: List<Pair<LocalDate, Boolean>>,
)

private data class SupplementStats(
    val supply: List<com.example.repsgrams.data.db.SupplyInventoryEntity>,
    val supplements: List<com.example.repsgrams.data.db.SupplementEntity>,
    val selectedId: Long?,
    val adherence: List<Pair<LocalDate, Boolean>>,
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val supplementRepository: SupplementRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val _isWeightView = MutableStateFlow(true)
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    private val _selectedSupplementId = MutableStateFlow<Long?>(null)

    fun setWeightView(isWeightView: Boolean) {
        _isWeightView.value = isWeightView
    }

    fun selectExercise(id: Long) {
        _selectedExerciseId.value = id
    }

    fun selectSupplement(id: Long) { _selectedSupplementId.value = id }

    fun logBodyweight(weightKg: Float) {
        viewModelScope.launch { progressRepository.logBodyweight(today, weightKg) }
    }

    fun restockSupply(supplementId: Long, newTotalServings: Int) {
        viewModelScope.launch { supplementRepository.restock(supplementId, newTotalServings) }
    }

    private val exerciseDataFlow = combine(
        progressRepository.observeAllExercises(),
        _selectedExerciseId,
        _isWeightView
    ) { exercises, selectedId, isWeight ->
        val chosenId = selectedId ?: exercises.firstOrNull()?.id
        Triple(exercises, chosenId, isWeight)
    }.flatMapLatest { (exercises, chosenId, isWeight) ->
        val historyFlow = chosenId?.let { progressRepository.observeExerciseHistory(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
        historyFlow.map { history ->
            ExerciseData(exercises, chosenId, history, isWeight)
        }
    }

    private val supplementStatsFlow = combine(
        progressRepository.observeSupplyInventory(),
        supplementRepository.observeAllSupplements(),
        supplementRepository.observeIntakesInRange(today.minusDays(29), today),
        _selectedSupplementId,
    ) { supply, supplements, logs, requestedId ->
        val active = supplements.filter { it.isActive }
        val selectedId = requestedId?.takeIf { id -> active.any { it.id == id } } ?: active.firstOrNull()?.id
        val byDate = logs.filter { it.supplementId == selectedId }.associateBy { it.date }
        SupplementStats(
            supply,
            supplements,
            selectedId,
            (29L downTo 0L).map { offset -> today.minusDays(offset) to (byDate[today.minusDays(offset)]?.taken == true) },
        )
    }

    private val userStatsFlow = combine(
        progressRepository.observeBodyweightHistory(today.minusMonths(3), today),
        cycleSettingsRepository.settings.flatMapLatest { settings ->
            progressRepository.observeStreakInfo(today, settings.adherenceGraceDays)
        },
        supplementStatsFlow,
    ) { bw, streak, supplements ->
        UserStats(bw, streak, supplements.supply, supplements.supplements, supplements.selectedId, supplements.adherence)
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        exerciseDataFlow,
        userStatsFlow
    ) { exData, stats ->
        val selectedId = exData.selectedExerciseId ?: exData.exercises.firstOrNull()?.id
        ProgressUiState(
            exercises = exData.exercises,
            selectedExerciseId = selectedId,
            exerciseHistory = exData.exerciseHistory,
            isWeightView = exData.isWeightView,
            currentStreak = stats.streakInfo.currentStreak,
            bestStreak = stats.streakInfo.bestStreak,
            bodyweightHistory = stats.bwHistory.map { it.date to it.weightKg },
            supplyInventory = stats.supply,
            supplements = stats.supplements,
            selectedSupplementId = stats.selectedSupplementId,
            supplementAdherence = stats.supplementAdherence,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProgressUiState()
    )

    companion object {
        fun provideFactory(
            progressRepository: ProgressRepository,
            cycleSettingsRepository: CycleSettingsRepository,
            supplementRepository: SupplementRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel(progressRepository, cycleSettingsRepository, supplementRepository) as T
            }
        }
    }
}
