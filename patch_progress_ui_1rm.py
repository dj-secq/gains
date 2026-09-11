import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

header_old = """                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Exercise History", style = MaterialTheme.typography.titleMedium)"""
header_new = """                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Exercise History", style = MaterialTheme.typography.titleMedium)
                        if (state.estimated1RM != null) {
                            Text("1RM: ${"%.1f".format(state.estimated1RM)} kg", style = MaterialTheme.typography.labelMedium, color = AppColors.workout)
                        }
                    }"""
text = text.replace(header_old, header_new)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)
