package com.example.repsgrams.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items



import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.FitnessCenter
import com.example.repsgrams.ui.theme.AppColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.ExerciseEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDictionaryRoute(
    viewModel: ExerciseDictionaryViewModel,
    onBack: () -> Unit
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var showDialog by remember { mutableStateOf(false) }
    var editingExercise by remember { mutableStateOf<ExerciseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Dictionary") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingExercise = null; showDialog = true }) {
                Text("+")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(exercises) { ex ->
                com.example.repsgrams.ui.components.IosCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.85f)
                        .clickable {
                            editingExercise = ex
                            showDialog = true
                        }
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ex.imageAssetName != null) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val resId = context.resources.getIdentifier(ex.imageAssetName, "drawable", context.packageName)
                                if (resId != 0) {
                                    androidx.compose.foundation.Image(
                                        painter = androidx.compose.ui.res.painterResource(id = resId),
                                        contentDescription = ex.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(ex.name.take(1).uppercase(), style = MaterialTheme.typography.displayMedium, color = AppColors.workout)
                                }
                            } else {
                                Text(ex.name.take(1).uppercase(), style = MaterialTheme.typography.displayMedium, color = AppColors.workout)
                            }
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(ex.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(ex.muscleGroup, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ExerciseDialog(
            exercise = editingExercise,
            onDismiss = { showDialog = false },
            onSave = { name, tracks, notes, muscleGroup ->
                if (editingExercise == null) {
                    viewModel.addExercise(name, tracks, notes, muscleGroup)
                } else {
                    viewModel.updateExercise(editingExercise!!, name, tracks, notes, muscleGroup)
                }
                showDialog = false
            },
            onDelete = {
                editingExercise?.let { ex ->
                    viewModel.deleteExercise(ex) { success ->
                        if (!success) {
                            scope.launch { snackbarHostState.showSnackbar("Cannot delete exercise that has past logs.") }
                        }
                        showDialog = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDialog(
    exercise: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String?, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var tracksWeight by remember { mutableStateOf(exercise?.tracksWeight ?: true) }
    var notes by remember { mutableStateOf(exercise?.notes ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "Back") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exercise == null) "New Exercise" else "Edit Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tracksWeight, onCheckedChange = { tracksWeight = it })
                    Text("Tracks Weight")
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = muscleGroup,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Muscle Group") },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Back", "Chest", "Legs", "Arms", "Shoulders", "Core", "Full Body", "Uncategorized").forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group) },
                                onClick = { muscleGroup = group; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, tracksWeight, notes, muscleGroup) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (exercise != null) {
                    TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
