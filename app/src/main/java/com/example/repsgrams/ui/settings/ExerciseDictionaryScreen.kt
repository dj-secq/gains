package com.example.repsgrams.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items



import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
                    IconButton(onClick = onBack) { Text("<") }
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(exercises) { ex ->
                ListItem(
                    headlineContent = { Text(ex.name) },
                    supportingContent = { Text(if (ex.tracksWeight) "Weighted" else "Bodyweight") },
                    modifier = Modifier.clickable {
                        editingExercise = ex
                        showDialog = true
                    }
                )
                HorizontalDivider()
            }
        }
    }

    if (showDialog) {
        ExerciseDialog(
            exercise = editingExercise,
            onDismiss = { showDialog = false },
            onSave = { name, tracks, notes ->
                if (editingExercise == null) {
                    viewModel.addExercise(name, tracks, notes)
                } else {
                    viewModel.updateExercise(editingExercise!!, name, tracks, notes)
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

@Composable
private fun ExerciseDialog(
    exercise: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var tracksWeight by remember { mutableStateOf(exercise?.tracksWeight ?: true) }
    var notes by remember { mutableStateOf(exercise?.notes ?: "") }

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
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, tracksWeight, notes) }, enabled = name.isNotBlank()) {
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
