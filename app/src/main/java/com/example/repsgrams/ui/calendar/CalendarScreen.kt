package com.example.repsgrams.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.repository.CalendarDayDetail
import com.example.repsgrams.data.repository.CalendarMonth
import com.example.repsgrams.data.repository.CalendarSessionDetail
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.calendar.CalendarDayStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@Composable
fun CalendarRoute(viewModel: CalendarViewModel) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val loadingDay by viewModel.loadingDay.collectAsStateWithLifecycle()
    CalendarScreen(
        month = month,
        selectedDay = selectedDay,
        loadingDay = loadingDay,
        today = viewModel.today,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onToday = viewModel::showToday,
        onSelectDate = viewModel::selectDate,
        onDismissDay = viewModel::closeDay,
        onReschedule = viewModel::rescheduleSelectedAs,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarScreen(
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
) {
    val locale = LocalConfiguration.current.locales[0]
    if (month == null) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPreviousMonth) { Text("‹") }
            Text(
                month.month.month.getDisplayName(TextStyle.FULL, locale) +
                    " ${month.month.year}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            TextButton(onClick = onNextMonth) { Text("›") }
        }
        TextButton(onClick = onToday, modifier = Modifier.align(Alignment.End)) { Text("Today") }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                Text(
                    it,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Text("✓ Complete", style = MaterialTheme.typography.labelSmall)
            Text("! Missed", style = MaterialTheme.typography.labelSmall)
            Text("○ Pending", style = MaterialTheme.typography.labelSmall)
        }
    }

    if (loadingDay && selectedDay == null) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } },
        )
    }
    selectedDay?.let { detail ->
        ModalBottomSheet(onDismissRequest = onDismissDay) {
            DayDetail(detail, today, onReschedule)
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
        CalendarDayStatus.COMPLETE -> colors.primaryContainer
        CalendarDayStatus.MISSED -> colors.errorContainer.copy(alpha = 0.55f)
        CalendarDayStatus.PENDING -> colors.secondaryContainer
        CalendarDayStatus.UPCOMING -> colors.surface
    }
    Surface(
        modifier = modifier.aspectRatio(0.9f).padding(2.dp)
            .alpha(if (day.inDisplayedMonth) 1f else 0.4f)
            .clickable { onSelectDate(day.date) },
        shape = MaterialTheme.shapes.small,
        color = background,
        border = if (isToday) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
            Text(
                day.slot.workoutDayLabel ?: "R",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                when (day.status) {
                    CalendarDayStatus.COMPLETE -> "✓"
                    CalendarDayStatus.MISSED -> "!"
                    CalendarDayStatus.PENDING -> "○"
                    CalendarDayStatus.UPCOMING -> "·"
                },
                color = when (day.status) {
                    CalendarDayStatus.MISSED -> colors.error
                    CalendarDayStatus.COMPLETE -> colors.primary
                    else -> Color.Unspecified
                },
            )
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
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(detail.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")), style = MaterialTheme.typography.headlineSmall)
        Text(
            detail.slot.workoutDayLabel?.let { "Planned: Workout $it" } ?: "Planned: Rest day",
            style = MaterialTheme.typography.titleMedium,
        )
        if (detail.date.isAfter(today)) {
            Text("No entries yet. This is the current plan for this date.")
        } else if (detail.sessions.isEmpty() && detail.slot.isWorkoutDay) {
            Text("Workout not done", color = MaterialTheme.colorScheme.error)
        }
        detail.sessions.forEach { SessionDetail(it, detail.unitSystem) }
        HorizontalDivider()
        Text("Supplements", style = MaterialTheme.typography.titleMedium)
        Text("Creatine: ${if (detail.supplements?.creatineTaken == true) "Logged · 5 g" else "Not logged"}")
        Text("Whey: ${if (detail.supplements?.wheyTaken == true) "Logged · ${detail.supplements.wheyServings} serving" else "Not logged"}")
        HorizontalDivider()
        Text("Reschedule from here", style = MaterialTheme.typography.titleMedium)
        Text("Choose the workout this date should represent. Logged history is preserved.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { pendingLabel = "A" }) { Text("Workout A") }
            Button(onClick = { pendingLabel = "B" }) { Text("Workout B") }
        }
        Spacer(Modifier.padding(bottom = 12.dp))
    }
    pendingLabel?.let { label ->
        AlertDialog(
            onDismissRequest = { pendingLabel = null },
            title = { Text("Shift the cycle?") },
            text = { Text("${detail.date.format(DateTimeFormatter.ofPattern("MMM d"))} will become Workout $label. Future slots will follow from it.") },
            confirmButton = {
                TextButton(onClick = { onReschedule(label); pendingLabel = null }) { Text("Shift cycle") }
            },
            dismissButton = { TextButton(onClick = { pendingLabel = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SessionDetail(session: CalendarSessionDetail, unitSystem: UnitSystem) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(session.workoutName, style = MaterialTheme.typography.titleMedium)
        Text(
            (if (session.completed) "Completed" else "Not completed") +
                (session.durationSeconds?.let { " · ${it / 60}:${(it % 60).toString().padStart(2, '0')}" } ?: ""),
        )
        if (session.sets.isEmpty()) Text("No sets logged", style = MaterialTheme.typography.bodySmall)
        session.sets.groupBy { it.exerciseName }.forEach { (exercise, sets) ->
            Text(exercise, fontWeight = FontWeight.SemiBold)
            sets.forEach { set ->
                val performance = set.reps?.let { "$it reps" }
                    ?: set.durationSeconds?.let { "$it sec" }
                    ?: "Logged"
                val weight = set.weightKg?.let {
                    val shown = if (unitSystem == UnitSystem.LB) it * 2.2046226f else it
                    " · ${"%.1f".format(shown)} ${unitSystem.name.lowercase()}"
                } ?: ""
                Text("Round ${set.roundNumber}: $performance$weight", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
