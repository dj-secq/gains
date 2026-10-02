package com.example.repsgrams.ui.session

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.domain.session.KeypadField
import com.example.repsgrams.domain.session.currentSetCaption
import com.example.repsgrams.domain.session.PLANNED_DURATION_PASSED
import com.example.repsgrams.domain.session.SetRowModel
import com.example.repsgrams.domain.session.cueText
import com.example.repsgrams.domain.session.formatRestClock
import com.example.repsgrams.domain.session.formatWeight
import com.example.repsgrams.domain.session.formatSessionElapsed
import com.example.repsgrams.domain.session.nextExerciseCaption
import com.example.repsgrams.domain.today.formatDose
import com.example.repsgrams.service.RestTimerService
import com.example.repsgrams.ui.components.BackChevron
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.ExerciseFigure
import com.example.repsgrams.ui.components.InkPill
import com.example.repsgrams.ui.components.MonoLabel
import com.example.repsgrams.ui.components.OutlinePill
import com.example.repsgrams.ui.components.StatusDot
import com.example.repsgrams.ui.components.TileTone
import com.example.repsgrams.ui.components.TextAction
import com.example.repsgrams.ui.components.shimmer
import com.example.repsgrams.ui.components.tileMuted
import com.example.repsgrams.ui.theme.DisplayNumeral
import com.example.repsgrams.ui.theme.HairlineDark
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.LabelDark
import com.example.repsgrams.ui.theme.MonoLabelStyle
import com.example.repsgrams.ui.theme.PaperDark
import com.example.repsgrams.ui.theme.PaperLight
import com.example.repsgrams.ui.theme.SignalRed
import com.example.repsgrams.ui.theme.doneGreen
import com.example.repsgrams.ui.today.TodaySupplement
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun WorkoutSessionRoute(viewModel: WorkoutSessionViewModel, onFinished: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }
    val view = LocalView.current

    fun leave() {
        scope.launch {
            viewModel.leaveWorkout()
            onFinished()
        }
    }

    BackHandler(enabled = state is WorkoutSessionUiState.Summary) { viewModel.saveSummary() }

    if (showCancelDialog) {
        BoardDialog(
            title = "Discard this workout?",
            message = "Nothing from this session will be saved.",
            confirmText = "Discard",
            dismissText = "Cancel",
            destructive = true,
            onConfirm = {
                showCancelDialog = false
                scope.launch {
                    viewModel.discardWorkout()
                    onFinished()
                }
            },
            onDismiss = { showCancelDialog = false },
        )
    }
    LaunchedEffect(viewModel) { viewModel.summaryDone.collect { onFinished() } }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, keepScreenOn) {
        val window = view.context.findActivity()?.window
        fun setAwake(awake: Boolean) {
            if (awake && keepScreenOn) {
                window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                RestTimerService.isSessionForeground = true
                viewModel.onSessionResumed()
                setAwake(true)
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                RestTimerService.isSessionForeground = false
                setAwake(false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        val resumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        if (resumed) {
            RestTimerService.isSessionForeground = true
            viewModel.onSessionResumed()
            setAwake(true)
        }
        onDispose {
            RestTimerService.isSessionForeground = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    WorkoutSessionScreen(
        state = state,
        provisionalRecord = viewModel.provisionalRecord,
        onAim = viewModel::aimKeypad,
        onKey = viewModel::keypadKey,
        onStep = viewModel::stepAimed,
        onDismissKeypad = viewModel::dismissKeypad,
        onCopyPrevious = viewModel::copyPreviousIntoActive,
        onLog = viewModel::logCurrent,
        onSkipBlock = viewModel::skipOptionalBlock,
        onNotesChanged = viewModel::updateNotes,
        onAddRest = viewModel::addRestSeconds,
        onSkipRest = viewModel::skipRest,
        onFinish = viewModel::finishWorkout,
        onStartHold = viewModel::startHold,
        onStopHold = viewModel::stopHold,
        onPrevious = viewModel::previousStep,
        onCancelWorkout = { showCancelDialog = true },
        onEndBlock = viewModel::endBlock,
        onAddRound = viewModel::addRound,
        onSkipProgression = viewModel::skipProgressionOnce,
        onAdjustRpe = viewModel::adjustRpe,
        onSubstitute = viewModel::substituteThisSession,
        onReplace = viewModel::replaceInProgram,
        onAddWarmup = viewModel::addWarmupRow,
        onLogWarmup = viewModel::logWarmup,
        onAddExercise = viewModel::addFreestyleExercise,
        onToggleSupplement = viewModel::toggleSummarySupplement,
        onDone = viewModel::saveSummary,
        onBack = ::leave,
    )
}

@Composable
fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,
    provisionalRecord: kotlinx.coroutines.flow.SharedFlow<Unit>,
    onAim: (KeypadField) -> Unit,
    onKey: (String) -> Unit,
    onStep: (Boolean) -> Unit,
    onDismissKeypad: () -> Unit,
    onCopyPrevious: () -> Unit,
    onLog: () -> Unit,
    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onStartHold: () -> Unit,
    onStopHold: () -> Unit,
    onPrevious: () -> Unit,
    onCancelWorkout: () -> Unit,
    onEndBlock: () -> Unit,
    onAddRound: () -> Unit,
    onSkipProgression: () -> Unit,
    onAdjustRpe: (Float) -> Unit,
    onSubstitute: (Long) -> Unit,
    onReplace: (Long) -> Unit,
    onAddWarmup: () -> Unit,
    onLogWarmup: () -> Unit,
    onAddExercise: (Long) -> Unit,
    onToggleSupplement: (Long) -> Unit,
    onDone: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(provisionalRecord) {
        provisionalRecord.collect {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar("Provisional record")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state) {
                WorkoutSessionUiState.Loading -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.fillMaxWidth().height(48.dp).clip(MaterialTheme.shapes.medium).shimmer())
                    Box(Modifier.fillMaxWidth().height(72.dp).clip(MaterialTheme.shapes.medium).shimmer())
                    Box(Modifier.fillMaxWidth().height(280.dp).clip(MaterialTheme.shapes.medium).shimmer())
                }
                is WorkoutSessionUiState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.onSurface)
                }
                is WorkoutSessionUiState.FreestyleEmpty -> FreestyleEmptySession(
                    state = state,
                    onAddExercise = onAddExercise,
                    onFinish = onFinish,
                    onCancelWorkout = onCancelWorkout,
                    onNotesChanged = onNotesChanged,
                    onBack = onBack,
                )
                is WorkoutSessionUiState.Active -> ActiveSession(
                    state = state,
                    onAim = onAim,
                    onKey = onKey,
                    onStep = onStep,
                    onDismissKeypad = onDismissKeypad,
                    onCopyPrevious = onCopyPrevious,
                    onLog = onLog,
                    onSkipBlock = onSkipBlock,
                    onAddRest = onAddRest,
                    onSkipRest = onSkipRest,
                    onFinish = onFinish,
                    onStartHold = onStartHold,
                    onStopHold = onStopHold,
                    onPrevious = onPrevious,
                    onCancelWorkout = onCancelWorkout,
                    onEndBlock = onEndBlock,
                    onAddRound = onAddRound,
                    onSkipProgression = onSkipProgression,
                    onAdjustRpe = onAdjustRpe,
                    onSubstitute = onSubstitute,
                    onReplace = onReplace,
                    onAddWarmup = onAddWarmup,
                    onLogWarmup = onLogWarmup,
                    onAddExercise = onAddExercise,
                    onNotesChanged = onNotesChanged,
                    onBack = onBack,
                )
                is WorkoutSessionUiState.Summary -> SummaryScreen(
                    state = state,
                    onToggleSupplement = onToggleSupplement,
                    onDone = onDone,
                )
            }
        }
    }
}

