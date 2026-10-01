package com.example.repsgrams.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.calendar.CalendarDayStatus
import com.example.repsgrams.domain.today.DOSE_DELETE
import com.example.repsgrams.domain.today.TodayAlternate
import com.example.repsgrams.domain.today.TodayExerciseLine
import com.example.repsgrams.domain.today.TodayHero
import com.example.repsgrams.domain.today.TodayMark
import com.example.repsgrams.domain.today.TodaySupply
import com.example.repsgrams.domain.today.applyDoseKey
import com.example.repsgrams.domain.today.figures
import com.example.repsgrams.domain.today.formatDose
import com.example.repsgrams.domain.today.formatElapsed
import com.example.repsgrams.domain.today.isSupplementScrollTarget
import com.example.repsgrams.domain.today.parseDose
import com.example.repsgrams.domain.today.restDaysCaption
import com.example.repsgrams.domain.today.restRowLabel
import com.example.repsgrams.domain.today.supplementItemIndex
import com.example.repsgrams.reminder.ReminderNotifications
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.InkPill
import com.example.repsgrams.ui.components.MonoLabel
import com.example.repsgrams.ui.components.StatusDot
import com.example.repsgrams.ui.components.TextAction
import com.example.repsgrams.ui.components.TileTone
import com.example.repsgrams.ui.components.shimmer
import com.example.repsgrams.ui.theme.DisplayNumeral
import com.example.repsgrams.ui.theme.HairlineDark
import com.example.repsgrams.ui.theme.HairlineLight
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.LabelDark
import com.example.repsgrams.ui.theme.LabelLight
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.MonoLabelStyle
import com.example.repsgrams.ui.theme.PaperLight
import com.example.repsgrams.ui.theme.SignalRed
import java.time.Instant
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenSupplements: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.openSession.collect(onOpenSession) }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        TodayScreen(
            state = state,
            onToggleSupplement = viewModel::setSupplementTaken,
            onStartWorkout = viewModel::startWorkout,
            onLogRestDay = viewModel::logRestDay,
            onResumeWorkout = viewModel::resumeWorkout,
            notificationTarget = notificationTarget,
            onNotificationHandled = onNotificationHandled,
            onOpenTemplates = onOpenTemplates,
            onOpenSupplements = onOpenSupplements,
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onToggleSupplement: (com.example.repsgrams.data.db.SupplementEntity, Boolean, Float?) -> Unit,
    onStartWorkout: (String) -> Unit,
    onLogRestDay: () -> Unit,
    onResumeWorkout: (Long) -> Unit,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenSupplements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TodayUiState.Loading -> Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium).shimmer())
            Box(Modifier.fillMaxWidth().height(56.dp).clip(MaterialTheme.shapes.medium).shimmer())
            Box(Modifier.fillMaxWidth().height(120.dp).clip(MaterialTheme.shapes.medium).shimmer())
        }

        is TodayUiState.Error -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                state.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        is TodayUiState.Content -> TodayContent(
            state = state,
            onToggleSupplement = onToggleSupplement,
            onStartWorkout = onStartWorkout,
            onLogRestDay = onLogRestDay,
            onResumeWorkout = onResumeWorkout,
            notificationTarget = notificationTarget,
            onNotificationHandled = onNotificationHandled,
            onOpenTemplates = onOpenTemplates,
            onOpenSupplements = onOpenSupplements,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodayContent(
    state: TodayUiState.Content,
    onToggleSupplement: (com.example.repsgrams.data.db.SupplementEntity, Boolean, Float?) -> Unit,
    onStartWorkout: (String) -> Unit,
    onLogRestDay: () -> Unit,
    onResumeWorkout: (Long) -> Unit,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenSupplements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var showAlternatives by remember { mutableStateOf(false) }
    var doseOverrides by remember { mutableStateOf<Map<Long, Float>>(emptyMap()) }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var draft by remember { mutableStateOf("") }
    val showUpNext = state.upNext.isNotEmpty()
    LaunchedEffect(notificationTarget, showUpNext, state.supplements.isNotEmpty()) {
        val target = notificationTarget ?: return@LaunchedEffect
        if (isSupplementScrollTarget(target) && state.supplements.isNotEmpty()) {
            listState.animateScrollToItem(supplementItemIndex(showUpNext))
        }
        onNotificationHandled()
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hero") {
            HeroTile(
                hero = state.hero,
                startedAt = state.sessionStartedAt,
                onPill = {
                    when (state.hero.pill) {
                        "RESUME" -> state.activeSessionId?.let(onResumeWorkout)
                        "START" -> state.suggestion.suggestedTemplate?.dayLabel?.let(onStartWorkout)
                    }
                },
                onAlternate = {
                    when (state.hero.alternate) {
                        TodayAlternate.SHEET -> showAlternatives = true
                        TodayAlternate.TEMPLATES -> onOpenTemplates()
                        TodayAlternate.NONE -> Unit
                    }
                },
            )
        }
        item(key = "week") { WeekStrip(state.week) }
        if (showUpNext) {
            item(key = "up-next") { UpNextTile(state.upNext) }
        }
        if (state.supplements.isNotEmpty()) {
            item(key = ReminderNotifications.TARGET_SUPPLEMENTS) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.supplements.forEach { row ->
                        val shown = doseOverrides[row.supplement.id] ?: row.actualAmount
                        SupplementRow(
                            row = row,
                            shown = shown,
                            onToggle = {
                                onToggleSupplement(row.supplement, !row.taken, shown)
                                doseOverrides = doseOverrides - row.supplement.id
                            },
                            onEdit = {
                                editingId = row.supplement.id
                                draft = formatDose(shown)
                            },
                        )
                    }
                }
            }
        }
        item(key = "footer") {
            FooterRow(streak = state.currentStreak, supply = state.supply, onOpenSupply = onOpenSupplements)
        }
    }

    val editing = state.supplements.find { it.supplement.id == editingId }
    if (editing != null) {
        BoardDialog(
            title = "Edit dose",
            onDismiss = { editingId = null },
            confirmText = "Save",
            onConfirm = {
                val parsed = parseDose(draft) ?: return@BoardDialog
                if (editing.taken) {
                    onToggleSupplement(editing.supplement, true, parsed)
                    doseOverrides = doseOverrides - editing.supplement.id
                } else {
                    doseOverrides = doseOverrides + (editing.supplement.id to parsed)
                }
                editingId = null
            },
            confirmEnabled = parseDose(draft) != null,
        ) {
            if (draft.isEmpty()) {
                MonoLabel("Dose")
            } else {
                Text(draft, style = DisplayNumeral.copy(fontSize = 40.sp, lineHeight = 44.sp))
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
                            onClick = { draft = applyDoseKey(draft, key) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    if (showAlternatives) {
        ModalBottomSheet(
            onDismissRequest = { showAlternatives = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            scrimColor = Color.Black.copy(alpha = 0.42f),
            shape = MaterialTheme.shapes.extraLarge,
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Something else", style = MaterialTheme.typography.titleLarge)
                state.templates.forEach { template ->
                    BoardTile(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        onClick = {
                            showAlternatives = false
                            onStartWorkout(template.dayLabel)
                        },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                template.dayLabel,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.widthIn(min = 32.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    restDaysCaption(template.restDaysAfter),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                BoardTile(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    onClick = {
                        showAlternatives = false
                        onLogRestDay()
                    },
                ) {
                    Text(
                        restRowLabel(state.suggestion.status),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroTile(hero: TodayHero, startedAt: Instant?, onPill: () -> Unit, onAlternate: () -> Unit) {
    BoardTile(modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp), tone = TileTone.Ink) {
        Column(Modifier.padding(16.dp)) {
            Text(hero.label, style = MonoLabelStyle, color = LabelDark)
            Spacer(Modifier.height(4.dp))
            when (val mark = hero.mark) {
                is TodayMark.Token -> Text(
                    mark.text,
                    style = DisplayNumeral.copy(fontSize = 72.sp, lineHeight = 76.sp),
                )
                is TodayMark.Words -> Text(
                    mark.text,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, lineHeight = 40.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TodayMark.Rest -> Text(
                    "REST",
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 56.sp, lineHeight = 60.sp),
                )
                TodayMark.Elapsed -> ElapsedMark(startedAt)
                TodayMark.None -> Unit
            }
            if (hero.caption != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    hero.caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LabelDark,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val pill = hero.pill
            if (pill != null) {
                Spacer(Modifier.height(12.dp))
                InkPill(text = pill, onClick = onPill, onInk = true)
            }
            val alternate = when (hero.alternate) {
                TodayAlternate.SHEET -> "Something else"
                TodayAlternate.TEMPLATES -> "Templates"
                TodayAlternate.NONE -> null
            }
            if (alternate != null) {
                TextAction(text = alternate, onClick = onAlternate, color = PaperLight)
            }
        }
    }
}

@Composable
private fun ElapsedMark(start: Instant?) {
    var now by remember(start) { mutableStateOf(Instant.now()) }
    LaunchedEffect(start) {
        while (true) {
            now = Instant.now()
            delay(1_000)
        }
    }
    val text = if (start == null) "0:00" else formatElapsed(start, now)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusDot(live = true)
        Text(text, style = DisplayNumeral.copy(fontSize = 56.sp, lineHeight = 60.sp))
    }
}

@Composable
private fun WeekStrip(days: List<CalendarDay>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonoLabel("Week")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day -> WeekMark(day) }
        }
    }
}

@Composable
private fun WeekMark(day: CalendarDay) {
    val dark = LocalDarkTheme.current
    val letter = day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.US).uppercase(Locale.US)
    val workout = day.status == CalendarDayStatus.COMPLETE && day.template != null
    val missed = day.status == CalendarDayStatus.MISSED
    val pending = day.status == CalendarDayStatus.PENDING
    val fill = when {
        workout && day.hasPr -> SignalRed
        workout || pending -> if (dark) PaperLight else Ink
        else -> Color.Transparent
    }
    val letterColor = when {
        workout && day.hasPr -> PaperLight
        workout || pending -> if (dark) Ink else PaperLight
        else -> if (dark) LabelDark else LabelLight
    }
    val hairline = if (dark) HairlineDark else HairlineLight
    Box(
        modifier = Modifier
            .size(40.dp)
            .then(if (missed) Modifier.border(1.dp, hairline, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(letter, style = MonoLabelStyle, color = letterColor)
    }
}

@Composable
private fun UpNextTile(lines: List<TodayExerciseLine>) {
    BoardTile(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            MonoLabel("Up next")
            lines.forEachIndexed { index, line ->
                if (line.supersetWithPrevious) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                } else if (index > 0) {
                    Spacer(Modifier.height(8.dp))
                } else {
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, style = MaterialTheme.typography.bodyLarge)
                        if (line.perSide) MonoLabel("Per side")
                    }
                    Text(
                        line.figures(),
                        style = MonoLabelStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SupplementRow(
    row: TodaySupplement,
    shown: Float,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
) {
    val ink = row.taken
    BoardTile(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        tone = if (ink) TileTone.Ink else TileTone.Paper,
        onClick = onToggle,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.supplement.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDose(shown), style = MonoLabelStyle.copy(fontSize = 15.sp, lineHeight = 18.sp))
                    Text(
                        row.supplement.unit.uppercase(Locale.US),
                        style = MonoLabelStyle,
                        color = if (ink) LabelDark else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextAction(
                text = "Edit dose",
                onClick = onEdit,
                color = if (ink) PaperLight else null,
            )
        }
    }
}

@Composable
private fun FooterRow(streak: Int, supply: TodaySupply?, onOpenSupply: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BoardTile(modifier = if (supply == null) Modifier.fillMaxWidth(0.4f) else Modifier.weight(2f)) {
            Column(Modifier.padding(16.dp)) {
                Text(streak.toString(), style = DisplayNumeral, maxLines = 1)
                MonoLabel("Streak")
                MonoLabel("Sessions")
            }
        }
        if (supply != null) {
            BoardTile(modifier = Modifier.weight(3f), onClick = onOpenSupply) {
                Column(Modifier.padding(16.dp)) {
                    Text(supply.servings.toString(), style = DisplayNumeral, maxLines = 1)
                    Text(
                        supply.name.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MonoLabel("Left")
                }
            }
        }
    }
}
