package com.example.repsgrams.ui.calendar
import com.example.repsgrams.ui.components.shimmer
import androidx.compose.ui.draw.clip

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
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.MonoLabel
import com.example.repsgrams.ui.components.TextAction
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.data.repository.CalendarDayDetail
import com.example.repsgrams.domain.calendar.CalendarDayStatus
import com.example.repsgrams.domain.schedule.SuggestionStatus
import com.example.repsgrams.data.repository.CalendarMonth
import com.example.repsgrams.data.repository.CalendarSessionDetail
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
    val locale = LocalConfiguration.current.locales[0]

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

    locale: Locale,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (month == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(MaterialTheme.shapes.medium).shimmer())
                    Box(modifier = Modifier.fillMaxWidth().height(350.dp).clip(MaterialTheme.shapes.medium).shimmer())
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = onPreviousMonth) { Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    month.month.month.getDisplayName(TextStyle.FULL, locale) + " ${month.month.year}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center,
                                )
                                TextAction("Today", onClick = onToday)
                            }
                            IconButton(onClick = onNextMonth) { Icon(Icons.Outlined.ArrowForwardIos, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }

                    item {
                        BoardTile(modifier = Modifier.fillMaxWidth()) {
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
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(14.dp))
                                Text("Complete", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Text("Missed", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Circle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Text("Pending", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }



            if (selectedDay != null || loadingDay) {
                ModalBottomSheet(
                    onDismissRequest = onDismissDay,
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrimColor = Color.Black.copy(alpha = 0.42f),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    if (loadingDay) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(Modifier.fillMaxWidth().height(28.dp).clip(MaterialTheme.shapes.medium).shimmer())
                            Box(Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.medium).shimmer())
                            Box(Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.medium).shimmer())
                        }
                    } else {
                        selectedDay?.let { DayDetail(it, today) }
                    }
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
        CalendarDayStatus.COMPLETE -> colors.onSurface.copy(alpha = 0.08f)
        else -> colors.surface
    }
    Surface(
        modifier = modifier.aspectRatio(0.9f).padding(2.dp)
            .alpha(if (day.inDisplayedMonth) 1f else 0.3f)
            .clickable { onSelectDate(day.date) },
        shape = MaterialTheme.shapes.small,
        color = background,
        border = when {
            isToday -> BorderStroke(1.5.dp, colors.onSurface)
            day.status == CalendarDayStatus.MISSED -> BorderStroke(1.dp, colors.outline)
            else -> null
        },
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
            if (day.template != null) {
                Text(
                    day.template.dayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                )
            } else if (day.status == CalendarDayStatus.COMPLETE) {
                Icon(
                    imageVector = Icons.Outlined.Coffee,
                    contentDescription = "Rest",
                    modifier = Modifier.size(20.dp),
                    tint = colors.onSurfaceVariant
                )
            } else {
                // An empty day is not a missed rest.
                Spacer(Modifier.size(20.dp))
            }
            if (day.status == CalendarDayStatus.UPCOMING) {
                Text("·", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else if (day.status == CalendarDayStatus.EMPTY) {
                Spacer(Modifier.size(14.dp))
            } else {
                Icon(
                    imageVector = when (day.status) {
                        CalendarDayStatus.COMPLETE -> Icons.Filled.CheckCircle
                        CalendarDayStatus.MISSED -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.Circle
                    },
                    contentDescription = null,
                    tint = when (day.status) {
                        CalendarDayStatus.COMPLETE -> colors.onSurface
                        else -> colors.onSurfaceVariant
                    },
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun plannedTitle(detail: CalendarDayDetail, today: LocalDate): String {
    detail.template?.dayLabel?.let { return "Planned: Workout $it" }
    val due = detail.liveDueDate
    val liveRestGap = detail.liveStatus == SuggestionStatus.REST_DAY &&
        !detail.date.isBefore(today) &&
        due != null &&
        detail.date.isBefore(due)
    // Template is null here. COMPLETE is a logged rest, the same signal as the coffee icon.
    val restLogged = detail.status == CalendarDayStatus.COMPLETE
    return if (restLogged || liveRestGap) "Planned: Rest day" else "No log"
}

@Composable
private fun DayDetail(
    detail: CalendarDayDetail,
    today: LocalDate,
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(detail.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))

        BoardTile(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(plannedTitle(detail, today), style = MaterialTheme.typography.titleMedium)
                detail.template?.let { MonoLabel(it.category) }
                if (detail.date.isAfter(today) && detail.projected) {
                    Text("No entries yet. This is the current plan for this date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else if (detail.status == CalendarDayStatus.MISSED) {
                    Text("Workout not done", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                detail.sessions.forEach { SessionDetail(it, detail.unitSystem) }
            }
        }

        Text("Supplements", style = MaterialTheme.typography.titleMedium)
        BoardTile(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                detail.supplements.forEachIndexed { index, pair ->
                    val (supp, log) = pair
                    val taken = log?.taken == true
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Normally we'd look up icon and color from token here.
                        // For simplicity since the app relies on semantic tokens, we just map it.
                        Icon(Icons.Outlined.Science, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                        Text("${supp.name}: ${if (taken) "Logged · ${log?.actualAmount} ${supp.unit}" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (index < detail.supplements.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                if (detail.supplements.isEmpty()) {
                    Text("No active supplements", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.padding(bottom = 24.dp))
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
        session.notes?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodyMedium)
        }
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
