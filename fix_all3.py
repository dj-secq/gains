import re

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    text = f.read()

if "val backupManager: com.example.repsgrams.data.repository.BackupManager" not in text:
    text = text.replace("    val reminderScheduler: ReminderScheduler", "    val reminderScheduler: ReminderScheduler\n    val backupManager: com.example.repsgrams.data.repository.BackupManager")
with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

if "onTrackedMeasurements: (Set<String>) -> Unit," not in text:
    text = text.replace("    onAutoAdvance: (Boolean) -> Unit,\n    onNavigateToTemplates:", "    onAutoAdvance: (Boolean) -> Unit,\n    onTrackedMeasurements: (Set<String>) -> Unit = {},\n    onNavigateToTemplates:")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

