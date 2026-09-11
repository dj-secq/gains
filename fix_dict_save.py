import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "r") as f:
    text = f.read()

save_old = """            onSave = { name, tracks, notes ->
                if (editingExercise == null) {
                    viewModel.addExercise(name, tracks, notes)
                } else {
                    viewModel.updateExercise(editingExercise!!, name, tracks, notes)
                }
                showDialog = false
            },"""
save_new = """            onSave = { name, tracks, notes, muscleGroup ->
                if (editingExercise == null) {
                    viewModel.addExercise(name, tracks, notes, muscleGroup)
                } else {
                    viewModel.updateExercise(editingExercise!!, name, tracks, notes, muscleGroup)
                }
                showDialog = false
            },"""
text = text.replace(save_old, save_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "w") as f:
    f.write(text)
