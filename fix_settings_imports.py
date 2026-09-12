import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

if "import androidx.compose.material.icons.outlined.Info" not in text:
    text = text.replace("import androidx.compose.material.icons.outlined.Settings", "import androidx.compose.material.icons.outlined.Settings\nimport androidx.compose.material.icons.outlined.Info")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

