import os
import re

def update_file(path):
    with open(path, 'r') as f:
        content = f.read()

    # Add imports if missing
    if 'import com.example.repsgrams.ui.components.IosButton' not in content:
        content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport com.example.repsgrams.ui.components.IosButton\nimport com.example.repsgrams.ui.components.IosCard\nimport com.example.repsgrams.ui.components.IosAlertDialog')
        
    # Replace Checkbox with Switch
    content = content.replace('Checkbox(checked =', 'Switch(checked =')
    
    # Replace Button(onClick... with IosButton
    # This is tricky with multiline, but we can do a simple replacement for AlertDialog to IosAlertDialog
    content = content.replace('AlertDialog(', 'IosAlertDialog(')
    content = content.replace('Card(', 'IosCard(')
    
    # We will leave the rest mostly as is to not break compilation easily.
    
    with open(path, 'w') as f:
        f.write(content)

update_file("/home/dj/AndroidStudioProjects/RepsGrams/app/src/main/java/com/example/repsgrams/ui/settings/ProgramEditorScreens.kt")
update_file("/home/dj/AndroidStudioProjects/RepsGrams/app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt")

