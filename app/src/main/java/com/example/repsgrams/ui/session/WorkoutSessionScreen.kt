package com.example.repsgrams.ui.session

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.domain.session.ExercisePill
import com.example.repsgrams.domain.session.KeypadField
import com.example.repsgrams.domain.session.PLANNED_DURATION_PASSED
import com.example.repsgrams.domain.session.SetRowModel
import com.example.repsgrams.domain.session.cueText
import com.example.repsgrams.domain.session.formatRestClock
import com.example.repsgrams.domain.session.formatSessionElapsed
import com.example.repsgrams.domain.session.nextExerciseCaption
import com.example.repsgrams.domain.today.formatDose
import com.example.repsgrams.service.RestTimerService
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.InkPill
import com.example.repsgrams.ui.components.MonoLabel
import com.example.repsgrams.ui.components.OutlinePill
import com.example.repsgrams.ui.components.SetCompleteCircle
import com.example.repsgrams.ui.components.StatusDot
import com.example.repsgrams.ui.components.TextAction
import com.example.repsgrams.ui.components.TileTone
import com.example.repsgrams.ui.components.shimmer
import com.example.repsgrams.ui.theme.DisplayNumeral
import com.example.repsgrams.ui.theme.HairlineDark
import com.example.repsgrams.ui.theme.HairlineLight
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.LabelDark
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.MonoLabelStyle
import com.example.repsgrams.ui.theme.PaperDark
import com.example.repsgrams.ui.theme.PaperLight
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

    BackHandler(enabled = state !is WorkoutSessionUiState.Summary) { leave() }
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
                is WorkoutSessionUiState.Active -> ActiveSession(
                    state = state,
                    onAim = onAim,
                    onKey = onKey,
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
    onNotesChanged: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showFinishDialog by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    val resting = state.restRemainingSeconds != null
    val seconds = state.exercise.repType == RepType.SECONDS
    val cue = cueText(state.exercise.notes)

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
            ExerciseStrip(state.pills)
            Text(state.exercise.name, style = MaterialTheme.typography.headlineLarge)
            if (cue != null) {
                Text(cue, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.exercise.perSide) MonoLabel("Per side")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextAction(text = "Note", onClick = { showNote = true })
                OutlinePill(text = "Previous", onClick = onPrevious)
            }
            if (state.isOptionalBlock && !resting) {
                TextAction(text = "Skip ${state.blockLabel}", onClick = onSkipBlock)
            }
            SetTable(
                rows = state.rows,
                seconds = seconds,
                tracksWeight = state.exercise.tracksWeight,
                resting = resting,
                holding = state.isHolding,
                saving = state.isSaving,
                onAim = onAim,
                onCopyPrevious = onCopyPrevious,
                onLog = onLog,
                onStopHold = onStopHold,
            )
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
                onSkipRest = onSkipRest,
            )
        } else if (state.keypadOpen && !state.isHolding) {
            SessionKeypad(
                field = state.keypadField,
                onKey = onKey,
                onDone = onDismissKeypad,
            )
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
private fun SessionBar(
    elapsedSeconds: Int,
    durationPassed: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextAction(text = "Close", onClick = onBack, contentDescription = "Close session")
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
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, HairlineDark),
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
private fun ExerciseStrip(pills: List<ExercisePill>) {
    val hairline = if (LocalDarkTheme.current) HairlineDark else HairlineLight
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        pills.groupBy { it.groupId }.values.forEach { group ->
            ExerciseGroup(group, hairline)
        }
    }
}

@Composable
private fun ExerciseGroup(group: List<ExercisePill>, hairline: Color) {
    val density = LocalDensity.current
    var width by remember { mutableStateOf(0.dp) }
    Column {
        Row(
            modifier = Modifier.onSizeChanged { width = with(density) { it.width.toDp() } },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            group.forEach { ExerciseIndexPill(it) }
        }
        if (group.first().superset && width > 0.dp) {
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .width(width)
                    .height(1.dp)
                    .background(hairline),
            )
        }
    }
}

