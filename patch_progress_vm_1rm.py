import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text = f.read()

calc_old = """            exerciseHistory = data.exerciseHistory,
            
            currentStreak = stats.streakInfo.currentStreak,"""
calc_new = """            exerciseHistory = data.exerciseHistory,
            estimated1RM = data.exerciseHistory
                .mapNotNull { row ->
                    val w = row.weightKg
                    val r = row.reps
                    if (w != null && r != null) w * (1f + r / 30f) else null
                }
                .maxOrNull(),
            
            currentStreak = stats.streakInfo.currentStreak,"""
text = text.replace(calc_old, calc_new)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text)
