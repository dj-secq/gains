package com.example.repsgrams.ui.progress

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
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.SupplyType
import com.example.repsgrams.domain.progress.SupplyStatus
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
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onExerciseSelected: (Long) -> Unit,
    onWeightViewToggled: (Boolean) -> Unit,
    onBodyweightLogged: (Float) -> Unit,
    onRestock: (SupplyType, Int) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
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
            item { SupplementAdherenceSection(state) }
            item { SupplySection(state, onRestock) }
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
                Text(
                    "No data recorded yet. Keep lifting! 🏋️‍♂️",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LineChart(points, modifier = Modifier.fillMaxWidth().height(150.dp))
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
                LineChart(state.bodyweightHistory.map { it.second }, modifier = Modifier.fillMaxWidth().height(100.dp))
            } else {
                Text(
                    "Log your weight to start seeing trends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SupplementAdherenceSection(state: ProgressUiState) {
    IosCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Creatine Adherence", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("30 Days", style = MaterialTheme.typography.bodyMedium)
                Text("${(state.creatineAdherence30d * 100).toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("90 Days", style = MaterialTheme.typography.bodyMedium)
                Text("${(state.creatineAdherence90d * 100).toInt()}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            
            if (state.proteinEstimate != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("Protein Target (Whey + Diet)", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Goal", style = MaterialTheme.typography.bodyMedium)
                    Text("${state.proteinEstimate.targetLow.toInt()} - ${state.proteinEstimate.targetHigh.toInt()}g / day", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Whey this week", style = MaterialTheme.typography.bodyMedium)
                    Text("~${state.proteinEstimate.wheyContributionThisWeek.toInt()}g", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
private fun SupplySection(state: ProgressUiState, onRestock: (SupplyType, Int) -> Unit) {
    IosCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Supplies", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            SupplyItem(
                name = "Whey",
                status = state.wheyStatus,
                total = state.wheyInventory?.totalServings ?: 65,
                onRestock = { onRestock(SupplyType.WHEY, state.wheyInventory?.totalServings ?: 65) }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SupplyItem(
                name = "Creatine",
                status = state.creatineStatus,
                total = state.creatineInventory?.totalServings ?: 30,
                onRestock = { onRestock(SupplyType.CREATINE, state.creatineInventory?.totalServings ?: 30) }
            )
        }
    }
}

@Composable
private fun SupplyItem(name: String, status: SupplyStatus?, total: Int, onRestock: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            val remain = status?.remaining?.toInt() ?: 0
            Text("$remain / $total remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (status?.estimatedRunOutDate != null) {
                Text("Runs out around ${status.estimatedRunOutDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        IosButton(text = "Restock", onClick = onRestock, isSecondary = true, modifier = Modifier.weight(0.5f))
    }
}

@Composable
fun LineChart(points: List<Float>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) return
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val max = points.maxOrNull() ?: 1f
        val min = points.minOrNull() ?: 0f
        val range = if (max == min) 1f else max - min
        
        val width = size.width
        val height = size.height
        val stepX = if (points.size > 1) width / (points.size - 1) else width
        
        var prevPoint: Offset? = null
        points.forEachIndexed { index, value ->
            val normalizedY = height - ((value - min) / range * height)
            val currentPoint = Offset(index * stepX, normalizedY)
            drawCircle(color = color, radius = 4.dp.toPx(), center = currentPoint)
            if (prevPoint != null) {
                drawLine(color = color, start = prevPoint!!, end = currentPoint, strokeWidth = 2.dp.toPx())
            }
            prevPoint = currentPoint
        }
    }
}
