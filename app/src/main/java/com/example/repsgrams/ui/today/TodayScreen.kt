package com.example.repsgrams.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repsgrams.ui.components.IosCard
import com.example.repsgrams.ui.components.IosButton
import com.example.repsgrams.ui.components.shimmer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.ui.theme.iosSpring
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.openSession.collect(onOpenSession) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Today", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        TodayScreen(
            state = state,
            onToggleSupplement = viewModel::setSupplementTaken,
            onStartWorkout = viewModel::startWorkout,
            onLogRestDay = viewModel::logRestDay,
            onResumeWorkout = viewModel::resumeWorkout,
            notificationTarget = notificationTarget,
            onNotificationHandled = onNotificationHandled,
            modifier = Modifier.padding(padding)
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
    modifier: Modifier = Modifier,
) {
    when (state) {
        TodayUiState.Loading -> Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(190.dp).clip(MaterialTheme.shapes.medium).shimmer())
            Box(Modifier.fillMaxWidth().height(150.dp).clip(MaterialTheme.shapes.medium).shimmer())
            Box(Modifier.fillMaxWidth().height(96.dp).clip(MaterialTheme.shapes.medium).shimmer())
        }

        is TodayUiState.Error -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

        is TodayUiState.Content -> TodayContent(
            state, onToggleSupplement, onStartWorkout, onLogRestDay, onResumeWorkout,
            notificationTarget, onNotificationHandled, modifier,
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
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var showAlternatives by remember { mutableStateOf(false) }
    LaunchedEffect(notificationTarget) {
        if (notificationTarget != null) {
            if (notificationTarget == "creatine" || notificationTarget == "whey") {
                listState.animateScrollToItem(1) // Approximate
            }
            onNotificationHandled()
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.lowSupplyWarnings.isNotEmpty()) {
            item {
                IosCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        state.lowSupplyWarnings.forEach { warning ->
                            Text(warning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        item {
            IosCard {
                Box(
                    modifier = Modifier.background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                com.example.repsgrams.ui.theme.AppColors.workout.copy(alpha = 0.1f),
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            when (state.suggestion.status) {
                                com.example.repsgrams.domain.schedule.SuggestionStatus.NO_HISTORY -> "Choose where to begin"
                                com.example.repsgrams.domain.schedule.SuggestionStatus.REST_DAY -> "Recovery day"
                                com.example.repsgrams.domain.schedule.SuggestionStatus.ON_TIME -> "Suggested today"
                                com.example.repsgrams.domain.schedule.SuggestionStatus.OVERDUE -> "Suggested next"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (state.suggestion.status == com.example.repsgrams.domain.schedule.SuggestionStatus.REST_DAY) {
                                "Rest day"
                            } else {
                                state.suggestion.suggestedTemplate?.name ?: "Choose a workout"
                            },
                            style = MaterialTheme.typography.titleLarge,
                        )
                        state.suggestion.suggestedTemplate?.let {
                            Spacer(Modifier.height(8.dp))
                            com.example.repsgrams.ui.components.CategoryChip(it.category)
                        }
                        val scheduleMessage = when (state.suggestion.status) {
                            com.example.repsgrams.domain.schedule.SuggestionStatus.REST_DAY ->
                                "${state.suggestion.daysUntilDue} day${if (state.suggestion.daysUntilDue == 1) "" else "s"} until ${state.suggestion.suggestedTemplate?.name ?: "your next workout"}"
                            com.example.repsgrams.domain.schedule.SuggestionStatus.OVERDUE ->
                                "Due ${state.suggestion.overdueByDays} day${if (state.suggestion.overdueByDays == 1) "" else "s"} ago"
                            else -> null
                        }
                        scheduleMessage?.let {
                            Spacer(Modifier.height(4.dp))
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(16.dp))
                        if (state.activeSessionId != null) {
                            IosButton(
                                text = "Resume Workout",
                                onClick = { onResumeWorkout(state.activeSessionId) }
                            )
                        } else if ((state.suggestion.status != com.example.repsgrams.domain.schedule.SuggestionStatus.REST_DAY)) {
                            IosButton(
                                text = "Start Workout ${state.suggestion.suggestedTemplate?.dayLabel}",
                                onClick = { onStartWorkout(requireNotNull(state.suggestion.suggestedTemplate?.dayLabel)) }
                            )
                        } else {
                            Text(
                                "Recover and keep your daily creatine routine.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.activeSessionId == null) {
                            TextButton(
                                onClick = { showAlternatives = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            ) { Text("Do something else", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }
        item { SupplementCard(state, onToggleSupplement) }
        item {
            IosCard {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        com.example.repsgrams.ui.theme.AppColors.streakAmber,
                                        com.example.repsgrams.ui.theme.AppColors.streakAmber.copy(alpha = 0.7f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            if (state.currentStreak > 0) androidx.compose.material.icons.Icons.Rounded.LocalFireDepartment else androidx.compose.material.icons.Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column {
                        Text("Current streak", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("${state.currentStreak} day${if (state.currentStreak != 1) "s" else ""}", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }
    }

    if (showAlternatives) {
        ModalBottomSheet(
            onDismissRequest = { showAlternatives = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            scrimColor = Color.Black.copy(alpha = 0.42f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Choose today’s activity", style = MaterialTheme.typography.titleLarge)
                state.templates.forEach { template ->
                    IosButton(
                        text = template.name,
                        onClick = {
                            showAlternatives = false
                            onStartWorkout(template.dayLabel)
                        },
                        isSecondary = template.id != state.suggestion.suggestedTemplate?.id,
                    )
                }
                IosButton(
                    text = "Rest today",
                    onClick = {
                        showAlternatives = false
                        onLogRestDay()
                    },
                    isSecondary = true,
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SupplementCard(
    state: TodayUiState.Content,
    onToggleSupplement: (com.example.repsgrams.data.db.SupplementEntity, Boolean, Float?) -> Unit,
) {
    com.example.repsgrams.ui.components.IosCard {
        Column {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Supplements today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            if (state.supplements.isEmpty()) {
                Text("No supplements scheduled today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
            state.supplements.forEachIndexed { index, suppState ->
                val (supp, taken, actualAmount) = suppState
                var amountText by remember(supp.id, actualAmount) { mutableStateOf(if (actualAmount % 1f == 0f) actualAmount.toInt().toString() else actualAmount.toString()) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.example.repsgrams.ui.components.IconBadge(
                        icon = when (supp.iconName.lowercase()) {
                            "water_drop" -> androidx.compose.material.icons.Icons.Outlined.WaterDrop
                            "bolt" -> androidx.compose.material.icons.Icons.Outlined.FlashlightOn
                            else -> androidx.compose.material.icons.Icons.Outlined.Science
                        },
                        tint = when (supp.colorToken.lowercase()) {
                            "orange" -> com.example.repsgrams.ui.theme.AppColors.workout
                            "teal", "creatineteal" -> com.example.repsgrams.ui.theme.AppColors.creatineTeal
                            "green", "wheygreen" -> com.example.repsgrams.ui.theme.AppColors.wheyGreen
                            "purple" -> com.example.repsgrams.ui.theme.AppColors.progressPurple
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(supp.name, style = MaterialTheme.typography.bodyLarge)
                        Text("Planned ${supp.doseAmount} ${supp.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        modifier = Modifier.width(84.dp),
                        label = { Text(supp.unit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    Switch(
                        checked = taken,
                        onCheckedChange = { checked -> onToggleSupplement(supp, checked, amountText.toFloatOrNull()) }
                    )
                }
                if (index < state.supplements.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}
