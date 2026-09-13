package com.example.repsgrams.ui.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import com.example.repsgrams.ui.theme.AppColors
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.ui.components.IosButton
import com.example.repsgrams.ui.components.IosCard

@Composable
fun ProgressRoute(viewModel: ProgressViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressScreen(
        state = state,
        onExerciseSelected = viewModel::selectExercise,
        onWeightViewToggled = viewModel::setWeightView,
        onBodyweightLogged = viewModel::logBodyweight,
        onRestock = viewModel::restockSupply,
        onSupplementSelected = viewModel::selectSupplement,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onExerciseSelected: (Long) -> Unit,
    onWeightViewToggled: (Boolean) -> Unit,
    onBodyweightLogged: (Float) -> Unit,
    onRestock: (Long, Int) -> Unit,
    onSupplementSelected: (Long) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Progress & Streaks", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                IosCard {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = AppColors.streakAmber, modifier = Modifier.size(20.dp))
                                Text("${state.currentStreak}", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Best Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Star, contentDescription = null, tint = AppColors.streakAmber, modifier = Modifier.size(20.dp))
                                Text("${state.bestStreak}", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                }
            }

            item { ExerciseChartSection(state, onExerciseSelected, onWeightViewToggled) }
            item { BodyweightSection(state, onBodyweightLogged) }
            if (state.supplyInventory.isNotEmpty()) {
                item { SupplySection(state, onRestock) }
            }
            if (state.supplements.any { it.isActive }) {
                item { SupplementAdherenceSection(state, onSupplementSelected) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplementAdherenceSection(state: ProgressUiState, onSelected: (Long) -> Unit) {
    val active = state.supplements.filter { it.isActive }
    val selected = active.firstOrNull { it.id == state.selectedSupplementId } ?: active.firstOrNull()
    val taken = state.supplementAdherence.count { it.second }
    val percentage = if (state.supplementAdherence.isEmpty()) 0 else taken * 100 / state.supplementAdherence.size
    var expanded by remember { mutableStateOf(false) }
    IosCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Supplement adherence", style = MaterialTheme.typography.titleMedium)
                    Text("$percentage% over the last 30 days", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("$taken / ${state.supplementAdherence.size}", fontWeight = FontWeight.SemiBold, color = AppColors.creatineTeal)
            }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selected?.name.orEmpty(), onValueChange = {}, readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    active.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { onSelected(item.id); expanded = false }) }
                }
            }
            val adherenceColor = AppColors.creatineTeal
            Canvas(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (state.supplementAdherence.isEmpty()) return@Canvas
                val gap = size.width / state.supplementAdherence.size
                state.supplementAdherence.forEachIndexed { index, (_, wasTaken) ->
                    drawCircle(
                        color = if (wasTaken) adherenceColor else Color.Gray.copy(alpha = 0.22f),
                        radius = (gap * 0.27f).coerceAtMost(5.dp.toPx()),
                        center = Offset(gap * (index + 0.5f), size.height / 2),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseChartSection(
    state: ProgressUiState,
    onExerciseSelected: (Long) -> Unit,
    onWeightViewToggled: (Boolean) -> Unit
) {
    IosCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Exercise Progress", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            var expanded by remember { mutableStateOf(false) }
            val selectedEx = state.exercises.find { it.id == state.selectedExerciseId }

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedEx?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    state.exercises.forEach { ex ->
                        DropdownMenuItem(
                            text = { Text(ex.name) },
                            onClick = { onExerciseSelected(ex.id); expanded = false }
                        )
                    }
                }
            }

            if (selectedEx?.tracksWeight == true) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Show Weight", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = state.isWeightView,
                        onCheckedChange = onWeightViewToggled,
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

            Spacer(modifier = Modifier.height(16.dp))
            val points = if (state.isWeightView && selectedEx?.tracksWeight == true) {
                state.exerciseHistory.mapNotNull { it.weightKg }
            } else {
                state.exerciseHistory.mapNotNull { it.reps?.toFloat() }
            }

            if (points.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    LineChart(listOf(0f, 1f, 0.5f, 2f, 1.5f, 3f), modifier = Modifier.fillMaxSize().alpha(0.1f), lineColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Log a few more sessions to see your progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LineChart(points, modifier = Modifier.fillMaxWidth().height(150.dp), lineColor = AppColors.workout)
            }
        }
    }
}

