import re

with open("app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")


supplements_old = """                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBadge(icon = Icons.Outlined.Science, tint = AppColors.creatineTeal)
                    Text("Creatine: ${if (detail.supplements?.creatineTaken == true) "Logged · 5 g" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBadge(icon = Icons.Outlined.FlashlightOn, tint = AppColors.wheyGreen)
                    Text("Whey: ${if (detail.supplements?.wheyTaken == true) "Logged · ${detail.supplements.wheyServings} serving" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }"""
supplements_new = """                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val crTaken = detail.supplements?.creatineTaken == true
                    IconBadge(icon = if (crTaken) Icons.Filled.Science else Icons.Outlined.Science, tint = AppColors.creatineTeal)
                    Text("Creatine: ${if (crTaken) "Logged · 5 g" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val whTaken = detail.supplements?.wheyTaken == true
                    IconBadge(icon = if (whTaken) Icons.Filled.WaterDrop else Icons.Outlined.WaterDrop, tint = AppColors.wheyGreen)
                    Text("Whey: ${if (whTaken) "Logged · ${detail.supplements?.wheyServings} serving" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }"""
text = text.replace(supplements_old, supplements_new)

with open("app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt", "w") as f:
    f.write(text)
