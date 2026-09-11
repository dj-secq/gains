import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

# Imports
imports = """import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import com.example.repsgrams.ui.theme.AppColors
import com.example.repsgrams.ui.components.IconBadge
import androidx.compose.ui.graphics.vector.ImageVector
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

# TopAppBar navigationIcon
nav_icon_old = """                navigationIcon = {
                    if (state !is WorkoutSessionUiState.Summary) {
                        TextButton(onClick = onBack) {
                            Text("< Back", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                },"""
nav_icon_new = """                navigationIcon = {
                    if (state !is WorkoutSessionUiState.Summary) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },"""
text = text.replace(nav_icon_old, nav_icon_new)

# +/- buttons for values
adjust_val_old = """            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { onAdjustValue(-1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("−") }
                OutlinedTextField(
                    value = state.valueInput,
                    onValueChange = onValueChanged,
                    label = { Text(if (exercise.repType == RepType.REPS) "Reps" else "Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedButton(onClick = { onAdjustValue(1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("+") }
            }"""
adjust_val_new = """            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(onClick = { onAdjustValue(-1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                OutlinedTextField(
                    value = state.valueInput,
                    onValueChange = onValueChanged,
                    label = { Text(if (exercise.repType == RepType.REPS) "Reps" else "Seconds") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                FilledTonalIconButton(onClick = { onAdjustValue(1) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }
            }"""
text = text.replace(adjust_val_old, adjust_val_new)

# +/- buttons for weight
adjust_weight_old = """                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onAdjustWeight(-step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("−") }
                    OutlinedTextField(
                        value = state.weightInput,
                        onValueChange = onWeightChanged,
                        label = { Text("Weight (${state.unitSystem.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedButton(onClick = { onAdjustWeight(step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small) { Text("+") }
                }"""
adjust_weight_new = """                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { onAdjustWeight(-step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Remove, contentDescription = "-") }
                    OutlinedTextField(
                        value = state.weightInput,
                        onValueChange = onWeightChanged,
                        label = { Text("Weight (${state.unitSystem.name.lowercase()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    FilledTonalIconButton(onClick = { onAdjustWeight(step) }, modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.small, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = AppColors.workout.copy(alpha=0.1f), contentColor = AppColors.workout)) { Icon(Icons.Outlined.Add, contentDescription = "+") }
                }"""
text = text.replace(adjust_weight_old, adjust_weight_new)

# Summary check rows
check_card_old = """            IosCard {
                Column {
                    CheckRow("Whey protein taken", state.wheyTaken, onWheyChanged)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    CheckRow("Creatine taken", state.creatineTaken, onCreatineChanged)
                }
            }"""
check_card_new = """            IosCard {
                Column {
                    CheckRow("Whey protein taken", Icons.Outlined.FlashlightOn, AppColors.wheyGreen, state.wheyTaken, onWheyChanged)
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    CheckRow("Creatine taken", Icons.Outlined.Science, AppColors.creatineTeal, state.creatineTaken, onCreatineChanged)
                }
            }"""
text = text.replace(check_card_old, check_card_new)

check_row_old = """@Composable
private fun CheckRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChecked(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked, 
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}"""
check_row_new = """@Composable
private fun CheckRow(label: String, icon: ImageVector, iconTint: Color, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChecked(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconBadge(icon = icon, tint = iconTint)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked, 
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = iconTint,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}"""
text = text.replace(check_row_old, check_row_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
