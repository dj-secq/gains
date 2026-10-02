package com.example.repsgrams.ui.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.repository.CalendarDayDetail
import com.example.repsgrams.data.repository.CalendarMonth
import com.example.repsgrams.data.repository.CalendarSessionDetail
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.calendar.CalendarDayStatus
import com.example.repsgrams.domain.calendar.DayMarkKind
import com.example.repsgrams.domain.calendar.CalendarSheetAction
import com.example.repsgrams.domain.calendar.calendarSheetAction
import com.example.repsgrams.domain.calendar.formatCalendarDate
import com.example.repsgrams.domain.calendar.formatCalendarSet
import com.example.repsgrams.domain.schedule.SuggestionStatus
import com.example.repsgrams.domain.session.formatSessionElapsed
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.DayMark
import com.example.repsgrams.ui.components.InkPill
import com.example.repsgrams.ui.components.OutlinePill
import com.example.repsgrams.ui.components.shimmer
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.SignalRed
import com.example.repsgrams.ui.theme.doneGreen
import com.example.repsgrams.ui.theme.MonoLabelStyle
import com.example.repsgrams.ui.theme.PaperLight
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarRoute(
    viewModel: CalendarViewModel,
    onOpenSession: (Long) -> Unit,
) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val loadingDay by viewModel.loadingDay.collectAsStateWithLifecycle()
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.openSession.collect(onOpenSession) }

    CalendarScreen(
        month = month,
        selectedDay = selectedDay,
        selectedDate = selectedDate,
        loadingDay = loadingDay,
        activeSessionId = activeSessionId,
        today = viewModel.today,
        onPreviousMonth = viewModel::previousMonth,
        onNextMonth = viewModel::nextMonth,
        onThisMonth = viewModel::showToday,
        onSelectDate = viewModel::selectDate,
        onDismissDay = viewModel::closeDay,
        onStart = viewModel::startSuggested,
        onResume = viewModel::resume,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    month: CalendarMonth?,
    selectedDay: CalendarDayDetail?,
    selectedDate: LocalDate?,
    loadingDay: Boolean,
    activeSessionId: Long?,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onDismissDay: () -> Unit,
    onStart: (Long) -> Unit,
    onResume: (Long) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (month == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).clip(MaterialTheme.shapes.medium).shimmer())
                    Box(modifier = Modifier.fillMaxWidth().height(350.dp).clip(MaterialTheme.shapes.medium).shimmer())
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item {
                        MonthHeader(
                            title = month.month.month.getDisplayName(TextStyle.FULL, Locale.US) +
                                " ${month.month.year}",
                            onPreviousMonth = onPreviousMonth,
                            onNextMonth = onNextMonth,
                            onThisMonth = onThisMonth,
                        )
                    }
                    item {
                        Column(Modifier.monthSwipe(onPreviousMonth, onNextMonth).padding(bottom = 8.dp)) {
                            WeekdayHeader()
                            month.days.chunked(7).forEach { week ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    week.forEach { day ->
                                        DayCell(
                                            day = day,
                                            selected = day.date == selectedDate,
                                            modifier = Modifier.weight(1f),
                                            onSelectDate = onSelectDate,
                                        )
                                    }
                                }
                            }
                            MonthLegend()
                        }
                    }
                }
            }

            if (selectedDate != null || loadingDay) {
                ModalBottomSheet(
                    onDismissRequest = onDismissDay,
                    containerColor = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    scrimColor = Color.Black.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                ) {
                    val detail = selectedDay
                    if (loadingDay || detail == null || detail.date != selectedDate) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.fillMaxWidth().height(28.dp).clip(MaterialTheme.shapes.medium).shimmer())
                            Box(Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.medium).shimmer())
                            Box(Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.medium).shimmer())
                        }
                    } else {
                        DayDetail(
                            detail = detail,
                            today = today,
                            activeSessionId = activeSessionId,
                            onStart = onStart,
                            onResume = onResume,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    title: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                OutlinePill(text = "Today", onClick = onThisMonth)
            }
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Next month",
                )
            }
        }
    }
}

private fun Modifier.monthSwipe(onPrevious: () -> Unit, onNext: () -> Unit): Modifier {
    return pointerInput(onPrevious, onNext) {
        var total = 0f
        detectHorizontalDragGestures(
            onDragStart = { total = 0f },
            onHorizontalDrag = { _, amount -> total += amount },
            onDragEnd = {
                val threshold = 72.dp.toPx()
                when {
                    total > threshold -> onPrevious()
                    total < -threshold -> onNext()
                }
            },
        )
    }
}

