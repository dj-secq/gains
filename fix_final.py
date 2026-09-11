import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text = f.read()

text = text.replace("UserStats(streakInfo, bwHistory, suppLogs, supplies, settings)", "UserStats(streakInfo, bwHistory, suppLogs, supplies, settings, emptyMap())")

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

text = text.replace("onTrackedMeasurements = viewModel::setTrackedMeasurements", "onTrackedMeasurements = { viewModel.setTrackedMeasurements(it) }")
with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    text = f.read()

if "override val backupManager" not in text:
    text = text.replace("override val reminderScheduler:", "override val backupManager: com.example.repsgrams.data.repository.BackupManager by lazy { com.example.repsgrams.data.repository.BackupManager(context) }\n    override val reminderScheduler:")

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    text = f.read()
if "val backupManager:" not in text:
    text = text.replace("val reminderScheduler:", "val backupManager: com.example.repsgrams.data.repository.BackupManager\n    val reminderScheduler:")
with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.write(text)
