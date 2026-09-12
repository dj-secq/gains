with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text = f.read()

text = text.replace("import com.example.repsgrams.data.repository.ExerciseSetHistoryRow", "import com.example.repsgrams.data.db.ExerciseSetHistoryRow")
text = text.replace("progressRepository.observeBodyweight(today.minusMonths(3), today)", "progressRepository.observeBodyweightHistory(today.minusMonths(3), today)")

import re
text = re.sub(r'val history = chosenId\?\.let \{ progressRepository\.getExerciseHistory\(it\) \} \?: emptyList\(\)\n\s*ExerciseData\(exercises, chosenId, history, isWeight\)', 
              'ExerciseData(exercises, chosenId, emptyList(), isWeight)', text)

text = re.sub(r'private val exerciseDataFlow = combine\([\s\S]*?ExerciseData\(exercises, chosenId, emptyList\(\), isWeight\)\n    \}',
"""private val exerciseDataFlow = combine(
        progressRepository.observeAllExercises(),
        _selectedExerciseId,
        _isWeightView
    ) { exercises, selectedId, isWeight ->
        val chosenId = selectedId ?: exercises.firstOrNull()?.id
        Triple(exercises, chosenId, isWeight)
    }.flatMapLatest { (exercises, chosenId, isWeight) ->
        val historyFlow = chosenId?.let { progressRepository.observeExerciseHistory(it) } ?: kotlinx.coroutines.flow.flowOf(emptyList())
        historyFlow.map { history ->
            ExerciseData(exercises, chosenId, history, isWeight)
        }
    }""", text)
text = text.replace("import kotlinx.coroutines.flow.combine", "import kotlinx.coroutines.flow.combine\nimport kotlinx.coroutines.flow.map")
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text2 = f.read()

text2 = re.sub(r'item \{ AdherenceCard\([\s\S]*?\}', '', text2)
text2 = re.sub(r'item \{ InventoryCard\([\s\S]*?\}', '', text2)
text2 = re.sub(r'@Composable\nprivate fun AdherenceCard\([\s\S]*?@Composable\nprivate fun InventoryCard', '@Composable\nprivate fun InventoryCard', text2)
text2 = re.sub(r'@Composable\nprivate fun InventoryCard\([\s\S]*?@Composable\nprivate fun ProgressMetric', '@Composable\nprivate fun ProgressMetric', text2)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text2)
