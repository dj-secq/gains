import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import com.example.repsgrams.ui.theme.AppColors
import androidx.compose.material3.Icon
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

emoji_old = """                            Text("🔥 ${state.currentStreak}", style = MaterialTheme.typography.titleLarge)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Best Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("⭐ ${state.bestStreak}", style = MaterialTheme.typography.titleLarge)"""
emoji_new = """                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = AppColors.streakAmber, modifier = Modifier.size(20.dp))
                                Text("${state.currentStreak}", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Best Streak", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.Star, contentDescription = null, tint = AppColors.streakAmber, modifier = Modifier.size(20.dp))
                                Text("${state.bestStreak}", style = MaterialTheme.typography.titleLarge)
                            }"""
text = text.replace(emoji_old, emoji_new)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)
