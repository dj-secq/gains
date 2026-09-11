import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryViewModel.kt", "r") as f:
    text = f.read()

add_old = """    fun addExercise(name: String, tracksWeight: Boolean, notes: String?) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createCustomExercise(name.trim(), tracksWeight, notes?.takeIf { it.isNotBlank() })
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
    }"""
add_new = """    fun addExercise(name: String, tracksWeight: Boolean, notes: String?, muscleGroup: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.createCustomExercise(name.trim(), tracksWeight, notes?.takeIf { it.isNotBlank() }, muscleGroup)
            }
        }
    }

    fun updateExercise(exercise: ExerciseEntity, newName: String, tracksWeight: Boolean, notes: String?, muscleGroup: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                repository.updateExercise(
                    exercise.copy(name = newName.trim(), tracksWeight = tracksWeight, notes = notes?.takeIf { it.isNotBlank() }, muscleGroup = muscleGroup)
                )
            }
        }
    }"""
text = text.replace(add_old, add_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryViewModel.kt", "w") as f:
    f.write(text)
