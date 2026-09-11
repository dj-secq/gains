import re

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

# Imports
imports = """import com.example.repsgrams.ui.components.IconBadge
import com.example.repsgrams.ui.theme.AppColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

# SupplementCard
text = text.replace(
    'SupplementRow("Creatine", "5 g · daily", state.creatineTaken, onCreatineChanged)',
    'SupplementRow("Creatine", "5 g · daily", Icons.Outlined.Science, AppColors.creatineTeal, state.creatineTaken, onCreatineChanged)'
)
text = text.replace(
    'SupplementRow(\n                label = "Whey protein",',
    'SupplementRow(\n                label = "Whey protein",\n                icon = Icons.Outlined.FlashlightOn,\n                iconTint = AppColors.wheyGreen,'
)

# SupplementRow
old_row = """@Composable
private fun SupplementRow(
    label: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
new_row = """@Composable
private fun SupplementRow(
    label: String,
    supportingText: String,
    icon: ImageVector,
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
text = text.replace(old_row, new_row)

# Hero Gradient
hero_old = """        item {
            IosCard {
                Column(modifier = Modifier.padding(16.dp)) {"""
hero_new = """        item {
            IosCard {
                Box(modifier = Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(AppColors.workout.copy(alpha = 0.1f), Color.Transparent)
                    )
                )) {
                Column(modifier = Modifier.padding(16.dp)) {"""
text = text.replace(hero_old, hero_new)

hero_old_end = """                            TextButton(onClick = { onStartWorkout("A") }) { Text("Workout A", style = MaterialTheme.typography.bodyMedium) }
                            TextButton(onClick = { onStartWorkout("B") }) { Text("Workout B", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
        }"""
hero_new_end = """                            TextButton(onClick = { onStartWorkout("A") }) { Text("Workout A", style = MaterialTheme.typography.bodyMedium) }
                            TextButton(onClick = { onStartWorkout("B") }) { Text("Workout B", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
                }
            }
        }"""
text = text.replace(hero_old_end, hero_new_end)

# Streak badge
streak_old = """        item {
            IosCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Current streak", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("🔥 ${state.currentStreak} day${if (state.currentStreak != 1) "s" else ""}", style = MaterialTheme.typography.headlineLarge)
                }
            }
        }"""
streak_new = """        item {
            IosCard {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(AppColors.streakAmber, AppColors.streakAmber.copy(alpha = 0.7f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (state.currentStreak > 0) Icons.Rounded.LocalFireDepartment else Icons.Outlined.LocalFireDepartment,
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
text = text.replace(streak_old, streak_new)

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)
