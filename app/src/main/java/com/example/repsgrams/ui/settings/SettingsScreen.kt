package com.example.repsgrams.ui.settings

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.CycleSettings
import com.example.repsgrams.data.datastore.UnitSystem
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.setMaster(true) }

    settings?.let { value ->
        SettingsScreen(
            settings = value,
            onMasterChanged = { enabled ->
                val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (enabled && !granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else viewModel.setMaster(enabled)
            },
            onWorkoutEnabled = viewModel::setWorkoutEnabled,
            onCreatineEnabled = viewModel::setCreatineEnabled,
            onWheyEnabled = viewModel::setWheyEnabled,
            onWorkoutTime = viewModel::setWorkoutTime,
            onCreatineTime = viewModel::setCreatineTime,
            onWheyDelay = viewModel::setWheyDelay,
            onCycleDate = viewModel::setCycleStartDate,
            onUnitSystem = viewModel::setUnitSystem,
            onWheyGrams = viewModel::setWheyServingGrams,
            onProteinGoal = viewModel::setProteinGoalMultiplier,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises
        )
    } ?: Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { CircularProgressIndicator() }
}

@Composable
private fun SettingsScreen(
    settings: CycleSettings,
    onMasterChanged: (Boolean) -> Unit,
    onWorkoutEnabled: (Boolean) -> Unit,
    onCreatineEnabled: (Boolean) -> Unit,
    onWheyEnabled: (Boolean) -> Unit,
    onWorkoutTime: (LocalTime) -> Unit,
    onCreatineTime: (LocalTime) -> Unit,
    onWheyDelay: (Int) -> Unit,
    onCycleDate: (LocalDate) -> Unit,
    onUnitSystem: (UnitSystem) -> Unit,
    onWheyGrams: (Float) -> Unit,
    onProteinGoal: (Float, Float) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.headlineLarge) }
        
        item { Text("Program Editor", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToTemplates) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Workout Templates")
                    Text(">")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToExercises) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Exercise Dictionary")
                    Text(">")
                }
            }
        }

        item { Text("General", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val context = LocalContext.current
                    Text("Cycle Start Date", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> onCycleDate(LocalDate.of(y, m + 1, d)) },
                            settings.cycleStartDate.year,
                            settings.cycleStartDate.monthValue - 1,
                            settings.cycleStartDate.dayOfMonth
                        ).show()
                    }) { Text(settings.cycleStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)) }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text("Unit System", style = MaterialTheme.typography.titleMedium)
                    Row {
                        FilterChip(selected = settings.unitSystem == UnitSystem.KG, onClick = { onUnitSystem(UnitSystem.KG) }, label = { Text("Metric (kg)") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = settings.unitSystem == UnitSystem.LB, onClick = { onUnitSystem(UnitSystem.LB) }, label = { Text("Imperial (lb)") })
                    }
                }
            }
        }

        item { Text("Supplements & Goals", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var wheyStr by remember(settings.wheyServingGrams) { mutableStateOf(settings.wheyServingGrams.toString()) }
                    OutlinedTextField(
                        value = wheyStr,
                        onValueChange = { wheyStr = it },
                        label = { Text("Whey Serving (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { wheyStr.toFloatOrNull()?.let { onWheyGrams(it) } }, modifier = Modifier.align(Alignment.End)) { Text("Save") }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    var lowStr by remember(settings.proteinGoalMultiplierLow) { mutableStateOf(settings.proteinGoalMultiplierLow.toString()) }
                    var highStr by remember(settings.proteinGoalMultiplierHigh) { mutableStateOf(settings.proteinGoalMultiplierHigh.toString()) }
                    Text("Protein Goal Multiplier (g/kg)", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = lowStr, onValueChange = { lowStr = it }, label = { Text("Low") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = highStr, onValueChange = { highStr = it }, label = { Text("High") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Button(onClick = { 
                        val l = lowStr.toFloatOrNull()
                        val h = highStr.toFloatOrNull()
                        if (l != null && h != null && l <= h) onProteinGoal(l, h)
                    }, modifier = Modifier.align(Alignment.End)) { Text("Save") }
                }
            }
        }

        item { Text("Reminders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                SettingToggle(
                    "Reminders",
                    "Allow workout and supplement notifications",
                    settings.remindersEnabled,
                    true,
                    onMasterChanged,
                )
            }
        }
        item {
            ReminderCard(
                title = "Workout day",
                description = "Only if today's workout hasn't started",
                enabled = settings.workoutReminderEnabled,
                masterEnabled = settings.remindersEnabled,
                onEnabled = onWorkoutEnabled,
            ) { TimeButton("Reminder time", settings.workoutReminderTime, onWorkoutTime) }
        }
        item {
            ReminderCard(
                title = "Daily creatine",
                description = "Only if 5 g hasn't been logged today",
                enabled = settings.creatineReminderEnabled,
                masterEnabled = settings.remindersEnabled,
                onEnabled = onCreatineEnabled,
            ) { TimeButton("Reminder time", settings.creatineReminderTime, onCreatineTime) }
        }
        item {
            ReminderCard(
                title = "Post-workout whey",
                description = "Scheduled when a workout is completed",
                enabled = settings.postWorkoutWheyReminderEnabled,
                masterEnabled = settings.remindersEnabled,
                onEnabled = onWheyEnabled,
            ) {
                Text("Delay after workout", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 20, 30, 45).forEach { minutes ->
                        FilterChip(
                            selected = settings.postWorkoutWheyDelayMinutes == minutes,
                            onClick = { onWheyDelay(minutes) },
                            label = { Text("$minutes m") },
                            enabled = settings.remindersEnabled && settings.postWorkoutWheyReminderEnabled,
                        )
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingToggle(title, description, enabled, masterEnabled, onEnabled)
            content()
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
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun TimeButton(label: String, time: LocalTime, onTime: (LocalTime) -> Unit) {
    val context = LocalContext.current
    TextButton(
        onClick = { TimePickerDialog(context, { _, hour, minute -> onTime(LocalTime.of(hour, minute)) }, time.hour, time.minute, false).show() },
    ) { Text("$label: ${time.format(DateTimeFormatter.ofPattern("h:mm a"))}") }
}
