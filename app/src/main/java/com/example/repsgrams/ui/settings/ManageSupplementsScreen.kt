package com.example.repsgrams.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.db.SupplementEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.ui.components.IosCard
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageSupplementsViewModel(private val repository: SupplementRepository) : ViewModel() {
    val supplements: StateFlow<List<SupplementEntity>> = repository.observeAllSupplements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val inventory: StateFlow<List<SupplyInventoryEntity>> = repository.observeInventory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    fun save(item: SupplementEntity) = viewModelScope.launch {
        if (item.id == 0L) repository.addSupplement(item) else repository.updateSupplement(item)
    }
    fun setActive(item: SupplementEntity, active: Boolean) = viewModelScope.launch {
        repository.updateSupplement(item.copy(isActive = active))
    }
    fun restock(id: Long, servings: Int) = viewModelScope.launch { repository.restock(id, servings) }
    fun delete(item: SupplementEntity) = viewModelScope.launch {
        if (!repository.deleteSupplement(item)) {
            _messages.emit("This supplement has intake history. Deactivate it instead of deleting it.")
        }
    }

    companion object {
        fun provideFactory(repository: SupplementRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = ManageSupplementsViewModel(repository) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSupplementsRoute(viewModel: ManageSupplementsViewModel, onNavigateBack: () -> Unit) {
    val supplements by viewModel.supplements.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<SupplementEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var restocking by remember { mutableStateOf<SupplementEntity?>(null) }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Supplements", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { creating = true }) { Text("Add supplement") } },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(supplements, key = { it.id }) { item ->
                val stock = inventory.firstOrNull { it.supplementId == item.id }
                IosCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("${item.doseAmount.clean()} ${item.unit} · ${item.scheduleType.scheduleLabel()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${stock?.servingsRemaining?.clean() ?: "—"} servings remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = item.isActive, onCheckedChange = { viewModel.setActive(item, it) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = item }, modifier = Modifier.weight(1f)) { Text("Edit") }
                            OutlinedButton(onClick = { restocking = item }, modifier = Modifier.weight(1f)) { Text("Restock") }
                            TextButton(onClick = { viewModel.delete(item) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (creating || editing != null) {
        SupplementEditorDialog(
            original = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { viewModel.save(it); creating = false; editing = null },
        )
    }
    restocking?.let { item ->
        var servings by remember(item.id) { mutableStateOf(item.containerSize.toString()) }
        AlertDialog(
            onDismissRequest = { restocking = null },
            title = { Text("Restock ${item.name}") },
            text = { OutlinedTextField(servings, { servings = it }, label = { Text("Total servings") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) },
            confirmButton = { Button(onClick = { servings.toIntOrNull()?.takeIf { it > 0 }?.let { viewModel.restock(item.id, it) }; restocking = null }) { Text("Restock") } },
            dismissButton = { TextButton(onClick = { restocking = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SupplementEditorDialog(original: SupplementEntity?, onDismiss: () -> Unit, onSave: (SupplementEntity) -> Unit) {
    var name by remember(original?.id) { mutableStateOf(original?.name.orEmpty()) }
    var amount by remember(original?.id) { mutableStateOf((original?.doseAmount ?: 1f).clean()) }
    var unit by remember(original?.id) { mutableStateOf(original?.unit ?: "serving") }
    var schedule by remember(original?.id) { mutableStateOf(original?.scheduleType ?: "daily") }
    var customDays by remember(original?.id) { mutableStateOf(original?.customDays.orEmpty()) }
    var container by remember(original?.id) { mutableStateOf((original?.containerSize ?: 30).toString()) }
    var threshold by remember(original?.id) { mutableStateOf((original?.lowSupplyThreshold ?: 5).toString()) }
    var colorToken by remember(original?.id) { mutableStateOf(original?.colorToken ?: "blue") }
    var iconName by remember(original?.id) { mutableStateOf(original?.iconName ?: "supplement") }
    val dose = amount.toFloatOrNull()
    val size = container.toIntOrNull()
    val low = threshold.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original == null) "New Supplement" else "Edit Supplement") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(amount, { amount = it }, label = { Text("Dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                }
                item {
                    Text("Color", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("orange", "teal", "green", "purple", "blue").forEach { value ->
                            FilterChip(selected = colorToken == value, onClick = { colorToken = value }, label = { Text(value.replaceFirstChar { it.uppercase() }) })
                        }
                    }
                }
                item {
                    Text("Icon", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("supplement", "science", "water_drop", "bolt").forEach { value ->
                            FilterChip(selected = iconName == value, onClick = { iconName = value }, label = { Text(value.replace('_', ' ').replaceFirstChar { it.uppercase() }) })
                        }
                    }
                }
                item {
                    Text("Schedule", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("daily", "workoutDayOnly", "customDays").forEach { value ->
                            FilterChip(selected = schedule == value, onClick = { schedule = value }, label = { Text(value.scheduleLabel()) })
                        }
                    }
                }
                if (schedule == "customDays") item { OutlinedTextField(customDays, { customDays = it }, label = { Text("Days, e.g. MONDAY,WEDNESDAY") }) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(container, { container = it }, label = { Text("Container servings") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(threshold, { threshold = it }, label = { Text("Low at") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && unit.isNotBlank() && dose != null && dose > 0 && size != null && size > 0 && low != null && low >= 0,
                onClick = {
                    val base = original ?: SupplementEntity(name = "", doseAmount = 1f, unit = "serving", scheduleType = "daily", containerSize = 30, lowSupplyThreshold = 5, colorToken = "blue", iconName = "supplement")
                    onSave(base.copy(name = name.trim(), doseAmount = dose!!, unit = unit.trim(), scheduleType = schedule, customDays = customDays.takeIf { schedule == "customDays" && it.isNotBlank() }, containerSize = size!!, lowSupplyThreshold = low!!, colorToken = colorToken, iconName = iconName))
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun Float.clean(): String = if (this % 1f == 0f) toInt().toString() else toString()
private fun String.scheduleLabel(): String = when (this) {
    "workoutDayOnly" -> "Workout days"
    "customDays" -> "Custom days"
    else -> "Daily"
}
