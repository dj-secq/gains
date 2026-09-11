import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

sig_old = """@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
) {"""
sig_new = """@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onExport: (Uri) -> Unit = {},
    onImport: (Uri) -> Unit = {}
) {"""
text = text.replace(sig_old, sig_new)

call_old = """            onAutoAdvance = viewModel::setAutoAdvanceEnabled,
            onTrackedMeasurements = viewModel::setTrackedMeasurements,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises
        )
    }
}"""
call_new = """            onAutoAdvance = viewModel::setAutoAdvanceEnabled,
            onTrackedMeasurements = viewModel::setTrackedMeasurements,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises,
            onExport = onExport,
            onImport = onImport
        )
    }
}"""
text = text.replace(call_old, call_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

