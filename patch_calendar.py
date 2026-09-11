import re

with open("app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt", "r") as f:
    text = f.read()

# Imports
imports = """import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
import com.example.repsgrams.ui.theme.AppColors
import com.example.repsgrams.ui.components.IconBadge
import androidx.compose.ui.graphics.vector.ImageVector
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

# Top Prev/Next
header_old = """                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onPreviousMonth) { Text("‹ Previous", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                        Text(
                            month.month.month.getDisplayName(TextStyle.FULL, locale) + " ${month.month.year}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                        )
                        TextButton(onClick = onNextMonth) { Text("Next ›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
                    }"""
header_new = """                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onPreviousMonth) { Icon(Icons.Outlined.ArrowBackIosNew, contentDescription = "Previous", tint = MaterialTheme.colorScheme.primary) }
                        Text(
                            month.month.month.getDisplayName(TextStyle.FULL, locale) + " ${month.month.year}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(onClick = onNextMonth) { Icon(Icons.Outlined.ArrowForwardIos, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary) }
                    }"""
text = text.replace(header_old, header_new)

# Legend
legend_old = """                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        Text("✓ Complete", style = MaterialTheme.typography.bodySmall)
                        Text("! Missed", style = MaterialTheme.typography.bodySmall)
                        Text("○ Pending", style = MaterialTheme.typography.bodySmall)
                    }"""
legend_new = """                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBadge(icon = Icons.Outlined.CheckCircle, tint = AppColors.successGreen)
                            Text("Complete", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBadge(icon = Icons.Outlined.ErrorOutline, tint = AppColors.warningRed)
                            Text("Missed", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconBadge(icon = Icons.Outlined.Circle, tint = MaterialTheme.colorScheme.outlineVariant)
                            Text("Pending", style = MaterialTheme.typography.bodySmall)
                        }
                    }"""
text = text.replace(legend_old, legend_new)

# DayCell
daycell_old = """@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    modifier: Modifier,
    onSelectDate: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = when (day.status) {
        CalendarDayStatus.COMPLETE -> colors.primary.copy(alpha = 0.15f)
        CalendarDayStatus.MISSED -> colors.error.copy(alpha = 0.15f)
        CalendarDayStatus.PENDING -> colors.secondaryContainer
        CalendarDayStatus.UPCOMING -> colors.surface
    }
    Surface(
        modifier = modifier.aspectRatio(0.9f).padding(2.dp)
            .alpha(if (day.inDisplayedMonth) 1f else 0.3f)
            .clickable { onSelectDate(day.date) },
        shape = MaterialTheme.shapes.small,
        color = background,
        border = if (isToday) BorderStroke(1.5.dp, colors.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
            Text(
                day.slot.workoutDayLabel ?: "R",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                when (day.status) {
                    CalendarDayStatus.COMPLETE -> "✓"
                    CalendarDayStatus.MISSED -> "!"
                    CalendarDayStatus.PENDING -> "○"
                    CalendarDayStatus.UPCOMING -> "·"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (day.status) {
                    CalendarDayStatus.MISSED -> colors.error
                    CalendarDayStatus.COMPLETE -> colors.primary
                    else -> colors.onSurfaceVariant
                },
            )
        }
    }
}"""
daycell_new = """@Composable
private fun DayCell(
    day: CalendarDay,
    isToday: Boolean,
    modifier: Modifier,
    onSelectDate: (LocalDate) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = when (day.status) {
        CalendarDayStatus.COMPLETE -> AppColors.successGreen.copy(alpha = 0.15f)
        CalendarDayStatus.MISSED -> AppColors.warningRed.copy(alpha = 0.15f)
        CalendarDayStatus.PENDING -> colors.secondaryContainer
        CalendarDayStatus.UPCOMING -> colors.surface
    }
    Surface(
        modifier = modifier.aspectRatio(0.9f).padding(2.dp)
            .alpha(if (day.inDisplayedMonth) 1f else 0.3f)
            .clickable { onSelectDate(day.date) },
        shape = MaterialTheme.shapes.small,
        color = background,
        border = if (isToday) BorderStroke(1.5.dp, colors.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
            Text(
                day.slot.workoutDayLabel ?: "R",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (day.status == CalendarDayStatus.UPCOMING) {
                Text("·", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            } else {
                Icon(
                    imageVector = when (day.status) {
                        CalendarDayStatus.COMPLETE -> Icons.Outlined.CheckCircle
                        CalendarDayStatus.MISSED -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.Circle
                    },
                    contentDescription = null,
                    tint = when (day.status) {
                        CalendarDayStatus.COMPLETE -> AppColors.successGreen
                        CalendarDayStatus.MISSED -> AppColors.warningRed
                        else -> colors.onSurfaceVariant
                    },
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}"""
text = text.replace(daycell_old, daycell_new)

# Supplements in Detail
supplements_old = """        Text("Supplements", style = MaterialTheme.typography.titleMedium)
        IosCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Creatine: ${if (detail.supplements?.creatineTaken == true) "Logged · 5 g" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Whey: ${if (detail.supplements?.wheyTaken == true) "Logged · ${detail.supplements.wheyServings} serving" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
            }
        }"""
supplements_new = """        Text("Supplements", style = MaterialTheme.typography.titleMedium)
        IosCard {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBadge(icon = Icons.Outlined.Science, tint = AppColors.creatineTeal)
                    Text("Creatine: ${if (detail.supplements?.creatineTaken == true) "Logged · 5 g" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    IconBadge(icon = Icons.Outlined.FlashlightOn, tint = AppColors.wheyGreen)
                    Text("Whey: ${if (detail.supplements?.wheyTaken == true) "Logged · ${detail.supplements.wheyServings} serving" else "Not logged"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }"""
text = text.replace(supplements_old, supplements_new)


with open("app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt", "w") as f:
    f.write(text)
