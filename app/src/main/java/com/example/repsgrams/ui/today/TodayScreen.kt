package com.example.repsgrams.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.openSession.collect(onOpenSession) }
    TodayScreen(
        state = state,
        onCreatineChanged = viewModel::setCreatineTaken,
        onWheyChanged = viewModel::setWheyTaken,
        onStartWorkout = viewModel::startWorkout,
        onResumeWorkout = viewModel::resumeWorkout,
        notificationTarget = notificationTarget,
        onNotificationHandled = onNotificationHandled,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onCreatineChanged: (Boolean) -> Unit,
    onWheyChanged: (Boolean) -> Unit,
    onStartWorkout: (String) -> Unit,
    onResumeWorkout: (Long) -> Unit,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
) {
    when (state) {
        TodayUiState.Loading -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { CircularProgressIndicator() }

        is TodayUiState.Error -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

        is TodayUiState.Content -> TodayContent(
            state, onCreatineChanged, onWheyChanged, onStartWorkout, onResumeWorkout,
            notificationTarget, onNotificationHandled,
        )
    }
}

@Composable
private fun TodayContent(
    state: TodayUiState.Content,
    onCreatineChanged: (Boolean) -> Unit,
    onWheyChanged: (Boolean) -> Unit,
    onStartWorkout: (String) -> Unit,
    onResumeWorkout: (Long) -> Unit,
    notificationTarget: String?,
    onNotificationHandled: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(notificationTarget) {
        if (notificationTarget != null) {
            if (notificationTarget == "creatine" || notificationTarget == "whey") {
                listState.animateScrollToItem(2)
            }
            onNotificationHandled()
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Today", style = MaterialTheme.typography.headlineLarge) }
        if (state.lowSupplyWarnings.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        state.lowSupplyWarnings.forEach { warning ->
                            Text(warning, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Day ${state.slot.dayNumber} of 5", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.slot.workoutDayLabel?.let { "Workout $it" } ?: "Rest day",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    if (state.activeSessionId != null) {
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { onResumeWorkout(state.activeSessionId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Resume Workout")
                        }
                    } else if (state.slot.isWorkoutDay) {
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { onStartWorkout(requireNotNull(state.slot.workoutDayLabel)) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Start Workout ${state.slot.workoutDayLabel}") }
                    } else {
                        Text(
                            "Recover and keep your daily creatine routine.",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (state.activeSessionId == null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onStartWorkout("A") }) { Text("Workout A") }
                            TextButton(onClick = { onStartWorkout("B") }) { Text("Workout B") }
                        }
                    }
                }
            }
        }
        item { SupplementCard(state, onCreatineChanged, onWheyChanged) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Current streak", style = MaterialTheme.typography.titleMedium)
                    Text("🔥 ${state.currentStreak} day${if (state.currentStreak != 1) "s" else ""}", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}

@Composable
private fun SupplementCard(
    state: TodayUiState.Content,
    onCreatineChanged: (Boolean) -> Unit,
    onWheyChanged: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text("Supplements today", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (state.creatineTaken && state.wheyTaken) "All logged" else "Tap a row to update",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            SupplementRow("Creatine", "5 g · daily", state.creatineTaken, onCreatineChanged)
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
            SupplementRow(
                label = "Whey protein",
                supportingText = if (state.slot.isWorkoutDay) {
                    "1 serving · after workout"
                } else {
                    "1 serving · optional today"
                },
                checked = state.wheyTaken,
                onCheckedChange = onWheyChanged,
            )
        }
    }
}

@Composable
private fun SupplementRow(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox) { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(supportingText, style = MaterialTheme.typography.bodySmall)
        }
    }
}
