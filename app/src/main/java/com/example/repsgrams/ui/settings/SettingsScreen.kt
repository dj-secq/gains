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
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.HealthAndSafety

import com.example.repsgrams.ui.theme.AppColors
import com.example.repsgrams.ui.components.IconBadge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.PermissionController

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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToSupplements: () -> Unit,
    onExportData: (Uri) -> Unit,
    onImportData: (Uri) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onExportData(uri)
    }
    
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportData(uri)
    }
    
    val context = LocalContext.current
    val hcPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class)
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(hcPermissions)) {
            viewModel.setHealthConnectEnabled(true)
        } else {
            viewModel.setHealthConnectEnabled(false)
        }
    }

    if (settings != null) {
        SettingsScreen(
            settings = settings!!,
            onExportClick = { exportLauncher.launch("repsgrams-backup-${LocalDate.now()}.zip") },
            onImportClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
            onMasterChanged = viewModel::setMaster,
            onTrackedMeasurements = viewModel::setTrackedMeasurements,
            onHealthConnectEnabled = { checked ->
                if (checked) {
                    if (androidx.health.connect.client.HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata") == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
                        permissionLauncher.launch(hcPermissions)
                    } else {
                        viewModel.setHealthConnectEnabled(false)
                    }
                } else {
                    viewModel.setHealthConnectEnabled(false)
                }
            },
            onVoiceCuesEnabled = viewModel::setVoiceCuesEnabled,
            onWorkoutEnabled = viewModel::setWorkoutEnabled,
            onWorkoutTime = viewModel::setWorkoutTime,
            onCreatineEnabled = viewModel::setCreatineEnabled,
            onCreatineTime = viewModel::setCreatineTime,
            onWheyEnabled = viewModel::setWheyEnabled,
            onWheyDelay = viewModel::setWheyDelay,
            onCycleDate = viewModel::setCycleStartDate,
            onUnitSystem = viewModel::setUnitSystem,
            onWheyGrams = viewModel::setWheyServingGrams,
            onRestTimerAutoAdvance = viewModel::setRestTimerAutoAdvance,
            onRestTimerVibrationEnabled = viewModel::setRestTimerVibrationEnabled,
            onDefaultRestSeconds = viewModel::updateDefaultRestSeconds,
            onThemeMode = viewModel::setThemeMode,
            onProteinGoal = viewModel::setProteinGoalMultiplier,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises,
        onNavigateToSupplements = onNavigateToSupplements,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: com.example.repsgrams.data.datastore.CycleSettings,
    onMasterChanged: (Boolean) -> Unit,
    onTrackedMeasurements: (Set<String>) -> Unit,
    onHealthConnectEnabled: (Boolean) -> Unit,
    onVoiceCuesEnabled: (Boolean) -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
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
    onRestTimerAutoAdvance: (Boolean) -> Unit,
    onRestTimerVibrationEnabled: (Boolean) -> Unit,
    onDefaultRestSeconds: (Int) -> Unit,
    onThemeMode: (com.example.repsgrams.data.datastore.ThemeMode) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToSupplements: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
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
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToTemplates() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.EventNote, tint = MaterialTheme.colorScheme.primary)
                                Text("Edit Workout Programs", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToExercises() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.FitnessCenter, tint = MaterialTheme.colorScheme.primary)
                                Text("Exercise Dictionary", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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
                                Text("Theme", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    IosButton(text = "System", onClick = { onThemeMode(com.example.repsgrams.data.datastore.ThemeMode.SYSTEM) }, isSecondary = settings.themeMode != com.example.repsgrams.data.datastore.ThemeMode.SYSTEM, modifier = Modifier.weight(1f))
                                    IosButton(text = "Light", onClick = { onThemeMode(com.example.repsgrams.data.datastore.ThemeMode.LIGHT) }, isSecondary = settings.themeMode != com.example.repsgrams.data.datastore.ThemeMode.LIGHT, modifier = Modifier.weight(1f))
                                    IosButton(text = "Dark", onClick = { onThemeMode(com.example.repsgrams.data.datastore.ThemeMode.DARK) }, isSecondary = settings.themeMode != com.example.repsgrams.data.datastore.ThemeMode.DARK, modifier = Modifier.weight(1f))
                                }
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
                    Text("REST TIMER", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    IconBadge(Icons.Outlined.Timer, AppColors.workout)
                                    Column {
                                        Text("Default Rest Time", style = MaterialTheme.typography.bodyLarge)
                                        Text("${settings.defaultRestSeconds} seconds", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (settings.defaultRestSeconds > 15) onDefaultRestSeconds(settings.defaultRestSeconds - 15) }) { Text("-") }
                                    IconButton(onClick = { onDefaultRestSeconds(settings.defaultRestSeconds + 15) }) { Text("+") }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingToggle(
                                "Vibrate on Finish",
                                "Vibrate device when rest period ends",
                                Icons.Outlined.Vibration,
                                AppColors.workout,
                                settings.restTimerVibrationEnabled,
                                true,
                                onRestTimerVibrationEnabled,
                            )
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
                                Icons.Outlined.NotificationsActive,
                                MaterialTheme.colorScheme.primary,
                                settings.remindersEnabled,
                                true,
                                onMasterChanged,
                            )
                        }
                    }
                }
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DATA & BACKUP", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().clickable { onExportClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.ImportExport, tint = AppColors.progressPurple)
                                Text("Export Data (Backup)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth().clickable { onImportClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.ImportExport, tint = AppColors.progressPurple)
                                Text("Import Data", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HEALTH & INTEGRATIONS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column {
                            SettingToggle(
                                "Health Connect", 
                                "Sync workouts to Health Connect", 
                                Icons.Outlined.HealthAndSafety, 
                                AppColors.workout, 
                                checked = settings.healthConnectEnabled, 
                                enabled = true, 
                                onChecked = { checked ->
                                    onHealthConnectEnabled(checked)
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingToggle(
                                "Voice Cues", 
                                "Audio cues during workout", 
                                Icons.Outlined.RecordVoiceOver, 
                                AppColors.creatineTeal, 
                                checked = settings.voiceCuesEnabled, 
                                enabled = true, 
                                onChecked = onVoiceCuesEnabled
                            )
                        }
                    }
                }
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MEASUREMENTS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Tracked Body Measurements", style = MaterialTheme.typography.bodyLarge)
                            Text("Select the measurements you want to track in Progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            val options = listOf("waist", "chest", "arms", "thighs", "calves", "shoulders", "neck")
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                options.forEach { option ->
                                    val isSelected = settings.trackedMeasurements.contains(option)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val newSet = if (isSelected) settings.trackedMeasurements - option else settings.trackedMeasurements + option
                                            onTrackedMeasurements(newSet)
                                        },
                                        label = { Text(option.replaceFirstChar { it.uppercase() }) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AppColors.progressPurple,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                ReminderCard(
                    title = "Workout Day",
                    description = "Only if today's workout hasn't started",
                    icon = Icons.Outlined.FitnessCenter,
                    iconTint = AppColors.workout,
                    enabled = settings.workoutReminderEnabled,
                    masterEnabled = settings.remindersEnabled,
                    onEnabled = onWorkoutEnabled,
                ) { TimeButton("Time", settings.workoutReminderTime, onWorkoutTime) }
            }
            
            item {
                ReminderCard(
                    title = "Daily Creatine",
                    description = "Only if 5 g hasn't been logged today",
                    icon = Icons.Outlined.Science,
                    iconTint = AppColors.creatineTeal,
                    enabled = settings.creatineReminderEnabled,
                    masterEnabled = settings.remindersEnabled,
                    onEnabled = onCreatineEnabled,
                ) { TimeButton("Time", settings.creatineReminderTime, onCreatineTime) }
            }
            
            item {
                ReminderCard(
                    title = "Post-Workout Whey",
                    description = "Scheduled when a workout is completed",
                    icon = Icons.Outlined.FlashlightOn,
                    iconTint = AppColors.wheyGreen,
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
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean,
    masterEnabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    IosCard {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingToggle(title, description, icon, iconTint, enabled, masterEnabled, onEnabled)
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
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(icon = icon, tint = iconTint)
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
