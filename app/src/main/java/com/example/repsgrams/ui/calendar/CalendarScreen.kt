package com.example.repsgrams.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import com.example.repsgrams.ui.theme.AppColors
import com.example.repsgrams.ui.components.IconBadge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.data.repository.CalendarDayDetail
import com.example.repsgrams.domain.calendar.CalendarDayStatus
import com.example.repsgrams.data.repository.CalendarMonth
import com.example.repsgrams.data.repository.CalendarSessionDetail
import com.example.repsgrams.ui.components.IosAlertDialog
import com.example.repsgrams.ui.components.IosButton
import com.example.repsgrams.ui.components.IosCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarRoute(viewModel: CalendarViewModel) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val loadingDay by viewModel.loadingDay.collectAsStateWithLifecycle()
    val today = viewModel.today
    val locale = Locale.getDefault()
    
    CalendarScreen(
        month = month,
        selectedDay = selectedDay,
        loadingDay = loadingDay,
        today = today,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToday = viewModel::showToday,
        onSelectDate = viewModel::selectDate,
        onDismissDay = viewModel::closeDay,
        onReschedule = viewModel::rescheduleSelectedAs,
        locale = locale,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    month: CalendarMonth?,
    selectedDay: CalendarDayDetail?,
    loadingDay: Boolean,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onDismissDay: () -> Unit,
    onReschedule: (String) -> Unit,
    locale: Locale,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Calendar", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onToday) {
                        Text("Today", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (month == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onPreviousMonth) { Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary) }
                        Text(
                            month.month.month.getDisplayName(TextStyle.FULL, locale) + " ${month.month.year}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(onClick = onNextMonth) { Icon(Icons.Outlined.ArrowForwardIos, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary) }
                    }
                    
                    IosCard {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                                    Text(
                                        it,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            month.days.chunked(7).forEach { week ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    week.forEach { day ->
                                        DayCell(day, day.date == today, Modifier.weight(1f), onSelectDate)
                                    }
                                }
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBadge(icon = Icons.Outlined.CheckCircle, tint = AppColors.successGreen)
                            Text("Complete", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBadge(icon = Icons.Outlined.ErrorOutline, tint = AppColors.warningRed)
                            Text("Missed", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBadge(icon = Icons.Outlined.Circle, tint = MaterialTheme.colorScheme.outlineVariant)
                            Text("Pending", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (loadingDay && selectedDay == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 8.dp) {
                        CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                    }
                }
            }
            
            selectedDay?.let { detail ->
                ModalBottomSheet(onDismissRequest = onDismissDay, containerColor = MaterialTheme.colorScheme.background) {
                    DayDetail(detail, today, onReschedule)
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    modifier: Modifier,
    onSelectDate: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = when (day.status) {
        CalendarDayStatus.COMPLETE -> AppColors.successGreen.copy(alpha = 0.15f)
        CalendarDayStatus.MISSED -> AppColors.warningRed.copy(alpha = 0.15f)
        CalendarDayStatus.PENDING -> colors.secondaryContainer
        CalendarDayStatus.UPCOMING -> colors.surface
    }
    Surface(
        modifier = modifier.aspectRatio(0.9f).padding(2.dp)
            .alpha(if (day.inDisplayedMonth) 1f else 0.3f)
            .clickable { onSelectDate(day.date) },
        shape = MaterialTheme.shapes.small,
        color = background,
        border = if (isToday) BorderStroke(1.5.dp, colors.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
            Text(
                day.slot.workoutDayLabel ?: "R",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (day.status == CalendarDayStatus.UPCOMING) {
                Text("·", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else {
                Icon(
                    imageVector = when (day.status) {
                        CalendarDayStatus.COMPLETE -> Icons.Outlined.CheckCircle
                        CalendarDayStatus.MISSED -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.Circle
                    },
                    contentDescription = null,
                    tint = when (day.status) {
                        CalendarDayStatus.COMPLETE -> AppColors.successGreen
                        CalendarDayStatus.MISSED -> AppColors.warningRed
                        else -> colors.onSurfaceVariant
                    },
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DayDetail(
    detail: CalendarDayDetail,
    today: LocalDate,
    onReschedule: (String) -> Unit,
) {
    var pendingLabel by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(detail.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
        
        IosCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    detail.slot.workoutDayLabel?.let { "Planned: Workout $it" } ?: "Planned: Rest day",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (detail.date.isAfter(today)) {
                    Text("No entries yet. This is the current plan for this date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (detail.sessions.isEmpty() && detail.slot.isWorkoutDay) {
                    Text("Workout not done", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                detail.sessions.forEach { SessionDetail(it, detail.unitSystem) }
            }
        }
        
        Text("Supplements", style = MaterialTheme.typography.titleMedium)
        IosCard {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBadge(icon = Icons.Outlined.Science, tint = AppColors.creatineTeal)
                    Text("Creatine: ${if (detail.supplements?.creatineTaken == true) "Logged · 5 g" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBadge(icon = Icons.Outlined.FlashlightOn, tint = AppColors.wheyGreen)
                    Text("Whey: ${if (detail.supplements?.wheyTaken == true) "Logged · ${detail.supplements.wheyServings} serving" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        
        Text("Reschedule from here", style = MaterialTheme.typography.titleMedium)
        IosCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Choose the workout this date should represent. Logged history is preserved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IosButton(text = "Workout A", onClick = { pendingLabel = "A" }, modifier = Modifier.weight(1f))
                    IosButton(text = "Workout B", onClick = { pendingLabel = "B" }, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.padding(bottom = 24.dp))
    }
    pendingLabel?.let { label ->
        IosAlertDialog(
            title = "Shift the cycle?",
            message = "${detail.date.format(DateTimeFormatter.ofPattern("MMM d"))} will become Workout $label. Future slots will follow from it.",
            confirmText = "Shift cycle",
            onConfirm = { onReschedule(label); pendingLabel = null },
            dismissText = "Cancel",
            onDismiss = { pendingLabel = null }
        )
    }
}

@Composable
private fun SessionDetail(session: CalendarSessionDetail, unitSystem: UnitSystem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Text(session.workoutName, style = MaterialTheme.typography.titleMedium)
        Text(
            (if (session.completed) "Completed" else "Not completed") +
                (session.durationSeconds?.let { " · ${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: ""),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (session.sets.isEmpty()) Text("No sets logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        session.sets.groupBy { it.exerciseName }.forEach { (exercise, sets) ->
            Text(exercise, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.padding(top = 8.dp))
            sets.forEach { set ->
                val performance = set.reps?.let { "$it reps" }
                    ?: set.durationSeconds?.let { "$it sec" }
                    ?: "Logged"
                val weight = set.weightKg?.let {
                    val shown = if (unitSystem == UnitSystem.LB) it * 2.2046226f else it
                    " · ${"%.1f".format(shown)} ${unitSystem.name.lowercase()}"
                } ?: ""
                Text("Round ${set.roundNumber}: $performance$weight", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
