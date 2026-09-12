with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

# Replace all `state.slot` with `state.suggestion.slot`
text = text.replace("state.slot", "state.suggestion.slot")
text = text.replace("item { SupplementCard(state,  onWheyChanged) }", "item { SupplementCard(state, viewModel) }")

# Replace SupplementCard and SupplementRow
import re
text = re.sub(r'@Composable\nprivate fun SupplementCard\([\s\S]*',
"""@Composable
private fun SupplementCard(state: TodayUiState.Content, viewModel: TodayViewModel) {
    com.example.repsgrams.ui.components.IosCard {
        Column {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Supplements today", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            if (state.supplements.isEmpty()) {
                Text("No supplements scheduled today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
            }
            state.supplements.forEachIndexed { index, suppState ->
                val (supp, taken) = suppState
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setSupplementTaken(supp, !taken) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    com.example.repsgrams.ui.components.IconBadge(
                        icon = androidx.compose.material.icons.Icons.Outlined.Science, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(supp.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${supp.doseAmount} ${supp.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = taken,
                        onCheckedChange = { viewModel.setSupplementTaken(supp, it) }
                    )
                }
                if (index < state.supplements.lastIndex) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp), 
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}
""", text)

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)

