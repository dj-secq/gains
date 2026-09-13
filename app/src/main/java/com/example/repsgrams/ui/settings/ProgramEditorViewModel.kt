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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

import com.example.repsgrams.data.datastore.CycleSettingsRepository

class ProgramEditorViewModel(
    private val repository: WorkoutRepository,
    private val cycleSettingsRepository: CycleSettingsRepository
) : ViewModel() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()
    val defaultRestSeconds: StateFlow<Int> = cycleSettingsRepository.settings.map { it.defaultRestSeconds }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 90
    )
    val templates: StateFlow<List<WorkoutTemplateEntity>> = repository.observeAllTemplates().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    val allExercises: StateFlow<List<ExerciseEntity>> = repository.observeAllExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun observeTemplate(templateId: Long): kotlinx.coroutines.flow.Flow<WorkoutTemplateEntity?> =
        templates.map { it.find { t -> t.id == templateId } }

    fun observeBlocks(templateId: Long): StateFlow<List<TemplateBlockEntity>> =
        repository.observeBlocksForTemplate(templateId).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )

    fun observeBlockExercises(blockId: Long): StateFlow<List<TemplateBlockExerciseEntity>> =
        repository.observeExercisesForBlock(blockId).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
        )

    fun addTemplate(name: String, dayLabel: String, category: String, restDaysAfter: Int) {
        viewModelScope.launch {
            if (name.isBlank() || dayLabel.isBlank()) {
                _messages.emit("Name and day label are required")
                return@launch
            }
            repository.insertTemplate(
                WorkoutTemplateEntity(
                    name = name.trim(),
                    dayLabel = dayLabel.trim(),
                    maxDurationMinutes = 60,
                    category = category,
                    restDaysAfter = restDaysAfter.coerceIn(0, 7),
                )
            )
        }
    }

    fun updateTemplate(template: WorkoutTemplateEntity) {
        viewModelScope.launch { repository.updateTemplate(template) }
    }

    fun deleteTemplate(template: WorkoutTemplateEntity) {
        viewModelScope.launch {
            if (repository.hasTemplateHistory(template.id)) {
                _messages.emit("This template has workout history and cannot be deleted. Rename or edit it instead.")
                return@launch
            }
            repository.deleteTemplate(template)
        }
    }

    fun swapTemplates(id1: Long, id2: Long) {
        viewModelScope.launch { repository.swapTemplates(id1, id2) }
    }

    fun updateTemplateCategory(templateId: Long, category: String) {
        viewModelScope.launch { repository.updateTemplateCategory(templateId, category) }
    }

    fun updateTemplateRestDays(templateId: Long, restDays: Int) {
        viewModelScope.launch { repository.updateTemplateRestDays(templateId, restDays) }
    }

    fun addBlock(templateId: Long, label: String, kind: com.example.repsgrams.data.db.BlockKind, targetRoundsMin: Int, targetRoundsMax: Int, restSecs: Int, restAfter: Int, isOptional: Boolean) {
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
                    restSecondsAfterBlock = restAfter,
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

    fun addExerciseToBlock(
        blockId: Long,
        exerciseId: Long,
        low: Int,
        high: Int,
        repType: RepType,
        perSide: Boolean,
    ) {
        viewModelScope.launch {
            repository.insertBlockExercise(
                TemplateBlockExerciseEntity(
                    blockId = blockId,
                    exerciseId = exerciseId,
                    orderIndex = 0,
                    targetValueLow = low,
                    targetValueHigh = high,
                    repType = repType,
                    perSide = perSide,
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
        fun factory(repository: WorkoutRepository, cycleSettingsRepository: CycleSettingsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgramEditorViewModel(repository, cycleSettingsRepository) as T
            }
        }
    }
}
