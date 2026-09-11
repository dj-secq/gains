import re

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

# Replace Phosphor imports and usages
text = text.replace("import com.dev778g.phosphoricon.PhIcons\n", "")
text = text.replace("import com.dev778g.phosphoricon.regular.*\n", "")
text = text.replace("import com.dev778g.phosphoricon.fill.*\n", "")
text = text.replace("import io.github.dev778gme.phosphoriconcompose.PhosphorIcons\n", "")
text = text.replace("import io.github.dev778gme.phosphoriconcompose.regular.Fire\n", "")
text = text.replace("import io.github.dev778gme.phosphoriconcompose.fill.Fire as FireFill\n", "")
text = text.replace("import io.github.dev778gme.phosphoriconcompose.regular.Flask\n", "")
text = text.replace("import io.github.dev778gme.phosphoriconcompose.regular.Lightning\n", "")
text = text.replace("import io.github.dev778gme.phosphoriconcompose.regular.CheckCircle\n", "")

text = text.replace("import androidx.compose.ui.Modifier", "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.outlined.Science\nimport androidx.compose.material.icons.outlined.FlashlightOn\nimport androidx.compose.material.icons.outlined.LocalFireDepartment\nimport androidx.compose.material.icons.rounded.LocalFireDepartment\nimport androidx.compose.material.icons.outlined.CheckCircle\nimport androidx.compose.ui.Modifier")

text = text.replace("PhIcons.Regular.Flask", "Icons.Outlined.Science")
text = text.replace("PhIcons.Regular.Lightning", "Icons.Outlined.FlashlightOn")
text = text.replace("PhIcons.Fill.Fire", "Icons.Rounded.LocalFireDepartment")
text = text.replace("PhIcons.Regular.Fire", "Icons.Outlined.LocalFireDepartment")

text = text.replace("PhosphorIcons.Regular.Flask", "Icons.Outlined.Science")
text = text.replace("PhosphorIcons.Regular.Lightning", "Icons.Outlined.FlashlightOn")
text = text.replace("PhosphorIcons.Fill.Fire", "Icons.Rounded.LocalFireDepartment")
text = text.replace("PhosphorIcons.Regular.Fire", "Icons.Outlined.LocalFireDepartment")

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)
