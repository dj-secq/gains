import re

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "r") as f:
    text = f.read()

text = text.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.runtime.rememberCoroutineScope\nimport androidx.compose.ui.platform.LocalContext\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.Dispatchers")

old_settings = """                com.example.repsgrams.ui.settings.SettingsRoute(
                    viewModel = settingsViewModel,
                    onNavigateToTemplates = { navController.navigate("templates") },
                    onNavigateToExercises = { navController.navigate("exercises") }
                )"""
new_settings = """                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                com.example.repsgrams.ui.settings.SettingsRoute(
                    viewModel = settingsViewModel,
                    onNavigateToTemplates = { navController.navigate("templates") },
                    onNavigateToExercises = { navController.navigate("exercises") },
                    onExport = { uri ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                context.contentResolver.openOutputStream(uri)?.use { 
                                    container.backupManager.exportToZip(it as java.io.FileOutputStream) 
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    onImport = { uri ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                context.contentResolver.openInputStream(uri)?.use {
                                    if (container.backupManager.importFromZip(it as java.io.FileInputStream)) {
                                        // Ideally restart app or reload data
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )"""
text = text.replace(old_settings, new_settings)

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "w") as f:
    f.write(text)
