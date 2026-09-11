import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

sig_old = """    onWeightViewToggled: (Boolean) -> Unit,
    onBodyweightLogged: (Float) -> Unit,
    onRestock: (SupplyType, Int) -> Unit,"""
sig_new = """    onWeightViewToggled: (Boolean) -> Unit,
    onBodyweightLogged: (Float) -> Unit,
    onBodyMeasurementLogged: (String, Float) -> Unit,
    onRestock: (SupplyType, Int) -> Unit,"""
text = text.replace(sig_old, sig_new)

call_old = """        onWeightViewToggled = viewModel::setWeightView,
        onBodyweightLogged = viewModel::logBodyweight,
        onRestock = viewModel::restockSupply,"""
call_new = """        onWeightViewToggled = viewModel::setWeightView,
        onBodyweightLogged = viewModel::logBodyweight,
        onBodyMeasurementLogged = viewModel::logBodyMeasurement,
        onRestock = viewModel::restockSupply,"""
text = text.replace(call_old, call_new)

sect_old = """            item { ExerciseChartSection(state, onExerciseSelected, onWeightViewToggled) }
            item { BodyweightSection(state, onBodyweightLogged) }
            item { SupplementAdherenceSection(state) }"""
sect_new = """            item { ExerciseChartSection(state, onExerciseSelected, onWeightViewToggled) }
            item { BodyweightSection(state, onBodyweightLogged) }
            if (state.trackedMeasurements.isNotEmpty()) {
                item { BodyMeasurementsSection(state, onBodyMeasurementLogged) }
            }
            item { SupplementAdherenceSection(state) }"""
text = text.replace(sect_old, sect_new)

# add BodyMeasurementsSection
meas_comp = """
@Composable
private fun BodyMeasurementsSection(state: ProgressUiState, onLog: (String, Float) -> Unit) {
    var selectedType by remember { mutableStateOf(state.trackedMeasurements.first()) }
    var input by remember { mutableStateOf("") }

    IosCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Measurements", style = MaterialTheme.typography.titleMedium)
                // Dropdown to select type
                var expanded by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { expanded = true }) { Text(selectedType) }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        state.trackedMeasurements.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { selectedType = m; expanded = false })
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("Today's $selectedType (cm)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IosButton(text = "Log", onClick = { input.toFloatOrNull()?.let { onLog(selectedType, it); input = "" } }, modifier = Modifier.weight(0.5f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            val history = state.bodyMeasurements[selectedType] ?: emptyList()
            if (history.isNotEmpty()) {
                LineChart(history.map { it.second }, modifier = Modifier.fillMaxWidth().height(100.dp), lineColor = AppColors.creatineTeal)
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    LineChart(listOf(30f, 32f, 31f, 33f), modifier = Modifier.fillMaxSize().alpha(0.1f), lineColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No logs yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
"""

text = text.replace("private fun BodyweightSection(state: ProgressUiState, onLog: (Float) -> Unit) {", meas_comp + "\n@Composable\nprivate fun BodyweightSection(state: ProgressUiState, onLog: (Float) -> Unit) {")

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)

