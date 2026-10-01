package com.example.repsgrams.ui.settings

import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.datastore.CycleSettings
import com.example.repsgrams.data.datastore.ThemeMode
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.datastore.formatPlateList
import com.example.repsgrams.data.datastore.parsePlateList
import com.example.repsgrams.domain.session.formatWeight
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.MonoChip
import com.example.repsgrams.ui.components.MonoLabel
import com.example.repsgrams.ui.components.TextAction
import com.example.repsgrams.ui.components.shimmer
import com.example.repsgrams.ui.gate.LocalGainsLaunchers
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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
    val launchers = LocalGainsLaunchers.current
    val context = LocalContext.current
    val hcPermissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
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
                    if (HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata") == HealthConnectClient.SDK_AVAILABLE) {
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
            onThemeMode = viewModel::setThemeMode,
            onKeepScreenOn = viewModel::setKeepScreenOn,
            onRpeEnabled = viewModel::setRpeEnabled,
            onHapticsEnabled = viewModel::setHapticsEnabled,
            onProgressionEnabled = viewModel::setProgressionEnabled,
            onProgressionIncrementKg = viewModel::setProgressionIncrementKg,
            onProgressionIncrementLb = viewModel::setProgressionIncrementLb,
            onBarbellKg = viewModel::setBarbellKg,
            onBarbellLb = viewModel::setBarbellLb,
            onPlatesKg = viewModel::setPlatesKg,
            onPlatesLb = viewModel::setPlatesLb,
            onWarmupRest = viewModel::setWarmupRest,
            onWorkingRest = viewModel::setWorkingRest,
            onSupersetRest = viewModel::setSupersetRest,
            onNavigateToTemplates = onNavigateToTemplates,
            onNavigateToExercises = onNavigateToExercises,
            onNavigateToSupplements = onNavigateToSupplements,
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(24.dp)).shimmer())
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp)).shimmer())
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(24.dp)).shimmer())
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: CycleSettings,
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
    onThemeMode: (ThemeMode) -> Unit,
    onKeepScreenOn: (Boolean) -> Unit,
    onRpeEnabled: (Boolean) -> Unit,
    onHapticsEnabled: (Boolean) -> Unit,
    onProgressionEnabled: (Boolean) -> Unit,
    onProgressionIncrementKg: (Float) -> Unit,
    onProgressionIncrementLb: (Float) -> Unit,
    onBarbellKg: (Float) -> Unit,
    onBarbellLb: (Float) -> Unit,
    onPlatesKg: (String) -> Unit,
    onPlatesLb: (String) -> Unit,
    onWarmupRest: (Int) -> Unit,
    onWorkingRest: (Int) -> Unit,
    onSupersetRest: (Int) -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToSupplements: () -> Unit,
) {
    val pounds = settings.unitSystem == UnitSystem.LB
    val unitWord = if (pounds) "lb" else "kg"
    val bar = if (pounds) settings.barbellLb else settings.barbellKg
    val increment = if (pounds) settings.progressionIncrementLb else settings.progressionIncrementKg
    var editingBar by remember { mutableStateOf(false) }
    var editingPlates by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val version = remember {
        val info = if (Build.VERSION.SDK_INT >= 33) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName ?: "1.0"
    }
    val healthAvailable = healthConnectAvailable()

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MonoChip("AUTO", settings.themeMode == ThemeMode.SYSTEM, { onThemeMode(ThemeMode.SYSTEM) }, Modifier.weight(1f))
                    MonoChip("LIGHT", settings.themeMode == ThemeMode.LIGHT, { onThemeMode(ThemeMode.LIGHT) }, Modifier.weight(1f))
                    MonoChip("DARK", settings.themeMode == ThemeMode.DARK, { onThemeMode(ThemeMode.DARK) }, Modifier.weight(1f))
                }
            }
            item {
                SettingGroup("Session") {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Units", modifier = Modifier.weight(1f).align(Alignment.CenterVertically), style = MaterialTheme.typography.bodyLarge)
                        MonoChip("KG", !pounds, { onUnitSystem(UnitSystem.KG) })
                        MonoChip("LB", pounds, { onUnitSystem(UnitSystem.LB) })
                    }
                    ValueRow("Bar", "${formatWeight(bar)} $unitWord", divided = true, onClick = { editingBar = true })
                    NavRow("Plates", divided = true, onClick = { editingPlates = true })
                    StepperRow("Warm-up rest", "${settings.defaultWarmupRestSeconds} s", divided = true, onMinus = {
                        onWarmupRest((settings.defaultWarmupRestSeconds - 15).coerceAtLeast(0))
                    }, onPlus = {
                        onWarmupRest((settings.defaultWarmupRestSeconds + 15).coerceAtMost(900))
                    })
                    StepperRow("Working rest", "${settings.defaultWorkingRestSeconds} s", divided = true, onMinus = {
                        onWorkingRest((settings.defaultWorkingRestSeconds - 15).coerceAtLeast(0))
                    }, onPlus = {
                        onWorkingRest((settings.defaultWorkingRestSeconds + 15).coerceAtMost(900))
                    })
                    StepperRow("Superset inside", "${settings.defaultSupersetIntraRestSeconds} s", divided = true, onMinus = {
                        onSupersetRest((settings.defaultSupersetIntraRestSeconds - 15).coerceAtLeast(0))
                    }, onPlus = {
                        onSupersetRest((settings.defaultSupersetIntraRestSeconds + 15).coerceAtMost(900))
                    })
                    SettingToggle("Sound", "Play a sound when rest ends", settings.restTimerSound != "off", true, divided = true) {
                        onRestTimerSound(if (it) "default" else "off")
                    }
                    SettingToggle("Vibrate on finish", "Vibrate when rest ends", settings.restTimerVibrationEnabled, true, divided = true, onChecked = onRestTimerVibrationEnabled)
                    SettingToggle("Keep screen on", "While a workout is open", settings.keepScreenOn, true, divided = true, onChecked = onKeepScreenOn)
                    SettingToggle("RPE column", "Off hides the column", settings.rpeEnabled, true, divided = true, onChecked = onRpeEnabled)
                    SettingToggle("Haptics", "When a set is completed", settings.hapticsEnabled, true, divided = true, onChecked = onHapticsEnabled)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ExactAlarmRow()
                    }
                }
            }
            item {
                SettingGroup("Program") {
                    NavRow("Templates", onClick = onNavigateToTemplates)
                    NavRow("Exercises", divided = true, onClick = onNavigateToExercises)
                    SettingToggle(
                        title = "Progression",
                        description = "If every working set hits the top of the rep range, add ${formatWeight(increment)} $unitWord next time and start at the low end of the range.",
                        checked = settings.progressionEnabled,
                        enabled = true,
                        divided = true,
                        onChecked = onProgressionEnabled,
                    )
                    StepperRow("Increment", "${formatWeight(increment)} $unitWord", divided = true, onMinus = {
                        val next = increment - if (pounds) 1f else 0.5f
                        if (pounds) onProgressionIncrementLb(next.coerceAtLeast(1f)) else onProgressionIncrementKg(next.coerceAtLeast(0.5f))
                    }, onPlus = {
                        val next = increment + if (pounds) 1f else 0.5f
                        if (pounds) onProgressionIncrementLb(next.coerceAtMost(50f)) else onProgressionIncrementKg(next.coerceAtMost(25f))
                    })
                    StepperRow(
                        label = "Adherence grace",
                        value = settings.adherenceGraceDays.toString(),
                        subtitle = "Extra sessions a streak can bridge. Does not change the due date.",
                        divided = true,
                        onMinus = { if (settings.adherenceGraceDays > 0) onAdherenceGraceDays(settings.adherenceGraceDays - 1) },
                        onPlus = { if (settings.adherenceGraceDays < 7) onAdherenceGraceDays(settings.adherenceGraceDays + 1) },
                    )
                }
            }
            item {
                SettingGroup("Measurements") {
                    Column(Modifier.padding(16.dp)) {
                        Text("Tracked Body Measurements", style = MaterialTheme.typography.bodyLarge)
                        Text("Shown on Progress when selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("waist", "chest", "arms", "thighs", "calves", "shoulders", "neck").forEach { option ->
                                val selected = settings.trackedMeasurements.contains(option)
                                MonoChip(
                                    text = option.replaceFirstChar { it.uppercase() },
                                    selected = selected,
                                    onClick = {
                                        val next = if (selected) settings.trackedMeasurements - option else settings.trackedMeasurements + option
                                        onTrackedMeasurements(next)
                                    },
                                )
                            }
                        }
                    }
                }
            }
            item {
                SettingGroup("Supplements") {
                    NavRow("Manage Supplements", onClick = onNavigateToSupplements)
                }
            }
            item {
                SettingGroup("Reminders") {
                    SettingToggle("Master", "Allow workout and supplement notifications", settings.remindersEnabled, true, onChecked = onMasterChanged)
                    ReminderRow(
                        label = "Workout",
                        time = settings.workoutReminderTime,
                        checked = settings.workoutReminderEnabled,
                        enabled = settings.remindersEnabled,
                        divided = true,
                        onChecked = onWorkoutEnabled,
                        onTime = onWorkoutTime,
                    )
                    ReminderRow(
                        label = "Supplements",
                        time = settings.creatineReminderTime,
                        checked = settings.creatineReminderEnabled,
                        enabled = settings.remindersEnabled,
                        divided = true,
                        onChecked = onCreatineEnabled,
                        onTime = onCreatineTime,
                    )
                }
            }
            item {
                SettingGroup("Health") {
                    SettingToggle(
                        title = "Health Connect",
                        description = if (healthAvailable) "Sync finished workouts and bodyweight" else "Not available on this device",
                        checked = healthAvailable && settings.healthConnectEnabled,
                        enabled = healthAvailable,
                        onChecked = onHealthConnectEnabled,
                    )
                }
            }
            item {
                SettingGroup("Data") {
                    NavRow(
                        label = "Export",
                        subtitle = "The zip contains the database and settings (units, reminders, theme, timer, plates). Anyone who can open the file can read the log.",
                        onClick = onExportClick,
                    )
                    NavRow("Import", divided = true, onClick = onImportClick)
                }
            }
            item {
                SettingGroup("About") {
                    ValueRow("Version", version)
                    Column(Modifier.padding(16.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Exercise photography", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Selected images from yuhonas/free-exercise-db, released into the public domain under the Unlicense. Images are bundled locally and are never fetched while using the app.",
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Fonts", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Doto, Space Grotesk, and Space Mono. SIL Open Font License.",
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (editingBar) {
        NumberDialog(
            title = "Bar",
            initial = formatWeight(bar),
            unit = unitWord,
            onDismiss = { editingBar = false },
            onConfirm = { value ->
                if (pounds) onBarbellLb(value) else onBarbellKg(value)
                editingBar = false
            },
        )
    }
    if (editingPlates) {
        PlateDialog(
            initial = if (pounds) settings.platesLb else settings.platesKg,
            unit = unitWord,
            onDismiss = { editingPlates = false },
            onConfirm = { stored ->
                if (pounds) onPlatesLb(stored) else onPlatesKg(stored)
                editingPlates = false
            },
        )
    }
}

@Composable
private fun SettingGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonoLabel(title, modifier = Modifier.padding(start = 16.dp))
        BoardTile(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit, subtitle: String? = null, divided: Boolean = false) {
    if (divided) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ValueRow(label: String, value: String, divided: Boolean = false, onClick: (() -> Unit)? = null) {
    if (divided) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    divided: Boolean = false,
    subtitle: String? = null,
) {
    if (divided) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextAction("−", onMinus)
        TextAction("+", onPlus)
    }
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    divided: Boolean = false,
    onChecked: (Boolean) -> Unit,
) {
    if (divided) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChecked)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun ReminderRow(
    label: String,
    time: LocalTime,
    checked: Boolean,
    enabled: Boolean,
    divided: Boolean,
    onChecked: (Boolean) -> Unit,
    onTime: (LocalTime) -> Unit,
) {
    val context = LocalContext.current
    if (divided) HorizontalDivider(Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextAction(
            text = time.format(DateTimeFormatter.ofPattern("h:mm a")),
            onClick = {
                if (!enabled) return@TextAction
                TimePickerDialog(context, { _, hour, minute -> onTime(LocalTime.of(hour, minute)) }, time.hour, time.minute, false).show()
            },
            enabled = enabled,
        )
        Switch(checked = checked, onCheckedChange = onChecked, enabled = enabled)
    }
}

@Composable
private fun healthConnectAvailable(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick += 1
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return remember(tick) {
        HealthConnectClient.getSdkStatus(context, "com.google.android.apps.healthdata") == HealthConnectClient.SDK_AVAILABLE
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
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
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
        Text(if (allowed) "On" else "Off", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun NumberDialog(title: String, initial: String, unit: String, onDismiss: () -> Unit, onConfirm: (Float) -> Unit) {
    var draft by remember(title, initial) { mutableStateOf(initial) }
    val parsed = draft.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }
    BoardDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = "Save",
        onConfirm = { parsed?.let(onConfirm) },
        confirmEnabled = parsed != null,
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            label = { Text(unit) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
        )
    }
}

@Composable
private fun PlateDialog(initial: String, unit: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var plates by remember(initial) { mutableStateOf(parsePlateList(initial).distinct().sortedDescending()) }
    var draft by remember(initial) { mutableStateOf("") }
    BoardDialog(
        title = "Plates",
        onDismiss = onDismiss,
        confirmText = "Save",
        onConfirm = { onConfirm(formatPlateList(plates)) },
        confirmEnabled = plates.isNotEmpty(),
        message = "Each number is one plate, used in unlimited pairs. Tap a plate to remove it.",
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            plates.forEach { plate ->
                MonoChip(formatWeight(plate), selected = true, onClick = { plates = plates.filterNot { it == plate } })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(unit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextAction("Add", onClick = {
                val value = draft.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f } ?: return@TextAction
                plates = (plates + value).distinct().sortedDescending()
                draft = ""
            })
        }
    }
}
