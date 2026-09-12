package com.example.repsgrams.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions






import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
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
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Template") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = day, onValueChange = { day = it }, label = { Text("Day Label (A/B)") })
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.addTemplate(name, day); showDialog = false }) { Text("Save") }
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

    if (showDialog) {
        val defaultRest by viewModel.defaultRestSeconds.collectAsStateWithLifecycle(90)
        var label by remember { mutableStateOf("") }
        var rMin by remember { mutableStateOf("3") }
        var rMax by remember { mutableStateOf("5") }
        var rest by remember(defaultRest) { mutableStateOf(defaultRest.toString()) }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("New Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (e.g. A, B1)") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = rMin, onValueChange = { rMin = it }, label = { Text("Min Rds") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = rMax, onValueChange = { rMax = it }, label = { Text("Max Rds") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    OutlinedTextField(value = rest, onValueChange = { rest = it }, label = { Text("Rest (s)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val min = rMin.toIntOrNull() ?: 1
                    val max = rMax.toIntOrNull() ?: min
                    val r = rest.toIntOrNull() ?: 60
                    if (min <= max) {
                        viewModel.addBlock(templateId, label, com.example.repsgrams.data.db.BlockKind.STANDARD, min, max, r, false)
                        showDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
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

    if (showDialog) {
        var selectedExId by remember { mutableStateOf<Long?>(null) }
        var expanded by remember { mutableStateOf(false) }
        var low by remember { mutableStateOf("8") }
        var high by remember { mutableStateOf("12") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Exercise") },
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
                        OutlinedTextField(value = low, onValueChange = { low = it }, label = { Text("Min Reps") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        OutlinedTextField(value = high, onValueChange = { high = it }, label = { Text("Max Reps") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val l = low.toIntOrNull() ?: 1
                    val h = high.toIntOrNull() ?: l
                    if (selectedExId != null && l <= h) {
                        viewModel.addExerciseToBlock(blockId, selectedExId!!)
                        showDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel") } }
        )
    }
}
