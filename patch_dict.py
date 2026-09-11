import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
"""
if "import androidx.compose.material.icons.Icons" not in text:
    text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

dialog_old = """    onSave: (String, Boolean, String?) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var tracksWeight by remember { mutableStateOf(exercise?.tracksWeight ?: true) }
    var notes by remember { mutableStateOf(exercise?.notes ?: "") }"""
dialog_new = """    onSave: (String, Boolean, String?, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(exercise?.name ?: "") }
    var tracksWeight by remember { mutableStateOf(exercise?.tracksWeight ?: true) }
    var notes by remember { mutableStateOf(exercise?.notes ?: "") }
    var muscleGroup by remember { mutableStateOf(exercise?.muscleGroup ?: "Back") }
    var expanded by remember { mutableStateOf(false) }"""
text = text.replace(dialog_old, dialog_new)

content_old = """                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tracksWeight, onCheckedChange = { tracksWeight = it })
                    Text("Tracks Weight")
                }"""
content_new = """                Row(verticalAlignment = Alignment.CenterVertically) {
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
                }"""
text = text.replace(content_old, content_new)

btn_old = """        confirmButton = {
            Button(onClick = { onSave(name, tracksWeight, notes) }, enabled = name.isNotBlank()) {"""
btn_new = """        confirmButton = {
            Button(onClick = { onSave(name, tracksWeight, notes, muscleGroup) }, enabled = name.isNotBlank()) {"""
text = text.replace(btn_old, btn_new)

nav_old = """                navigationIcon = {
                    IconButton(onClick = onBack) { Text("<") }
                }"""
nav_new = """                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Back") }
                }"""
text = text.replace(nav_old, nav_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "w") as f:
    f.write(text)
