package com.example.repsgrams.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.ui.components.BackChevron
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.MonoChip
import com.example.repsgrams.ui.components.MonoLabel
import com.example.repsgrams.ui.components.TextAction
import com.example.repsgrams.ui.components.exerciseImageResource
import java.util.Locale
import kotlinx.coroutines.launch

private val MUSCLE_GROUPS = listOf("Back", "Chest", "Legs", "Arms", "Shoulders", "Core", "Full Body")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDictionaryRoute(
    viewModel: ExerciseDictionaryViewModel,
    onBack: () -> Unit,
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<ExerciseEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExerciseEntity?>(null) }
    val muscles = remember(exercises) { listOf("All") + exercises.map { it.muscleGroup }.distinct().sorted() }
    val shown = exercises
        .filter { it.name.contains(query, ignoreCase = true) }
        .filter { muscle == null || it.muscleGroup == muscle }
        .sortedBy { it.name.lowercase(Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercises") },
                navigationIcon = { BackChevron(onClick = onBack) },
                actions = {
                    TextAction("+", onClick = { editing = null; creating = true }, contentDescription = "Add")
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                singleLine = true,
                decorationBox = { inner ->
                    Column {
                        MonoLabel("Search")
                        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("Exercise", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            inner()
                        }
                    }
                },
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(muscles) { name ->
                    val selected = muscle == name || (muscle == null && name == "All")
                    MonoChip(name, selected, onClick = { muscle = if (name == "All") null else name })
                }
            }
            if (shown.isEmpty()) {
                Text(
                    "No exercise",
                    modifier = Modifier.padding(16.dp).heightIn(min = 48.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.id }) { exercise ->
                        BoardTile(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            onClick = { editing = exercise },
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(exercise.name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                MonoLabel(exercise.muscleGroup)
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        ExerciseDialog(
            exercise = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, tracks, notes, muscleGroup ->
                val current = editing
                if (current == null) viewModel.addExercise(name, tracks, notes, muscleGroup)
                else viewModel.updateExercise(current, name, tracks, notes, muscleGroup)
                creating = false
                editing = null
            },
            onDelete = {
                pendingDelete = editing
                editing = null
                creating = false
            },
        )
    }
    pendingDelete?.let { exercise ->
        BoardDialog(
            title = "Delete exercise",
            message = "Logged sessions that use it stay. An exercise with logs cannot be deleted.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.deleteExercise(exercise) { success ->
                    if (!success) {
                        scope.launch { snackbarHostState.showSnackbar("Cannot delete exercise that has past logs.") }
                    }
                }
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun ExerciseDialog(
    exercise: ExerciseEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String?, String) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var tracksWeight by remember(exercise?.id) { mutableStateOf(exercise?.tracksWeight ?: true) }
    var notes by remember(exercise?.id) { mutableStateOf(exercise?.notes.orEmpty()) }
    var muscleGroup by remember(exercise?.id) { mutableStateOf(exercise?.muscleGroup ?: "Back") }
    val muscles = if (muscleGroup in MUSCLE_GROUPS) MUSCLE_GROUPS else listOf(muscleGroup) + MUSCLE_GROUPS
    val image = exerciseImageResource(exercise?.imageAssetName)
    BoardDialog(
        title = if (exercise == null) "New exercise" else "Edit exercise",
        onDismiss = onDismiss,
        confirmText = "Save",
        confirmEnabled = name.isNotBlank() && muscleGroup.isNotBlank(),
        onConfirm = { onSave(name, tracksWeight, notes, muscleGroup) },
    ) {
        if (image != 0) {
            Image(
                painter = painterResource(image),
                contentDescription = exercise?.name,
                modifier = Modifier.fillMaxWidth().height(96.dp),
                contentScale = ContentScale.Fit,
            )
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tracks weight", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            androidx.compose.material3.Switch(checked = tracksWeight, onCheckedChange = { tracksWeight = it })
        }
        MonoLabel("Muscle")
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            muscles.forEach { group ->
                MonoChip(group, muscleGroup == group, onClick = { muscleGroup = group })
            }
        }
        if (exercise != null) {
            TextAction("Delete", onClick = onDelete)
        }
    }
}
