package com.example.repsgrams.ui.session

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.domain.progression.ProgressionSuggestion

@Composable
fun WorkoutSessionRoute(viewModel: WorkoutSessionViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.restFinished.collect {
            context.getSystemService(Vibrator::class.java)?.vibrate(
                VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        }
    }
    LaunchedEffect(viewModel) { viewModel.summaryDone.collect { onDone() } }
    WorkoutSessionScreen(
        state = state,
        onValueChanged = viewModel::updateValue,
        onWeightChanged = viewModel::updateWeight,
        onAdjustValue = viewModel::adjustValue,
        onAdjustWeight = viewModel::adjustWeight,
        onLog = viewModel::logCurrent,
        onSkipBlock = viewModel::skipOptionalBlock,
        onAddRest = viewModel::addRestSeconds,
        onSkipRest = viewModel::skipRest,
        onFinish = viewModel::finishWorkout,
        onWheyChanged = viewModel::setWheyTaken,
        onCreatineChanged = viewModel::setCreatineTaken,
        onDone = viewModel::saveSummary,
    )
}

@Composable
fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,
    onValueChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAdjustValue: (Int) -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onLog: () -> Unit,
    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onWheyChanged: (Boolean) -> Unit,
    onCreatineChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    when (state) {
        WorkoutSessionUiState.Loading -> Centered { CircularProgressIndicator() }
        is WorkoutSessionUiState.Error -> Centered {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
        is WorkoutSessionUiState.Active -> ActiveSession(
            state, onValueChanged, onWeightChanged, onAdjustValue, onLog,
            onAdjustWeight, onSkipBlock, onAddRest, onSkipRest, onFinish,
        )
        is WorkoutSessionUiState.Summary -> SummaryScreen(
            state, onWheyChanged, onCreatineChanged, onDone,
        )
    }
}

@Composable
private fun ActiveSession(
    state: WorkoutSessionUiState.Active,
    onValueChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAdjustValue: (Int) -> Unit,
    onLog: () -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Workout ${state.dayLabel}", style = MaterialTheme.typography.headlineSmall)
                Text(formatTime(state.elapsedSeconds), style = MaterialTheme.typography.titleMedium)
            }
            TextButton(onClick = { showFinishDialog = true }) { Text("End") }
        }
        if (state.elapsedSeconds >= 35 * 60) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (state.elapsedSeconds >= state.maxDurationMinutes * 60) {
                        "You reached the ${state.maxDurationMinutes}-minute soft limit. Finish safely when ready."
                    } else "5 min left — consider skipping Optional Core.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        Text(
            "Block ${state.blockNumber} of ${state.blockCount} · ${state.blockLabel}",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            if (state.blockKind == BlockKind.WARM_UP) "Warm-up checklist"
            else "Round ${state.roundNumber} of ${state.roundCount}",
            style = MaterialTheme.typography.labelLarge,
        )

        if (state.restRemainingSeconds != null) {
            RestCard(
                state.restRemainingSeconds,
                state.exercise.name,
                state.roundNumber,
                state.roundCount,
                onAddRest,
                onSkipRest,
            )
        } else {
            ExerciseCard(state, onValueChanged, onWeightChanged, onAdjustValue, onAdjustWeight, onLog)
            if (state.isOptionalBlock) {
                OutlinedButton(onClick = onSkipBlock, modifier = Modifier.fillMaxWidth()) {
                    Text("Skip optional core")
                }
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("End workout?") },
            text = { Text("Your logged sets will be saved and this session will be marked complete.") },
            confirmButton = { TextButton(onClick = onFinish) { Text("End workout") } },
            dismissButton = { TextButton(onClick = { showFinishDialog = false }) { Text("Keep going") } },
        )
    }
}

@Composable
private fun ExerciseCard(
    state: WorkoutSessionUiState.Active,
    onValueChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAdjustValue: (Int) -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onLog: () -> Unit,
) {
    val exercise = state.exercise
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.headlineMedium)
            state.progressionSuggestion?.let { suggestion ->
                Text(
                    if (suggestion == ProgressionSuggestion.INCREASE_WEIGHT) {
                        "Ready to increase weight"
                    } else "Ready for more challenge",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Every logged round in your previous matching workout reached this rep target. " +
                        "Adjust only when you feel ready.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "Target ${exercise.targetValueLow}–${exercise.targetValueHigh} " +
                    if (exercise.repType == RepType.REPS) "reps" else "seconds",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (exercise.perSide) Text("Per side", style = MaterialTheme.typography.labelLarge)
            exercise.notes?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { onAdjustValue(-1) }) { Text("−") }
                OutlinedTextField(
                    value = state.valueInput,
                    onValueChange = onValueChanged,
                    label = { Text(if (exercise.repType == RepType.REPS) "Reps" else "Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedButton(onClick = { onAdjustValue(1) }) { Text("+") }
            }
            if (exercise.tracksWeight) {
                val step = if (state.unitSystem == UnitSystem.KG) 1f else 2.5f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = { onAdjustWeight(-step) }) { Text("−") }
                    OutlinedTextField(
                        value = state.weightInput,
                        onValueChange = onWeightChanged,
                        label = { Text("Weight (${state.unitSystem.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedButton(onClick = { onAdjustWeight(step) }) { Text("+") }
                }
            }
            Button(
                onClick = onLog,
                enabled = state.valueInput.isNotBlank() && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.blockKind == BlockKind.WARM_UP) "Done" else "Log set")
            }
        }
    }
}

@Composable
private fun RestCard(
    remaining: Int,
    nextExercise: String,
    round: Int,
    roundCount: Int,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rest", style = MaterialTheme.typography.titleLarge)
            Text(formatTime(remaining), style = MaterialTheme.typography.displayLarge)
            Text("Next: $nextExercise · round $round of $roundCount")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { onAddRest(15) }) { Text("+15 sec") }
                Button(onClick = onSkipRest) { Text("Skip") }
            }
        }
    }
}

@Composable
private fun SummaryScreen(
    state: WorkoutSessionUiState.Summary,
    onWheyChanged: (Boolean) -> Unit,
    onCreatineChanged: (Boolean) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Workout complete", style = MaterialTheme.typography.headlineLarge)
        Text(state.workoutName, style = MaterialTheme.typography.titleLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                SummaryMetric(state.setCount.toString(), "sets")
                SummaryMetric(
                    formatTime(state.durationSeconds),
                    "of ${state.maxDurationMinutes}:00 target",
                )
            }
        }
        Text("Post-workout check-in", style = MaterialTheme.typography.titleMedium)
        CheckRow("Whey protein taken", state.wheyTaken, onWheyChanged)
        CheckRow("Creatine taken", state.creatineTaken, onCreatineChanged)
        Spacer(Modifier.height(8.dp))
        Text("1 whey serving and 5 g creatine are selected by default. Adjust if needed.")
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Save and return to Today") }
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onChecked(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

private fun formatTime(totalSeconds: Int): String =
    "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
