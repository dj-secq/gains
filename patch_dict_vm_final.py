import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryViewModel.kt", "r") as f:
    text = f.read()

add_old = """    fun addExercise(name: String, tracksWeight: Boolean, notes: String?) {
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
    }"""
add_new = """    fun addExercise(name: String, tracksWeight: Boolean, notes: String?, muscleGroup: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertExercise(
                    ExerciseEntity(name = name.trim(), tracksWeight = tracksWeight, notes = notes?.takeIf { it.isNotBlank() }, muscleGroup = muscleGroup, isCustom = true)
                )
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

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseLibraryScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
"""
if "import androidx.compose.material.icons.outlined.ArrowBackIosNew" not in text:
    text = text.replace("import androidx.compose.material.icons.Icons", imports)

text = text.replace("Icon(androidx.compose.material.icons.outlined.ArrowBackIosNew", "Icon(Icons.Outlined.ArrowBackIosNew")
text = text.replace("Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBackIos", "Icon(Icons.Outlined.ArrowBackIosNew")

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseLibraryScreen.kt", "w") as f:
    f.write(text)
