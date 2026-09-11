import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

sig_old = """    onRestSound: (String) -> Unit,
    onVibration: (Boolean) -> Unit,
    onAutoAdvance: (Boolean) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit
) {"""
sig_new = """    onRestSound: (String) -> Unit,
    onVibration: (Boolean) -> Unit,
    onAutoAdvance: (Boolean) -> Unit,
    onTrackedMeasurements: (Set<String>) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit
) {"""
text = text.replace(sig_old, sig_new)

call_old = """        onRestSound = viewModel::setRestSound,
        onVibration = viewModel::setVibrationEnabled,
        onAutoAdvance = viewModel::setAutoAdvanceEnabled,
        onNavigateToTemplates = onNavigateToTemplates,
        onNavigateToExercises = onNavigateToExercises
    )
}"""
call_new = """        onRestSound = viewModel::setRestSound,
        onVibration = viewModel::setVibrationEnabled,
        onAutoAdvance = viewModel::setAutoAdvanceEnabled,
        onTrackedMeasurements = viewModel::setTrackedMeasurements,
        onNavigateToTemplates = onNavigateToTemplates,
        onNavigateToExercises = onNavigateToExercises
    )
}"""
text = text.replace(call_old, call_new)

imports = """import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

meas_section = """                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column {
                                Text("Tracked Measurements", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("Waist", "Chest", "Arms", "Thighs", "Calves").forEach { m ->
                                        val selected = settings.trackedMeasurements.contains(m)
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                val newSet = if (selected) settings.trackedMeasurements - m else settings.trackedMeasurements + m
                                                onTrackedMeasurements(newSet)
                                            },
                                            label = { Text(m) }
                                        )
                                    }
                                }
                            }"""

text = text.replace("                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)", meas_section + "\n                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)", 1)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

