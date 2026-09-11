import re

with open("app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.filled.CheckCircle
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

day_old = """            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
            Text(
                day.slot.workoutDayLabel ?: "R",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (day.status == CalendarDayStatus.UPCOMING) {"""
day_new = """            Text(day.date.dayOfMonth.toString(), style = MaterialTheme.typography.bodySmall)
            if (day.slot.workoutDayLabel != null) {
                Text(
                    day.slot.workoutDayLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Coffee,
                    contentDescription = "Rest",
                    modifier = Modifier.size(20.dp),
                    tint = colors.onSurfaceVariant
                )
            }
            if (day.status == CalendarDayStatus.UPCOMING) {"""
text = text.replace(day_old, day_new)

day_icon_old = """                Icon(
                    imageVector = when (day.status) {
                        CalendarDayStatus.COMPLETE -> Icons.Outlined.CheckCircle
                        CalendarDayStatus.MISSED -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.Circle
                    },"""
day_icon_new = """                Icon(
                    imageVector = when (day.status) {
                        CalendarDayStatus.COMPLETE -> Icons.Filled.CheckCircle
                        CalendarDayStatus.MISSED -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.Circle
                    },"""
text = text.replace(day_icon_old, day_icon_new)

with open("app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt", "w") as f:
    f.write(text)
