package com.example.repsgrams.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew

import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.ui.components.IosCard
import com.example.repsgrams.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    exercises: List<ExerciseEntity>,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val allCategories = remember(exercises) {
        listOf("All") + exercises.map { it.muscleGroup }.distinct().sorted()
    }

    val filtered = exercises.filter {
        it.name.contains(searchQuery, ignoreCase = true) &&
        (selectedCategory == null || selectedCategory == "All" || it.muscleGroup == selectedCategory)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Library") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { cat ->
                    val isSelected = selectedCategory == cat || (selectedCategory == null && cat == "All")
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = if (cat == "All") null else cat },
                        label = { Text(cat) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No exercises found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(filtered) { ex ->
                        IosCard {
                            Column {
                                val resourceId = com.example.repsgrams.ui.components.exerciseImageResource(ex.imageAssetName)
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(MaterialTheme.shapes.large).background(AppColors.workout.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (resourceId != 0) {
                                        androidx.compose.foundation.Image(
                                            painter = painterResource(resourceId),
                                            contentDescription = ex.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = AppColors.workout, modifier = Modifier.size(48.dp))
                                    }
                                }
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(ex.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Muscle group badge
                                    Surface(
                                        color = AppColors.wheyGreen.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                    ) {
                                        Text(
                                            ex.muscleGroup,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AppColors.wheyGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