@Composable
private fun BodyweightSection(state: ProgressUiState, onLog: (Float) -> Unit) {
    var bwInput by remember { mutableStateOf("") }
    IosCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bodyweight", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = bwInput,
                    onValueChange = { bwInput = it },
                    label = { Text("Today's weight") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IosButton(text = "Log", onClick = { bwInput.toFloatOrNull()?.let { onLog(it); bwInput = "" } }, modifier = Modifier.weight(0.5f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (state.bodyweightHistory.isNotEmpty()) {
                LineChart(state.bodyweightHistory.map { it.second }, modifier = Modifier.fillMaxWidth().height(100.dp), lineColor = AppColors.progressPurple)
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    LineChart(listOf(75f, 74.8f, 74.2f, 74.5f, 73.9f), modifier = Modifier.fillMaxSize().alpha(0.1f), lineColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Log your weight to start seeing trends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


@Composable
private fun SupplySection(state: ProgressUiState, onRestock: (Long, Int) -> Unit) {
    var restockTarget by remember { mutableStateOf<com.example.repsgrams.data.db.SupplyInventoryEntity?>(null) }
    var restockInput by remember { mutableStateOf("") }

    restockTarget?.let { inv ->
        val suppName = state.supplements.find { it.id == inv.supplementId }?.name ?: "Supplement"
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { restockTarget = null },
            title = { Text("Restock $suppName") },
            text = {
                OutlinedTextField(
                    value = restockInput,
                    onValueChange = { if (it.all(Char::isDigit)) restockInput = it },
                    label = { Text("New container size (servings)") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    restockInput.toIntOrNull()?.let { onRestock(inv.supplementId, it) }
                    restockTarget = null
                    restockInput = ""
                }) { Text("Restock") }
            },
            dismissButton = {
                TextButton(onClick = { restockTarget = null }) { Text("Cancel") }
            },
        )
    }

    IosCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Supply Remaining", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            state.supplyInventory.forEachIndexed { index, inv ->
                val supp = state.supplements.find { it.id == inv.supplementId }
                val fraction = (inv.servingsRemaining / inv.totalServings.coerceAtLeast(1)).coerceIn(0f, 1f)
                val isLow = supp != null && inv.servingsRemaining <= supp.lowSupplyThreshold
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                supp?.name ?: "Unknown",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${inv.servingsRemaining.toInt()} / ${inv.totalServings} servings",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)),
                            color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    IosButton(
                        text = "Restock",
                        onClick = { restockTarget = inv; restockInput = "" },
                        isSecondary = true,
                        modifier = Modifier.width(80.dp),
                    )
                }
                if (index < state.supplyInventory.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (data.isEmpty()) return

    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        )
    }

    Canvas(modifier = modifier) {
        val horizontalPadding = 4.dp.toPx()
        val verticalPadding = 8.dp.toPx()
        val chartWidth = (size.width - horizontalPadding * 2).coerceAtLeast(1f)
        val chartHeight = (size.height - verticalPadding * 2).coerceAtLeast(1f)
        val minValue = data.minOrNull() ?: 0f
        val maxValue = data.maxOrNull() ?: minValue
        val range = (maxValue - minValue).takeIf { it > 0f } ?: 1f

        val points = data.mapIndexed { index, value ->
            val x = if (data.size == 1) {
                horizontalPadding + chartWidth / 2f
            } else {
                horizontalPadding + chartWidth * index / data.lastIndex
            }
            val y = verticalPadding + chartHeight * (1f - (value - minValue) / range)
            Offset(x, y)
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.zipWithNext().forEach { (start, end) ->
                val controlX = (start.x + end.x) / 2f
                cubicTo(controlX, start.y, controlX, end.y, end.x, end.y)
            }
        }
        val areaPath = Path().apply {
            moveTo(points.first().x, size.height - verticalPadding)
            lineTo(points.first().x, points.first().y)
            points.zipWithNext().forEach { (start, end) ->
                val controlX = (start.x + end.x) / 2f
                cubicTo(controlX, start.y, controlX, end.y, end.x, end.y)
            }
            lineTo(points.last().x, size.height - verticalPadding)
            close()
        }

        clipRect(right = size.width * drawProgress.value) {
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = verticalPadding,
                    endY = size.height - verticalPadding,
                ),
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.75.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        if (drawProgress.value >= 0.99f) {
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = points.last())
            drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = points.last())
        }
    }
}
