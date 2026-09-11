import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

# Imports
imports = """import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ArrowForwardIos
import com.example.repsgrams.ui.theme.AppColors
import com.example.repsgrams.ui.components.IconBadge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

# DATA & TEMPLATES rows
data_old = """                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToTemplates() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Edit Workout Programs", style = MaterialTheme.typography.bodyLarge)
                                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToExercises() }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Exercise Dictionary", style = MaterialTheme.typography.bodyLarge)
                                Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }"""
data_new = """                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToTemplates() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.EventNote, tint = MaterialTheme.colorScheme.primary)
                                Text("Edit Workout Programs", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Row(modifier = Modifier.fillMaxWidth().clickable { onNavigateToExercises() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.FitnessCenter, tint = MaterialTheme.colorScheme.primary)
                                Text("Exercise Dictionary", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Outlined.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }"""
text = text.replace(data_old, data_new)

# SettingToggle definition
toggle_def_old = """@Composable
private fun SettingToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }"""
toggle_def_new = """@Composable
private fun SettingToggle(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    enabled: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(icon = icon, tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }"""
text = text.replace(toggle_def_old, toggle_def_new)

# SettingToggle calls
text = text.replace(
"""                            SettingToggle(
                                "Master Reminders",
                                "Allow workout and supplement notifications",
                                settings.remindersEnabled,
                                true,
                                onMasterChanged,
                            )""",
"""                            SettingToggle(
                                "Master Reminders",
                                "Allow workout and supplement notifications",
                                Icons.Outlined.NotificationsActive,
                                MaterialTheme.colorScheme.primary,
                                settings.remindersEnabled,
                                true,
                                onMasterChanged,
                            )"""
)

# ReminderCard definition
rem_card_old = """@Composable
private fun ReminderCard(
    title: String,
    description: String,
    enabled: Boolean,
    masterEnabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    IosCard {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingToggle(title, description, enabled, masterEnabled, onEnabled)"""
rem_card_new = """@Composable
private fun ReminderCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean,
    masterEnabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    IosCard {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SettingToggle(title, description, icon, iconTint, enabled, masterEnabled, onEnabled)"""
text = text.replace(rem_card_old, rem_card_new)

# ReminderCard calls
text = text.replace(
"""                ReminderCard(
                    title = "Workout Day",
                    description = "Only if today's workout hasn't started",
                    enabled = settings.workoutReminderEnabled,""",
"""                ReminderCard(
                    title = "Workout Day",
                    description = "Only if today's workout hasn't started",
                    icon = Icons.Outlined.FitnessCenter,
                    iconTint = AppColors.workout,
                    enabled = settings.workoutReminderEnabled,""")

text = text.replace(
"""                ReminderCard(
                    title = "Daily Creatine",
                    description = "Only if 5 g hasn't been logged today",
                    enabled = settings.creatineReminderEnabled,""",
"""                ReminderCard(
                    title = "Daily Creatine",
                    description = "Only if 5 g hasn't been logged today",
                    icon = Icons.Outlined.Science,
                    iconTint = AppColors.creatineTeal,
                    enabled = settings.creatineReminderEnabled,""")

text = text.replace(
"""                ReminderCard(
                    title = "Post-Workout Whey",
                    description = "Scheduled when a workout is completed",
                    enabled = settings.postWorkoutWheyReminderEnabled,""",
"""                ReminderCard(
                    title = "Post-Workout Whey",
                    description = "Scheduled when a workout is completed",
                    icon = androidx.compose.material.icons.outlined.FlashlightOn,
                    iconTint = AppColors.wheyGreen,
                    enabled = settings.postWorkoutWheyReminderEnabled,""")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)
