package com.example.repsgrams.ui.settings

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.HealthAndSafety

import com.example.repsgrams.ui.theme.AppColors
import com.example.repsgrams.ui.components.IconBadge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.ui.components.IosButton
import com.example.repsgrams.ui.components.IosCard
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

import android.net.Uri
import androidx.compose.runtime.SideEffect
import com.example.repsgrams.ui.gate.LocalGainsLaunchers

@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToExerciseLibrary: () -> Unit,
    onNavigateToSupplements: () -> Unit,
    onExportData: (Uri) -> Unit,
    onImportData: (Uri) -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val launchers = LocalGainsLaunchers.current

    val context = LocalContext.current
    val hcPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class)
    )
    SideEffect {
        launchers.onExport = { uri -> if (uri != null) onExportData(uri) }
        launchers.onImport = { uri -> if (uri != null) onImportData(uri) }
        launchers.onHealth = { granted ->
            viewModel.setHealthConnectEnabled(granted.containsAll(hcPermissions))
        }
    }

    if (settings != null) {
        SettingsScreen(
            settings = settings!!,
            onExportClick = { launchers.export.launch("gains-backup-${LocalDate.now()}.zip") },
            onImportClick = {
                launchers.import.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
            },
            onMasterChanged = viewModel::setMaster,
            onTrackedMeasurements = viewModel::setTrackedMeasurements,
            onHealthConnectEnabled = { checked ->
                if (checked) {
                    if (androidx.health.connect.client.HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata") == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) {
                        launchers.health.launch(hcPermissions)
                    } else {
                        viewModel.setHealthConnectEnabled(false)
                    }
                } else {
                    viewModel.setHealthConnectEnabled(false)
                }
            },
            onWorkoutEnabled = viewModel::setWorkoutEnabled,
            onWorkoutTime = viewModel::setWorkoutTime,
            onCreatineEnabled = viewModel::setCreatineEnabled,
            onCreatineTime = viewModel::setCreatineTime,
            onAdherenceGraceDays = viewModel::setAdherenceGraceDays,
            onUnitSystem = viewModel::setUnitSystem,
            onRestTimerSound = viewModel::setRestTimerSound,
            onRestTimerVibrationEnabled = viewModel::setRestTimerVibrationEnabled,
            onDefaultRestSeconds = viewModel::updateDefaultRestSeconds,
            onThemeMode = viewModel::setThemeMode,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises,
            onNavigateToExerciseLibrary = onNavigateToExerciseLibrary,
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
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onWorkoutEnabled: (Boolean) -> Unit,
    onWorkoutTime: (LocalTime) -> Unit,
    onCreatineEnabled: (Boolean) -> Unit,
    onCreatineTime: (LocalTime) -> Unit,
    onAdherenceGraceDays: (Int) -> Unit,
    onUnitSystem: (UnitSystem) -> Unit,
    onRestTimerSound: (String) -> Unit,
    onRestTimerVibrationEnabled: (Boolean) -> Unit,
    onDefaultRestSeconds: (Int) -> Unit,
    onThemeMode: (com.example.repsgrams.data.datastore.ThemeMode) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToExerciseLibrary: () -> Unit,
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
                            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToExerciseLibrary() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.FitnessCenter, tint = AppColors.workout)
                                Text("Exercise Library", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Adherence grace period", style = MaterialTheme.typography.bodyLarge)
                                    Text("Extra sessions a streak can bridge. Does not change the due date.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { onAdherenceGraceDays(settings.adherenceGraceDays - 1) }, enabled = settings.adherenceGraceDays > 0) { Text("−") }
                                Text(settings.adherenceGraceDays.toString(), style = MaterialTheme.typography.titleMedium)
                                IconButton(onClick = { onAdherenceGraceDays(settings.adherenceGraceDays + 1) }, enabled = settings.adherenceGraceDays < 7) { Text("+") }
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
                                "Sound",
                                "Play a sound when rest ends",
                                Icons.Outlined.VolumeUp,
                                AppColors.workout,
                                settings.restTimerSound != "off",
                                true,
                                { enabled -> onRestTimerSound(if (enabled) "default" else "off") },
                            )
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
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
                                ExactAlarmRow()
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SUPPLEMENTS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigateToSupplements() }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            IconBadge(icon = Icons.Outlined.Science, tint = AppColors.creatineTeal)
                            Text("Manage Supplements", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
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
                            HorizontalDivider(modifier = Modifier.padding(start = 61.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingToggle(
                                "Workout Day",
                                if (settings.remindersEnabled) {
                                    "Only if today's workout hasn't started"
                                } else {
                                    "Turn on Master Reminders to enable"
                                },
                                Icons.Outlined.FitnessCenter,
                                AppColors.workout,
                                settings.workoutReminderEnabled,
                                settings.remindersEnabled,
                                onWorkoutEnabled,
                            )
                            if (settings.remindersEnabled && settings.workoutReminderEnabled) {
                                Box(modifier = Modifier.padding(start = 61.dp, end = 16.dp, bottom = 12.dp)) {
                                    TimeButton("Reminder time", settings.workoutReminderTime, onWorkoutTime)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 61.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingToggle(
                                "Due Supplements",
                                if (settings.remindersEnabled) {
                                    "Scheduled today and not yet logged"
                                } else {
                                    "Turn on Master Reminders to enable"
                                },
                                Icons.Outlined.Science,
                                AppColors.creatineTeal,
                                settings.creatineReminderEnabled,
                                settings.remindersEnabled,
                                onCreatineEnabled,
                            )
                            if (settings.remindersEnabled && settings.creatineReminderEnabled) {
                                Box(modifier = Modifier.padding(start = 61.dp, end = 16.dp, bottom = 12.dp)) {
                                    TimeButton("Reminder time", settings.creatineReminderTime, onCreatineTime)
                                }
                            }
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Export Data (Backup)", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "Anyone who can open the file can read the log.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
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
                                "Sync finished workouts and bodyweight",
                                Icons.Outlined.HealthAndSafety,
                                AppColors.workout,
                                checked = settings.healthConnectEnabled,
                                enabled = true,
                                onChecked = { checked ->
                                    onHealthConnectEnabled(checked)
                                }
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "OPEN SOURCE CREDITS",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                    IosCard {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Exercise photography", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Selected images from yuhonas/free-exercise-db, released into the public domain under the Unlicense. Images are bundled locally and are never fetched while using the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun ExactAlarmRow() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val allowed = remember(resumeTick) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.canScheduleExactAlarms() == true
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(icon = Icons.Outlined.Timer, tint = AppColors.workout)
        Column(modifier = Modifier.weight(1f)) {
            Text("Exact rest alarm", style = MaterialTheme.typography.bodyLarge)
            Text(
                if (allowed) {
                    "Allowed. Used if the app is killed or the phone is in Doze."
                } else {
                    "Off. This process can still alert with the screen off. If it is killed or the phone is in Doze, the alarm is required. Reopening shows overtime and does not clear the timer. Force-stop still ends it."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (allowed) "On" else "Off",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onChecked,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(icon = icon, tint = iconTint.copy(alpha = if (enabled) 1f else 0.45f))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.72f),
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
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
