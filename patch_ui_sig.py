import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

sig_old = """        onWeightChanged = viewModel::updateWeight,
        onAdjustValue = viewModel::adjustValue,
        onAdjustWeight = viewModel::adjustWeight,
        onLog = viewModel::logCurrent,
        onSkipBlock = viewModel::skipOptionalBlock,"""
sig_new = """        onWeightChanged = viewModel::updateWeight,
        onAdjustValue = viewModel::adjustValue,
        onAdjustWeight = viewModel::adjustWeight,
        onLog = viewModel::logCurrent,
        onSkipBlock = viewModel::skipOptionalBlock,
        onNotesChanged = viewModel::updateNotes,
        onRpeTagChanged = viewModel::updateRpeTag,"""
text = text.replace(sig_old, sig_new)

def_old = """    onAdjustValue: (Int) -> Unit,
    onLog: () -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
) {"""
def_new = """    onAdjustValue: (Int) -> Unit,
    onLog: () -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onNotesChanged: (String) -> Unit = {},
    onRpeTagChanged: (String?) -> Unit = {},
) {"""
text = text.replace(def_old, def_new)

call_old = """                    state, onValueChanged, onWeightChanged, onAdjustValue, onLog,
                    onAdjustWeight, onSkipBlock, onAddRest, onSkipRest, onFinish,
                )"""
call_new = """                    state, onValueChanged, onWeightChanged, onAdjustValue, onLog,
                    onAdjustWeight, onSkipBlock, onAddRest, onSkipRest, onFinish,
                    onNotesChanged, onRpeTagChanged,
                )"""
text = text.replace(call_old, call_new)

card_old = """    onAdjustValue: (Int) -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onLog: () -> Unit,
) {"""
card_new = """    onAdjustValue: (Int) -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onLog: () -> Unit,
    onRpeTagChanged: (String?) -> Unit,
) {"""
text = text.replace(card_old, card_new)

card_call_old = """        } else {
            ExerciseCard(state, onValueChanged, onWeightChanged, onAdjustValue, onAdjustWeight, onLog)
        }"""
card_call_new = """        } else {
            ExerciseCard(state, onValueChanged, onWeightChanged, onAdjustValue, onAdjustWeight, onLog, onRpeTagChanged)
            
            // In-session notes
            OutlinedTextField(
                value = state.notesInput,
                onValueChange = onNotesChanged,
                label = { Text("Session Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
        }"""
text = text.replace(card_call_old, card_call_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
