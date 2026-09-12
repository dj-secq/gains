import re
with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

target = """                        Text(subtitleText, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                        Spacer(Modifier.height(4.dp))
                        Text(titleText, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))"""

replacement = """                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(subtitleText, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant))
                            if (suggestion.suggestedTemplate != null) {
                                Spacer(Modifier.width(8.dp))
                                com.example.repsgrams.ui.components.CategoryChip(category = suggestion.suggestedTemplate.category)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(titleText, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp))"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)
