import re

def insert_after(text, search, insert):
    if search not in text:
        print(f"Failed to find {search}")
    return text.replace(search, search + "\n" + insert)

# 1. AppContainer.kt
with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    text = f.read()

text = insert_after(text, "import com.example.repsgrams.data.repository.WorkoutRepository", "import com.example.repsgrams.data.repository.BackupManager")
text = insert_after(text, "    val reminderScheduler: ReminderScheduler", "    val backupManager: BackupManager")
text = insert_after(text, "    override val reminderScheduler: ReminderScheduler =", "    override val backupManager: BackupManager by lazy { BackupManager(appContext) }")
with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.write(text)

# 2. CycleSettingsRepository.kt
with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "r") as f:
    text = f.read()

text = insert_after(text, "    val restTimerAutoAdvance: Boolean,", "    val trackedMeasurements: Set<String> = emptySet(),")
text = insert_after(text, "    suspend fun setRestTimerAutoAdvance(enabled: Boolean)", "    suspend fun setTrackedMeasurements(measurements: Set<String>)")
text = insert_after(text, "        val REST_TIMER_AUTO_ADVANCE = booleanPreferencesKey(\"rest_timer_auto_advance\")", "        val TRACKED_MEASUREMENTS = androidx.datastore.preferences.core.stringSetPreferencesKey(\"tracked_measurements\")")
text = insert_after(text, "                restTimerAutoAdvance = prefs[PreferencesKeys.REST_TIMER_AUTO_ADVANCE] ?: false,", "                trackedMeasurements = prefs[PreferencesKeys.TRACKED_MEASUREMENTS] ?: emptySet(),")
text = insert_after(text, "    override suspend fun setRestTimerAutoAdvance(enabled: Boolean) = update(REST_TIMER_AUTO_ADVANCE, enabled)", "    override suspend fun setTrackedMeasurements(measurements: Set<String>) { context.cycleSettingsDataStore.edit { it[PreferencesKeys.TRACKED_MEASUREMENTS] = measurements } }")
with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "w") as f:
    f.write(text)

# 3. SettingsViewModel.kt
with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "r") as f:
    text = f.read()
text = insert_after(text, "    fun setRestTimerAutoAdvance(enabled: Boolean) {", "        viewModelScope.launch { repository.setRestTimerAutoAdvance(enabled) }\n    }\n    fun setTrackedMeasurements(measurements: Set<String>) {\n        viewModelScope.launch { repository.setTrackedMeasurements(measurements) }\n    }")
# Oops wait, the search string might be bad. Let's do it safer.
text = text.replace("    fun setRestTimerAutoAdvance(enabled: Boolean) {\n        viewModelScope.launch { repository.setRestTimerAutoAdvance(enabled) }\n    }", "    fun setRestTimerAutoAdvance(enabled: Boolean) {\n        viewModelScope.launch { repository.setRestTimerAutoAdvance(enabled) }\n    }\n\n    fun setTrackedMeasurements(measurements: Set<String>) {\n        viewModelScope.launch { repository.setTrackedMeasurements(measurements) }\n    }")
with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "w") as f:
    f.write(text)

# 4. SettingsScreen.kt
with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

text = insert_after(text, "import androidx.compose.ui.Modifier", "import android.content.Intent\nimport android.net.Uri\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\nimport androidx.compose.foundation.layout.ExperimentalLayoutApi\nimport androidx.compose.foundation.layout.FlowRow\nimport androidx.compose.material3.FilterChip")

text = text.replace("    onNavigateToExercises: () -> Unit,\n) {", "    onNavigateToExercises: () -> Unit,\n    onExport: (Uri) -> Unit = {},\n    onImport: (Uri) -> Unit = {}\n) {")
text = text.replace("            onNavigateToExercises = onNavigateToExercises,\n        )\n    }\n}", "            onNavigateToExercises = onNavigateToExercises,\n            onTrackedMeasurements = { viewModel.setTrackedMeasurements(it) },\n            onExport = onExport,\n            onImport = onImport\n        )\n    }\n}")
text = text.replace("    onNavigateToExercises: () -> Unit,\n) {\n    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()", "    onNavigateToExercises: () -> Unit,\n    onTrackedMeasurements: (Set<String>) -> Unit = {},\n    onExport: (Uri) -> Unit = {},\n    onImport: (Uri) -> Unit = {}\n) {\n    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()")


new_ui = """                            IosButton(text = "Exercise Dictionary", onClick = onNavigateToExercises, isSecondary = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DATA", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val context = LocalContext.current
                            val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                                uri?.let { onExport(it) }
                            }
                            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                                uri?.let { onImport(it) }
                            }
                            
                            IosButton(text = "Export Backup (.zip)", onClick = { exportLauncher.launch("repsgrams_backup.zip") }, isSecondary = true, modifier = Modifier.fillMaxWidth())
                            IosButton(text = "Import Backup (.zip)", onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }, isSecondary = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }"""

text = text.replace("""                            IosButton(text = "Exercise Dictionary", onClick = onNavigateToExercises, isSecondary = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }""", new_ui)

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

# Insert before "Unit System" section
text = text.replace("""                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column {
                                Text("Unit System",""", meas_section + """\n                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)\n                            Column {\n                                Text("Unit System",""")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

