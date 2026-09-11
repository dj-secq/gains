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
private fun SupplementAdherenceSection(state: ProgressUiState) {
    IosCard {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Creatine Adherence", style = MaterialTheme.typography.titleMedium)
            
            Row(modifier = Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                BarWithLabel("30 Days", state.creatineAdherence30d, AppColors.creatineTeal, 0)
                BarWithLabel("90 Days", state.creatineAdherence90d, AppColors.creatineTeal, 100)
            }
            
            if (state.proteinEstimate != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Text("Protein Target (Whey + Diet)", style = MaterialTheme.typography.titleMedium)
                
                val avgTarget = (state.proteinEstimate.targetLow + state.proteinEstimate.targetHigh) / 2
                val progress = if (avgTarget > 0) (state.proteinEstimate.wheyContributionThisWeek / avgTarget).coerceIn(0f, 1f) else 0f
                
                ProgressRing(
                    progress = progress,
                    color = AppColors.wheyGreen,
                    centerText = "~${state.proteinEstimate.wheyContributionThisWeek.toInt()}g",
                    centerSubText = "of ${avgTarget.toInt()}g goal",
                    modifier = Modifier.fillMaxWidth().height(160.dp).padding(vertical = 8.dp)
                )
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
fun LineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (points.isEmpty()) return
    
    var animationProgress by remember(points) { mutableStateOf(0f) }
    LaunchedEffect(points) {
        androidx.compose.animation.core.animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(durationMillis = 700, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        ) { value, _ -> animationProgress = value }
    }
    
    val transparentColor = lineColor.copy(alpha = 0f)
    val fillGradient = Brush.verticalGradient(
        colors = listOf(lineColor.copy(alpha = 0.25f), transparentColor)
    )
    
    Canvas(modifier = modifier) {
        val max = points.maxOrNull() ?: 1f
        val min = points.minOrNull() ?: 0f
        val range = if (max == min) 1f else max - min
        
        val width = size.width
        val height = size.height
        val stepX = if (points.size > 1) width / (points.size - 1) else width
        
        val path = Path()
        val fillPath = Path()
        
        val plottedPoints = points.mapIndexed { index, value ->
            val normalizedY = height - ((value - min) / range * height)
            Offset(index * stepX, normalizedY)
        }
        
        if (plottedPoints.isNotEmpty()) {
            path.moveTo(plottedPoints.first().x, plottedPoints.first().y)
            fillPath.moveTo(plottedPoints.first().x, height)
            fillPath.lineTo(plottedPoints.first().x, plottedPoints.first().y)
            
            for (i in 1 until plottedPoints.size) {
                val current = plottedPoints[i]
                val prev = plottedPoints[i - 1]
                val controlPointX = (prev.x + current.x) / 2
                path.cubicTo(
                    controlPointX, prev.y,
                    controlPointX, current.y,
                    current.x, current.y
                )
                fillPath.cubicTo(
                    controlPointX, prev.y,
                    controlPointX, current.y,
                    current.x, current.y
                )
            }
            fillPath.lineTo(plottedPoints.last().x, height)
            fillPath.close()
            
            clipRect(right = width * animationProgress) {
                drawPath(
                    path = fillPath,
                    brush = fillGradient
                )
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5f.dp.toPx(), cap = StrokeCap.Round)
                )
                
                val lastPoint = plottedPoints.last()
                if (animationProgress >= 0.99f) {
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = lastPoint)
                }
            }
        }
    }
}

@Composable
fun BarWithLabel(label: String, value: Float, color: Color, delayMs: Int) {
    var anim by remember(value) { mutableStateOf(0f) }
    LaunchedEffect(value) {
        delay(delayMs.toLong())
        androidx.compose.animation.core.animate(0f, value, animationSpec = androidx.compose.animation.core.tween(500)) { v, _ -> anim = v }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(anim.coerceAtLeast(0.02f))
                    .width(32.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("${(value * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    centerText: String,
    centerSubText: String,
    modifier: Modifier = Modifier
) {
    var anim by remember(progress) { mutableStateOf(0f) }
    LaunchedEffect(progress) {
        androidx.compose.animation.core.animate(0f, progress, animationSpec = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { v, _ -> anim = v }
    }
    
    val trackColor = color.copy(alpha = 0.2f)
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val padding = 12.dp.toPx() / 2
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
                topLeft = Offset(padding, padding),
                size = androidx.compose.ui.geometry.Size(size.width - padding * 2, size.height - padding * 2)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = anim * 360f,
                useCenter = false,
                style = stroke,
                topLeft = Offset(padding, padding),
                size = androidx.compose.ui.geometry.Size(size.width - padding * 2, size.height - padding * 2)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(centerSubText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
