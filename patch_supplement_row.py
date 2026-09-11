import re

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

sig_old = """private fun SupplementRow(
    label: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {"""
sig_new = """private fun SupplementRow(
    label: String,
    supportingText: String,
    iconOutlined: androidx.compose.ui.graphics.vector.ImageVector,
    iconFilled: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {"""
text = text.replace(sig_old, sig_new)

icon_old = "        com.example.repsgrams.ui.components.IconBadge(icon = icon, tint = iconTint)"
icon_new = "        com.example.repsgrams.ui.components.IconBadge(icon = if (checked) iconFilled else iconOutlined, tint = iconTint)"
text = text.replace(icon_old, icon_new)

call_old = """            SupplementRow("Creatine", "5 g · daily", androidx.compose.material.icons.Icons.Outlined.Science, com.example.repsgrams.ui.theme.AppColors.creatineTeal, state.creatineTaken, onCreatineChanged)
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SupplementRow(
                label = "Whey protein",
                supportingText = if (state.slot.isWorkoutDay) {
                    "1 serving · after workout"
                } else {
                    "1 serving · optional today"
                },
                icon = androidx.compose.material.icons.Icons.Outlined.FlashlightOn,"""
call_new = """            SupplementRow("Creatine", "5 g · daily", androidx.compose.material.icons.Icons.Outlined.Science, androidx.compose.material.icons.Icons.Filled.Science, com.example.repsgrams.ui.theme.AppColors.creatineTeal, state.creatineTaken, onCreatineChanged)
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SupplementRow(
                label = "Whey protein",
                supportingText = if (state.slot.isWorkoutDay) {
                    "1 serving · after workout"
                } else {
                    "1 serving · optional today"
                },
                iconOutlined = androidx.compose.material.icons.Icons.Outlined.WaterDrop,
                iconFilled = androidx.compose.material.icons.Icons.Filled.WaterDrop,"""
text = text.replace(call_old, call_new)

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)
