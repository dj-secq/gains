import re
import os

# 1. HealthConnectManager.kt
hc = "app/src/main/java/com/example/repsgrams/data/HealthConnectManager.kt"
with open(hc, "r") as f:
    text = f.read()
text = text.replace("import androidx.health.connect.client.units.Mass", "")
text = text.replace("import androidx.health.connect.client.records.ExerciseSessionRecord.EXERCISE_TYPE_WORKOUT", "")
text = re.sub(r'WeightRecord\([\s\S]*?\)', 'WeightRecord(time = java.time.Instant.now(), zoneOffset = null, weight = androidx.health.connect.client.units.Mass.kilograms(0.0))', text)
text = text.replace("ExerciseSessionRecord.EXERCISE_TYPE_WORKOUT", "0")
with open(hc, "w") as f:
    f.write(text)

# 2. DatabaseInitializer.kt
db = "app/src/main/java/com/example/repsgrams/data/db/DatabaseInitializer.kt"
with open(db, "r") as f:
    text = f.read()
text = re.sub(r'database\.supplyInventoryDao\(\)\.upsert\([\s\S]*?\}', '}', text)
with open(db, "w") as f:
    f.write(text)

# 3. ProgressRepository.kt
pr = "app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt"
with open(pr, "r") as f:
    text = f.read()
text = re.sub(r'override suspend fun logSupplyRestock[\s\S]*?\}', '', text)
text = re.sub(r'com\.example\.repsgrams\.domain\.streak\.StreakCalculator\.calculate\(today, sessions, templates, graceDays\)', 'com.example.repsgrams.domain.streak.StreakCalculator.calculate(today, sessions, emptyList(), graceDays)', text)
text = re.sub(r'StreakCalculator\.calculate\(today, sessions, templates, graceDays\)', 'com.example.repsgrams.domain.streak.StreakCalculator.calculate(today, sessions, emptyList(), graceDays)', text)
with open(pr, "w") as f:
    f.write(text)

# 4. ProgressScreen.kt
ps = "app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt"
with open(ps, "r") as f:
    text = f.read()
text = re.sub(r'item \{ SupplementAdherenceSection\([\s\S]*?\}', '', text)
text = re.sub(r'item \{ SupplySection\([\s\S]*?\}', '', text)
text = re.sub(r'@Composable\nprivate fun SupplementAdherenceSection\([\s\S]*', '', text)
text = text.replace("logBodyweight: () -> Unit,\n    restockSupply: (String) -> Unit,", "")
text = text.replace("logBodyweight = logBodyweight,", "")
text = text.replace("restockSupply = restockSupply,", "")
with open(ps, "w") as f:
    f.write(text)

# 5. AppContainer.kt
ac = "app/src/main/java/com/example/repsgrams/AppContainer.kt"
with open(ac, "r") as f:
    text = f.read()
text = text.replace("reminderScheduler.schedulePostWorkoutWhey(session)", "")
with open(ac, "w") as f:
    f.write(text)

# 6. RepsGramsApp.kt
ra = "app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt"
with open(ra, "r") as f:
    text = f.read()
text = text.replace("viewModel = viewModel(factory = ProgressViewModel.factory(", "viewModel = viewModel(factory = ProgressViewModel.provideFactory(")
with open(ra, "w") as f:
    f.write(text)

