package com.example.repsgrams.ui.settings

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.ui.components.IosButton
import com.example.repsgrams.ui.components.IosCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    if (settings != null) {
        SettingsScreen(
            settings = settings!!,
            onMasterChanged = viewModel::setMaster,
            onWorkoutEnabled = viewModel::setWorkoutEnabled,
            onWorkoutTime = viewModel::setWorkoutTime,
            onCreatineEnabled = viewModel::setCreatineEnabled,
            onCreatineTime = viewModel::setCreatineTime,
            onWheyEnabled = viewModel::setWheyEnabled,
            onWheyDelay = viewModel::setWheyDelay,
            onCycleDate = viewModel::setCycleStartDate,
            onUnitSystem = viewModel::setUnitSystem,
            onWheyGrams = viewModel::setWheyServingGrams,
            onProteinGoal = viewModel::setProteinGoalMultiplier,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: com.example.repsgrams.data.datastore.CycleSettings,
    onMasterChanged: (Boolean) -> Unit,
    onWorkoutEnabled: (Boolean) -> Unit,
    onWorkoutTime: (LocalTime) -> Unit,
    onCreatineEnabled: (Boolean) -> Unit,
    onCreatineTime: (LocalTime) -> Unit,
    onWheyEnabled: (Boolean) -> Unit,
    onWheyDelay: (Int) -> Unit,
    onCycleDate: (LocalDate) -> Unit,
    onUnitSystem: (UnitSystem) -> Unit,
    onWheyGrams: (Float) -> Unit,
    onProteinGoal: (Float, Float) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DATA & TEMPLATES", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToTemplates() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Edit Workout Programs", style = MaterialTheme.typography.bodyLarge)
                                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToExercises() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Exercise Dictionary", style = MaterialTheme.typography.bodyLarge)
                                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GENERAL", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Cycle Start Date", style = MaterialTheme.typography.bodyLarge)
                                val context = LocalContext.current
                                TextButton(onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d -> onCycleDate(LocalDate.of(y, m + 1, d)) },
                                        settings.cycleStartDate.year,
                                        settings.cycleStartDate.monthValue - 1,
                                        settings.cycleStartDate.dayOfMonth
                                    ).show()
                                }) { Text(settings.cycleStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE), style = MaterialTheme.typography.bodyLarge) }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Column {
                                Text("Unit System", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IosButton(text = "Metric (kg)", onClick = { onUnitSystem(UnitSystem.KG) }, isSecondary = settings.unitSystem != UnitSystem.KG, modifier = Modifier.weight(1f))
                                    IosButton(text = "Imperial (lb)", onClick = { onUnitSystem(UnitSystem.LB) }, isSecondary = settings.unitSystem != UnitSystem.LB, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SUPPLEMENTS & GOALS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            var wheyStr by remember(settings.wheyServingGrams) { mutableStateOf(settings.wheyServingGrams.toString()) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = wheyStr,
                                    onValueChange = { wheyStr = it },
                                    label = { Text("Whey Serving (g)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                IosButton(text = "Save", onClick = { wheyStr.toFloatOrNull()?.let { onWheyGrams(it) } }, modifier = Modifier.weight(0.5f))
                            }
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            var lowStr by remember(settings.proteinGoalMultiplierLow) { mutableStateOf(settings.proteinGoalMultiplierLow.toString()) }
                            var highStr by remember(settings.proteinGoalMultiplierHigh) { mutableStateOf(settings.proteinGoalMultiplierHigh.toString()) }
                            Column {
                                Text("Protein Goal Multiplier (g/kg)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(value = lowStr, onValueChange = { lowStr = it }, label = { Text("Low") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                    OutlinedTextField(value = highStr, onValueChange = { highStr = it }, label = { Text("High") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                IosButton(text = "Save Goal", onClick = { 
                                    val l = lowStr.toFloatOrNull()
                                    val h = highStr.toFloatOrNull()
                                    if (l != null && h != null && l <= h) onProteinGoal(l, h)
                                })
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NOTIFICATIONS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column {
                            SettingToggle(
                                "Master Reminders",
                                "Allow workout and supplement notifications",
                                settings.remindersEnabled,
                                true,
                                onMasterChanged,
                            )
                        }
                    }
                }
            }
            
            item {
                ReminderCard(
                    title = "Workout Day",
                    description = "Only if today's workout hasn't started",
                    enabled = settings.workoutReminderEnabled,
                    masterEnabled = settings.remindersEnabled,
                    onEnabled = onWorkoutEnabled,
                ) { TimeButton("Time", settings.workoutReminderTime, onWorkoutTime) }
            }
            
            item {
                ReminderCard(
                    title = "Daily Creatine",
                    description = "Only if 5 g hasn't been logged today",
                    enabled = settings.creatineReminderEnabled,
                    masterEnabled = settings.remindersEnabled,
                    onEnabled = onCreatineEnabled,
                ) { TimeButton("Time", settings.creatineReminderTime, onCreatineTime) }
            }
            
            item {
                ReminderCard(
                    title = "Post-Workout Whey",
                    description = "Scheduled when a workout is completed",
                    enabled = settings.postWorkoutWheyReminderEnabled,
                    masterEnabled = settings.remindersEnabled,
                    onEnabled = onWheyEnabled,
                ) {
                    Column {
                        Text("Delay after workout", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 20, 30, 45).forEach { minutes ->
                                IosButton(
                                    text = "${minutes}m", 
                                    onClick = { onWheyDelay(minutes) }, 
                                    isSecondary = settings.postWorkoutWheyDelayMinutes != minutes,
                                    modifier = Modifier.weight(1f),
                                    enabled = settings.remindersEnabled && settings.postWorkoutWheyReminderEnabled
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    title: String,
    description: String,
    enabled: Boolean,
    masterEnabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    IosCard {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingToggle(title, description, enabled, masterEnabled, onEnabled)
            if (enabled && masterEnabled) {
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onChecked, 
            enabled = enabled,
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

@Composable
private fun TimeButton(label: String, time: LocalTime, onTime: (LocalTime) -> Unit) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        TextButton(
            onClick = { TimePickerDialog(context, { _, hour, minute -> onTime(LocalTime.of(hour, minute)) }, time.hour, time.minute, false).show() },
        ) { Text(time.format(DateTimeFormatter.ofPattern("h:mm a")), style = MaterialTheme.typography.bodyLarge) }
    }
}
