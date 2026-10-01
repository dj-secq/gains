package com.example.repsgrams.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.ui.components.BackChevron
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.MonoChip
import com.example.repsgrams.ui.components.TextAction






import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.db.ScheduleMode
import com.example.repsgrams.data.db.TemplateBlockEntity
import com.example.repsgrams.data.db.TemplateBlockExerciseEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.today.restDaysCaption
import com.example.repsgrams.domain.today.weekdayCaption
import com.example.repsgrams.ui.components.MonoLabel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListRoute(
    viewModel: ProgramEditorViewModel,
    onNavigateToTemplate: (Long) -> Unit,
    onBack: () -> Unit
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val activeProgram = programs.filter { it.active }.minByOrNull { it.id }
    val weekly = activeProgram?.scheduleMode == ScheduleMode.WEEKLY
    var showDialog by remember { mutableStateOf(false) }
    var showProgram by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }
    var pendingDuplicate by remember { mutableStateOf<WorkoutTemplateEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Workout Templates") },
                navigationIcon = { BackChevron(onClick = onBack) },
                actions = { TextAction("+", onClick = { showDialog = true }, contentDescription = "Add") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text("Programs", style = MaterialTheme.typography.titleMedium)
                programs.forEach { program ->
                    BoardTile(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        onClick = { viewModel.activateProgram(program.id) },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(program.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (program.active) MonoLabel("Active")
                        }
                    }
                }
                TextAction("Add program", onClick = { showProgram = true })
                if (activeProgram != null) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MonoChip(
                            text = "Rotation",
                            selected = activeProgram.scheduleMode == ScheduleMode.ROTATION,
                            onClick = { viewModel.setScheduleMode(activeProgram.id, ScheduleMode.ROTATION) },
                        )
                        MonoChip(
                            text = "Weekly",
                            selected = activeProgram.scheduleMode == ScheduleMode.WEEKLY,
                            onClick = { viewModel.setScheduleMode(activeProgram.id, ScheduleMode.WEEKLY) },
                        )
                    }
                }
            }
            items(templates) { template ->
                val idx = templates.indexOf(template)
                BoardTile(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigateToTemplate(template.id) },
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(template.name, style = MaterialTheme.typography.titleMedium)
                        Text("Day: ${template.dayLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (weekly) weekdayCaption(template.weekday) else restDaysCaption(template.restDaysAfter),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (idx > 0) {
                                TextAction("Up", onClick = { viewModel.swapTemplates(template.id, templates[idx - 1].id) })
                            }
                            if (idx < templates.lastIndex) {
                                TextAction("Down", onClick = { viewModel.swapTemplates(template.id, templates[idx + 1].id) })
                            }
                            RowMenu(
                                onDelete = { pendingDelete = template },
                                onDuplicate = { pendingDuplicate = template },
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { template ->
        BoardDialog(
            title = "Delete this workout?",
            message = "Logged sessions stay on the calendar.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteTemplate(template)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    pendingDuplicate?.let { template ->
        var day by remember(template.id) { mutableStateOf("") }
        BoardDialog(
            title = "Duplicate",
            onDismiss = { pendingDuplicate = null },
            confirmText = "Duplicate",
            confirmEnabled = day.isNotBlank(),
            onConfirm = {
                viewModel.duplicateTemplate(template.id, day)
                pendingDuplicate = null
            },
            content = {
                OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Day label") })
            },
        )
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var day by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Custom") }
        var customCategory by remember { mutableStateOf("") }
        var restDays by remember { mutableIntStateOf(1) }
        var weekday by remember { mutableStateOf<Int?>(null) }
        BoardDialog(
            title = "New Template",
            onDismiss = { showDialog = false },
            confirmText = "Save",
            confirmEnabled = name.isNotBlank() && day.isNotBlank() && (!weekly || weekday != null),
            onConfirm = {
                viewModel.addTemplate(
                    name,
                    day,
                    customCategory.trim().takeIf { category == "Custom" && it.isNotEmpty() } ?: category,
                    restDays,
                    weekday = if (weekly) weekday else null,
                )
                showDialog = false
            },
            content = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Day Label (A/B)") })
                Text("Category", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.repsgrams.ui.theme.CategoryColors.Categories.forEach { option ->
                        MonoChip(text = option, selected = category == option, onClick = { category = option })
                    }
                }
                if (category == "Custom") {
                    OutlinedTextField(value = customCategory, onValueChange = { customCategory = it }, label = { Text("Custom category (optional)") })
                }
                if (weekly) {
                    Text("Weekday", style = MaterialTheme.typography.labelLarge)
                    WeekdayChips(selected = weekday, onSelect = { weekday = it })
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rest days after: $restDays", modifier = Modifier.weight(1f))
                        IconButton(onClick = { restDays = (restDays - 1).coerceAtLeast(0) }) { Text("−") }
                        IconButton(onClick = { restDays = (restDays + 1).coerceAtMost(7) }) { Text("+") }
                    }
                }
            },
        )
    }

    if (showProgram) {
        var name by remember { mutableStateOf("") }
        BoardDialog(
            title = "New program",
            onDismiss = { showProgram = false },
            confirmText = "Save",
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                viewModel.addProgram(name)
                showProgram = false
            },
            content = {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorRoute(
    templateId: Long,
    viewModel: ProgramEditorViewModel,
    onNavigateToBlock: (Long) -> Unit,
    onBack: () -> Unit
) {
    val blocksFlow = remember(templateId) { viewModel.observeBlocks(templateId) }
    val blocks by blocksFlow.collectAsStateWithLifecycle(emptyList())
    var showDialog by remember { mutableStateOf(false) }
    var editingBlock by remember { mutableStateOf<TemplateBlockEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TemplateBlockEntity?>(null) }

    val template by remember(templateId) { viewModel.observeTemplate(templateId) }.collectAsStateWithLifecycle(null)
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Template Blocks") },
                navigationIcon = { BackChevron(onClick = onBack) },
                actions = { TextAction("+", onClick = { showDialog = true }, contentDescription = "Add") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                template?.let { t ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Template Settings", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        var name by remember(t.id, t.name) { mutableStateOf(t.name) }
                        var dayLabel by remember(t.id, t.dayLabel) { mutableStateOf(t.dayLabel) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = dayLabel, onValueChange = { dayLabel = it }, label = { Text("Day") }, modifier = Modifier.width(96.dp), singleLine = true)
                        }
                        TextButton(
                            onClick = { viewModel.updateTemplate(t.copy(name = name.trim(), dayLabel = dayLabel.trim())) },
                            enabled = name.isNotBlank() && dayLabel.isNotBlank() && (name.trim() != t.name || dayLabel.trim() != t.dayLabel),
                        ) { Text("Save name and label") }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Category: ")
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(t.category)
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    com.example.repsgrams.ui.theme.CategoryColors.Categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                viewModel.updateTemplateCategory(t.id, cat)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        var customCategory by remember(t.id, t.category) { mutableStateOf(if (t.category in com.example.repsgrams.ui.theme.CategoryColors.Categories) "" else t.category) }
                        OutlinedTextField(
                            value = customCategory,
                            onValueChange = { customCategory = it },
                            label = { Text("Custom category") },
                            supportingText = { Text("Optional; saving replaces the selected category") },
                            trailingIcon = {
                                TextButton(onClick = { if (customCategory.isNotBlank()) viewModel.updateTemplateCategory(t.id, customCategory.trim()) }) { Text("Save") }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )

                        val weeklyTemplate = programs.find { it.id == t.programId }?.scheduleMode == ScheduleMode.WEEKLY
                        if (weeklyTemplate) {
                            Text("Weekday", style = MaterialTheme.typography.labelLarge)
                            WeekdayChips(selected = t.weekday, onSelect = { viewModel.assignWeekday(t.id, it) })
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rest Days After: ${t.restDaysAfter}")
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { if (t.restDaysAfter > 0) viewModel.updateTemplateRestDays(t.id, t.restDaysAfter - 1) }) { Text("-") }
                                IconButton(onClick = { if (t.restDaysAfter < 7) viewModel.updateTemplateRestDays(t.id, t.restDaysAfter + 1) }) { Text("+") }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Duration: ${t.maxDurationMinutes} min", modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                viewModel.updateTemplate(t.copy(maxDurationMinutes = (t.maxDurationMinutes - 5).coerceAtLeast(5)))
                            }) { Text("−") }
                            IconButton(onClick = {
                                viewModel.updateTemplate(t.copy(maxDurationMinutes = (t.maxDurationMinutes + 5).coerceAtMost(240)))
                            }) { Text("+") }
                        }
                    }
                    HorizontalDivider()
                }
            }

            items(blocks.sortedBy { it.orderIndex }) { block ->
                val idx = blocks.indexOf(block)
                BoardTile(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = { onNavigateToBlock(block.id) },
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(block.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${block.kind.editorLabel()} · ${block.targetRoundsMin}-${block.targetRoundsMax} rounds, ${block.restSecondsBetweenRounds}s rest",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (idx > 0) {
                                TextAction("Up", onClick = { viewModel.swapBlocks(block.id, blocks[idx - 1].id, templateId) })
                            }
                            if (idx < blocks.lastIndex) {
                                TextAction("Down", onClick = { viewModel.swapBlocks(block.id, blocks[idx + 1].id, templateId) })
                            }
                            RowMenu(onEdit = { editingBlock = block }, onDelete = { pendingDelete = block })
                        }
                    }
                }
            }
        }
    }

    val warmupRest by viewModel.warmupRestSeconds.collectAsStateWithLifecycle(90)
    val workingRest by viewModel.workingRestSeconds.collectAsStateWithLifecycle(180)
    if (showDialog || editingBlock != null) {
        val original = editingBlock
        var label by remember(original?.id) { mutableStateOf(original?.label.orEmpty()) }
        var rMin by remember(original?.id) { mutableStateOf((original?.targetRoundsMin ?: 3).toString()) }
        var rMax by remember(original?.id) { mutableStateOf((original?.targetRoundsMax ?: 5).toString()) }
        var rest by remember(original?.id) {
            mutableStateOf(
                when {
                    original != null -> original.restSecondsBetweenRounds?.toString().orEmpty()
                    else -> workingRest.toString()
                },
            )
        }
        var restAfter by remember(original?.id) {
            mutableStateOf(
                when {
                    original != null -> original.restSecondsAfterBlock?.toString().orEmpty()
                    else -> workingRest.toString()
                },
            )
        }
        var optional by remember(original?.id) { mutableStateOf(original?.isOptional ?: false) }
        var kind by remember(original?.id) { mutableStateOf(original?.kind ?: BlockKind.STANDARD) }
        BoardDialog(
            title = if (original == null) "New Block" else "Edit Block",
            onDismiss = { showDialog = false; editingBlock = null },
            confirmText = if (original == null) "Add" else "Save",
            onConfirm = {
                val min = rMin.toIntOrNull() ?: 1
                val max = rMax.toIntOrNull() ?: min
                val r = rest.toIntOrNull()
                val rAfter = restAfter.toIntOrNull()
                val restOk = if (original == null) r != null && rAfter != null && r >= 0 && rAfter >= 0 else {
                    (rest.isBlank() || (r != null && r >= 0)) && (restAfter.isBlank() || (rAfter != null && rAfter >= 0))
                }
                if (label.isNotBlank() && min > 0 && min <= max && restOk) {
                    if (original == null) {
                        viewModel.addBlock(templateId, label.trim(), kind, min, max, r ?: 0, rAfter ?: 0, optional)
                    } else {
                        viewModel.updateBlock(
                            original.copy(
                                label = label.trim(),
                                targetRoundsMin = min,
                                targetRoundsMax = max,
                                restSecondsBetweenRounds = if (rest.isBlank()) null else r,
                                restSecondsAfterBlock = if (restAfter.isBlank()) null else rAfter,
                                isOptional = optional,
                                kind = kind,
                            ),
                        )
                    }
                    showDialog = false
                    editingBlock = null
                }
            },
            content = {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. A, B1)") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rMin, onValueChange = { rMin = it }, label = { Text("Min Rds") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = rMax, onValueChange = { rMax = it }, label = { Text("Max Rds") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rest, onValueChange = { rest = it }, label = { Text("Rest between rounds (s)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = restAfter, onValueChange = { restAfter = it }, label = { Text("Rest before next block (s)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Text("Kind", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BlockKind.entries.forEach { option ->
                        MonoChip(
                            option.editorLabel(),
                            kind == option,
                            onClick = {
                                kind = option
                                if (original == null) {
                                    if (option == BlockKind.WARM_UP) {
                                        rest = "0"
                                        restAfter = warmupRest.toString()
                                    } else {
                                        rest = workingRest.toString()
                                        restAfter = workingRest.toString()
                                    }
                                }
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Optional", modifier = Modifier.weight(1f))
                    Switch(checked = optional, onCheckedChange = { optional = it })
                }
            },
        )
    }

    pendingDelete?.let { block ->
        BoardDialog(
            title = "Delete this block?",
            message = "The exercises in this block are removed from the template.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteBlock(block)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorRoute(
    blockId: Long,
    viewModel: ProgramEditorViewModel,
    onBack: () -> Unit
) {
    val exLinksFlow = remember(blockId) { viewModel.observeBlockExercises(blockId) }
    val exLinks by exLinksFlow.collectAsStateWithLifecycle(emptyList())
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingLink by remember { mutableStateOf<TemplateBlockExerciseEntity?>(null) }
    var pendingDelete by remember { mutableStateOf<TemplateBlockExerciseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Block Exercises") },
                navigationIcon = { BackChevron(onClick = onBack) },
                actions = { TextAction("+", onClick = { showDialog = true }, contentDescription = "Add") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(exLinks.sortedBy { it.orderIndex }) { link ->
                val ex = allExercises.find { it.id == link.exerciseId }
                val idx = exLinks.indexOf(link)
                BoardTile(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(ex?.name ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${link.targetValueLow}-${link.targetValueHigh} ${link.repType.name}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (idx > 0) {
                                TextAction("Up", onClick = { viewModel.swapBlockExercises(link.id, exLinks[idx - 1].id, blockId) })
                            }
                            if (idx < exLinks.lastIndex) {
                                TextAction("Down", onClick = { viewModel.swapBlockExercises(link.id, exLinks[idx + 1].id, blockId) })
                            }
                            RowMenu(onEdit = { editingLink = link }, onDelete = { pendingDelete = link })
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { link ->
        BoardDialog(
            title = "Delete this exercise?",
            message = "It leaves this block. The exercise stays in the dictionary.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteBlockExercise(link)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }

    if (showDialog || editingLink != null) {
        val original = editingLink
        var selectedExId by remember(original?.id) { mutableStateOf(original?.exerciseId) }
        var expanded by remember { mutableStateOf(false) }
        var low by remember(original?.id) { mutableStateOf((original?.targetValueLow ?: 8).toString()) }
        var high by remember(original?.id) { mutableStateOf((original?.targetValueHigh ?: 12).toString()) }
        var repType by remember(original?.id) { mutableStateOf(original?.repType ?: RepType.REPS) }
        var perSide by remember(original?.id) { mutableStateOf(original?.perSide ?: false) }

        BoardDialog(
            title = if (original == null) "Add Exercise" else "Edit Exercise Target",
            onDismiss = { showDialog = false; editingLink = null },
            confirmText = if (original == null) "Add" else "Save",
            onConfirm = {
                val l = low.toIntOrNull() ?: 1
                val h = high.toIntOrNull() ?: l
                if (selectedExId != null && l > 0 && l <= h) {
                    if (original == null) {
                        viewModel.addExerciseToBlock(blockId, selectedExId!!, l, h, repType, perSide)
                    } else {
                        viewModel.updateBlockExercise(original.copy(exerciseId = selectedExId!!, targetValueLow = l, targetValueHigh = h, repType = repType, perSide = perSide))
                    }
                    showDialog = false
                    editingLink = null
                }
            },
            content = {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    val selName = allExercises.find { it.id == selectedExId }?.name ?: "Select Exercise"
                    OutlinedTextField(
                        value = selName, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        allExercises.forEach { e ->
                            DropdownMenuItem(text = { Text(e.name) }, onClick = { selectedExId = e.id; expanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = low, onValueChange = { low = it }, label = { Text("Minimum") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = high, onValueChange = { high = it }, label = { Text("Maximum") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RepType.entries.forEach { type ->
                        MonoChip(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            selected = repType == type,
                            onClick = { repType = type },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Per side", modifier = Modifier.weight(1f))
                    Switch(checked = perSide, onCheckedChange = { perSide = it })
                }
            },
        )
    }
}

@Composable
private fun WeekdayChips(selected: Int?, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DayOfWeek.entries.forEach { day ->
            MonoChip(
                text = day.getDisplayName(TextStyle.SHORT, Locale.US),
                selected = selected == day.value,
                onClick = { onSelect(day.value) },
            )
        }
    }
}

@Composable
private fun RowMenu(
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (onEdit != null) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { open = false; onEdit() })
            }
            if (onDuplicate != null) {
                DropdownMenuItem(text = { Text("Duplicate") }, onClick = { open = false; onDuplicate() })
            }
            DropdownMenuItem(text = { Text("Delete") }, onClick = { open = false; onDelete() })
        }
    }
}

private fun BlockKind.editorLabel(): String = when (this) {
    BlockKind.WARM_UP -> "Warm-up"
    BlockKind.SUPERSET -> "Superset"
    BlockKind.STANDARD -> "Standard"
}

