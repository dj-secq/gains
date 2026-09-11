package com.example.repsgrams.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.db.TemplateBlockEntity
import com.example.repsgrams.data.db.TemplateBlockExerciseEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProgramEditorViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {
    val templates: StateFlow<List<WorkoutTemplateEntity>> = repository.observeAllTemplates().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    
    val allExercises: StateFlow<List<ExerciseEntity>> = repository.observeAllExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun observeBlocks(templateId: Long): StateFlow<List<TemplateBlockEntity>> =
        repository.observeBlocksForTemplate(templateId).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )

    fun observeBlockExercises(blockId: Long): StateFlow<List<TemplateBlockExerciseEntity>> =
        repository.observeExercisesForBlock(blockId).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )

    fun addTemplate(name: String, dayLabel: String) {
        viewModelScope.launch {
            repository.insertTemplate(
                WorkoutTemplateEntity(name = name, dayLabel = dayLabel, maxDurationMinutes = 60)
            )
        }
    }
    
    fun deleteTemplate(template: WorkoutTemplateEntity) {
        viewModelScope.launch { repository.deleteTemplate(template) }
    }

    fun addBlock(templateId: Long, label: String, kind: com.example.repsgrams.data.db.BlockKind, targetRoundsMin: Int, targetRoundsMax: Int, restSecs: Int, isOptional: Boolean) {
        viewModelScope.launch {
            repository.insertBlock(
                TemplateBlockEntity(
                    templateId = templateId,
                    orderIndex = 0, // Repository will fix order
                    label = label,
                    kind = kind,
                    targetRoundsMin = targetRoundsMin,
                    targetRoundsMax = targetRoundsMax,
                    restSecondsBetweenRounds = restSecs,
                    isOptional = isOptional
                )
            )
        }
    }

    fun updateBlock(block: TemplateBlockEntity) {
        viewModelScope.launch { repository.updateBlock(block) }
    }

    fun deleteBlock(block: TemplateBlockEntity) {
        viewModelScope.launch { repository.deleteBlock(block) }
    }
    
    fun swapBlocks(b1Id: Long, b2Id: Long, templateId: Long) {
        viewModelScope.launch { repository.swapBlocks(b1Id, b2Id, templateId) }
    }

    fun addExerciseToBlock(blockId: Long, exerciseId: Long) {
        viewModelScope.launch {
            repository.insertBlockExercise(
                TemplateBlockExerciseEntity(
                    blockId = blockId,
                    exerciseId = exerciseId,
                    orderIndex = 0,
                    targetValueLow = 8,
                    targetValueHigh = 12,
                    repType = RepType.REPS,
                    perSide = false
                )
            )
        }
    }

    fun updateBlockExercise(ex: TemplateBlockExerciseEntity) {
        viewModelScope.launch { repository.updateBlockExercise(ex) }
    }

    fun deleteBlockExercise(ex: TemplateBlockExerciseEntity) {
        viewModelScope.launch { repository.deleteBlockExercise(ex) }
    }

    fun swapBlockExercises(e1Id: Long, e2Id: Long, blockId: Long) {
        viewModelScope.launch { repository.swapBlockExercises(e1Id, e2Id, blockId) }
    }

    companion object {
        fun factory(repository: WorkoutRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgramEditorViewModel(repository) as T
            }
        }
    }
}
