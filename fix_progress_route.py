import re
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

text = re.sub(r'fun ProgressRoute\([\s\S]*?\}\n\n',
"""fun ProgressRoute(viewModel: ProgressViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        state = state,
        onExerciseSelected = viewModel::selectExercise,
        onWeightViewToggled = viewModel::setWeightView,
        onBodyweightLogged = { },
        onRestock = { _, _ -> }
    )
}

""", text)
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text2 = f.read()

text2 = text2.replace("fun logBodyweight(weightKg: Float) {\n        viewModelScope.launch {\n            progressRepository.logBodyweight(today, weightKg)\n        }\n    }", "")

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text2)

