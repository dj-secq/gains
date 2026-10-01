package com.example.repsgrams.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.BodyMeasurementLogEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.domain.progress.ProgressMetric
import com.example.repsgrams.domain.progress.ProgressPoint
import com.example.repsgrams.domain.progress.ProgressRecord
import com.example.repsgrams.domain.progress.formatProgressDate
import com.example.repsgrams.domain.progress.measurementToDisplay
import com.example.repsgrams.domain.progress.progressRecords
import com.example.repsgrams.domain.progress.progressSeries
import com.example.repsgrams.domain.session.formatWeight
import com.example.repsgrams.domain.today.DOSE_DELETE
import com.example.repsgrams.domain.today.applyDoseKey
import com.example.repsgrams.domain.today.parseDose
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.MonoChip
import com.example.repsgrams.ui.components.MonoLabel
import com.example.repsgrams.ui.components.OutlinePill
import com.example.repsgrams.ui.components.RestockDialog
import com.example.repsgrams.ui.components.TextAction
import com.example.repsgrams.ui.components.TileTone
import com.example.repsgrams.ui.theme.DisplayNumeral
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.MonoLabelStyle
import com.example.repsgrams.ui.theme.PaperLight
import com.example.repsgrams.ui.theme.SignalRed
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProgressRoute(viewModel: ProgressViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        state = state,
        onExerciseSelected = viewModel::selectExercise,
        onMetricSelected = viewModel::selectMetric,
        onBodyweightLogged = viewModel::logBodyweight,
        onMeasurementLogged = viewModel::logMeasurement,
        onDeleteRecord = viewModel::deleteRecord,
        onRestock = viewModel::restockSupply,
        onSupplementSelected = viewModel::selectSupplement,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onExerciseSelected: (Long) -> Unit,
    onMetricSelected: (ProgressMetric) -> Unit,
    onBodyweightLogged: (Float) -> Unit,
    onMeasurementLogged: (String, Float) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onRestock: (Long, Int) -> Unit,
    onSupplementSelected: (Long) -> Unit,
) {
    var showExercises by remember { mutableStateOf(false) }
    var showSupplements by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ProgressRecord?>(null) }
    var numberEntry by remember { mutableStateOf<NumberEntry?>(null) }
    var restockTarget by remember { mutableStateOf<SupplyInventoryEntity?>(null) }
    val lengthUnit = if (state.unitSystem == UnitSystem.LB) "IN" else "CM"
    val records = progressRecords(
        state.records,
        state.exercises.associate { it.id to it.name },
        state.unitSystem,
    )

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ExerciseHero(
                    state = state,
                    onOpenExercises = { showExercises = true },
                    onMetricSelected = onMetricSelected,
                )
            }
            item {
                ValueTile(
                    title = "Bodyweight",
                    value = state.todayBodyweight?.let(::formatWeight)
                        ?: state.bodyweightHistory.lastOrNull()?.second?.let(::formatWeight),
                    unit = if (state.unitSystem == UnitSystem.LB) "LB" else "KG",
                    onLog = {
                        numberEntry = NumberEntry(
                            title = "Bodyweight",
                            unit = if (state.unitSystem == UnitSystem.LB) "LB" else "KG",
                            initial = state.todayBodyweight?.let(::formatWeight).orEmpty(),
                            onValue = onBodyweightLogged,
                        )
                    },
                ) {
                    if (state.bodyweightHistory.size >= 2) {
                        val points = state.bodyweightHistory.map { (date, value) ->
                            ProgressPoint(date, value, false, "${formatWeight(value)} ${weightWord(state.unitSystem)}")
                        }
                        MiniChart(points, "bodyweight")
                    }
                }
            }
            if (records.isNotEmpty()) {
                item { RecordList(records) { pendingDelete = it } }
            }
            items(state.trackedMeasurements, key = { it }) { type ->
                val logs = state.measurementLogs[type].orEmpty()
                val todayLog = logs.lastOrNull { it.date == state.today }
                ValueTile(
                    title = type.replaceFirstChar { it.titlecase(Locale.US) },
                    value = logs.maxByOrNull { it.date }?.let { formatWeight(measurementToDisplay(it.valueCm, state.unitSystem)) },
                    unit = lengthUnit,
                    onLog = {
                        numberEntry = NumberEntry(
                            title = type.replaceFirstChar { it.titlecase(Locale.US) },
                            unit = lengthUnit,
                            initial = todayLog?.let { formatWeight(measurementToDisplay(it.valueCm, state.unitSystem)) }.orEmpty(),
                            onValue = { onMeasurementLogged(type, it) },
                        )
                    },
                )
            }
            if (state.supplyInventory.isNotEmpty()) {
                item { SupplySection(state) { restockTarget = it } }
            }
            if (state.supplements.any { it.isActive }) {
                item { AdherenceSection(state) { showSupplements = true } }
            }
        }
    }

    if (showExercises) {
        ExerciseSheet(
            exercises = state.exercises,
            onDismiss = { showExercises = false },
            onSelected = {
                onExerciseSelected(it)
                showExercises = false
            },
        )
    }
    if (showSupplements) {
        SupplementSheet(
            names = state.supplements.filter { it.isActive },
            onDismiss = { showSupplements = false },
            onSelected = {
                onSupplementSelected(it)
                showSupplements = false
            },
        )
    }
    numberEntry?.let { entry ->
        NumberKeypadDialog(
            entry = entry,
            onDismiss = { numberEntry = null },
            onConfirm = {
                entry.onValue(it)
                numberEntry = null
            },
        )
    }
    pendingDelete?.let { row ->
        BoardDialog(
            title = "Delete record",
            onDismiss = { pendingDelete = null },
            confirmText = "Delete",
            onConfirm = {
                onDeleteRecord(row.id)
                pendingDelete = null
            },
            message = "${row.label} for ${row.exerciseName} will be removed.",
            destructive = true,
        )
    }
    restockTarget?.let { inventory ->
        val name = state.supplements.find { it.id == inventory.supplementId }?.name ?: "Supplement"
        RestockDialog(
            name = name,
            initialServings = inventory.totalServings.toString(),
            onDismiss = { restockTarget = null },
            onConfirm = { servings ->
                onRestock(inventory.supplementId, servings)
                restockTarget = null
            },
        )
    }
}