@Composable
private fun WeekdayHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.SHORT, Locale.US).take(3).uppercase(Locale.US),
                    modifier = Modifier.weight(1f),
                    style = MonoLabelStyle,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun DayCell(
    day: CalendarDay,
    selected: Boolean,
    modifier: Modifier,
    onSelectDate: (LocalDate) -> Unit,
) {
    val dark = LocalDarkTheme.current
    val ring = if (dark) PaperLight else Ink
    BoxWithConstraints(
        modifier = modifier
            .heightIn(min = 52.dp)
            .padding(vertical = 4.dp)
            .alpha(if (day.inDisplayedMonth) 1f else 0.55f)
            .then(if (day.inDisplayedMonth) Modifier.clickable { onSelectDate(day.date) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val markSize = if (maxWidth < 44.dp) maxWidth else 40.dp
        Box(modifier = Modifier.size(markSize), contentAlignment = Alignment.Center) {
            if (selected) {
                Box(Modifier.matchParentSize().border(1.dp, ring, CircleShape))
            }
            DayMark(
                day = day,
                label = day.date.dayOfMonth.toString(),
                size = if (selected) (markSize - 4.dp).coerceAtLeast(0.dp) else markSize,
            )
        }
    }
}

@Composable
private fun MonthLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(DayMarkKind.TRAINED, "Trained")
        LegendItem(DayMarkKind.MISSED, "Missed")
        LegendItem(DayMarkKind.PR, "Record")
        LegendItem(DayMarkKind.PENDING, "Today")
    }
}

@Composable
private fun LegendItem(kind: DayMarkKind, caption: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DayMark(kind = kind, label = "", size = 16.dp)
        Text(caption, style = MonoLabelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayDetail(
    detail: CalendarDayDetail,
    today: LocalDate,
    activeSessionId: Long?,
    onStart: (Long) -> Unit,
    onResume: (Long) -> Unit,
) {
    val action = calendarSheetAction(
        isToday = detail.date == today,
        hasActiveSession = activeSessionId != null,
        completedWorkout = detail.status == CalendarDayStatus.COMPLETE && detail.template != null,
        restDay = detail.liveStatus == SuggestionStatus.REST_DAY,
        hasTemplate = detail.template != null,
    )
    val supplements = detail.supplements.filter { (supplement, _) -> supplement.isActive }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(formatCalendarDate(detail.date), style = MaterialTheme.typography.titleLarge)
        if (detail.sessions.isEmpty()) {
            detail.template?.name?.let { name ->
                Text(name, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "No log",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            detail.sessions.forEach { session ->
                SessionBlock(session, detail.unitSystem)
            }
        }
        supplements.forEach { (supplement, log) ->
            val taken = log?.taken == true
            BoardTile(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        supplement.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (taken) "taken" else "not taken",
                        style = MonoLabelStyle,
                        color = if (taken) doneGreen() else SignalRed,
                    )
                }
            }
        }
        if (action != null) {
            InkPill(
                text = if (action == CalendarSheetAction.START) "START" else "RESUME",
                onClick = {
                    when (action) {
                        CalendarSheetAction.START -> detail.template?.id?.let(onStart)
                        CalendarSheetAction.RESUME -> activeSessionId?.let(onResume)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SessionBlock(session: CalendarSessionDetail, unitSystem: UnitSystem) {
    BoardTile(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                session.workoutName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            session.durationSeconds?.let { seconds ->
                Text(
                    formatSessionElapsed(seconds),
                    style = MonoLabelStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!session.completed) {
            Text(
                "In progress",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        session.notes?.let { notes ->
            Text(notes, style = MaterialTheme.typography.bodyMedium)
        }
        val groups = session.sets.groupBy { it.exerciseName }.entries.toList()
        groups.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                pair.forEach { (exercise, sets) ->
                    DayExerciseColumn(
                        exercise = exercise,
                        lines = sets.mapNotNull { set ->
                            formatCalendarSet(set.reps, set.durationSeconds, set.weightKg, unitSystem)
                        },
                        columns = if (pair.size == 1) 2 else 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun DayExerciseColumn(
    exercise: String,
    lines: List<String>,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            exercise,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        lines.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { line ->
                    Text(
                        line,
                        modifier = Modifier.weight(1f),
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