@Composable
private fun ExerciseIndexPill(pill: ExercisePill) {
    val dark = LocalDarkTheme.current
    val fill = when {
        !pill.selected -> Color.Transparent
        dark -> PaperLight
        else -> Ink
    }
    val label = when {
        !pill.selected -> if (dark) PaperLight else Ink
        dark -> Ink
        else -> PaperLight
    }
    Surface(
        modifier = Modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(50),
        color = fill,
        contentColor = label,
        border = if (pill.selected) null else BorderStroke(1.dp, if (dark) HairlineDark else HairlineLight),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(pill.label, style = MaterialTheme.typography.titleMedium)
            Text(
                pill.fraction,
                style = MonoLabelStyle,
                color = if (pill.selected) label else LabelDark,
            )
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
    saving: Boolean,
    onAim: (KeypadField) -> Unit,
    onCopyPrevious: () -> Unit,
    onLog: () -> Unit,
    onStopHold: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonoLabel("Set", Modifier.width(36.dp))
            MonoLabel("Prev", Modifier.weight(1.3f))
            MonoLabel("Load", Modifier.weight(1f))
            MonoLabel(if (seconds) "Sec" else "Reps", Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(row.label, modifier = Modifier.width(36.dp), style = MonoLabelStyle)
                Box(
                    modifier = Modifier
                        .weight(1.3f)
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
                    Text(row.previousText, color = LabelDark, style = MaterialTheme.typography.bodyLarge)
                }
                NumeralCell(
                    text = row.loadText,
                    active = row.active,
                    modifier = Modifier.weight(1f),
                    enabled = row.active && tracksWeight && !resting && !holding,
                    description = "Load",
                    onClick = { onAim(KeypadField.LOAD) },
                )
                NumeralCell(
                    text = row.repsText,
                    active = row.active,
                    modifier = Modifier.weight(1f),
                    enabled = row.active && !resting && !holding,
                    description = if (seconds) "Seconds" else "Reps",
                    onClick = { onAim(KeypadField.VALUE) },
                )
                SetCompleteCircle(
                    complete = row.complete,
                    onClick = {
                        if (row.active && !resting && !saving) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (holding) onStopHold() else onLog()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun NumeralCell(
    text: String,
    active: Boolean,
    modifier: Modifier,
    enabled: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (enabled) Modifier.clickable(onClick = onClick).semantics { contentDescription = description } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (active) DisplayNumeral else MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RestDock(
    remaining: Int,
    overtime: Int?,
    caption: String,
    onAddRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
) {
    val expired = remaining == 0 || (overtime != null && overtime > 0)
    BoardTile(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    caption,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextAction(text = "Skip", onClick = onSkipRest)
            }
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
        listOf(key("1"), key("2"), key("3"), key("−", "minus", "Decrease")),
        listOf(key("4"), key("5"), key("6"), key("+", "plus", "Increase")),
        listOf(key("7"), key("8"), key("9"), null),
        listOf(key(".", ".", "Decimal"), key("0"), key("⌫", "delete", "Backspace"), key("Done", "done", "Done")),
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val dark = LocalDarkTheme.current
    val hairline = if (dark) HairlineDark else HairlineLight
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .alpha(if (!enabled) 0.38f else if (pressed) 0.55f else 1f)
            .clip(shape)
            .border(1.dp, hairline, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, maxLines = 1)
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
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                    Text(line.detail, style = MonoLabelStyle, color = LabelDark)
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
    Column(modifier = modifier) {
        Text(value, style = DisplayNumeral, maxLines = 1, overflow = TextOverflow.Ellipsis)
        MonoLabel(caption)
    }
}

@Composable
private fun SupplementTile(row: TodaySupplement, onToggle: () -> Unit) {
    val ink = row.taken
    BoardTile(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        tone = if (ink) TileTone.Ink else TileTone.Paper,
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                color = if (ink) LabelDark else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
