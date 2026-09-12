package com.example.repsgrams.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.db.SupplementEntity
import com.example.repsgrams.data.repository.SupplementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageSupplementsViewModel(private val supplementRepository: SupplementRepository) : ViewModel() {
    val supplements: StateFlow<List<SupplementEntity>> = supplementRepository.observeAllSupplements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        fun provideFactory(repository: SupplementRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ManageSupplementsViewModel(repository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSupplementsRoute(
    viewModel: ManageSupplementsViewModel,
    onNavigateBack: () -> Unit,
) {
    val supplements by viewModel.supplements.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Supplements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Basic implementation for now
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Custom Supplements List", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            supplements.forEach { supplement ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(supplement.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${supplement.doseAmount} ${supplement.unit} (${supplement.scheduleType})", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