private data class NumberEntry(
    val title: String,
    val unit: String,
    val initial: String,
    val onValue: (Float) -> Unit,
)

@Composable
private fun ExerciseHero(
    state: ProgressUiState,
    onOpenExercises: () -> Unit,
    onMetricSelected: (ProgressMetric) -> Unit,
) {
    val dark = LocalDarkTheme.current
    val exercise = state.exercises.find { it.id == state.selectedExerciseId }
    val series = progressSeries(
        rows = state.exerciseHistory,
        metric = state.metric,
        unit = state.unitSystem,
        tracksWeight = exercise?.tracksWeight == true,
    )
    var selected by remember(exercise?.id, state.metric, series.loadMetrics) { mutableIntStateOf(-1) }
    val index = when {
        series.points.isEmpty() -> -1
        selected !in series.points.indices -> series.points.lastIndex
        else -> selected
    }
    val point = series.points.getOrNull(index)
    val line = if (dark) PaperLight else Ink
    BoardTile(
        modifier = Modifier.fillMaxWidth(),
        tone = if (dark) TileTone.Ink else TileTone.Paper,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.heightIn(min = 48.dp).clickable(enabled = state.exercises.isNotEmpty(), onClick = onOpenExercises),
                contentAlignment = Alignment.CenterStart,
            ) {
                MonoLabel(exercise?.name ?: "Exercise")
            }
            if (point != null) {
                Text(formatWeight(point.value), style = DisplayNumeral, maxLines = 1, overflow = TextOverflow.Ellipsis)
                MonoLabel(series.unit)
            }
            if (series.loadMetrics) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProgressMetric.entries.forEach { metric ->
                        MonoChip(
                            text = metric.name,
                            selected = metric == state.metric,
                            onClick = { onMetricSelected(metric) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            if (series.points.isEmpty()) {
                Text(
                    "Log a few more sessions to see your progress",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SeriesChart(
                    points = series.points,
                    animationKey = if (series.loadMetrics) "${exercise?.id}:${state.metric}" else exercise?.id,
                    lineColor = line,
                    selectedIndex = index,
                    onSelect = { selected = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
                if (point != null) {
                    Text(
                        "${formatProgressDate(point.date)} · ${point.detail}",
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            MonoLabel("Longest ${state.bestStreak} sessions")
        }
    }
}

@Composable
private fun ValueTile(
    title: String,
    value: String?,
    unit: String,
    onLog: () -> Unit,
    chart: (@Composable () -> Unit)? = null,
) {
    BoardTile(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                value ?: "—",
                style = if (value == null) MaterialTheme.typography.titleLarge else DisplayNumeral,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MonoLabel(unit)
            chart?.invoke()
            Spacer(Modifier.height(4.dp))
            OutlinePill(text = "Log", onClick = onLog)
        }
    }
}

@Composable
private fun MiniChart(points: List<ProgressPoint>, animationKey: Any) {
    var selected by remember(animationKey) { mutableIntStateOf(-1) }
    val index = if (selected in points.indices) selected else points.lastIndex
    val point = points.getOrNull(index)
    val line = if (LocalDarkTheme.current) PaperLight else Ink
    SeriesChart(
        points = points,
        animationKey = animationKey,
        lineColor = line,
        selectedIndex = index,
        onSelect = { selected = it },
        modifier = Modifier.fillMaxWidth().height(88.dp),
    )
    if (point != null) {
        Text(
            "${formatProgressDate(point.date)} · ${point.detail}",
            style = MonoLabelStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RecordList(records: List<ProgressRecord>, onDelete: (ProgressRecord) -> Unit) {
    BoardTile(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            MonoLabel("Records", modifier = Modifier.padding(vertical = 8.dp))
            records.groupBy { it.exerciseId }.forEach { (_, group) ->
                Text(
                    group.first().exerciseName,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                group.forEachIndexed { index, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.label,
                            modifier = Modifier.weight(1f),
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(row.value, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            formatProgressDate(row.date),
                            style = MonoLabelStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextAction("Delete", onClick = { onDelete(row) }, destructive = true)
                    }
                    if (index < group.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplySection(state: ProgressUiState, onRestock: (SupplyInventoryEntity) -> Unit) {
    BoardTile(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Supply", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            state.supplyInventory.forEachIndexed { index, inventory ->
                val supplement = state.supplements.find { it.id == inventory.supplementId }
                val fraction = (inventory.servingsRemaining / inventory.totalServings.coerceAtLeast(1)).coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(supplement?.name ?: "Supplement", style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${inventory.servingsRemaining.toInt()} / ${inventory.totalServings} servings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.onSurface,
                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    OutlinePill(text = "Restock", onClick = { onRestock(inventory) }, modifier = Modifier.width(96.dp))
                }
                if (index < state.supplyInventory.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun AdherenceSection(state: ProgressUiState, onOpen: () -> Unit) {
    val active = state.supplements.filter { it.isActive }
    val selected = active.firstOrNull { it.id == state.selectedSupplementId } ?: active.first()
    val taken = state.supplementAdherence.count { it.second }
    val dot = MaterialTheme.colorScheme.onSurface
    BoardTile(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .then(if (active.size > 1) Modifier.clickable(onClick = onOpen) else Modifier),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    MonoLabel(selected.name)
                }
                Text("$taken/30", style = DisplayNumeral.copy(fontSize = 32.sp, lineHeight = 36.sp))
            }
            Canvas(Modifier.fillMaxWidth().height(32.dp)) {
                if (state.supplementAdherence.isEmpty()) return@Canvas
                val gap = size.width / state.supplementAdherence.size
                val radius = (gap * 0.32f).coerceAtMost(5.dp.toPx())
                state.supplementAdherence.forEachIndexed { index, (_, wasTaken) ->
                    val center = Offset(gap * (index + 0.5f), size.height / 2f)
                    if (wasTaken) {
                        drawCircle(dot, radius, center)
                    } else {
                        drawCircle(dot, radius, center, style = Stroke(1.dp.toPx()))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseSheet(
    exercises: List<com.example.repsgrams.data.db.ExerciseEntity>,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val shown = exercises
        .filter { it.name.contains(query, ignoreCase = true) }
        .sortedBy { it.name.lowercase(Locale.US) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                decorationBox = { inner ->
                    Column {
                        MonoLabel("Search")
                        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text("Exercise", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            inner()
                        }
                    }
                },
            )
            if (shown.isEmpty()) {
                Text(
                    "No exercise",
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.id }) { exercise ->
                    BoardTile(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = { onSelected(exercise.id) },
                    ) {
                        Text(
                            exercise.name,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplementSheet(
    names: List<com.example.repsgrams.data.db.SupplementEntity>,
    onDismiss: () -> Unit,
    onSelected: (Long) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.42f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(names, key = { it.id }) { supplement ->
                BoardTile(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    onClick = { onSelected(supplement.id) },
                ) {
                    Text(
                        supplement.name,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberKeypadDialog(
    entry: NumberEntry,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
) {
    var draft by remember(entry.title, entry.initial) { mutableStateOf(entry.initial) }
    var fresh by remember(entry.title, entry.initial) { mutableStateOf(entry.initial.isNotEmpty()) }
    val parsed = parseDose(draft)?.takeIf { it > 0f }
    BoardDialog(
        title = entry.title,
        onDismiss = onDismiss,
        confirmText = "Log",
        onConfirm = { parsed?.let(onConfirm) },
        confirmEnabled = parsed != null,
    ) {
        if (draft.isEmpty()) {
            MonoLabel(entry.unit)
        } else {
            Text(draft, style = DisplayNumeral.copy(fontSize = 40.sp, lineHeight = 44.sp))
            MonoLabel(entry.unit)
        }
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", DOSE_DELETE),
        ).forEach { keys ->
            Row(Modifier.fillMaxWidth()) {
                keys.forEach { key ->
                    TextAction(
                        text = if (key == DOSE_DELETE) "Delete" else key,
                        onClick = {
                            draft = if (fresh && key != DOSE_DELETE) {
                                fresh = false
                                applyDoseKey("", key)
                            } else {
                                fresh = false
                                applyDoseKey(draft, key)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesChart(
    points: List<ProgressPoint>,
    animationKey: Any?,
    lineColor: Color,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val drawProgress = remember(animationKey) { Animatable(0f) }
    LaunchedEffect(animationKey) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(1f, tween(durationMillis = 400, easing = EaseOutCubic))
    }
    Canvas(
        modifier.pointerInput(points) {
            detectTapGestures { tap ->
                val offsets = chartOffsets(points.map { it.value }, size.width.toFloat(), size.height.toFloat(), 4.dp.toPx(), 8.dp.toPx())
                val nearest = offsets.indices.minByOrNull { abs(offsets[it].x - tap.x) } ?: return@detectTapGestures
                onSelect(nearest)
            }
        },
    ) {
        val offsets = chartOffsets(points.map { it.value }, size.width, size.height, 4.dp.toPx(), 8.dp.toPx())
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.zipWithNext().forEach { (start, end) ->
                val controlX = (start.x + end.x) / 2f
                cubicTo(controlX, start.y, controlX, end.y, end.x, end.y)
            }
        }
        clipRect(right = size.width * drawProgress.value) {
            if (offsets.size > 1) {
                drawPath(path, lineColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            offsets.forEachIndexed { index, center ->
                val point = points[index]
                when {
                    point.record -> drawCircle(SignalRed, 3.dp.toPx(), center)
                    index == offsets.lastIndex -> drawCircle(lineColor, 3.dp.toPx(), center)
                }
                if (index == selectedIndex) {
                    drawCircle(lineColor, radius = 6.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                }
            }
        }
    }
}

private fun chartOffsets(values: List<Float>, width: Float, height: Float, horizontalPad: Float, verticalPad: Float): List<Offset> {
    if (values.isEmpty()) return emptyList()
    val chartWidth = (width - horizontalPad * 2).coerceAtLeast(1f)
    val chartHeight = (height - verticalPad * 2).coerceAtLeast(1f)
    val minValue = values.min()
    val maxValue = values.max()
    val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f
    return values.mapIndexed { index, value ->
        val x = if (values.size == 1) horizontalPad + chartWidth / 2f else horizontalPad + chartWidth * index / values.lastIndex
        val y = verticalPad + chartHeight * (1f - (value - minValue) / range)
        Offset(x, y)
    }
}

private fun weightWord(unit: UnitSystem): String = if (unit == UnitSystem.LB) "lb" else "kg"
