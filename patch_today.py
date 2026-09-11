import re

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

# Add imports
imports = """
import com.example.repsgrams.ui.components.IconBadge
import com.example.repsgrams.ui.theme.AppColors
import com.dev778g.phosphoricon.PhIcons
import com.dev778g.phosphoricon.regular.*
import com.dev778g.phosphoricon.fill.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

# Find SupplementCard usage
text = text.replace('SupplementRow("Creatine", "5 g · daily", state.creatineTaken, onCreatineChanged)',
                    'SupplementRow("Creatine", "5 g · daily", PhIcons.Regular.Flask, AppColors.creatineTeal, state.creatineTaken, onCreatineChanged)')
text = text.replace('SupplementRow(\n                label = "Whey protein",',
                    'SupplementRow(\n                label = "Whey protein",\n                icon = PhIcons.Regular.Lightning,\n                iconTint = AppColors.wheyGreen,')

# Find SupplementRow definition
row_def = """@Composable
private fun SupplementRow(
    label: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconBadge(icon = icon, tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
text = re.sub(r'@Composable\s*private fun SupplementRow.*?\}\n\}', row_def, text, flags=re.DOTALL)

# Hero card gradient and Streak badge
text = text.replace("""item {
            IosCard {""", """item {
            IosCard {
                Box(modifier = Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(AppColors.workout.copy(alpha = 0.1f), Color.Transparent)
                    )
                )) {""")
text = text.replace("""TextButton(onClick = { onStartWorkout("B") }) { Text("Workout B", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }""", """TextButton(onClick = { onStartWorkout("B") }) { Text("Workout B", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
                }
            }
        }""")

streak_block = """item {
            IosCard {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(AppColors.streakAmber, AppColors.streakAmber.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            if (state.currentStreak > 0) PhIcons.Fill.Fire else PhIcons.Regular.Fire,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column {
                        Text("Current streak", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text("${state.currentStreak} day${if (state.currentStreak != 1) "s" else ""}", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        }"""
text = re.sub(r'item \{\s*IosCard \{\s*Column\(modifier = Modifier\.padding\(16\.dp\)\) \{\s*Text\("Current streak".*?\}\s*\}\s*\}', streak_block, text, flags=re.DOTALL)

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)
