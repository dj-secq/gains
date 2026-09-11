import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

# Fix SettingsScreen signature
sig_old = """    onProteinGoal: (Float, Float) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
) {"""
sig_new = """    onProteinGoal: (Float, Float) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onTrackedMeasurements: (Set<String>) -> Unit = {},
    onExport: (Uri) -> Unit = {},
    onImport: (Uri) -> Unit = {}
) {"""
text = text.replace(sig_old, sig_new)

# Fix SettingsRoute call
call_old = """            onProteinGoal = viewModel::setProteinGoalMultiplier,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises,
        )"""
call_new = """            onProteinGoal = viewModel::setProteinGoalMultiplier,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises,
            onTrackedMeasurements = { viewModel.setTrackedMeasurements(it) },
            onExport = onExport,
            onImport = onImport
        )"""
text = text.replace(call_old, call_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

