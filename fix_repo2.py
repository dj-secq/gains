import re
with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "r") as f:
    text = f.read()

# Fix combine lambda
text = re.sub(r'database\.workoutSessionDao\(\)\.observeAll\(\),\n\s*database\.supplementIntakeLogDao\(\)\.observeAll\(\),\n\s*database\.workoutTemplateDao\(\)\.observeAll\(\)\n\s*\) \{ sessions, supplements, templates ->\n\s*streakCalculator\.calculate\(today, sessions, supplements, templates, graceDays\)',
"""database.workoutSessionDao().observeAll(),
            database.workoutTemplateDao().observeAll()
        ) { sessions, templates ->
            com.example.repsgrams.domain.streak.StreakCalculator.calculate(today, sessions, templates, graceDays)""", text)

text = re.sub(r'database\.workoutSessionDao\(\)\.observeAll\(\),\n\s*database\.supplementIntakeLogDao\(\)\.observeAll\(\),\n\s*database\.workoutTemplateDao\(\)\.observeAll\(\)\n\s*\) \{ sessions, supplements, templates ->\n\s*com\.example\.repsgrams\.domain\.streak\.StreakCalculator\.calculate\(today, sessions, emptyList\(\), graceDays\)', 
"""database.workoutSessionDao().observeAll(),
            database.workoutTemplateDao().observeAll()
        ) { sessions, templates ->
            com.example.repsgrams.domain.streak.StreakCalculator.calculate(today, sessions, templates, graceDays)""", text)


# Also there are references to restockSupply in ProgressRepository
text = re.sub(r'override suspend fun logSupplyRestock[\s\S]*', '}', text)

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "r") as f:
    text2 = f.read()
text2 = text2.replace("ProgressViewModel.provideFactory(container.progressRepository, container.cycleSettingsRepository", "ProgressViewModel.provideFactory(container.progressRepository, container.cycleSettingsRepository)")
text2 = text2.replace("ProgressViewModel.provideFactory(container.progressRepository, container.cycleSettingsRepository, container.supplementRepository, container.scheduleRepository)", "ProgressViewModel.provideFactory(container.progressRepository, container.cycleSettingsRepository)")

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "w") as f:
    f.write(text2)
