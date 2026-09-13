package com.example.repsgrams.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import com.example.repsgrams.ui.theme.AppColors
import com.example.repsgrams.ui.components.IconBadge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import com.example.repsgrams.service.RestTimerService
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.IconButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.repsgrams.ui.components.shimmer

import androidx.activity.compose.BackHandler
@Composable
fun WorkoutSessionRoute(viewModel: WorkoutSessionViewModel, onFinished: () -> Unit) {

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCancelDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = state !is WorkoutSessionUiState.Summary) {
        showCancelDialog = true
    }

    if (showCancelDialog) {
        IosAlertDialog(
            title = "Discard this workout?",
            message = "Nothing from this session will be saved.",
            confirmText = "Discard",
            dismissText = "Cancel",
            onConfirm = {
                showCancelDialog = false
                viewModel.cancelWorkout()
                onFinished()
            },
            onDismiss = { showCancelDialog = false }
        )
    }
    LaunchedEffect(viewModel) { viewModel.summaryDone.collect { onFinished() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                RestTimerService.isSessionForeground = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                RestTimerService.isSessionForeground = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            RestTimerService.isSessionForeground = false
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    WorkoutSessionScreen(
        state = state,
        prAchieved = viewModel.prAchieved,
        onValueChanged = viewModel::updateValue,
        onWeightChanged = viewModel::updateWeight,
        onAdjustValue = viewModel::adjustValue,
        onAdjustWeight = viewModel::adjustWeight,
        onLog = viewModel::logCurrent,
        onSkipBlock = viewModel::skipOptionalBlock,
        onNotesChanged = viewModel::updateNotes,
        onRpeTagChanged = viewModel::updateRpeTag,
        onAddRest = viewModel::addRestSeconds,
        onSkipRest = viewModel::skipRest,
        onFinish = viewModel::finishWorkout,
        onStartHold = viewModel::startHold,
        onStopHold = viewModel::stopHold,
        onPrevious = viewModel::previousStep,
        onCancelWorkout = { showCancelDialog = true },
        onDone = viewModel::saveSummary,
        onBack = { showCancelDialog = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,
    prAchieved: kotlinx.coroutines.flow.SharedFlow<List<com.example.repsgrams.data.db.PersonalRecordEntity>>,
    onValueChanged: (String) -> Unit,
    onWeightChanged: (String) -> Unit,
    onAdjustValue: (Int) -> Unit,
    onAdjustWeight: (Float) -> Unit,
    onLog: () -> Unit,
    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onStartHold: () -> Unit,
    onStopHold: () -> Unit,
    onPrevious: () -> Unit,
    onCancelWorkout: () -> Unit,
    onDone: () -> Unit,
    onNotesChanged: (String) -> Unit = {},
    onRpeTagChanged: (String?) -> Unit = {},
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(prAchieved) {
        prAchieved.collect { prs ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar("🎉 New Personal Record!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    if (state is WorkoutSessionUiState.Active) {
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Previous step", tint = MaterialTheme.colorScheme.primary)
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
                WorkoutSessionUiState.Loading -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.medium).shimmer())
                    Box(Modifier.fillMaxWidth().height(320.dp).clip(MaterialTheme.shapes.medium).shimmer())
                    Box(Modifier.fillMaxWidth().height(56.dp).clip(MaterialTheme.shapes.small).shimmer())
                }
                is WorkoutSessionUiState.Error -> Centered {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
                                is WorkoutSessionUiState.Active -> ActiveSession(
                    state = state,
                    onValueChanged = onValueChanged,
                    onWeightChanged = onWeightChanged,
                    onAdjustValue = onAdjustValue,
                    onLog = onLog,
                    onAdjustWeight = onAdjustWeight,
                    onSkipBlock = onSkipBlock,
                    onAddRest = onAddRest,
                    onSkipRest = onSkipRest,
                    onFinish = onFinish,
                    onStartHold = onStartHold,
                    onStopHold = onStopHold,
                    onCancelWorkout = onCancelWorkout,
                    onNotesChanged = onNotesChanged,
                    onRpeTagChanged = onRpeTagChanged,
                )
                is WorkoutSessionUiState.Summary -> SummaryScreen(
                    state,   onDone,
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
    onStartHold: () -> Unit,
    onStopHold: () -> Unit,
    onCancelWorkout: () -> Unit,
    onNotesChanged: (String) -> Unit = {},
    onRpeTagChanged: (String?) -> Unit = {},
) {
    var showFinishDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        com.example.repsgrams.ui.components.CategoryChip(state.category)
        val rest = state.restRemainingSeconds
        if (rest != null) {
            RestCard(rest, state.exercise.name, state.roundNumber, state.roundCount, onAddRest, onSkipRest)
        } else {
            ExerciseCard(
                state = state,
                onValueChanged = onValueChanged,
                onWeightChanged = onWeightChanged,
                onAdjustValue = onAdjustValue,
                onAdjustWeight = onAdjustWeight,
                onLog = onLog,
                onStartHold = onStartHold,
                onStopHold = onStopHold,
                onRpeTagChanged = onRpeTagChanged,
            )


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

            // In-session notes
            OutlinedTextField(
                value = state.notesInput,
                onValueChange = onNotesChanged,
                label = { Text("Session Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
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
    onStartHold: () -> Unit,
    onStopHold: () -> Unit,
    onRpeTagChanged: (String?) -> Unit,
) {
    val exercise = state.exercise
    val haptic = LocalHapticFeedback.current
    var showExerciseImage by remember(exercise.id) { mutableStateOf(false) }
    IosCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${state.blockLabel} · Set ${state.roundNumber} of ${state.roundCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExerciseMedia(
                    imageAssetName = exercise.imageAssetName,
                    exerciseName = exercise.name,
                    modifier = Modifier.size(72.dp),
                    onClick = { showExerciseImage = true },
                )
                Text(exercise.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }

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

            if (exercise.repType == RepType.SECONDS) {
                if (state.isHolding) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                        Text(
                            text = "${state.holdElapsedSeconds}s",
                            style = MaterialTheme.typography.displayLarge,
                            color = AppColors.workout
                        )
                        LinearProgressIndicator(
                            progress = {
                                if (exercise.targetValueHigh > 0) (state.holdElapsedSeconds.toFloat() / exercise.targetValueHigh).coerceIn(0f, 1f) else 0f
                            },
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                            color = AppColors.workout
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        IosButton(text = "Stop", onClick = onStopHold, isSecondary = true, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    IosButton(text = "Start Hold", onClick = onStartHold, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

                    // Allow manual entry fallback
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustValue(-1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                        OutlinedTextField(
                            value = state.valueInput,
                            onValueChange = onValueChanged,
                            label = { Text("Manual Seconds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustValue(1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustValue(-1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                    OutlinedTextField(
                        value = state.valueInput,
                        onValueChange = onValueChanged,
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustValue(1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }
                }
            }
            if (exercise.tracksWeight) {
                val step = if (state.unitSystem == UnitSystem.KG) 1f else 2.5f
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustWeight(-step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                    OutlinedTextField(
                        value = state.weightInput,
                        onValueChange = onWeightChanged,
                        label = { Text("Weight (${state.unitSystem.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FilledTonalIconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdjustWeight(step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

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
            }
            IosButton(
                text = if (state.blockKind == BlockKind.WARM_UP) "Done" else "Log set",
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLog()
                },
                enabled = state.valueInput.isNotBlank() && !state.isSaving
            )
        }
    }

    if (showExerciseImage) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showExerciseImage = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { showExerciseImage = false },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 20.dp,
            ) {
                Column {
                    ExerciseMedia(
                        imageAssetName = exercise.imageAssetName,
                        exerciseName = exercise.name,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1.25f),
                    )
                    Text(
                        exercise.name,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseMedia(
    imageAssetName: String?,
    exerciseName: String,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
) {
    val resourceId = com.example.repsgrams.ui.components.exerciseImageResource(imageAssetName)
    val interactiveModifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Box(
        modifier = interactiveModifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (resourceId != 0) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(resourceId),
                contentDescription = "$exerciseName demonstration",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        } else {
            Text(
                exerciseName.take(1).uppercase(),
                style = MaterialTheme.typography.headlineLarge,
                color = AppColors.workout,
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
private fun CheckRow(label: String, icon: ImageVector, iconTint: Color, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChecked(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconBadge(icon = icon, tint = iconTint)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconTint,
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
