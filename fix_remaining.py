import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.IconButton
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

text = text.replace("    private var progressionSuggestion: ProgressionSuggestion? = null", "    private var progressionSuggestion: ProgressionSuggestion? = null\n    private var rpeTagInput: String? = null\n    private var notesInput: String = \"\"\n    private var lastTimeRound: PriorRound? = null")

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)

