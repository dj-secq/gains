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
if "import androidx.compose.foundation.layout.FlowRow" not in text:
    text = text.replace("import androidx.compose.foundation.layout.Row", imports + "import androidx.compose.foundation.layout.Row")

# Replace ExerciseCard content
card_content_old = """    val exercise = state.exercise
    IosCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    "Set ${state.roundNumber} of ${state.roundCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            
            Text(
                "Target ${exercise.targetValueLow}–${exercise.targetValueHigh} " + if (exercise.repType == RepType.REPS) "reps" else "seconds",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (exercise.perSide) Text("Per side", style = MaterialTheme.typography.labelLarge)"""
card_content_new = """    val exercise = state.exercise
    val haptic = LocalHapticFeedback.current
    IosCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { /* TODO Phase 11 exercise swap */ }) {
                        Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap")
                    }
                    Text(
                        "Set ${state.roundNumber} of ${state.roundCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "Target ${exercise.targetValueLow}–${exercise.targetValueHigh} " + if (exercise.repType == RepType.REPS) "reps" else "seconds",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (exercise.perSide) Text("Per side", style = MaterialTheme.typography.labelLarge)
                }
                state.lastTimeRound?.let { prior ->
                    val w = prior.weightKg?.let { " @ ${if (it % 1f == 0f) it.toInt() else it}${state.unitSystem.name.lowercase()}" } ?: ""
                    Text("Last: ${prior.reps ?: 0}$w", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }"""
text = text.replace(card_content_old, card_content_new)

stepper_old = """                FilledTonalIconButton(onClick = { onAdjustValue(-1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                OutlinedTextField(
                    value = state.valueInput,
                    onValueChange = onValueChanged,
                    label = { Text(if (exercise.repType == RepType.REPS) "Reps" else "Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                FilledTonalIconButton(onClick = { onAdjustValue(1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }"""
stepper_new = """                FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustValue(-1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                OutlinedTextField(
                    value = state.valueInput,
                    onValueChange = onValueChanged,
                    label = { Text(if (exercise.repType == RepType.REPS) "Reps" else "Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustValue(1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }"""
text = text.replace(stepper_old, stepper_new)

stepper_weight_old = """                    FilledTonalIconButton(onClick = { onAdjustWeight(-step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                    OutlinedTextField(
                        value = state.weightInput,
                        onValueChange = onWeightChanged,
                        label = { Text("Weight (${state.unitSystem.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FilledTonalIconButton(onClick = { onAdjustWeight(step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }"""
stepper_weight_new = """                    FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustWeight(-step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                    OutlinedTextField(
                        value = state.weightInput,
                        onValueChange = onWeightChanged,
                        label = { Text("Weight (${state.unitSystem.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustWeight(step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }"""
text = text.replace(stepper_weight_old, stepper_weight_new)

rpe_chip = """
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Easy", "Right", "Hard").forEach { tag ->
                    FilterChip(
                        selected = state.rpeTagInput == tag,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onRpeTagChanged(if (state.rpeTagInput == tag) null else tag)
                        },
                        label = { Text(tag) }
                    )
                }
            }"""
button_old = """            IosButton(
                text = if (state.blockKind == BlockKind.WARM_UP) "Done" else "Log set",
                onClick = onLog,
                enabled = state.valueInput.isNotBlank() && !state.isSaving
            )"""
button_new = """            IosButton(
                text = if (state.blockKind == BlockKind.WARM_UP) "Done" else "Log set",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLog()
                },
                enabled = state.valueInput.isNotBlank() && !state.isSaving
            )"""
text = text.replace(button_old, rpe_chip + "\n" + button_new)

# Up Next Strip
upnext_strip = """
        if (state.upNextExercises.isNotEmpty()) {
            IosCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Up Next", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    state.upNextExercises.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
"""
text = text.replace("            // In-session notes", upnext_strip + "\n            // In-session notes")


with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
