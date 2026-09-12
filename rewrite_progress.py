with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write("""package com.example.repsgrams.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.CycleSettings
import com.example.repsgrams.data.db.BodyweightLogEntity
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.repository.ExerciseSetHistoryRow
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.domain.streak.StreakInfo
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class ProgressUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val selectedExerciseId: Long? = null,
    val exerciseHistory: List<ExerciseSetHistoryRow> = emptyList(),
    val isWeightView: Boolean = true,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val bodyweightHistory: List<Pair<LocalDate, Float>> = emptyList()
)

private data class ExerciseData(
    val exercises: List<ExerciseEntity>,
    val selectedExerciseId: Long?,
    val exerciseHistory: List<ExerciseSetHistoryRow>,
    val isWeightView: Boolean
)

private data class UserStats(
    val bwHistory: List<BodyweightLogEntity>,
    val streakInfo: StreakInfo
)

class ProgressViewModel(
    private val progressRepository: ProgressRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val _isWeightView = MutableStateFlow(true)
    private val _selectedExerciseId = MutableStateFlow<Long?>(null)

    fun setWeightView(isWeightView: Boolean) {
        _isWeightView.value = isWeightView
    }

    fun selectExercise(id: Long) {
        _selectedExerciseId.value = id
    }

    private val exerciseDataFlow = combine(
        progressRepository.observeAllExercises(),
        _selectedExerciseId,
        _isWeightView
    ) { exercises, selectedId, isWeight ->
        val chosenId = selectedId ?: exercises.firstOrNull()?.id
        val history = chosenId?.let { progressRepository.getExerciseHistory(it) } ?: emptyList()
        ExerciseData(exercises, chosenId, history, isWeight)
    }

    private val userStatsFlow = combine(
        progressRepository.observeBodyweight(today.minusMonths(3), today),
        cycleSettingsRepository.settings.flatMapLatest { settings ->
            progressRepository.observeStreakInfo(today, settings.adherenceGraceDays)
        }
    ) { bw, streak ->
        UserStats(bw, streak)
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
            bodyweightHistory = stats.bwHistory.map { it.date to it.weightKg }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState()
    )

    companion object {
        fun provideFactory(
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
""")
