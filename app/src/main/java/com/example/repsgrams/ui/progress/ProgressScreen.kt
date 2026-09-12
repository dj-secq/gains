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
        onBodyweightLogged = { },
        onRestock = { _, _ -> }
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
fun LineChart(
    data: List<Float>,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    lineColor: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.primary
) {
    // Dummy implementation to fix compilation
    androidx.compose.foundation.layout.Box(modifier = modifier)
}
