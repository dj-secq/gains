package com.example.repsgrams.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions






import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.db.TemplateBlockEntity
import com.example.repsgrams.data.db.TemplateBlockExerciseEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListRoute(
    viewModel: ProgramEditorViewModel,
    onNavigateToTemplate: (Long) -> Unit,
    onBack: () -> Unit
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbarHostState.showSnackbar(it) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Workout Templates") },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(templates) { template ->
                ListItem(
                    headlineContent = { Text(template.name) },
                    supportingContent = { Text("Day: ${template.dayLabel}") },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val idx = templates.indexOf(template)
                            Text(
                                "☰",
                                modifier = Modifier.padding(8.dp).dragToReorder(
                                    onUp = { if (idx > 0) viewModel.swapTemplates(template.id, templates[idx - 1].id) },
                                    onDown = { if (idx < templates.lastIndex) viewModel.swapTemplates(template.id, templates[idx + 1].id) },
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (idx > 0) {
                                IconButton(onClick = { viewModel.swapTemplates(template.id, templates[idx - 1].id) }) {
                                    Text("↑")
                                }
                            }
                            if (idx < templates.size - 1) {
                                IconButton(onClick = { viewModel.swapTemplates(template.id, templates[idx + 1].id) }) {
                                    Text("↓")
                                }
                            }
                            IconButton(onClick = { viewModel.deleteTemplate(template) }) {
                                Text("X", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    modifier = Modifier.clickable { onNavigateToTemplate(template.id) }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var day by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Custom") }
        var customCategory by remember { mutableStateOf("") }
        var restDays by remember { mutableIntStateOf(1) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Day Label (A/B)") })
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.example.repsgrams.ui.theme.CategoryColors.Categories.forEach { option ->
                            FilterChip(selected = category == option, onClick = { category = option }, label = { Text(option) })
                        }
                    }
                    if (category == "Custom") {
                        OutlinedTextField(value = customCategory, onValueChange = { customCategory = it }, label = { Text("Custom category (optional)") })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rest days after: $restDays", modifier = Modifier.weight(1f))
                        IconButton(onClick = { restDays = (restDays - 1).coerceAtLeast(0) }) { Text("−") }
                        IconButton(onClick = { restDays = (restDays + 1).coerceAtMost(7) }) { Text("+") }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.addTemplate(name, day, customCategory.trim().takeIf { category == "Custom" && it.isNotEmpty() } ?: category, restDays); showDialog = false },
                    enabled = name.isNotBlank() && day.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
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

    val template by remember(templateId) { viewModel.observeTemplate(templateId) }.collectAsStateWithLifecycle(null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Template Blocks") },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
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

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Rest Days After: ${t.restDaysAfter}")
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { if (t.restDaysAfter > 0) viewModel.updateTemplateRestDays(t.id, t.restDaysAfter - 1) }) { Text("-") }
                            IconButton(onClick = { viewModel.updateTemplateRestDays(t.id, t.restDaysAfter + 1) }) { Text("+") }
                        }
                    }
                    HorizontalDivider()
                }
            }

            items(blocks.sortedBy { it.orderIndex }) { block ->
                ListItem(
                    headlineContent = { Text(block.label) },
                    supportingContent = { Text("${block.targetRoundsMin}-${block.targetRoundsMax} rounds, ${block.restSecondsBetweenRounds}s rest") },
                    trailingContent = {
                        Row {
                            val idx = blocks.indexOf(block)
                            Text(
                                "☰",
                                modifier = Modifier.padding(8.dp).dragToReorder(
                                    onUp = { if (idx > 0) viewModel.swapBlocks(block.id, blocks[idx - 1].id, templateId) },
                                    onDown = { if (idx < blocks.lastIndex) viewModel.swapBlocks(block.id, blocks[idx + 1].id, templateId) },
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { editingBlock = block }) { Text("Edit") }
                            IconButton(onClick = {
                                val idx = blocks.indexOf(block)
                                if (idx > 0) viewModel.swapBlocks(block.id, blocks[idx - 1].id, templateId)
                            }) { Text("↑") }
                            IconButton(onClick = {
                                val idx = blocks.indexOf(block)
                                if (idx < blocks.size - 1) viewModel.swapBlocks(block.id, blocks[idx + 1].id, templateId)
                            }) { Text("↓") }
                            IconButton(onClick = { viewModel.deleteBlock(block) }) { Text("X", color = MaterialTheme.colorScheme.error) }
                        }
                    },
                    modifier = Modifier.clickable { onNavigateToBlock(block.id) }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog || editingBlock != null) {
        val defaultRest by viewModel.defaultRestSeconds.collectAsStateWithLifecycle(90)
        val original = editingBlock
        var label by remember(original?.id) { mutableStateOf(original?.label.orEmpty()) }
        var rMin by remember(original?.id) { mutableStateOf((original?.targetRoundsMin ?: 3).toString()) }
        var rMax by remember(original?.id) { mutableStateOf((original?.targetRoundsMax ?: 5).toString()) }
        var rest by remember(original?.id, defaultRest) { mutableStateOf((original?.restSecondsBetweenRounds ?: defaultRest).toString()) }
        var restAfter by remember(original?.id, defaultRest) { mutableStateOf((original?.restSecondsAfterBlock ?: defaultRest).toString()) }
        var optional by remember(original?.id) { mutableStateOf(original?.isOptional ?: false) }
        AlertDialog(
            onDismissRequest = { showDialog = false; editingBlock = null },
            title = { Text(if (original == null) "New Block" else "Edit Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. A, B1)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = rMin, onValueChange = { rMin = it }, label = { Text("Min Rds") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = rMax, onValueChange = { rMax = it }, label = { Text("Max Rds") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = rest, onValueChange = { rest = it }, label = { Text("Rest between rounds (s)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = restAfter, onValueChange = { restAfter = it }, label = { Text("Rest before next block (s)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Optional", modifier = Modifier.weight(1f))
                        Switch(checked = optional, onCheckedChange = { optional = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val min = rMin.toIntOrNull() ?: 1
                    val max = rMax.toIntOrNull() ?: min
                    val r = rest.toIntOrNull() ?: 60
                    val rAfter = restAfter.toIntOrNull() ?: 60
                    if (label.isNotBlank() && min > 0 && min <= max && r >= 0 && rAfter >= 0) {
                        if (original == null) {
                            viewModel.addBlock(templateId, label.trim(), com.example.repsgrams.data.db.BlockKind.STANDARD, min, max, r, rAfter, optional)
                        } else {
                            viewModel.updateBlock(
                                original.copy(
                                    label = label.trim(),
                                    targetRoundsMin = min,
                                    targetRoundsMax = max,
                                    restSecondsBetweenRounds = r,
                                    restSecondsAfterBlock = rAfter,
                                    isOptional = optional,
                                ),
                            )
                        }
                        showDialog = false
                        editingBlock = null
                    }
                }) { Text(if (original == null) "Add" else "Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false; editingBlock = null }) { Text("Cancel") } }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Block Exercises") },
                navigationIcon = { IconButton(onClick = onBack) { Text("<") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) { Text("+") }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(exLinks.sortedBy { it.orderIndex }) { link ->
                val ex = allExercises.find { it.id == link.exerciseId }
                ListItem(
                    headlineContent = { Text(ex?.name ?: "Unknown") },
                    supportingContent = { Text("${link.targetValueLow}-${link.targetValueHigh} ${link.repType.name}") },
                    trailingContent = {
                        Row {
                            val idx = exLinks.indexOf(link)
                            Text(
                                "☰",
                                modifier = Modifier.padding(8.dp).dragToReorder(
                                    onUp = { if (idx > 0) viewModel.swapBlockExercises(link.id, exLinks[idx - 1].id, blockId) },
                                    onDown = { if (idx < exLinks.lastIndex) viewModel.swapBlockExercises(link.id, exLinks[idx + 1].id, blockId) },
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { editingLink = link }) { Text("Edit") }
                            IconButton(onClick = {
                                val idx = exLinks.indexOf(link)
                                if (idx > 0) viewModel.swapBlockExercises(link.id, exLinks[idx - 1].id, blockId)
                            }) { Text("↑") }
                            IconButton(onClick = {
                                val idx = exLinks.indexOf(link)
                                if (idx < exLinks.size - 1) viewModel.swapBlockExercises(link.id, exLinks[idx + 1].id, blockId)
                            }) { Text("↓") }
                            IconButton(onClick = { viewModel.deleteBlockExercise(link) }) { Text("X", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog || editingLink != null) {
        val original = editingLink
        var selectedExId by remember(original?.id) { mutableStateOf(original?.exerciseId) }
        var expanded by remember { mutableStateOf(false) }
        var low by remember(original?.id) { mutableStateOf((original?.targetValueLow ?: 8).toString()) }
        var high by remember(original?.id) { mutableStateOf((original?.targetValueHigh ?: 12).toString()) }
        var repType by remember(original?.id) { mutableStateOf(original?.repType ?: RepType.REPS) }
        var perSide by remember(original?.id) { mutableStateOf(original?.perSide ?: false) }

        AlertDialog(
            onDismissRequest = { showDialog = false; editingLink = null },
            title = { Text(if (original == null) "Add Exercise" else "Edit Exercise Target") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            FilterChip(selected = repType == type, onClick = { repType = type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Per side", modifier = Modifier.weight(1f))
                        Switch(checked = perSide, onCheckedChange = { perSide = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
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
                }) { Text(if (original == null) "Add" else "Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false; editingLink = null }) { Text("Cancel") } }
        )
    }
}

private fun Modifier.dragToReorder(onUp: () -> Unit, onDown: () -> Unit): Modifier = pointerInput(onUp, onDown) {
    detectDragGesturesAfterLongPress { change, dragAmount ->
        change.consume()
        when {
            dragAmount.y < -8.dp.toPx() -> onUp()
            dragAmount.y > 8.dp.toPx() -> onDown()
        }
    }
}
