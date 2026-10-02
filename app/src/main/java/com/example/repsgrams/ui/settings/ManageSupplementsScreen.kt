package com.example.repsgrams.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
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
import com.example.repsgrams.ui.components.BackChevron
import com.example.repsgrams.ui.components.BoardDialog
import com.example.repsgrams.ui.components.BoardTile
import com.example.repsgrams.ui.components.MonoChip
import com.example.repsgrams.ui.components.RestockDialog
import com.example.repsgrams.ui.components.TextAction
import com.example.repsgrams.ui.components.tileMuted
import com.example.repsgrams.ui.theme.doneGreen
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
    var pendingDelete by remember { mutableStateOf<SupplementEntity?>(null) }
    LaunchedEffect(viewModel) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Supplements", fontWeight = FontWeight.Bold) },
                navigationIcon = { BackChevron(onClick = onNavigateBack) },
                actions = { TextAction("+", onClick = { creating = true }, contentDescription = "Add") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(supplements, key = { it.id }) { item ->
                val stock = inventory.firstOrNull { it.supplementId == item.id }
                var menu by remember(item.id) { mutableStateOf(false) }
                BoardTile(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.setActive(item, !item.isActive) },
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${item.doseAmount.clean()} ${item.unit} · ${item.scheduleType.scheduleLabel()}",
                                color = tileMuted(),
                            )
                            Text(
                                "${stock?.servingsRemaining?.clean() ?: "—"} servings remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = tileMuted(),
                            )
                        }
                        Text(
                            if (item.isActive) "On" else "Off",
                            modifier = Modifier.padding(end = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (item.isActive) doneGreen() else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box {
                            IconButton(onClick = { menu = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; editing = item })
                                DropdownMenuItem(text = { Text("Restock") }, onClick = { menu = false; restocking = item })
                                DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; pendingDelete = item })
                            }
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
    pendingDelete?.let { item ->
        BoardDialog(
            title = "Delete ${item.name}",
            message = "A supplement with intake history is deactivated instead of deleted.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                viewModel.delete(item)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
    restocking?.let { item ->
        RestockDialog(
            name = item.name,
            initialServings = item.containerSize.toString(),
            onDismiss = { restocking = null },
            onConfirm = { servings ->
                viewModel.restock(item.id, servings)
                restocking = null
            },
        )
    }
}

@Composable
private fun SupplementEditorDialog(original: SupplementEntity?, onDismiss: () -> Unit, onSave: (SupplementEntity) -> Unit) {
    var name by remember(original?.id) { mutableStateOf(original?.name.orEmpty()) }
    var amount by remember(original?.id) { mutableStateOf((original?.doseAmount ?: 1f).clean()) }
    var unit by remember(original?.id) { mutableStateOf(original?.unit ?: "serving") }
    var schedule by remember(original?.id) { mutableStateOf(original?.scheduleType ?: "daily") }
    var container by remember(original?.id) { mutableStateOf((original?.containerSize ?: 30).toString()) }
    var threshold by remember(original?.id) { mutableStateOf((original?.lowSupplyThreshold ?: 5).toString()) }
    var days by remember(original?.id) { mutableStateOf(parseWeekdays(original?.customDays)) }
    val dose = amount.toFloatOrNull()
    val size = container.toIntOrNull()
    val low = threshold.toIntOrNull()
    val canSave = name.isNotBlank() && unit.isNotBlank() && dose != null && dose > 0 && size != null && size > 0 && low != null && low >= 0 &&
        (schedule != "customDays" || days.isNotEmpty())
    BoardDialog(
        title = if (original == null) "New Supplement" else "Edit Supplement",
        onDismiss = onDismiss,
        confirmText = "Save",
        confirmEnabled = canSave,
        onConfirm = {
            val base = original ?: SupplementEntity(name = "", doseAmount = 1f, unit = "serving", scheduleType = "daily", containerSize = 30, lowSupplyThreshold = 5, colorToken = "blue", iconName = "supplement")
            onSave(
                base.copy(
                    name = name.trim(),
                    doseAmount = dose!!,
                    unit = unit.trim(),
                    scheduleType = schedule,
                    customDays = formatWeekdays(days).takeIf { schedule == "customDays" },
                    containerSize = size!!,
                    lowSupplyThreshold = low!!,
                ),
            )
        },
        content = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(amount, { amount = it }, label = { Text("Dose") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text("Schedule", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("daily", "workoutDayOnly", "customDays").forEach { value ->
                        MonoChip(text = value.scheduleLabel(), selected = schedule == value, onClick = { schedule = value })
                    }
                }
                if (schedule == "customDays") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        WEEKDAYS.forEach { day ->
                            MonoChip(
                                text = day,
                                selected = day in days,
                                onClick = { days = if (day in days) days - day else days + day },
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(container, { container = it }, label = { Text("Container servings") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(threshold, { threshold = it }, label = { Text("Low at") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
    )
}

private val WEEKDAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

internal fun parseWeekdays(stored: String?): Set<String> {
    if (stored.isNullOrBlank()) return emptySet()
    val tokens = stored.split(',').map { it.trim().uppercase() }
    return WEEKDAYS.filter { code -> tokens.any { it == code || it.startsWith(code) } }.toSet()
}

internal fun formatWeekdays(days: Set<String>): String =
    WEEKDAYS.filter { it in days }.joinToString(",")

private fun Float.clean(): String = if (this % 1f == 0f) toInt().toString() else toString()
private fun String.scheduleLabel(): String = when (this) {
    "workoutDayOnly" -> "Workout days"
    "customDays" -> "Custom"
    else -> "Daily"
}
