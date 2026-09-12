with open("app/src/main/java/com/example/repsgrams/ui/settings/ManageSupplementsScreen.kt", "r") as f:
    text = f.read()
text = text.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\nimport androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.ArrowBack")
text = text.replace("Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, contentDescription = \"Back\")", "Icon(Icons.Filled.ArrowBack, contentDescription = \"Back\")")
with open("app/src/main/java/com/example/repsgrams/ui/settings/ManageSupplementsScreen.kt", "w") as f:
    f.write(text)
