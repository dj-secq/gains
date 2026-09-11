package com.example.repsgrams.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExerciseDictionaryViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {
    val exercises: StateFlow<List<ExerciseEntity>> = repository.observeAllExercises().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )

    fun addExercise(name: String, tracksWeight: Boolean, notes: String?) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertExercise(
                    ExerciseEntity(name = name.trim(), tracksWeight = tracksWeight, notes = notes?.takeIf { it.isNotBlank() })
                )
            }
        }
    }

    fun updateExercise(exercise: ExerciseEntity, newName: String, tracksWeight: Boolean, notes: String?) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.updateExercise(
                    exercise.copy(name = newName.trim(), tracksWeight = tracksWeight, notes = notes?.takeIf { it.isNotBlank() })
                )
            }
        }
    }

    fun deleteExercise(exercise: ExerciseEntity, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteExercise(exercise)
            onResult(success)
        }
    }

    companion object {
        fun factory(repository: WorkoutRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExerciseDictionaryViewModel(repository) as T
            }
        }
    }
}
