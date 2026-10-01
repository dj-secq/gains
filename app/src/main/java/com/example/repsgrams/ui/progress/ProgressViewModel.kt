package com.example.repsgrams.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.HealthConnectManager
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.BodyMeasurementLogEntity
import com.example.repsgrams.data.db.BodyweightLogEntity
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.ExerciseSetHistoryRow
import com.example.repsgrams.data.db.PersonalRecordEntity
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.domain.progress.ProgressMetric
import com.example.repsgrams.domain.progress.bodyweightToDisplay
import com.example.repsgrams.domain.progress.bodyweightToKilograms
import com.example.repsgrams.domain.progress.measurementToCentimeters
import com.example.repsgrams.domain.progress.orderedMeasurements
import com.example.repsgrams.domain.streak.StreakInfo
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val selectedExerciseId: Long? = null,
    val exerciseHistory: List<ExerciseSetHistoryRow> = emptyList(),
    val metric: ProgressMetric = ProgressMetric.BEST,
    val bestStreak: Int = 0,
    val unitSystem: UnitSystem = UnitSystem.KG,
    val bodyweightHistory: List<Pair<LocalDate, Float>> = emptyList(),
    val todayBodyweight: Float? = null,
    val supplyInventory: List<com.example.repsgrams.data.db.SupplyInventoryEntity> = emptyList(),
    val supplements: List<com.example.repsgrams.data.db.SupplementEntity> = emptyList(),
    val selectedSupplementId: Long? = null,
    val supplementAdherence: List<Pair<LocalDate, Boolean>> = emptyList(),
    val records: List<PersonalRecordEntity> = emptyList(),
    val trackedMeasurements: List<String> = emptyList(),
    val measurementLogs: Map<String, List<BodyMeasurementLogEntity>> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
)

private data class ExerciseData(
    val exercises: List<ExerciseEntity>,
    val selectedExerciseId: Long?,
    val exerciseHistory: List<ExerciseSetHistoryRow>,
)

private data class UserStats(
    val bwHistory: List<BodyweightLogEntity>,
    val streakInfo: StreakInfo,
    val unitSystem: UnitSystem,
    val supply: List<com.example.repsgrams.data.db.SupplyInventoryEntity>,
    val supplements: List<com.example.repsgrams.data.db.SupplementEntity>,
    val selectedSupplementId: Long?,
    val supplementAdherence: List<Pair<LocalDate, Boolean>>,
    val records: List<PersonalRecordEntity>,
    val trackedMeasurements: List<String>,
    val measurementLogs: Map<String, List<BodyMeasurementLogEntity>>,
)

private data class SupplementStats(
    val supply: List<com.example.repsgrams.data.db.SupplyInventoryEntity>,
    val supplements: List<com.example.repsgrams.data.db.SupplementEntity>,
    val selectedId: Long?,
    val adherence: List<Pair<LocalDate, Boolean>>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val supplementRepository: SupplementRepository,
    private val healthConnectManager: HealthConnectManager,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)
    private val _selectedSupplementId = MutableStateFlow<Long?>(null)
    private val _metric = MutableStateFlow(ProgressMetric.BEST)

    fun selectExercise(id: Long) {
        _selectedExerciseId.value = id
    }

    fun selectMetric(metric: ProgressMetric) {
        _metric.value = metric
    }

    fun selectSupplement(id: Long) { _selectedSupplementId.value = id }

    fun logBodyweight(weight: Float) {
        val unitSystem = uiState.value.unitSystem
        viewModelScope.launch {
            val kilograms = bodyweightToKilograms(weight, unitSystem)
            progressRepository.logBodyweight(today, kilograms)
            if (!cycleSettingsRepository.settings.first().healthConnectEnabled) return@launch
            healthConnectManager.writeBodyweight(Instant.now(), kilograms)
        }
    }

    fun logMeasurement(type: String, displayValue: Float) {
        val unitSystem = uiState.value.unitSystem
        viewModelScope.launch {
            progressRepository.logBodyMeasurement(type, today, measurementToCentimeters(displayValue, unitSystem))
        }
    }

    fun deleteRecord(id: Long) {
        viewModelScope.launch { progressRepository.deletePersonalRecord(id) }
    }

    fun restockSupply(supplementId: Long, newTotalServings: Int) {
        viewModelScope.launch { supplementRepository.restock(supplementId, newTotalServings) }
    }

    private val exerciseDataFlow = combine(
        progressRepository.observeAllExercises(),
        _selectedExerciseId,
    ) { exercises, selectedId ->
        exercises to (selectedId ?: exercises.firstOrNull()?.id)
    }.flatMapLatest { (exercises, chosenId) ->
        val historyFlow = chosenId?.let { progressRepository.observeExerciseHistory(it) } ?: flowOf(emptyList())
        historyFlow.map { history -> ExerciseData(exercises, chosenId, history) }
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

    private val libraryFlow = combine(
        progressRepository.observeAllPersonalRecords(),
        cycleSettingsRepository.settings,
        progressRepository.observeAllBodyMeasurements(),
    ) { records, settings, measurements ->
        val types = orderedMeasurements(settings.trackedMeasurements)
        Triple(records, types, measurements.filter { it.type in settings.trackedMeasurements }.groupBy { it.type })
    }

    private val userStatsFlow = combine(
        progressRepository.observeBodyweightHistory(today.minusMonths(3), today),
        cycleSettingsRepository.settings.flatMapLatest { settings ->
            progressRepository.observeStreakInfo(today, settings.adherenceGraceDays).map { streak ->
                streak to settings.unitSystem
            }
        },
        supplementStatsFlow,
        libraryFlow,
    ) { bw, streakAndUnit, supplements, library ->
        UserStats(
            bw,
            streakAndUnit.first,
            streakAndUnit.second,
            supplements.supply,
            supplements.supplements,
            supplements.selectedId,
            supplements.adherence,
            library.first,
            library.second,
            library.third,
        )
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        exerciseDataFlow,
        userStatsFlow,
        _metric,
    ) { exData, stats, metric ->
        val selectedId = exData.selectedExerciseId ?: exData.exercises.firstOrNull()?.id
        ProgressUiState(
            exercises = exData.exercises,
            selectedExerciseId = selectedId,
            exerciseHistory = exData.exerciseHistory,
            metric = metric,
            bestStreak = stats.streakInfo.bestStreak,
            unitSystem = stats.unitSystem,
            bodyweightHistory = stats.bwHistory.map { it.date to bodyweightToDisplay(it.weightKg, stats.unitSystem) },
            todayBodyweight = stats.bwHistory.lastOrNull { it.date == today }?.weightKg?.let {
                bodyweightToDisplay(it, stats.unitSystem)
            },
            supplyInventory = stats.supply,
            supplements = stats.supplements,
            selectedSupplementId = stats.selectedSupplementId,
            supplementAdherence = stats.supplementAdherence,
            records = stats.records,
            trackedMeasurements = stats.trackedMeasurements,
            measurementLogs = stats.measurementLogs,
            today = today,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProgressUiState(),
    )

    companion object {
        fun provideFactory(
            progressRepository: ProgressRepository,
            cycleSettingsRepository: CycleSettingsRepository,
            supplementRepository: SupplementRepository,
            healthConnectManager: HealthConnectManager,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel(
                    progressRepository,
                    cycleSettingsRepository,
                    supplementRepository,
                    healthConnectManager,
                ) as T
            }
        }
    }
}
