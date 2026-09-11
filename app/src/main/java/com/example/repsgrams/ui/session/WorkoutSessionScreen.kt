package com.example.repsgrams.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.domain.progression.ProgressionSuggestion
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.ui.components.IosAlertDialog
import com.example.repsgrams.ui.components.IosButton
import com.example.repsgrams.ui.components.IosCard

@Composable
fun WorkoutSessionRoute(viewModel: WorkoutSessionViewModel, onFinished: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.summaryDone.collect { onFinished() } }
    
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
        onBack = onFinished
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (state is WorkoutSessionUiState.Active) {
                        Text(state.workoutName, fontWeight = FontWeight.Bold)
                    } else if (state is WorkoutSessionUiState.Summary) {
                        Text("Summary", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    if (state !is WorkoutSessionUiState.Summary) {
                        TextButton(onClick = onBack) {
                            Text("< Back", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val rest = state.restRemainingSeconds
        if (rest != null) {
            RestCard(rest, state.exercise.name, state.roundNumber, state.roundCount, onAddRest, onSkipRest)
        } else {
            ExerciseCard(state, onValueChanged, onWeightChanged, onAdjustValue, onAdjustWeight, onLog)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (rest == null) {
            if (state.isOptionalBlock) {
                IosButton(text = "Skip optional core", onClick = onSkipBlock, isSecondary = true)
            }
            TextButton(onClick = { showFinishDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("End workout early", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showFinishDialog) {
        IosAlertDialog(
            title = "End workout?",
            message = "Your logged sets will be saved and this session will be marked complete.",
            confirmText = "End workout",
            onConfirm = { showFinishDialog = false; onFinish() },
            dismissText = "Keep going",
            onDismiss = { showFinishDialog = false }
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
    IosCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${state.blockLabel} · Set ${state.roundNumber} of ${state.roundCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(exercise.name, style = MaterialTheme.typography.titleLarge)
            
            state.progressionSuggestion?.let { suggestion ->
                Text(
                    if (suggestion == ProgressionSuggestion.INCREASE_WEIGHT) "Ready to increase weight" else "Ready for more challenge",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            
            Text(
                "Target ${exercise.targetValueLow}–${exercise.targetValueHigh} " + if (exercise.repType == RepType.REPS) "reps" else "seconds",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (exercise.perSide) Text("Per side", style = MaterialTheme.typography.labelLarge)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onAdjustValue(-1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("−") }
                OutlinedTextField(
                    value = state.valueInput,
                    onValueChange = onValueChanged,
                    label = { Text(if (exercise.repType == RepType.REPS) "Reps" else "Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedButton(onClick = { onAdjustValue(1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("+") }
            }
            if (exercise.tracksWeight) {
                val step = if (state.unitSystem == UnitSystem.KG) 1f else 2.5f
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onAdjustWeight(-step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("−") }
                    OutlinedTextField(
                        value = state.weightInput,
                        onValueChange = onWeightChanged,
                        label = { Text("Weight (${state.unitSystem.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedButton(onClick = { onAdjustWeight(step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("+") }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            IosButton(
                text = if (state.blockKind == BlockKind.WARM_UP) "Done" else "Log set",
                onClick = onLog,
                enabled = state.valueInput.isNotBlank() && !state.isSaving
            )
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
    IosCard {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Rest", style = MaterialTheme.typography.titleLarge)
            Text(formatTime(remaining), style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp))
            Text("Next: $nextExercise · set $round of $roundCount", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IosButton(text = "+15 sec", onClick = { onAddRest(15) }, isSecondary = true, modifier = Modifier.weight(1f))
                IosButton(text = "Skip", onClick = onSkipRest, modifier = Modifier.weight(1f))
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
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Workout complete", style = MaterialTheme.typography.headlineLarge)
        
        IosCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                SummaryMetric(state.setCount.toString(), "sets")
                SummaryMetric(formatTime(state.durationSeconds), "of ${state.maxDurationMinutes}:00 target")
            }
        }
        
        Text("Post-workout check-in", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        IosCard {
            Column {
                CheckRow("Whey protein taken", state.wheyTaken, onWheyChanged)
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                CheckRow("Creatine taken", state.creatineTaken, onCreatineChanged)
            }
        }
        Text("1 whey serving and 5 g creatine are selected by default. Adjust if needed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        Spacer(modifier = Modifier.height(16.dp))
        IosButton(text = "Save and return to Today", onClick = onDone)
    }
}

@Composable
private fun SummaryMetric(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChecked(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked, 
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                uncheckedBorderColor = Color.Transparent
            )
        )
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
