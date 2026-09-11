import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "r") as f:
    text = f.read()

# Add OptIn to ExerciseDialog
optin_old = """@Composable
private fun ExerciseDialog("""
optin_new = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseDialog("""
text = text.replace(optin_old, optin_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseLibraryScreen.kt", "r") as f:
    text = f.read()

lib_old = """Icon(androidx.compose.material.icons.Icons.AutoMirrored.Outlined.ArrowBackIos"""
lib_new = """Icon(androidx.compose.material.icons.Icons.Outlined.ArrowBackIosNew"""
text = text.replace(lib_old, lib_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseLibraryScreen.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.WaterDrop
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)