@Composable
private fun ActiveSession(
    state: WorkoutSessionUiState.Active,
    onAim: (KeypadField) -> Unit,
    onKey: (String) -> Unit,
    onStep: (Boolean) -> Unit,
    onDismissKeypad: () -> Unit,
    onCopyPrevious: () -> Unit,
    onLog: () -> Unit,
    onSkipBlock: () -> Unit,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onFinish: () -> Unit,
    onStartHold: () -> Unit,
    onStopHold: () -> Unit,
    onPrevious: () -> Unit,
    onCancelWorkout: () -> Unit,
    onEndBlock: () -> Unit,
    onAddRound: () -> Unit,
    onSkipProgression: () -> Unit,
    onAdjustRpe: (Float) -> Unit,
    onSubstitute: (Long) -> Unit,
    onReplace: (Long) -> Unit,
    onAddWarmup: () -> Unit,
    onLogWarmup: () -> Unit,
    onAddExercise: (Long) -> Unit,
    onNotesChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var showPlates by remember { mutableStateOf(false) }
    var exerciseMenu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<Boolean?>(null) }
    var addingExercise by remember { mutableStateOf(false) }
    var pendingReplace by remember { mutableStateOf<CatalogExercise?>(null) }
    val resting = state.restRemainingSeconds != null
    val seconds = state.exercise.repType == RepType.SECONDS
    val cue = cueText(state.exercise.notes)
    val haptic = LocalHapticFeedback.current
    val warmupOpen = state.rows.any { it.warmup && !it.complete }

    Column(modifier = Modifier.fillMaxSize()) {
        SessionBar(
            elapsedSeconds = state.elapsedSeconds,
            durationPassed = state.durationPassed,
            onBack = onBack,
            onFinish = { showFinishDialog = true },
            onDiscard = onCancelWorkout,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                currentSetCaption(state.rows),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExerciseFigure(
                    imageAssetName = state.exercise.imageAssetName,
                    contentDescription = state.exercise.name,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    state.exercise.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Box {
                    IconButton(onClick = { exerciseMenu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Exercise")
                    }
                    DropdownMenu(
                        expanded = exerciseMenu,
                        onDismissRequest = { exerciseMenu = false },
                        containerColor = PaperDark,
                        tonalElevation = 0.dp,
                        shadowElevation = 2.dp,
                    ) {
                        DropdownMenuItem(
                            text = { Text("This session only") },
                            onClick = {
                                exerciseMenu = false
                                picker = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Replace in program") },
                            onClick = {
                                exerciseMenu = false
                                picker = true
                            },
                        )
                        if (state.showAddWarmup) {
                            DropdownMenuItem(
                                text = { Text("Add warm-up") },
                                onClick = {
                                    exerciseMenu = false
                                    onAddWarmup()
                                },
                            )
                        }
                        if (state.showAddExercise) {
                            DropdownMenuItem(
                                text = { Text("Add exercise") },
                                onClick = {
                                    exerciseMenu = false
                                    addingExercise = true
                                },
                            )
                        }
                    }
                }
            }
            if (state.insteadOf != null) MonoLabel(state.insteadOf)
            if (cue != null) {
                Text(cue, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.exercise.perSide) MonoLabel("Per side")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SessionAction("Note", Icons.Outlined.Edit, onClick = { showNote = true })
                SessionAction("Previous", Icons.Outlined.History, onClick = onPrevious)
                if (state.plate != null) {
                    SessionAction("Plates", Icons.Outlined.FitnessCenter, onClick = { showPlates = true })
                }
            }
            if (state.progressionLine != null) {
                Text(state.progressionLine, style = MaterialTheme.typography.bodyLarge)
                TextAction(
                    text = if (state.progressionSkipped) "Skipped" else "Skip once",
                    enabled = !state.progressionSkipped,
                    onClick = onSkipProgression,
                )
            }
            if (state.isOptionalBlock && !resting) {
                TextAction(text = "Skip ${state.blockLabel}", onClick = onSkipBlock)
            }
            if (state.showEndBlock && !resting) {
                TextAction(text = "End block", onClick = onEndBlock)
            }
            if (state.showAddRound) {
                TextAction(text = "Add round", onClick = onAddRound)
            }
            BoardTile(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                    SetTable(
                        rows = state.rows,
                        seconds = seconds,
                        tracksWeight = state.exercise.tracksWeight,
                        resting = resting,
                        holding = state.isHolding,
                        showRpe = state.rpeEnabled,
                        keypadOpen = state.keypadOpen,
                        keypadField = state.keypadField,
                        onAim = onAim,
                        onCopyPrevious = onCopyPrevious,
                    )
                }
            }
            if (warmupOpen && !resting && !state.isHolding) {
                TextAction(text = "Log warm-up", onClick = onLogWarmup)
            }
            if (state.rpeEnabled && !resting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonoLabel("RPE", Modifier.weight(1f))
                    TextAction(text = "−", onClick = { onAdjustRpe(-0.5f) })
                    Text(state.rpe?.let { formatWeight(it) } ?: "—", style = MaterialTheme.typography.titleMedium)
                    TextAction(text = "+", onClick = { onAdjustRpe(0.5f) })
                }
            }
            if (seconds && !state.isHolding && !resting) {
                TextAction(text = "Start hold", onClick = onStartHold)
            }
            if (state.isHolding) {
                TextAction(text = "Stop", onClick = onStopHold)
            }
            Spacer(Modifier.height(12.dp))
        }
        if (resting) {
            RestDock(
                remaining = state.restRemainingSeconds ?: 0,
                overtime = state.restOvertimeSeconds,
                caption = state.restCaption.ifBlank { nextExerciseCaption(state.exercise.name) },
                onAddRest = onAddRest,
            )
        } else if (state.keypadOpen && !state.isHolding) {
            SessionKeypad(
                field = state.keypadField,
                onKey = onKey,
                onDone = onDismissKeypad,
            )
        }
        val canStep = !resting && !state.isHolding && !state.isSaving
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!resting && state.nextLine.isNotBlank()) {
                Text(
                    state.nextLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinePill(
                    text = "−",
                    enabled = canStep,
                    onClick = { onStep(false) },
                    modifier = Modifier
                        .width(64.dp)
                        .semantics { contentDescription = "Decrease" },
                )
                InkPill(
                    text = "Next",
                    enabled = !state.isSaving,
                    onClick = {
                        if (resting) {
                            onSkipRest()
                        } else if (state.isHolding) {
                            if (state.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onStopHold()
                        } else {
                            if (state.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLog()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                OutlinePill(
                    text = "+",
                    enabled = canStep,
                    onClick = { onStep(true) },
                    modifier = Modifier
                        .width(64.dp)
                        .semantics { contentDescription = "Increase" },
                )
            }
        }
    }

    if (showFinishDialog) {
        BoardDialog(
            title = "End workout?",
            message = "Logged sets are kept.",
            confirmText = "End",
            dismissText = "Keep going",
            destructive = false,
            onConfirm = {
                showFinishDialog = false
                onFinish()
            },
            onDismiss = { showFinishDialog = false },
            content = {
                state.endLines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            },
        )
    }
    if (showPlates && state.plate != null) {
        val plate = state.plate
        BoardDialog(
            title = "Plates",
            confirmText = "Done",
            dismissText = "Close",
            onConfirm = { showPlates = false },
            onDismiss = { showPlates = false },
            content = {
                if (plate.dumbbell) {
                    Text(plate.caption, style = MaterialTheme.typography.titleLarge)
                } else {
                    if (plate.struck != null && plate.makeable != null) {
                        Text(plate.struck, textDecoration = TextDecoration.LineThrough)
                        Text(plate.makeable, textDecoration = TextDecoration.Underline)
                        if (plate.difference != null) MonoLabel(plate.difference)
                    }
                    Text(plate.caption, style = MaterialTheme.typography.titleMedium)
                    if (plate.assumption != null) {
                        Text(plate.assumption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
        )
    }
    if (addingExercise) {
        ExerciseCatalogDialog(
            catalog = state.catalog,
            onPick = {
                addingExercise = false
                onAddExercise(it)
            },
            onDismiss = { addingExercise = false },
        )
    }
    if (picker != null) {
        val replace = picker == true
        BoardDialog(
            title = "Exercise",
            confirmText = "Close",
            dismissText = "Cancel",
            onConfirm = { picker = null },
            onDismiss = { picker = null },
            content = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    state.catalog.forEach { item ->
                        TextAction(
                            text = item.name,
                            onClick = {
                                picker = null
                                if (replace) pendingReplace = item else onSubstitute(item.id)
                            },
                        )
                    }
                }
            },
        )
    }
    pendingReplace?.let { chosen ->
        BoardDialog(
            title = "Replace in the program?",
            message = "Older sessions stay as logged.",
            confirmText = "Replace",
            onConfirm = {
                onReplace(chosen.id)
                pendingReplace = null
            },
            onDismiss = { pendingReplace = null },
        )
    }
    if (showNote) {
        BoardDialog(
            title = "Note",
            confirmText = "Done",
            dismissText = "Close",
            onConfirm = { showNote = false },
            onDismiss = { showNote = false },
            content = {
                OutlinedTextField(
                    value = state.notesInput,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Session note") },
                    minLines = 3,
                )
            },
        )
    }
}

@Composable
private fun FreestyleEmptySession(
    state: WorkoutSessionUiState.FreestyleEmpty,
    onAddExercise: (Long) -> Unit,
    onFinish: () -> Unit,
    onCancelWorkout: () -> Unit,
    onNotesChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var showFinish by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        SessionBar(
            elapsedSeconds = state.elapsedSeconds,
            durationPassed = false,
            onBack = onBack,
            onFinish = { showFinish = true },
            onDiscard = onCancelWorkout,
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Empty workout", style = MaterialTheme.typography.titleLarge)
            TextAction(text = "Add exercise", onClick = { adding = true })
            TextAction(text = "Note", onClick = { showNote = true })
        }
    }
    if (adding) {
        ExerciseCatalogDialog(
            catalog = state.catalog,
            onPick = {
                adding = false
                onAddExercise(it)
            },
            onDismiss = { adding = false },
        )
    }
    if (showFinish) {
        BoardDialog(
            title = "End workout?",
            message = "Logged sets are kept.",
            confirmText = "End",
            dismissText = "Keep going",
            onConfirm = {
                showFinish = false
                onFinish()
            },
            onDismiss = { showFinish = false },
        )
    }
    if (showNote) {
        BoardDialog(
            title = "Note",
            confirmText = "Done",
            dismissText = "Close",
            onConfirm = { showNote = false },
            onDismiss = { showNote = false },
            content = {
                OutlinedTextField(
                    value = state.notesInput,
                    onValueChange = onNotesChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Session note") },
                    minLines = 3,
                )
            },
        )
    }
}

@Composable
private fun ExerciseCatalogDialog(
    catalog: List<CatalogExercise>,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    BoardDialog(
        title = "Exercise",
        confirmText = "Close",
        dismissText = "Cancel",
        onConfirm = onDismiss,
        onDismiss = onDismiss,
        content = {
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                catalog.forEach { item ->
                    TextAction(text = item.name, onClick = { onPick(item.id) })
                }
            }
        },
    )
}

@Composable
private fun SessionBar(
    elapsedSeconds: Int,
    durationPassed: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BackChevron(onClick = onBack)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                formatSessionElapsed(elapsedSeconds),
                style = DisplayNumeral.copy(fontSize = 32.sp, lineHeight = 36.sp),
                maxLines = 1,
            )
            if (durationPassed) {
                Text(
                    PLANNED_DURATION_PASSED,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        TextAction(text = "End", onClick = onFinish)
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = menu,
                onDismissRequest = { menu = false },
                containerColor = PaperDark,
                tonalElevation = 0.dp,
                shadowElevation = 16.dp,
            ) {
                DropdownMenuItem(
                    text = { Text("Discard", color = LabelDark) },
                    onClick = {
                        menu = false
                        onDiscard()
                    },
                )
            }
        }
    }
}

@Composable
private fun SetTable(
    rows: List<SetRowModel>,
    seconds: Boolean,
    tracksWeight: Boolean,
    resting: Boolean,
    holding: Boolean,
    showRpe: Boolean,
    keypadOpen: Boolean,
    keypadField: KeypadField,
    onAim: (KeypadField) -> Unit,
    onCopyPrevious: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TableHeader("Set", Modifier.weight(SetColumn))
            TableHeader("Prev", Modifier.weight(PrevColumn), align = TextAlign.Start)
            TableHeader("Load", Modifier.weight(LoadColumn))
            TableHeader(if (seconds) "Sec" else "Reps", Modifier.weight(ValueColumn))
            if (showRpe) TableHeader("RPE", Modifier.weight(RpeColumn))
        }
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(SetColumn).heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        row.label,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    if (row.complete) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = "Logged",
                            modifier = Modifier.padding(start = 2.dp).size(16.dp),
                            tint = doneGreen(),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(PrevColumn)
                        .heightIn(min = 48.dp)
                        .then(
                            if (row.copyable && !resting && !holding) {
                                Modifier
                                    .clickable(onClick = onCopyPrevious)
                                    .semantics { contentDescription = "Copy previous ${row.previousText}" }
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        row.previousText,
                        color = LabelDark,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                NumeralCell(
                    text = row.loadText,
                    emphasized = row.active,
                    aimed = row.active && keypadOpen && keypadField == KeypadField.LOAD,
                    modifier = Modifier.weight(LoadColumn),
                    enabled = row.active && tracksWeight && !resting && !holding,
                    description = "Load",
                    onClick = { onAim(KeypadField.LOAD) },
                )
                NumeralCell(
                    text = row.repsText,
                    emphasized = row.active,
                    aimed = row.active && keypadOpen && keypadField == KeypadField.VALUE,
                    modifier = Modifier.weight(ValueColumn),
                    enabled = row.active && !resting && !holding,
                    description = if (seconds) "Seconds" else "Reps",
                    onClick = { onAim(KeypadField.VALUE) },
                )
                if (showRpe) {
                    Text(
                        row.rpeText ?: "—",
                        modifier = Modifier.weight(RpeColumn),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private const val SetColumn = 0.9f
private const val PrevColumn = 1.3f
private const val LoadColumn = 1.15f
private const val ValueColumn = 0.95f
private const val RpeColumn = 0.85f

@Composable
private fun TableHeader(text: String, modifier: Modifier, align: TextAlign = TextAlign.Center) {
    Text(
        text.uppercase(Locale.US),
        modifier = modifier,
        style = MonoLabelStyle,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        maxLines = 1,
    )
}

@Composable
private fun NumeralCell(
    text: String,
    emphasized: Boolean,
    aimed: Boolean,
    modifier: Modifier,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .heightIn(min = 48.dp)
            .then(if (emphasized && !aimed) Modifier.border(1.dp, HairlineDark, shape) else Modifier)
            .clip(shape)
            .background(if (aimed) PaperLight else Color.Transparent)
            .then(if (enabled) Modifier.clickable(onClick = onClick).semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = if (aimed) Ink else PaperLight,
            style = if (emphasized) {
                DisplayNumeral.copy(fontSize = 28.sp, lineHeight = 32.sp)
            } else {
                MaterialTheme.typography.titleMedium
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SessionAction(text: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .border(1.dp, HairlineDark, shape)
            .clip(shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
    }
}

@Composable
private fun RestDock(
    remaining: Int,
    overtime: Int?,
    caption: String,
    onAddRest: (Int) -> Unit,
) {
    val expired = remaining == 0 || (overtime != null && overtime > 0)
    BoardTile(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Rest", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextAction(text = "−15", onClick = { onAddRest(-15) })
                TextAction(text = "+15", onClick = { onAddRest(15) })
            }
            Text(
                formatRestClock(remaining, overtime),
                style = DisplayNumeral,
                color = if (expired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SessionKeypad(
    field: KeypadField,
    onKey: (String) -> Unit,
    onDone: () -> Unit,
) {
    val rows = listOf(
        listOf(key("1"), key("2"), key("3")),
        listOf(key("4"), key("5"), key("6")),
        listOf(key("7"), key("8"), key("9")),
        listOf(key(".", ".", "Decimal"), key("0"), key("⌫", "delete", "Backspace")),
        listOf(key("Done", "done", "Done")),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { spec ->
                    if (spec == null) {
                        Spacer(Modifier.weight(1f).height(48.dp))
                    } else {
                        val decimalLocked = spec.key == "." && field != KeypadField.LOAD
                        KeyButton(
                            label = spec.label,
                            description = spec.description,
                            enabled = !decimalLocked,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (spec.key == "done") onDone() else onKey(spec.key)
                            },
                        )
                    }
                }
            }
        }
    }
}

private data class KeySpec(val label: String, val key: String, val description: String)

private fun key(label: String, token: String = label, description: String = label) = KeySpec(label, token, description)

@Composable
private fun KeyButton(
    label: String,
    description: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val done = label == "Done"
    val shape = RoundedCornerShape(12.dp)
    val fill = if (done) PaperLight else PaperDark
    val content = if (done) Ink else PaperLight
    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (!enabled) 0.38f else if (pressed) 0.82f else 1f)
            .then(if (done) Modifier else Modifier.border(1.dp, HairlineDark, shape))
            .clip(shape)
            .background(fill)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = content,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun SummaryScreen(
    state: WorkoutSessionUiState.Summary,
    onToggleSupplement: (Long) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(state.workoutName, style = MaterialTheme.typography.headlineLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryFigure(state.setCount.toString(), "Sets", Modifier.weight(1f))
            SummaryFigure(formatSessionElapsed(state.durationSeconds), "Duration", Modifier.weight(1f))
        }
        if (state.notes.isNotBlank()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MonoLabel("Note")
                Text(state.notes, style = MaterialTheme.typography.bodyLarge)
            }
        }
        state.records.forEach { line ->
            BoardTile(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), tone = TileTone.Ink) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusDot(live = true)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        line.exerciseName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(line.detail, style = MonoLabelStyle, color = tileMuted())
                }
            }
        }
        state.supplements.forEach { row ->
            SupplementTile(row, onToggle = { onToggleSupplement(row.supplement.id) })
        }
        InkPill(text = "Done", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun SummaryFigure(value: String, caption: String, modifier: Modifier) {
    BoardTile(modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(value, style = DisplayNumeral, maxLines = 1, overflow = TextOverflow.Ellipsis)
            MonoLabel(caption)
        }
    }
}

@Composable
private fun SupplementTile(row: TodaySupplement, onToggle: () -> Unit) {
    val taken = row.taken
    BoardTile(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (taken) Icons.Outlined.Check else Icons.Outlined.Circle,
                contentDescription = if (taken) "Taken" else "Not taken",
                modifier = Modifier.size(18.dp),
                tint = if (taken) doneGreen() else SignalRed,
            )
            Text(
                row.supplement.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(formatDose(row.actualAmount), style = MaterialTheme.typography.titleMedium)
            Text(
                row.supplement.unit.uppercase(Locale.US),
                style = MonoLabelStyle,
                color = tileMuted(),
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
