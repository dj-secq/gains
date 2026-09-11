import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text = f.read()

state_old = """    val exerciseHistory: List<ExerciseSetHistoryRow> = emptyList(),
    
    val currentStreak: Int = 0,"""
state_new = """    val exerciseHistory: List<ExerciseSetHistoryRow> = emptyList(),
    val estimated1RM: Float? = null,
    
    val currentStreak: Int = 0,"""
text = text.replace(state_old, state_new)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text)
