import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

sig_old = """    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onWheyChanged: (Boolean) -> Unit,
    onCreatineChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit
) {"""
sig_new = """    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onWheyChanged: (Boolean) -> Unit,
    onCreatineChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
    onNotesChanged: (String) -> Unit = {},
    onRpeTagChanged: (String?) -> Unit = {},
    onBack: () -> Unit
) {"""
text = text.replace(sig_old, sig_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
