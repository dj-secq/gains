import re

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
"""
if "import androidx.compose.material.icons.Icons" not in text:
    text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

dest_old = """private enum class TopLevelDestination(val route: String, val label: String, val symbol: String) {
    TODAY("today", "Today", "●"),
    CALENDAR("calendar", "Calendar", "□"),
    PROGRESS("progress", "Progress", "↗"),
    SETTINGS("settings", "Settings", "⚙"),
}"""
dest_new = """private enum class TopLevelDestination(val route: String, val label: String, val iconOutlined: ImageVector, val iconFilled: ImageVector) {
    TODAY("today", "Today", Icons.Outlined.Home, Icons.Filled.Home),
    CALENDAR("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    PROGRESS("progress", "Progress", Icons.Outlined.TrendingUp, Icons.Filled.TrendingUp),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}"""
text = text.replace(dest_old, dest_new)

nav_old = """                                Text(
                                    text = destination.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = contentColor
                                )"""
nav_new = """                                Icon(
                                    imageVector = if (selected) destination.iconFilled else destination.iconOutlined,
                                    contentDescription = destination.label,
                                    tint = contentColor
                                )"""
text = text.replace(nav_old, nav_new)

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "w") as f:
    f.write(text)
