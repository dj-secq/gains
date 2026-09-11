import re

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    lines = f.readlines()

new_lines = []
imports_bm = 0
for line in lines:
    if "import com.example.repsgrams.data.repository.BackupManager" in line:
        imports_bm += 1
        if imports_bm > 1: continue
    new_lines.append(line)
with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.writelines(new_lines)


with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "r") as f:
    text = f.read()

# make sure it implements setTrackedMeasurements
if "override suspend fun setTrackedMeasurements" not in text:
    method = """
    override suspend fun setTrackedMeasurements(measurements: Set<String>) {
        dataStore.edit { it[PreferencesKeys.TRACKED_MEASUREMENTS_KEY] = measurements }
    }
"""
    text = text.replace("    override suspend fun setRestTimerAutoAdvance(enabled: Boolean)", method + "    override suspend fun setRestTimerAutoAdvance(enabled: Boolean)")

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "w") as f:
    f.write(text)


with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

text = text.replace("@Composable\n\n@Composable\nprivate fun BodyMeasurementsSection", "@Composable\nprivate fun BodyMeasurementsSection")
text = text.replace("@Composable\n@Composable\nprivate fun BodyMeasurementsSection", "@Composable\nprivate fun BodyMeasurementsSection")
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)


with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text = f.read()

text = text.replace("UserStats(streak, bw, supps, supplies, settings)", "UserStats(streak, bw, supps, supplies, settings, emptyMap())")
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text)

