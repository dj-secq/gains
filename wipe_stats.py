import re

with open("app/src/main/java/com/example/repsgrams/domain/progress/ProgressStatsCalculator.kt", "r") as f:
    text = f.read()

text = re.sub(r'fun calculateCreatineAdherence\([\s\S]*?fun', 'fun', text)
text = re.sub(r'fun calculateProteinEstimate\([\s\S]*?fun', 'fun', text)
text = re.sub(r'fun calculateSupplyStatus\([\s\S]*?\}', 'fun calculateSupplyStatus(inventory: com.example.repsgrams.data.db.SupplyInventoryEntity, today: java.time.LocalDate, lowThreshold: Float = 5f): SupplyStatus = SupplyStatus(false, null)', text)

with open("app/src/main/java/com/example/repsgrams/domain/progress/ProgressStatsCalculator.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/data/db/DatabaseInitializer.kt", "r") as f:
    text2 = f.read()

text2 = re.sub(r'val wheyId = [\s\S]*?\}', '}', text2)
with open("app/src/main/java/com/example/repsgrams/data/db/DatabaseInitializer.kt", "w") as f:
    f.write(text2)

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "r") as f:
    text3 = f.read()

text3 = text3.replace("val streak = StreakCalculator.calculate(today, sessions, templates, graceDays)", "val streak = com.example.repsgrams.domain.streak.StreakCalculator.calculate(today, sessions, templates, graceDays)")
text3 = re.sub(r'override suspend fun logSupplyRestock[\s\S]*?\}', '', text3)
with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "w") as f:
    f.write(text3)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text4 = f.read()
# Completely remove AdherenceCard and InventoryCard functions
text4 = re.sub(r'@Composable\nprivate fun AdherenceCard\([\s\S]*?@Composable\nprivate fun InventoryCard', '@Composable\nprivate fun InventoryCard', text4)
text4 = re.sub(r'@Composable\nprivate fun InventoryCard\([\s\S]*?@Composable\nprivate fun ProgressMetric', '@Composable\nprivate fun ProgressMetric', text4)

text4 = text4.replace("fun ProgressScreen(\n    state: ProgressUiState,\n    onExerciseSelected: (Long) -> Unit,\n    onToggleView: (Boolean) -> Unit,\n    logBodyweight: () -> Unit,\n    restockSupply: (String) -> Unit,\n    modifier: Modifier = Modifier\n)", "fun ProgressScreen(\n    state: ProgressUiState,\n    onExerciseSelected: (Long) -> Unit,\n    onToggleView: (Boolean) -> Unit,\n    modifier: Modifier = Modifier\n)")

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text4)

