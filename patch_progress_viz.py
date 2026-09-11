import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

# Imports for new visualizations
imports = """import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
"""
text = text.replace("import androidx.compose.material3.Icon", imports + "import androidx.compose.material3.Icon")

# LineChart replacement
linechart_old = """@Composable
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
}"""
linechart_new = """@Composable
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
}"""
text = text.replace(linechart_old, linechart_new)

# SupplementAdherenceSection replacement
adherence_old = """@Composable
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
}"""
adherence_new = """@Composable
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
}"""
text = text.replace(adherence_old, adherence_new)

# Update LineChart calls
text = text.replace(
    'LineChart(points, modifier = Modifier.fillMaxWidth().height(150.dp))',
    'LineChart(points, modifier = Modifier.fillMaxWidth().height(150.dp), lineColor = AppColors.workout)'
)

text = text.replace(
    'LineChart(state.bodyweightHistory.map { it.second }, modifier = Modifier.fillMaxWidth().height(100.dp))',
    'LineChart(state.bodyweightHistory.map { it.second }, modifier = Modifier.fillMaxWidth().height(100.dp), lineColor = AppColors.progressPurple)'
)

# Empty states
empty_exercise_old = """            if (points.isEmpty()) {
                Text(
                    "No data recorded yet. Keep lifting! 🏋️‍♂️",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {"""
empty_exercise_new = """            if (points.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    LineChart(listOf(0f, 1f, 0.5f, 2f, 1.5f, 3f), modifier = Modifier.fillMaxSize().alpha(0.1f), lineColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Log a few more sessions to see your progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {"""
text = text.replace(empty_exercise_old, empty_exercise_new)

empty_bw_old = """            if (state.bodyweightHistory.isNotEmpty()) {
                LineChart(state.bodyweightHistory.map { it.second }, modifier = Modifier.fillMaxWidth().height(100.dp), lineColor = AppColors.progressPurple)
            } else {
                Text(
                    "Log your weight to start seeing trends.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }"""
empty_bw_new = """            if (state.bodyweightHistory.isNotEmpty()) {
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
            }"""
text = text.replace(empty_bw_old, empty_bw_new)


with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)
