import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

# Replace SettingsRoute
target = """@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    if (settings != null) {
        SettingsScreen(
            settings = settings!!,"""

replacement = """import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onExportData: (Uri) -> Unit,
    onImportData: (Uri) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onExportData(uri)
    }
    
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportData(uri)
    }

    if (settings != null) {
        SettingsScreen(
            settings = settings!!,
            onExportClick = { exportLauncher.launch("repsgrams-backup-${LocalDate.now()}.zip") },
            onImportClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },"""

text = text.replace(target, replacement)

# Add onExportClick and onImportClick to SettingsScreen arguments
target_args = """    onTrackedMeasurements: (Set<String>) -> Unit,
    onHealthConnectEnabled: (Boolean) -> Unit,
    onVoiceCuesEnabled: (Boolean) -> Unit,"""
replacement_args = """    onTrackedMeasurements: (Set<String>) -> Unit,
    onHealthConnectEnabled: (Boolean) -> Unit,
    onVoiceCuesEnabled: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,"""
text = text.replace(target_args, replacement_args)

# Map clickable to onExportClick and onImportClick
text = text.replace("clickable { /* TODO: Export */ }", "clickable { onExportClick() }")
text = text.replace("clickable { /* TODO: Import */ }", "clickable { onImportClick() }")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

