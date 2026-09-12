import re
with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

text = re.sub(r'onCreatineChanged: \(Boolean\) -> Unit,\s*onWheyChanged: \(Boolean\) -> Unit,', '', text)
text = text.replace("onCreatineChanged = onCreatineChanged,\n        onWheyChanged = onWheyChanged,\n", "")
text = text.replace("onCreatineChanged, onWheyChanged, ", "")
text = re.sub(r'fun SupplementCard\([\s\S]*?\{', 'fun SupplementCard(state: TodayUiState.Content, viewModel: com.example.repsgrams.ui.today.TodayViewModel) {', text)
text = text.replace("SupplementCard(state)", "SupplementCard(state, viewModel)")
with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text2 = f.read()

text2 = text2.replace("onWheyChanged = viewModel::setWheyTaken,", "")
text2 = text2.replace("onCreatineChanged = viewModel::setCreatineTaken,", "")
text2 = re.sub(r'onWheyChanged: \(Boolean\) -> Unit,\n\s*onCreatineChanged: \(Boolean\) -> Unit,', '', text2)
with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text2)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text3 = f.read()

text3 = re.sub(r'AdherenceCard\(state\.creatineAdherence30d, state\.creatineAdherence90d\)', 'AdherenceCard(state.supplementAdherence30d, state.supplementAdherence90d)', text3)
text3 = re.sub(r'SupplyCard\(\n\s*state\.wheyStatus,\n\s*state\.wheyInventory,\n\s*SupplyType.WHEY\n\s*\)', '', text3)
text3 = re.sub(r'SupplyCard\(\n\s*state\.creatineStatus,\n\s*state\.creatineInventory,\n\s*SupplyType.CREATINE\n\s*\)', '', text3)
text3 = re.sub(r'if \(state\.proteinEstimate != null\) \{[\s\S]*?\}\n', '', text3)
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text3)
