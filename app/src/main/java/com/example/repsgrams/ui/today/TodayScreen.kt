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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.repsgrams.ui.components.IosCard
import com.example.repsgrams.ui.components.IosButton
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
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

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
            
            
            onStartWorkout = viewModel::startWorkout,
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
    
    
    onStartWorkout: (String) -> Unit,
    onResumeWorkout: (Long) -> Unit,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        TodayUiState.Loading -> Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        is TodayUiState.Error -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

        is TodayUiState.Content -> TodayContent(
            state,   onStartWorkout, onResumeWorkout,
            notificationTarget, onNotificationHandled, modifier,
        )
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState.Content,
    
    
    onStartWorkout: (String) -> Unit,
    onResumeWorkout: (Long) -> Unit,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
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
                        Text("Day ${1} of 5", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.suggestion.suggestedTemplate?.dayLabel?.let { "Workout $it" } ?: "Rest day",
                            style = MaterialTheme.typography.titleLarge,
                        )
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
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onStartWorkout("A") }) { Text("Workout A", style = MaterialTheme.typography.bodyMedium) }
                                TextButton(onClick = { onStartWorkout("B") }) { Text("Workout B", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    }
                }
            }
        }
        item { SupplementCard(state) }
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
}

@Composable
private fun SupplementCard(state: TodayUiState.Content, ) {
    com.example.repsgrams.ui.components.IosCard {
        Column {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Supplements today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            if (state.supplements.isEmpty()) {
                Text("No supplements scheduled today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
            state.supplements.forEachIndexed { index, suppState ->
                val (supp, taken) = suppState
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.example.repsgrams.ui.components.IconBadge(
                        icon = androidx.compose.material.icons.Icons.Outlined.Science, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(supp.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${supp.doseAmount} ${supp.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = taken,
                        onCheckedChange = { }
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
