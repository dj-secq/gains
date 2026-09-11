with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.FlashlightOn
"""

text = text.replace("import androidx.compose.material.icons.Icons", imports + "import androidx.compose.material.icons.Icons")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)
