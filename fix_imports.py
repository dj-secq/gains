import re
import os

files = {
    "app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt": ["import androidx.compose.ui.draw.clip"],
    "app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt": ["import androidx.compose.ui.draw.clip"],
    "app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt": ["import androidx.compose.ui.draw.clip"],
    "app/src/main/java/com/example/repsgrams/ui/components/IosComponents.kt": ["import androidx.compose.ui.draw.clip", "import androidx.compose.animation.core.animateFloat"]
}

for filepath, imports in files.items():
    with open(filepath, "r") as f:
        text = f.read()
    
    for imp in imports:
        if imp not in text:
            text = text.replace("package ", f"{imp}\npackage ")
    
    with open(filepath, "w") as f:
        f.write(text)

