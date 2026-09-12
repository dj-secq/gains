import re
with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

# Fix conflicting declarations
text = re.sub(r'onNavigateToSupplements: \(\) -> Unit,\n\s*onNavigateToSupplements: \(\) -> Unit,', 'onNavigateToSupplements: () -> Unit,', text)
text = re.sub(r'onNavigateToSupplements = onNavigateToSupplements,\n\s*onNavigateToSupplements = onNavigateToSupplements,', 'onNavigateToSupplements = onNavigateToSupplements,', text)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ManageSupplementsScreen.kt", "r") as f:
    text2 = f.read()
text2 = text2.replace("Icons.AutoMirrored.Filled.ArrowBack", "androidx.compose.material.icons.filled.ArrowBack")
with open("app/src/main/java/com/example/repsgrams/ui/settings/ManageSupplementsScreen.kt", "w") as f:
    f.write(text2)
